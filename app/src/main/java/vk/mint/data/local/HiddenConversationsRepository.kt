package vk.mint.data.local

import kotlinx.coroutines.flow.first

/**
 * Скрытые диалоги (Hidden Conversations) — локальное хранилище списка peer_id.
 *
 * Наследует общий [JsonIdListRepository] для потокобезопасного персистенса JSON.
 */
class HiddenConversationsRepository(
    private val prefs: VkMintPrefs,
) : JsonIdListRepository("HiddenConvsRepo") {

    override suspend fun getRawData(): String = prefs.data.first().hiddenConvsData

    override suspend fun setRawData(json: String) {
        prefs.setHiddenConvsData(json)
    }

    /** Скрыть диалог: добавляет peer_id в список (dedup). */
    suspend fun hide(peerId: Long): List<Long> = addFirst(peerId)

    /** Вернуть диалог из скрытых: удаляет peer_id из списка. */
    suspend fun unhide(peerId: Long): List<Long> = remove(peerId)

    /** Проверить, скрыт ли конкретный диалог. */
    suspend fun isHidden(peerId: Long): Boolean = contains(peerId)
}
