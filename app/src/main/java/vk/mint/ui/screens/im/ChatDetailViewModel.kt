package vk.mint.ui.screens.im

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import vk.mint.api.VKApiClient
import vk.mint.auth.exchange.ExchangeAuthRepository
import vk.mint.data.model.Message
import vk.mint.data.model.MessageReaction
import vk.mint.data.model.ReactionItem
import vk.mint.data.model.UserProfile
import vk.mint.realtime.LongPollClient
import vk.mint.realtime.LongPollEvent
import vk.mint.util.AppLog

/**
 * P3: ChatDetailViewModel — централизованное управление состоянием чата / диалога.
 *
 * Переносит логику пагинации сообщений, отправки, оптимистичных апдейтов,
 * удаления, реакций, статуса набора текста и обработки LongPoll-событий
 * из многотысячестрочного Composable-экрана в устойчивый к пересозданиям ViewModel.
 */
class ChatDetailViewModel(
    private val apiClient: VKApiClient,
    private val exchangeAuthRepo: ExchangeAuthRepository? = null,
    private val longPollClient: LongPollClient? = null,
) : ViewModel() {

    data class ChatDetailUiState(
        val peerId: Long = 0L,
        val currentTitle: String = "",
        val currentPhoto: String? = null,
        val messages: List<Message> = emptyList(),
        val chatProfiles: Map<Long, UserProfile> = emptyMap(),
        val loading: Boolean = true,
        val loadingOlder: Boolean = false,
        val endReached: Boolean = false,
        val errorText: String? = null,
        val sending: Boolean = false,
        val muted: Boolean = false,
        val pinnedMessage: Message? = null,
        val lastActivity: VKApiClient.LastActivity? = null,
        val typingUsers: Map<Long, Long> = emptyMap(),
        val isChannel: Boolean = false,
        val channelWriteDenied: Boolean = false,
        val hasUnreadMark: Boolean = false,
    )

    private val _uiState = MutableStateFlow(ChatDetailUiState())
    val uiState: StateFlow<ChatDetailUiState> = _uiState.asStateFlow()

    @Volatile
    private var historyLoading = false

    @Volatile
    private var olderLoading = false

    /**
     * Первичная загрузка истории диалога, профилей авторов и информации о чате.
     */
    fun loadHistory(
        peerId: Long,
        initialTitle: String = "",
        initialPhoto: String? = null,
        pageSize: Int = 50,
    ) {
        if (historyLoading) return
        historyLoading = true

        _uiState.update {
            it.copy(
                peerId = peerId,
                currentTitle = if (initialTitle.isNotBlank()) initialTitle else it.currentTitle,
                currentPhoto = initialPhoto ?: it.currentPhoto,
                loading = true,
                endReached = false,
                errorText = null,
            )
        }

        viewModelScope.launch {
            try {
                exchangeAuthRepo?.keepAlive()
            } catch (_: Exception) {
                // keepAlive failure не блокирует загрузку истории
            }

            try {
                val result = apiClient.messagesGetHistoryWithProfiles(peerId, count = pageSize)
                val cleanMessages = result.messages.distinctBy { it.id }
                val isEnd = cleanMessages.size < pageSize

                _uiState.update {
                    it.copy(
                        messages = cleanMessages,
                        chatProfiles = result.profiles,
                        endReached = isEnd,
                        loading = false,
                        errorText = if (cleanMessages.isEmpty()) {
                            result.failure ?: apiClient.lastApiError ?: "Нет сообщений"
                        } else null,
                    )
                }

                // Загружаем online статус собеседника для 1-to-1 чатов
                if (peerId > 0 && peerId < 2_000_000_000L) {
                    try {
                        val act = apiClient.messagesGetLastActivity(peerId)
                        _uiState.update { it.copy(lastActivity = act) }
                    } catch (_: Exception) {}
                }

                // Загружаем закрепленное сообщение, push settings и права
                try {
                    val chats = apiClient.messagesGetConversationsById(listOf(peerId))
                    val chat = chats.firstOrNull()
                    val isGroupChat = peerId >= 2_000_000_000L
                    val isMuted = chat?.pushSettings?.isMuted() == true
                    val isChan = chat?.isChannel == true
                    val writeDenied = chat?.canWrite?.let { !it.allowed } ?: false
                    val titleUpdate = chat?.peer?.title?.takeIf { it.isNotBlank() && it != "Диалог" }
                    val photoUpdate = chat?.peer?.photo?.takeIf { it.isNotBlank() }

                    _uiState.update { current ->
                        current.copy(
                            pinnedMessage = if (isGroupChat) chat?.pinnedMessage else null,
                            muted = isMuted,
                            isChannel = current.isChannel || isChan,
                            channelWriteDenied = writeDenied,
                            currentTitle = titleUpdate ?: current.currentTitle,
                            currentPhoto = photoUpdate ?: current.currentPhoto,
                            hasUnreadMark = (chat?.unreadCount ?: 0) > 0,
                        )
                    }
                } catch (_: Exception) {}

            } catch (ce: CancellationException) {
                throw ce
            } catch (e: Exception) {
                AppLog.e("ChatDetailViewModel", "loadHistory failed for peer=$peerId", e)
                _uiState.update {
                    it.copy(
                        loading = false,
                        errorText = "Не удалось загрузить: ${e.message}",
                    )
                }
            } finally {
                historyLoading = false
            }
        }
    }

    /**
     * Фоновое обновление свежих сообщений.
     */
    fun reloadMessages(peerId: Long, pageSize: Int = 50) {
        viewModelScope.launch {
            try {
                val currentCount = _uiState.value.messages.size
                val targetCount = maxOf(currentCount, pageSize)
                val fresh = apiClient.messagesGetHistory(peerId, count = targetCount).distinctBy { it.id }
                if (fresh.isNotEmpty()) {
                    _uiState.update { it.copy(messages = fresh, errorText = null) }
                }
            } catch (ce: CancellationException) {
                throw ce
            } catch (e: Exception) {
                AppLog.w("ChatDetailViewModel", "reloadMessages error: ${e.message}")
            }
        }
    }

    /**
     * Подгрузка более старых сообщений (пагинация скролла вверх).
     */
    fun loadOlder(
        peerId: Long,
        pageSize: Int = 50,
        isChannelMode: Boolean = false,
        channelFilter: String? = null,
    ) {
        val s = _uiState.value
        if (olderLoading || s.endReached || s.messages.isEmpty()) return
        olderLoading = true

        _uiState.update { it.copy(loadingOlder = true) }

        viewModelScope.launch {
            try {
                val current = _uiState.value.messages
                val older: List<Message> = if (isChannelMode) {
                    val oldest = current.minOfOrNull { it.id } ?: 0L
                    apiClient.channelsGetHistory(
                        peerId,
                        count = pageSize,
                        startCmid = oldest,
                        filter = channelFilter,
                    ).messages
                } else {
                    apiClient.messagesGetHistory(peerId, count = pageSize, offset = current.size)
                }.filter { np -> current.none { it.id == np.id } }

                val isEnd = older.isEmpty() || older.size < pageSize
                val updated = if (older.isNotEmpty()) (current + older).distinctBy { it.id } else current

                _uiState.update {
                    it.copy(
                        messages = updated,
                        endReached = isEnd,
                        loadingOlder = false,
                    )
                }
            } catch (ce: CancellationException) {
                throw ce
            } catch (e: Exception) {
                AppLog.w("ChatDetailViewModel", "loadOlder failed: ${e.message}")
                _uiState.update { it.copy(loadingOlder = false) }
            } finally {
                olderLoading = false
            }
        }
    }

    /**
     * Прямая установка списка сообщений (например, при загрузке каналов или сбросе).
     */
    fun setMessages(messages: List<Message>) {
        _uiState.update { it.copy(messages = messages) }
    }

    /**
     * Функциональная трансформация списка сообщений (например, обновление ASR транскриптов).
     */
    fun updateMessages(transform: (List<Message>) -> List<Message>) {
        _uiState.update { it.copy(messages = transform(it.messages)) }
    }

    /**
     * Отправка текстового сообщения с оптимистичной вставкой и автоматическим
     * откатом при ошибке сети/VK API.
     */
    fun sendMessage(
        peerId: Long,
        text: String,
        replyTarget: Message? = null,
        onSuccess: (Long) -> Unit = {},
        onError: (String) -> Unit = {},
    ) {
        val cleanText = text.trim()
        if (cleanText.isBlank() || _uiState.value.sending) return

        val optimistic = Message(
            id = -System.currentTimeMillis(),
            peerId = peerId,
            fromId = 0,
            date = System.currentTimeMillis() / 1000,
            text = cleanText,
            out = 1,
            readState = 0,
            replyMessage = replyTarget,
        )

        _uiState.update {
            it.copy(
                messages = applyOptimisticMessage(it.messages, optimistic),
                sending = true,
            )
        }

        viewModelScope.launch {
            try {
                val replyCmid = replyTarget?.conversationMessageId
                val sentId = apiClient.messagesSend(peerId, cleanText, replyCmid = replyCmid)
                if (sentId > 0) {
                    _uiState.update { it.copy(sending = false) }
                    onSuccess(sentId)
                    reloadMessages(peerId)
                } else {
                    val errCode = apiClient.lastApiErrorCode
                    val isChannelDeny = peerId < 0 && (errCode == 901 || errCode == 902)
                    val errMsg = if (isChannelDeny) {
                        "Писать в этот канал нельзя (вы не админ)"
                    } else {
                        "Не удалось отправить сообщение"
                    }
                    _uiState.update {
                        it.copy(
                            messages = removeMessageById(it.messages, optimistic.id),
                            sending = false,
                            errorText = errMsg,
                        )
                    }
                    onError(errMsg)
                }
            } catch (ce: CancellationException) {
                throw ce
            } catch (e: Exception) {
                AppLog.e("ChatDetailViewModel", "sendMessage failed", e)
                val errMsg = "Ошибка отправки: ${e.message}"
                _uiState.update {
                    it.copy(
                        messages = removeMessageById(it.messages, optimistic.id),
                        sending = false,
                        errorText = errMsg,
                    )
                }
                onError(errMsg)
            }
        }
    }

    /**
     * Удаление сообщения (по cmid или fallback по id).
     */
    fun deleteMessage(peerId: Long, message: Message) {
        val cmid = message.conversationMessageId
        val msgId = message.id

        viewModelScope.launch {
            try {
                val ok = if (cmid != null && cmid > 0) {
                    apiClient.messagesDeleteByCmid(peerId, cmid, deleteForAll = true)
                } else {
                    apiClient.messagesDelete(msgId, deleteForAll = true)
                }
                if (ok) {
                    _uiState.update { it.copy(messages = removeMessageById(it.messages, msgId)) }
                }
            } catch (ce: CancellationException) {
                throw ce
            } catch (e: Exception) {
                AppLog.e("ChatDetailViewModel", "deleteMessage error", e)
            }
        }
    }

    /**
     * Редактирование сообщения.
     */
    fun editMessage(
        peerId: Long,
        messageId: Long,
        newText: String,
        attachmentStr: String? = null,
        onSuccess: () -> Unit = {},
    ) {
        viewModelScope.launch {
            try {
                val msg = _uiState.value.messages.firstOrNull { it.id == messageId }
                val cmid = msg?.conversationMessageId
                val editRef = if (cmid != null && cmid > 0) cmid else messageId
                val ok = apiClient.messagesEdit(
                    peerId = peerId,
                    cmid = editRef,
                    message = newText,
                    attachment = attachmentStr,
                )
                if (ok) {
                    reloadMessages(peerId)
                    onSuccess()
                }
            } catch (ce: CancellationException) {
                throw ce
            } catch (e: Exception) {
                AppLog.e("ChatDetailViewModel", "editMessage error", e)
            }
        }
    }

    /**
     * Поставить / снять реакцию с оптимистичным пересчетом счетчиков и чипов.
     */
    fun reactToMessage(peerId: Long, messageId: Long, reactionId: Int) {
        val msg = _uiState.value.messages.firstOrNull { it.id == messageId } ?: return
        val cmid = msg.conversationMessageId
        val ref = if (cmid != null && cmid > 0) cmid else messageId
        val myReaction = msg.reactions?.userReaction
        val isToggleOff = myReaction != null && myReaction != 0 && myReaction == reactionId

        _uiState.update {
            it.copy(messages = applyReactionToggle(it.messages, messageId, reactionId, isToggleOff))
        }

        viewModelScope.launch {
            try {
                val ok = if (isToggleOff) {
                    apiClient.messagesDeleteReaction(peerId, ref)
                } else {
                    apiClient.messagesSendReaction(peerId, ref, reactionId)
                }
                if (!ok) {
                    reloadMessages(peerId)
                }
            } catch (ce: CancellationException) {
                throw ce
            } catch (e: Exception) {
                AppLog.e("ChatDetailViewModel", "reactToMessage failed, rolling back", e)
                reloadMessages(peerId)
            }
        }
    }

    /**
     * Переключение уведомлений (mute/unmute).
     */
    fun toggleMute(peerId: Long, onSuccess: (Boolean) -> Unit = {}) {
        val newState = !_uiState.value.muted
        _uiState.update { it.copy(muted = newState) }

        viewModelScope.launch {
            try {
                val newSettings = apiClient.messagesSetConversationPushSettings(peerId, disabled = newState)
                if (newSettings != null) {
                    val serverMuted = newSettings.isMuted()
                    _uiState.update { it.copy(muted = serverMuted) }
                    onSuccess(serverMuted)
                } else {
                    _uiState.update { it.copy(muted = !newState) }
                }
            } catch (ce: CancellationException) {
                throw ce
            } catch (e: Exception) {
                _uiState.update { it.copy(muted = !newState) }
                AppLog.e("ChatDetailViewModel", "toggleMute failed", e)
            }
        }
    }

    /**
     * Обработка событий LongPoll (ReadOutbox, ReadInbox, NewMessage, EditMessage, Reset, Typing).
     */
    fun onLongPollEvent(peerId: Long, ev: LongPollEvent, isPinnedToNewest: Boolean) {
        when (ev) {
            is LongPollEvent.ReadOutbox -> {
                if (ev.peerId == peerId) {
                    _uiState.update {
                        it.copy(messages = applyReadState(it.messages, ev.upToMsgId, isOut = true))
                    }
                }
            }
            is LongPollEvent.ReadInbox -> {
                if (ev.peerId == peerId) {
                    _uiState.update {
                        it.copy(messages = applyReadState(it.messages, ev.upToMsgId, isOut = false))
                    }
                }
            }
            is LongPollEvent.Typing -> {
                if (ev.peerId == peerId) {
                    val updated = _uiState.value.typingUsers.toMutableMap()
                    updated[ev.userId] = System.currentTimeMillis()
                    _uiState.update { it.copy(typingUsers = updated) }
                }
            }
            is LongPollEvent.NewMessage -> {
                if (ev.peerId == peerId && isPinnedToNewest) {
                    reloadMessages(peerId)
                }
            }
            is LongPollEvent.EditMessage -> {
                if (ev.peerId == peerId) {
                    reloadMessages(peerId)
                }
            }
            is LongPollEvent.Reset -> {
                reloadMessages(peerId)
            }
            else -> {}
        }
    }

    /**
     * Запуск наблюдения за LongPoll-событиями.
     */
    fun startLongPollSubscription(peerId: Long, isPinnedProvider: () -> Boolean) {
        val lp = longPollClient ?: return
        viewModelScope.launch {
            lp.events.collect { ev ->
                onLongPollEvent(peerId, ev, isPinnedProvider())
            }
        }
    }

    companion object {
        /** Проверка, прочитано ли сообщение до указанного cmid (или id). */
        fun isReadUpTo(msg: Message, upToCmid: Long): Boolean {
            val cmid = msg.conversationMessageId
            return if (cmid != null && cmid > 0) {
                cmid <= upToCmid
            } else {
                msg.id > 0 && msg.id <= upToCmid
            }
        }

        /** Обновление readState для входящих или исходящих сообщений. */
        fun applyReadState(messages: List<Message>, upToCmid: Long, isOut: Boolean): List<Message> {
            return messages.map { msg ->
                val matchesDirection = if (isOut) msg.isOut else !msg.isOut
                if (matchesDirection && isReadUpTo(msg, upToCmid)) {
                    msg.copy(readState = 1)
                } else {
                    msg
                }
            }
        }

        /** Оптимистичное добавление сообщения в начало списка. */
        fun applyOptimisticMessage(messages: List<Message>, message: Message): List<Message> {
            return listOf(message) + messages
        }

        /** Удаление сообщения по id. */
        fun removeMessageById(messages: List<Message>, id: Long): List<Message> {
            return messages.filterNot { it.id == id }
        }

        /** Чистая функция пересчета реакций для оптимистичного UI. */
        fun applyReactionToggle(
            messages: List<Message>,
            messageId: Long,
            reactionId: Int,
            isToggleOff: Boolean,
        ): List<Message> {
            return messages.map { m ->
                if (m.id != messageId) m else {
                    val old = m.reactions
                    val oldCount = old?.count ?: 0
                    val oldUser = old?.userReaction
                    val hadUser = oldUser != null && oldUser != 0
                    val currentItems = old?.items ?: emptyList()

                    val newItems: List<ReactionItem> = if (isToggleOff) {
                        currentItems.mapNotNull { item ->
                            if (item.id == reactionId) {
                                if (item.count > 1) item.copy(count = item.count - 1) else null
                            } else item
                        }
                    } else {
                        val afterRemove = if (oldUser != null && oldUser != reactionId) {
                            currentItems.mapNotNull { item ->
                                if (item.id == oldUser) {
                                    if (item.count > 1) item.copy(count = item.count - 1) else null
                                } else item
                            }
                        } else currentItems
                        val found = afterRemove.any { it.id == reactionId }
                        if (found) {
                            afterRemove.map { if (it.id == reactionId) it.copy(count = it.count + 1) else it }
                        } else {
                            afterRemove + ReactionItem(id = reactionId, count = 1)
                        }
                    }

                    val newReaction = MessageReaction(
                        count = if (isToggleOff) {
                            if (oldCount > 0) oldCount - 1 else 0
                        } else {
                            if (hadUser) oldCount else oldCount + 1
                        },
                        userReaction = if (isToggleOff) null else reactionId,
                        items = newItems,
                    )
                    m.copy(reactions = newReaction)
                }
            }
        }

        fun provideFactory(
            apiClient: VKApiClient,
            exchangeAuthRepo: ExchangeAuthRepository? = null,
            longPollClient: LongPollClient? = null,
        ): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                ChatDetailViewModel(apiClient, exchangeAuthRepo, longPollClient)
            }
        }
    }
}
