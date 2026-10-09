package vk.mint.data.local

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import vk.mint.util.AppLog

/**
 * Скрытые диалоги (Hidden Conversations) — локальное хранилище списка peer_id.
 *
 * Позволяет исключить выбранные чаты из основного списка мессенджера и поиска.
 * Доступ к списку открывается только после ввода мастер-PIN (или PIN скрытых чатов).
 *
 * Потокобезопасность: load-modify-save защищены Mutex, DataStore атомарен.
 * Структура JSON: `[2000000062, 152094335, -123456]` (массив Long).
 */
class HiddenConversationsRepository(private val prefs: VkMintPrefs) {

    private val gson = Gson()
    private val type = object : TypeToken<List<Long>>() {}.type
    private val mutex = Mutex()

    /**
     * Загрузить список скрытых peer_id.
     * Пустая строка → пустой список. При ошибке парсинга — пустой список.
     */
    suspend fun load(): List<Long> {
        val raw = prefs.data.first().hiddenConvsData
        if (raw.isBlank()) return emptyList()
        return try {
            gson.fromJson<List<Long>>(raw, type) ?: emptyList()
        } catch (e: Exception) {
            AppLog.w("HiddenConvsRepo", "Failed to parse hiddenConvsData: ${e.message}")
            emptyList()
        }
    }

    /** Сохранить полный список скрытых peer_id (атомарно). */
    suspend fun save(peerIds: List<Long>) {
        val json = gson.toJson(peerIds)
        prefs.setHiddenConvsData(json)
        AppLog.d("HiddenConvsRepo", "Saved ${peerIds.size} hidden: $peerIds")
    }

    /** Скрыть диалог: добавляет peer_id в список (dedup). */
    suspend fun hide(peerId: Long): List<Long> = mutex.withLock {
        val current = load().toMutableList()
        current.remove(peerId)
        current.add(0, peerId)
        save(current)
        current
    }

    /** Вернуть диалог из скрытых: удаляет peer_id из списка. */
    suspend fun unhide(peerId: Long): List<Long> = mutex.withLock {
        val current = load().toMutableList()
        current.remove(peerId)
        save(current)
        current
    }

    /** Очистить список скрытых диалогов (раскрыть все). */
    suspend fun clear(): List<Long> = mutex.withLock {
        save(emptyList())
        emptyList()
    }

    /** Проверить, скрыт ли конкретный диалог. */
    suspend fun isHidden(peerId: Long): Boolean = load().contains(peerId)
}
