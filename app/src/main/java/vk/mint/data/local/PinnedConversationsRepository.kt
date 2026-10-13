package vk.mint.data.local

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.withLock

/**
 * Fix #276: PinnedConversationsRepository — локальное хранилище закреплённых
 * диалогов (peer_id список в порядке закрепления; 0-й элемент — верхний).
 *
 * Наследует общий [JsonIdListRepository] для потокобезопасного персистенса JSON.
 */
class PinnedConversationsRepository(
    private val prefs: VkMintPrefs,
) : JsonIdListRepository("PinnedConvsRepo") {

    override suspend fun getRawData(): String = prefs.data.first().pinnedConvsData

    override suspend fun setRawData(json: String) {
        prefs.setPinnedConvsData(json)
    }

    /**
     * Закрепить диалог. Добавляет peer_id в НАЧАЛО списка (чтобы чат встал
     * наверх закреплённого блока — как в нативном VK).
     * Если уже закреплён — сначала убирает из старой позиции, потом в начало.
     */
    suspend fun pin(peerId: Long): List<Long> = addFirst(peerId)

    /** Открепить диалог. Убирает peer_id из списка. No-op если не был закреплён. */
    suspend fun unpin(peerId: Long): List<Long> = remove(peerId)

    /**
     * Переставить закреплённый диалог с [fromIndex] на [toIndex] (drag&drop).
     * Индексы — в рамках списка закреплённых. Возвращает новый порядок.
     * No-op если индексы невалидны.
     */
    suspend fun reorder(fromIndex: Int, toIndex: Int): List<Long> = mutex.withLock {
        val current = load().toMutableList()
        if (fromIndex !in current.indices) return@withLock current
        if (toIndex !in current.indices) return@withLock current
        if (fromIndex == toIndex) return@withLock current
        val item = current.removeAt(fromIndex)
        current.add(toIndex, item)
        save(current)
        current
    }

    /**
     * Сеттер списка целиком (для drag&drop когда UI уже посчитал новый порядок).
     * Просто персистит переданный список.
     */
    suspend fun setOrder(peerIds: List<Long>) {
        save(peerIds)
    }

    /** Проверить, закреплён ли диалог (без загрузки всего списка в UI). */
    suspend fun isPinned(peerId: Long): Boolean = contains(peerId)
}
