package vk.mint.data.local

import kotlinx.coroutines.flow.first

/**
 * Fix #356 #MSG-ARCHIVE: ArchivedConversationsRepository — локальное хранилище
 * архивных диалогов (peer_id список).
 *
 * Наследует общий [JsonIdListRepository] для потокобезопасного персистенса JSON.
 */
class ArchivedConversationsRepository(
    private val prefs: VkMintPrefs,
) : JsonIdListRepository("ArchivedConvsRepo") {

    override suspend fun getRawData(): String = prefs.data.first().archivedConvsData

    override suspend fun setRawData(json: String) {
        prefs.setArchivedConvsData(json)
    }

    /** Архивировать диалог: добавляет peer_id в начало списка (no-op если уже там — dedup). */
    suspend fun archive(peerId: Long): List<Long> = addFirst(peerId)

    /** Разархивировать диалог. Убирает peer_id из списка. No-op если не был там. */
    suspend fun unarchive(peerId: Long): List<Long> = remove(peerId)

    /** Проверить, в архиве ли диалог (без загрузки всего списка в UI). */
    suspend fun isArchived(peerId: Long): Boolean = contains(peerId)
}
