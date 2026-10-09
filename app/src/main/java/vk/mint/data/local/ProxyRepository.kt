package vk.mint.data.local

import android.util.Patterns
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.Authenticator
import okhttp3.Credentials
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import vk.mint.VkMintApp
import vk.mint.data.model.ProxyConfig
import vk.mint.data.model.ProxyType
import vk.mint.util.AppLog
import java.io.IOException
import java.net.InetSocketAddress
import java.net.Proxy
import java.net.ProxySelector
import java.net.SocketAddress
import java.net.URI
import java.util.concurrent.TimeUnit

sealed class ProxyPingResult {
    data class Success(val latencyMs: Long) : ProxyPingResult()
    data class Error(val message: String) : ProxyPingResult()
}

/**
 * Менеджер и репозиторий прокси-серверов.
 *
 * Управляет списком серверов (HTTP / SOCKS5) с поддержкой авторизации,
 * проверкой задержки (ping) и динамической маршрутизацией через OkHttp
 * без перезапуска приложения (OkHttpClient ProxySelector + Authenticator).
 */
class ProxyRepository(private val prefs: VkMintPrefs) {

    private val gson = Gson()
    private val type = object : TypeToken<List<ProxyConfig>>() {}.type
    private val mutex = Mutex()

    private val _proxiesFlow = MutableStateFlow<List<ProxyConfig>>(emptyList())
    val proxiesFlow: StateFlow<List<ProxyConfig>> = _proxiesFlow.asStateFlow()

    private val _activeProxyFlow = MutableStateFlow<ProxyConfig?>(null)
    val activeProxyFlow: StateFlow<ProxyConfig?> = _activeProxyFlow.asStateFlow()

    @Volatile
    var currentActiveProxy: ProxyConfig? = null
        private set

    val proxySelector: ProxySelector = object : ProxySelector() {
        override fun select(uri: URI?): List<Proxy> {
            val active = currentActiveProxy ?: return listOf(Proxy.NO_PROXY)
            return try {
                val addr = if (isIpAddress(active.host)) {
                    InetSocketAddress(active.host, active.port)
                } else {
                    InetSocketAddress.createUnresolved(active.host, active.port)
                }
                val type = when (active.type) {
                    ProxyType.SOCKS -> Proxy.Type.SOCKS
                    ProxyType.HTTP -> Proxy.Type.HTTP
                }
                listOf(Proxy(type, addr))
            } catch (e: Exception) {
                AppLog.w("ProxyRepo", "Failed to select proxy: ${e.message}")
                listOf(Proxy.NO_PROXY)
            }
        }

        override fun connectFailed(uri: URI?, sa: SocketAddress?, ioe: IOException?) {
            AppLog.w("ProxyRepo", "Proxy connection failed to $uri via $sa: ${ioe?.message}")
        }
    }

    val proxyAuthenticator: Authenticator = object : Authenticator {
        override fun authenticate(route: Route?, response: Response): Request? {
            val active = currentActiveProxy ?: return null
            if (!active.authEnabled || active.username.isBlank()) return null
            if (response.request.header("Proxy-Authorization") != null) {
                // Если заголовок уже был отправлен, но сервер вернул 407 — креды неверны, не зацикливаемся
                return null
            }
            val credential = Credentials.basic(active.username, active.password)
            return response.request.newBuilder()
                .header("Proxy-Authorization", credential)
                .build()
        }
    }

    /**
     * Первичная инициализация из DataStore (вызывается в onCreate).
     */
    suspend fun init() = mutex.withLock {
        val snapshot = prefs.data.first()
        var list = parseList(snapshot.proxyListData)
        val activeId = snapshot.proxyActiveId

        // Авто-миграция legacy прокси (если список пуст, но legacy-поля заполнены)
        if (list.isEmpty() && snapshot.netProxyEnabled && snapshot.netProxyHost.isNotBlank()) {
            val legacy = ProxyConfig(
                title = "Импортированный прокси",
                type = ProxyType.HTTP,
                host = snapshot.netProxyHost,
                port = snapshot.netProxyPort,
            )
            list = listOf(legacy)
            prefs.setProxyListData(gson.toJson(list))
            prefs.setProxyActiveId(legacy.id)
            currentActiveProxy = legacy
            _activeProxyFlow.value = legacy
            _proxiesFlow.value = list
            AppLog.i("ProxyRepo", "Migrated legacy proxy: ${legacy.formattedAddress}")
            return@withLock
        }

        _proxiesFlow.value = list
        val active = list.firstOrNull { it.id == activeId }
        currentActiveProxy = active
        _activeProxyFlow.value = active

        AppLog.d("ProxyRepo", "Initialized ${list.size} proxies, active: ${active?.displayTitle ?: "DIRECT"}")
    }

    suspend fun load(): List<ProxyConfig> {
        val raw = prefs.data.first().proxyListData
        val list = parseList(raw)
        _proxiesFlow.value = list
        return list
    }

    suspend fun addProxy(config: ProxyConfig): List<ProxyConfig> = mutex.withLock {
        val current = parseList(prefs.data.first().proxyListData).toMutableList()
        current.removeAll { it.id == config.id }
        current.add(config)
        saveList(current)
        current
    }

    suspend fun updateProxy(config: ProxyConfig): List<ProxyConfig> = mutex.withLock {
        val current = parseList(prefs.data.first().proxyListData).toMutableList()
        val index = current.indexOfFirst { it.id == config.id }
        if (index >= 0) {
            current[index] = config
        } else {
            current.add(config)
        }
        saveList(current)

        if (currentActiveProxy?.id == config.id) {
            currentActiveProxy = config
            _activeProxyFlow.value = config
            evictOkHttpPool()
        }
        current
    }

    suspend fun deleteProxy(id: String): List<ProxyConfig> = mutex.withLock {
        val current = parseList(prefs.data.first().proxyListData).toMutableList()
        current.removeAll { it.id == id }
        saveList(current)

        if (currentActiveProxy?.id == id) {
            currentActiveProxy = null
            _activeProxyFlow.value = null
            prefs.setProxyActiveId("")
            prefs.setNetProxyEnabled(false)
            evictOkHttpPool()
        }
        current
    }

    suspend fun setActiveProxy(id: String?) = mutex.withLock {
        val list = parseList(prefs.data.first().proxyListData)
        val chosen = list.firstOrNull { it.id == id }

        currentActiveProxy = chosen
        _activeProxyFlow.value = chosen
        prefs.setProxyActiveId(chosen?.id.orEmpty())

        if (chosen != null) {
            prefs.setNetProxyEnabled(true)
            prefs.setNetProxyHost(chosen.host)
            prefs.setNetProxyPort(chosen.port)
        } else {
            prefs.setNetProxyEnabled(false)
        }

        evictOkHttpPool()
        AppLog.i("ProxyRepo", "Active proxy set to: ${chosen?.displayTitle ?: "DIRECT"}")
    }

    suspend fun testProxy(proxy: ProxyConfig): ProxyPingResult = withContext(Dispatchers.IO) {
        val start = System.currentTimeMillis()
        try {
            val socketProxy = Proxy(
                if (proxy.type == ProxyType.SOCKS) Proxy.Type.SOCKS else Proxy.Type.HTTP,
                if (isIpAddress(proxy.host)) InetSocketAddress(proxy.host, proxy.port)
                else InetSocketAddress.createUnresolved(proxy.host, proxy.port)
            )

            val testClient = OkHttpClient.Builder()
                .proxy(socketProxy)
                .connectTimeout(5, TimeUnit.SECONDS)
                .readTimeout(5, TimeUnit.SECONDS)
                .callTimeout(7, TimeUnit.SECONDS)
                .apply {
                    if (proxy.authEnabled && proxy.username.isNotBlank()) {
                        proxyAuthenticator { _, resp ->
                            if (resp.request.header("Proxy-Authorization") != null) null
                            else resp.request.newBuilder()
                                .header("Proxy-Authorization", Credentials.basic(proxy.username, proxy.password))
                                .build()
                        }
                    }
                }
                .build()

            val request = Request.Builder()
                .url("https://api.vk.com/robots.txt")
                .head()
                .build()

            testClient.newCall(request).execute().use { resp ->
                val latency = System.currentTimeMillis() - start
                if (resp.isSuccessful || resp.code == 404 || resp.code == 407 || resp.code in 300..399) {
                    ProxyPingResult.Success(latency)
                } else {
                    ProxyPingResult.Error("HTTP ${resp.code}")
                }
            }
        } catch (e: Exception) {
            val msg = e.localizedMessage ?: e.message ?: "Ошибка"
            val clean = when {
                msg.contains("timeout", ignoreCase = true) -> "Таймаут"
                msg.contains("failed to connect", ignoreCase = true) -> "Сбой подключения"
                msg.contains("refused", ignoreCase = true) -> "Отклонено сервером"
                else -> msg.take(30)
            }
            ProxyPingResult.Error(clean)
        }
    }

    private suspend fun saveList(list: List<ProxyConfig>) {
        val json = gson.toJson(list)
        prefs.setProxyListData(json)
        _proxiesFlow.value = list
    }

    private fun parseList(raw: String): List<ProxyConfig> {
        if (raw.isBlank()) return emptyList()
        return try {
            gson.fromJson<List<ProxyConfig>>(raw, type) ?: emptyList()
        } catch (e: Exception) {
            AppLog.w("ProxyRepo", "parseList error: ${e.message}")
            emptyList()
        }
    }

    private fun evictOkHttpPool() {
        try {
            VkMintApp.getOrNull()?.httpClient?.connectionPool?.evictAll()
        } catch (_: Exception) {}
    }

    @Suppress("DEPRECATION")
    private fun isIpAddress(host: String): Boolean {
        return Patterns.IP_ADDRESS.matcher(host).matches() || host.contains(":")
    }
}
