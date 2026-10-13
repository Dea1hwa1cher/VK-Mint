package vk.mint.data.local

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import vk.mint.util.AppLog

/**
 * Базовый абстрактный репозиторий для локального персистенса списков ID (peer_id Long)
 * в формате JSON в DataStore preferences.
 *
 * Инкапсулирует:
 * - Gson-сериализацию/десериализацию списка `List<Long>`.
 * - Mutex-блокировку для потокобезопасности load-modify-save секций (защита от TOCTOU).
 * - Общие операции: load, save, addFirst (с дедупликацией), remove, contains, clear.
 */
abstract class JsonIdListRepository(
    protected val logTag: String,
) {
    protected val gson: Gson = Gson()
    protected val type = object : TypeToken<List<Long>>() {}.type
    protected val mutex: Mutex = Mutex()

    /** Считать сырую JSON-строку из хранилища (DataStore). */
    protected abstract suspend fun getRawData(): String

    /** Записать сырую JSON-строку в хранилище (DataStore). */
    protected abstract suspend fun setRawData(json: String)

    /**
     * Загрузить список Long ID.
     * Пустая строка -> пустой список. При ошибке парсинга -> пустой список.
     */
    suspend fun load(): List<Long> {
        val raw = getRawData()
        if (raw.isBlank()) return emptyList()
        return try {
            gson.fromJson<List<Long>>(raw, type) ?: emptyList()
        } catch (e: Exception) {
            AppLog.w(logTag, "Failed to parse $logTag data: ${e.message}")
            emptyList()
        }
    }

    /** Сохранить полный список ID (атомарно). */
    suspend fun save(ids: List<Long>) {
        val json = gson.toJson(ids)
        setRawData(json)
        AppLog.d(logTag, "Saved ${ids.size} items: $ids")
    }

    /** Добавить ID в начало списка (no-op дедупликация, если уже есть). */
    suspend fun addFirst(id: Long): List<Long> = mutex.withLock {
        val current = load().toMutableList()
        current.remove(id)
        current.add(0, id)
        save(current)
        current
    }

    /** Удалить ID из списка. No-op если отсутствует. */
    suspend fun remove(id: Long): List<Long> = mutex.withLock {
        val current = load().toMutableList()
        current.remove(id)
        save(current)
        current
    }

    /** Очистить список полностью. */
    open suspend fun clear(): List<Long> = mutex.withLock {
        save(emptyList())
        emptyList()
    }

    /** Проверить наличие ID в списке. */
    suspend fun contains(id: Long): Boolean = load().contains(id)
}
