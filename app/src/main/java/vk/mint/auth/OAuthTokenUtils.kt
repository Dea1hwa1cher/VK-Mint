package vk.mint.auth

/**
 * Утилиты для валидации и извлечения OAuth-токенов из URL/фрагментов/буфера.
 *
 * Переиспользуется между [AuthActivity] и [vk.mint.ui.MainActivity].
 */
object OAuthTokenUtils {

    /**
     * Fix #191: проверяет, содержит ли строка OAuth-токен.
     * Ищет "access_token=" И "user_id=" в любом месте строки.
     */
    fun containsOAuthToken(text: String): Boolean {
        return text.contains("access_token=", ignoreCase = true) &&
            text.contains("user_id=", ignoreCase = true)
    }

    /**
     * Fix #191: извлекает OAuth payload (параметры после # или ?) из строки.
     *
     * Принимает полный URL, фрагмент, или просто параметры.
     * Возвращает строку вида "access_token=...&user_id=...&expires_in=...".
     */
    fun extractOAuthPayload(text: String): String? {
        val trimmed = text.trim()
        val hashIdx = trimmed.lastIndexOf('#')
        if (hashIdx >= 0 && hashIdx < trimmed.length - 1) {
            return trimmed.substring(hashIdx + 1)
        }
        val qIdx = trimmed.indexOf('?')
        if (qIdx >= 0 && qIdx < trimmed.length - 1) {
            return trimmed.substring(qIdx + 1)
        }
        if (trimmed.contains("access_token=")) {
            return trimmed
        }
        return null
    }

    /**
     * Парсит параметры payload в пару (userId, accessToken) или null.
     */
    fun parsePayloadToToken(payload: String): Pair<Long, String>? {
        return try {
            val params = payload.split("&").mapNotNull { pair ->
                val parts = pair.split("=", limit = 2)
                if (parts.size == 2) parts[0] to parts[1] else null
            }.toMap()
            val at = params["access_token"] ?: return null
            val uid = params["user_id"]?.toLongOrNull() ?: return null
            if (at.isBlank() || at.length < 10) return null
            uid to at
        } catch (_: Exception) {
            null
        }
    }
}
