package vk.mint.data.local

import java.util.concurrent.ConcurrentHashMap

/**
 * Резолвер локальных псевдонимов пользователей.
 *
 * Хранит текущую карту псевдонимов (userId -> alias), синхронизируемую с DataStore.
 * Позволяет любому компоненту приложения мгновенно получить локальное имя пользователя
 * без блокировок и асинхронных задержек в UI.
 */
object UserAliasResolver {

    private val cache = ConcurrentHashMap<Long, String>()

    /** Обновить карту псевдонимов из снапшота настроек. */
    fun update(aliases: Map<Long, String>) {
        cache.clear()
        cache.putAll(aliases)
    }

    /** Получить псевдоним пользователя или null, если он не задан. */
    fun getAlias(userId: Long): String? = cache[userId]

    /**
     * Разрешить имя пользователя: возвращает псевдоним, если задан,
     * иначе возвращает [defaultName].
     */
    fun resolveName(userId: Long, defaultName: String): String {
        val a = cache[userId]
        return if (!a.isNullOrBlank()) a else defaultName
    }

    /** Проверить, установлен ли псевдоним для данного userId. */
    fun hasAlias(userId: Long): Boolean = cache.containsKey(userId)
}
