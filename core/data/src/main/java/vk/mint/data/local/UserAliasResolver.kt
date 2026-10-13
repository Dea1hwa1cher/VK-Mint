package vk.mint.data.local


/**
 * Резолвер локальных псевдонимов пользователей.
 *
 * Хранит текущую карту псевдонимов (userId -> alias), синхронизируемую с DataStore.
 * Позволяет любому компоненту приложения мгновенно получить локальное имя пользователя
 * без блокировок и асинхронных задержек в UI.
 */
object UserAliasResolver {

    @Volatile
    private var cache: Map<Long, String> = emptyMap()

    /** Обновить карту псевдонимов из снапшота настроек. */
    fun update(aliases: Map<Long, String>) {
        cache = aliases
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
