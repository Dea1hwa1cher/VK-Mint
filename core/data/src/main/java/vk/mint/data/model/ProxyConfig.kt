package vk.mint.data.model

import com.google.gson.annotations.SerializedName
import java.net.URI
import java.util.UUID

enum class ProxyType {
    @SerializedName("http")
    HTTP,
    @SerializedName("socks")
    SOCKS;

    val displayName: String
        get() = when (this) {
            HTTP -> "HTTP / HTTPS"
            SOCKS -> "SOCKS5"
        }
}

/**
 * Модель конфигурации прокси-сервера.
 */
data class ProxyConfig(
    @SerializedName("id")
    val id: String = UUID.randomUUID().toString(),
    @SerializedName("title")
    val title: String = "",
    @SerializedName("type")
    val type: ProxyType = ProxyType.HTTP,
    @SerializedName("host")
    val host: String = "",
    @SerializedName("port")
    val port: Int = 8080,
    @SerializedName("auth_enabled")
    val authEnabled: Boolean = false,
    @SerializedName("username")
    val username: String = "",
    @SerializedName("password")
    val password: String = "",
) {
    val displayTitle: String
        get() = title.ifBlank { "$host:$port" }

    val formattedAddress: String
        get() = "$host:$port"

    companion object {
        /**
         * Парсинг прокси из строки (поддерживает URI формат http/socks5://user:pass@host:port
         * и формат host:port:user:pass или host:port).
         */
        fun parseFromString(raw: String): ProxyConfig? {
            val text = raw.trim()
            if (text.isBlank()) return null

            try {
                if (text.startsWith("http://", ignoreCase = true) ||
                    text.startsWith("https://", ignoreCase = true) ||
                    text.startsWith("socks://", ignoreCase = true) ||
                    text.startsWith("socks5://", ignoreCase = true)
                ) {
                    val uri = URI(text)
                    val scheme = uri.scheme?.lowercase() ?: "http"
                    val type = if (scheme.startsWith("socks")) ProxyType.SOCKS else ProxyType.HTTP
                    val host = uri.host ?: return null
                    val port = if (uri.port > 0) uri.port else if (type == ProxyType.SOCKS) 1080 else 8080
                    val userInfo = uri.userInfo
                    val (user, pass) = if (!userInfo.isNullOrBlank()) {
                        val parts = userInfo.split(":", limit = 2)
                        Pair(parts.getOrElse(0) { "" }, parts.getOrElse(1) { "" })
                    } else {
                        Pair("", "")
                    }
                    return ProxyConfig(
                        title = "$host:$port",
                        type = type,
                        host = host,
                        port = port,
                        authEnabled = user.isNotBlank(),
                        username = user,
                        password = pass,
                    )
                }

                // Формат host:port:user:pass или host:port
                val parts = text.split(":")
                if (parts.size >= 2) {
                    val host = parts[0].trim()
                    val port = parts[1].trim().toIntOrNull() ?: return null
                    val hasAuth = parts.size >= 4
                    val user = if (hasAuth) parts[2].trim() else ""
                    val pass = if (hasAuth) parts[3].trim() else ""
                    return ProxyConfig(
                        title = "$host:$port",
                        type = ProxyType.HTTP,
                        host = host,
                        port = port,
                        authEnabled = hasAuth && user.isNotBlank(),
                        username = user,
                        password = pass,
                    )
                }
            } catch (_: Exception) {
                return null
            }
            return null
        }
    }
}
