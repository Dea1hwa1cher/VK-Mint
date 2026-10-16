package vk.mint.auth

import java.net.URLDecoder

/**
 * Утилиты для валидации и извлечения OAuth-токенов из URL/фрагментов/буфера.
 *
 * Переиспользуется между [AuthActivity], [vk.mint.ui.MainActivity] и [LandingScreen].
 */
object OAuthTokenUtils {

    /**
     * Результат парсинга токена.
     */
    data class OAuthToken(
        val accessToken: String,
        val userId: Long,
        val expiresIn: Long = 0L,
    )

    /**
     * Fix #191: проверяет, содержит ли строка OAuth-токен.
     * Ищет "access_token=" И "user_id=" в любом месте строки (включая URL-encoded варианты %3D).
     */
    fun containsOAuthToken(text: String): Boolean {
        val lower = text.lowercase()
        val hasToken = lower.contains("access_token=") || lower.contains("access_token%3d")
        val hasUser = lower.contains("user_id=") || lower.contains("user_id%3d")
        return hasToken && hasUser
    }

    /**
     * Fix #191: извлекает OAuth payload (параметры после # или ?) из строки.
     *
     * Принимает полный URL, фрагмент, или просто параметры.
     * Учитывает URL-encoded фрагмент %23 и очищает возможный прилипший текст.
     */
    fun extractOAuthPayload(text: String): String? {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return null

        // 1. Ищем "#" (обычный фрагмент)
        val hashIdx = trimmed.lastIndexOf('#')
        if (hashIdx >= 0 && hashIdx < trimmed.length - 1) {
            val candidate = trimmed.substring(hashIdx + 1)
            return sanitizePayload(candidate)
        }

        // 2. Ищем "%23" (URL-encoded фрагмент)
        val encHashIdx = trimmed.lastIndexOf("%23", ignoreCase = true)
        if (encHashIdx >= 0 && encHashIdx < trimmed.length - 3) {
            val candidate = trimmed.substring(encHashIdx + 3)
            return sanitizePayload(candidate)
        }

        // 3. Ищем "?" (query string)
        val qIdx = trimmed.indexOf('?')
        if (qIdx >= 0 && qIdx < trimmed.length - 1) {
            var candidate = trimmed.substring(qIdx + 1).substringBefore('#')
            val encHash = candidate.indexOf("%23", ignoreCase = true)
            if (encHash >= 0) {
                candidate = candidate.substring(0, encHash)
            }
            return sanitizePayload(candidate)
        }

        // 4. Строка может сразу начинаться с параметров или содержать access_token=
        val atIdx = trimmed.indexOf("access_token=", ignoreCase = true)
        if (atIdx >= 0) {
            return sanitizePayload(trimmed.substring(atIdx))
        }

        val encAtIdx = trimmed.indexOf("access_token%3d", ignoreCase = true)
        if (encAtIdx >= 0) {
            return sanitizePayload(trimmed.substring(encAtIdx))
        }

        return null
    }

    /**
     * Отрезает лишний текст/пробелы/переводы строк, которые браузеры могут добавить
     * при шаринге ссылки (например "OAuth Blank\nhttps://...").
     */
    private fun sanitizePayload(raw: String): String {
        val firstWhitespace = raw.indexOfFirst { it.isWhitespace() }
        val tokenPart = if (firstWhitespace >= 0) raw.substring(0, firstWhitespace) else raw
        return tokenPart.trim()
    }

    /**
     * Парсит произвольную строку (URL, фрагмент, параметры или текст со ссылкой) в [OAuthToken].
     * Возвращает null, если токен или userId не найдены или невалидны.
     */
    fun parse(input: String): OAuthToken? {
        val payload = extractOAuthPayload(input) ?: input.trim()
        if (payload.isEmpty()) return null

        val params = mutableMapOf<String, String>()

        // Разделяем по & или %26
        val pairs = payload.split(Regex("(?i)&|%26"))
        for (pair in pairs) {
            val eqParts = pair.split(Regex("(?i)=|%3d"), limit = 2)
            if (eqParts.size == 2) {
                val key = safeUrlDecode(eqParts[0]).trim().lowercase()
                val value = safeUrlDecode(eqParts[1]).trim()
                if (key.isNotEmpty() && value.isNotEmpty()) {
                    params[key] = value
                }
            }
        }

        val at = params["access_token"] ?: return null
        if (at.isBlank() || at.length < 10) return null

        val rawUid = params["user_id"] ?: return null
        // Извлекаем только цифры из user_id на случай прилипших символов/пробелов
        val digitsOnly = rawUid.filter { it.isDigit() }
        val uid = digitsOnly.toLongOrNull() ?: return null
        if (uid <= 0L) return null

        val expIn = params["expires_in"]?.filter { it.isDigit() }?.toLongOrNull() ?: 0L

        return OAuthToken(
            accessToken = at,
            userId = uid,
            expiresIn = expIn,
        )
    }

    /**
     * Парсит параметры payload в пару (userId, accessToken) или null.
     * Сохранено для обратной совместимости с существующим кодом.
     */
    fun parsePayloadToToken(payload: String): Pair<Long, String>? {
        val token = parse(payload) ?: return null
        return token.userId to token.accessToken
    }

    private fun safeUrlDecode(s: String): String {
        return try {
            URLDecoder.decode(s, "UTF-8")
        } catch (_: Exception) {
            s.replace("+", " ")
        }
    }

    /**
     * Маскирует токен или чувствительную строку для безопасного логирования.
     * Показывает первые 8 и последние 4 символа для длинных значений (>12 символов)
     * и "***" для коротких.
     */
    fun maskTokenPrefix(v: String): String =
        if (v.length > 12) "${v.take(8)}…${v.takeLast(4)}" else "***"
}
