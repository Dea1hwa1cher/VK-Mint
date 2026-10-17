package vk.mint.auth

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.view.View
import android.webkit.CookieManager
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.webkit.WebSettingsCompat
import androidx.webkit.WebViewFeature
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import vk.mint.auth.exchange.AuthDomainsConfig
import vk.mint.auth.exchange.AuthDomainsSettingsIcon
import vk.mint.auth.exchange.SilentTokenExchanger
import vk.mint.ui.components.M3ELoadingIndicator
import vk.mint.ui.components.VkButton
import vk.mint.ui.components.VkButtonMode
import vk.mint.ui.components.VkButtonSize
import vk.mint.ui.components.VkGroup
import vk.mint.ui.components.VkGroupMode
import vk.mint.ui.components.VkPanelHeader
import vk.mint.ui.icons.VkIcons
import vk.mint.util.AppLog

/**
 * #VKAUTH-V2 (2026-08-06): чистая реализация WebView-входа с нуля.
 *
 * Цели (по запросу пользователя — переписать с нуля):
 *  1. **Полный cookie capture** — снимаем ВСЕ 9 remix-ключей при onPageFinished
 *     для каждого URL (не только remixsid). Кормим silentRefreshViaRemixsid
 *     (Path 1.5) для удержания сессии при смене IP.
 *  2. **Чистый MVI** — Loading/Success/Error, без thrashing/recreate/retention.
 *     WebView грузит m.vk.ru один раз, polling remixsid, при найденном →
 *     submitOAuthToken со всеми cookies.
 *  3. **Primary CTA** — кнопка «Войти через VK» на Landing.
 *
 * Cookie capture: onPageFinished → CookieManager.getCookie(url) для каждого
 * посещённого VK-домена → парсим name=value → выбираем 9 нужных → storage.
 * Это покрывает случай, когда VK ставит cookies на разные домены
 * (.vk.ru, .login.vk.ru, vk.ru host-only) — снимаем со всех.
 *
 * Reuse из exchange-слоя (не дублируем):
 *  - SilentTokenExchanger.parseSilentToken / parseDirectAccessToken
 *  - AuthDomainsConfig.mobileWebUrl()
 *  - PendingAuthResult (SSO callback)
 */
private const val TAG = "VkAuthWebViewV2"

/**
 * #SSO-DEEPLINK-FIX: Chrome Mobile User-Agent БЕЗ WebView-маркера.
 *
 * `WebSettings.getDefaultUserAgent(ctx)` возвращает UA с маркером WebView
 * (`Version/4.0`), по которому VK ID SDK определяет среду WebView и НЕ
 * генерирует deep-link `intent://qr.vk.ru/ca?q=…` для запуска офиц. VK app
 * (кнопка «Войти через приложение»). В итоге страница id.vk.ru/auth грузится,
 * но VK app не открывается.
 *
 * Убираем `Version/4.0 ` (и возможный `; wv`) — получаем настоящий Chrome
 * Mobile UA (реальный Android-версии и модели устройства), на который VK ID
 * отвечает стандартным web-flow с генерацией intent:// deep-link.
 * Chrome UA безопасен для веб-страниц (см. OAuthWebViewActivity.CHROME_UA):
 * VKAndroidApp-UA наоборот ломает web-flow (error 3).
 */
private fun chromeMobileUserAgent(ctx: android.content.Context): String {
    val def = WebSettings.getDefaultUserAgent(ctx)
    val cleaned = def
        .replace("Version/4.0 ", "")
        .replace("; wv", ";")
    // #WEBVIEW-OUTDATED-FIX: если системный WebView старый (Chrome < 100) —
    // VK mobile блокирует вход («Your browser is out of date»).
    // Подставляем свежий Chrome Mobile UA, который VK ID принимает.
    val verMatch = Regex("Chrome/(\\d+)").find(cleaned)
    val chromeVer = verMatch?.groupValues?.get(1)?.toIntOrNull() ?: 0
    if (chromeVer < 100) {
        // #WEBVIEW-OUTDATED-FIX: свежий Chrome Mobile UA (тот же, что в OAuthWebViewActivity).
        return vk.mint.auth.OAuthWebViewActivity.CHROME_UA
    }
    return cleaned
}

// 9 remix-ключей для полного cookie-set (silentRefreshViaRemixsid, Path 1.5).
// Без них VK отвергает silent refresh после смены IP (§55 #SSO-FULL-COOKIE-SET).
private val SESSION_COOKIE_NAMES = setOf(
    "remixsid", "remixnsid", "httoken", "remixnttpid",
    "remixuacck", "remixuas", "remixdmgr", "remixmvk-fp",
    "p",  // persistent login (.login.vk.ru) — critical для cross-IP, опционален.
)

/**
 * Чистый экран WebView-авторизации.
 *
 * @param onTokenExchange вызывается когда remixsid найден в CookieManager.
 *   Параметры: (remixsid, sessionCookies) — sessionCookies содержит все 9
 *   найденных remix-ключей для submitOAuthToken.
 * @param onSilentTokenExchanged SSO callback (silent_token / direct access_token
 *   из shouldOverrideUrlLoading).
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun VkAuthWebViewScreenV2(
    isLoading: Boolean,
    onTokenExchange: (remixsid: String, SessionCookies, WebView) -> Unit,
    onBack: () -> Unit,
    onCancel: () -> Unit = {},
    onSilentTokenExchanged: (accessToken: String, userId: Long) -> Unit = { _, _ -> },
    silentMode: Boolean = false,
    // Fix #370 #LOGOUT-WEBTOKEN-CLEAR: запуск после logout (AuthActivity передаёт
    // reason="logout" из EXTRA_AUTH_REASON). При ПЕРВОМ onPageFinished на хосте
    // m.vk.ru/m.vk.com удаляем ВСЕ ключи *:web_token:login:auth из localStorage
    // (WebTokenAuth.clearAllWebTokenKeysNow) — РОВНО ОДИН раз, до ввода логина,
    // чтобы валидный web_token предыдущего аккаунта не подобрался «молча» при
    // следующем логине, а свежий токен новой сессии не затрагивался.
    clearStoredWebTokenOnLoad: Boolean = false,
    // #VKID-ONLY (vk.id.md F-apply): стартовый URL WebView. По умолчанию —
    // VK ID entry (m.vk.ru/login?app_id=6287487), чтобы кнопка «Войти через VK»
    // авторизовала ТОЛЬКО через VK ID SDK (silent_token exchange). Caller может
    // передать AuthDomainsConfig.mobileWebUrl() для legacy root-загрузки.
    startUrl: String = AuthDomainsConfig.vkIdLoginUrl(),
    modifier: Modifier = Modifier,
) {
    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    var statusText by remember { mutableStateOf("Открываем VK…") }
    var loading by remember { mutableStateOf(true) }
    // P0-3 #AUTH-AUDIT: safety-net использует этот флаг вместо loading.
    // loading=true означает «страница грузится» (onPageStarted загрузил),
    // а pageStartedReceived — «onPageStarted ВООБЩЕ был». Если loading=true
    // но pageStartedReceived=true — всё ок, страница грузится. Если loading=true
    // и pageStartedReceived=false через 6 сек — chromium starvation → reload.
    var pageStartedReceived by remember { mutableStateOf(false) }
    // #BLACKSCREEN-RENDERER (2026-09-24): webViewEpoch — инкремент пересоздаёт
    // AndroidView (key) когда chromium renderer мёртв; rendererFailed включает
    // оверлей с диагностикой вместо вечного чёрного экрана.
    var webViewEpoch by remember { mutableIntStateOf(0) }
    var rendererFailed by remember { mutableStateOf(false) }
    var isExchanging by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    // #AUTH-TOAST-STATUS (2026-09-24): в silent-режиме экран невидим (stealth,
    // Fix #384) — статус авторизации показываем Toast'ом поверх MainActivity:
    // боковая панель и навигация остаются видимыми и кликабельными.
    fun authToast(text: String, long: Boolean = false) {
        if (!silentMode) return
        try {
            android.widget.Toast.makeText(
                context, text,
                if (long) android.widget.Toast.LENGTH_LONG else android.widget.Toast.LENGTH_SHORT,
            ).show()
        } catch (e: Exception) {
            AppLog.w(TAG, "authToast failed: ${e.message}")
        }
    }

    // Флаг причины закрытия — для точного лога в onDispose.
    var userClosing by remember { mutableStateOf(false) }
    var authSucceeded by remember { mutableStateOf(false) }

    val handleClose: () -> Unit = {
        userClosing = true
        onBack()
    }

    // PendingAuthResult polling — SSO callback (silent_token из shouldOverrideUrlLoading).
    LaunchedEffect(Unit) {
        val startTime = System.currentTimeMillis()
        val timeoutMs = 120_000L
        while (System.currentTimeMillis() - startTime < timeoutMs) {
            val result = PendingAuthResult.consume()
            if (result != null) {
                val token = result.first
                val uid = result.second
                AppLog.i(TAG, "PendingAuthResult: token found (user_id=$uid)")
                authSucceeded = true
                onSilentTokenExchanged(token, uid)
                return@LaunchedEffect
            }
            delay(500L)
        }
        AppLog.d(TAG, "PendingAuthResult: timeout (120s)")
    }

    // Cookie polling — remixsid. При найденном → снимаем полный cookie-set
    // и вызываем onTokenExchange.
    //
    // P2-9 #AUTH-AUDIT: адаптивный интервал polling.
    // Первые 30 сек — 500мс (быстрый вход, обычно remixsid появляется за 2-10 сек
    // после ввода логина/пароля). После 30 сек — 2000мс (пользователь видимо
    // вводит 2FA код или QR-подтверждение, нет смысла будить CPU каждую секунду).
    // Уменьшает wakeup на Android Doze когда экран долго включён.
    DisposableEffect(Unit) {
        if (silentMode) authToast("Авторизация: проверяем сессию…")
        val pollJob = coroutineScope.launch(Dispatchers.Default) {
            val startTime = System.currentTimeMillis()
            val timeoutMs = if (silentMode) 30_000L else 300_000L
            val fastIntervalMs = 500L   // первые 30 сек
            val slowIntervalMs = 2000L  // после 30 сек
            val fastPhaseMs = 30_000L
            AppLog.i(TAG, "remixsid polling запущен (адаптивный: ${fastIntervalMs}мс → ${slowIntervalMs}мс после ${fastPhaseMs/1000}с)")

            while (isActive) {
                val remixsid = getRemixSidFromCookieManager()
                if (remixsid != null) {
                    val wv = webViewRef
                    if (wv != null) {
                        AppLog.i(TAG, "remixsid найден! длина=${remixsid.length}")
                        // Снимаем полный cookie-set со всех VK-доменов.
                        val cookies = captureSessionCookies()
                        AppLog.i(TAG, "session cookies captured: " +
                            "remixsid=${cookies.remixsid != null}, " +
                            "remixnsid=${cookies.remixnsid != null}, " +
                            "httoken=${cookies.httoken != null}, " +
                            "p=${cookies.p != null}")
                        withContext(Dispatchers.Main) {
                            isExchanging = true
                            statusText = "Сессия найдена, получаем токен…"
                            authToast("Сессия найдена, получаем токен…")
                            onTokenExchange(remixsid, cookies, wv)
                            // Fix #377 #DOZE-COOKIE-FLUSH: после успешного обмена
                            // сбрасываем CookieManager на диск. Раньше flush был
                            // только в normal-mode перед loadUrl; в silent-ветке
                            // (Doze re-login) ротации кукисов, полученные WebView
                            // во время обмена, оставались только в памяти процесса —
                            // смерть WebView-процесса (Doze) их теряла, и следующий
                            // silent refresh шёл со stale-куками.
                            try {
                                CookieManager.getInstance().flush()
                                AppLog.i(TAG, "CookieManager.flush() после onTokenExchange (silent re-login, #DOZE-COOKIE-FLUSH)")
                            } catch (e: Exception) {
                                AppLog.w(TAG, "CookieManager.flush() failed: ${e.message}")
                            }
                        }
                    } else {
                        AppLog.w(TAG, "remixsid найден, но webViewRef == null")
                    }
                    return@launch
                }

                if (System.currentTimeMillis() - startTime > timeoutMs) {
                    withContext(Dispatchers.Main) {
                        statusText = if (silentMode)
                            "Не удалось автоматически войти. Возврат на экран входа."
                        else
                            "Таймаут авторизации. Вернитесь и попробуйте снова."
                        handleClose()
                    }
                    return@launch
                }

                // P2-9: адаптивный интервал.
                val elapsed = System.currentTimeMillis() - startTime
                val interval = if (elapsed < fastPhaseMs) fastIntervalMs else slowIntervalMs
                delay(interval)
            }
        }

        // P0-3 #AUTH-AUDIT: safety-net — reload через 6 сек если onPageStarted
        // не сработал (loading остался true). Chromium renderer starvation
        // (cr_ChildProcessConn: Failed to establish the service connection)
        // приводит к тому, что onPageStarted NEVER fires → m.vk.ru не грузится.
        // Variant A (commit 89a71efe5) удалил recreate safety-net (который thrash'ил
        // 3 destroy+recreate за 3 сек). Этот safety-net — ОДИН reload, без recreate.
        // Если reload тоже не помогает — пользователь жмёт «Отмена»/«Офлайн».
        // #BLACKSCREEN-RENDERER (2026-09-24): эскалационная версия safety-net.
        // Логкат юзера (HyperOS, WebView 151.0.7922.202): renderer НЕ поднялся
        // ВООБЩЕ (cr_ChildProcessConn: Failed to establish the service connection
        // ×2) — onPageStarted не приходит никогда, onProgressChanged застревает
        // на 10%, evaluateJavascript не отвечает → вечный цикл reload + чёрный
        // экран. Одиночный reload такой случай не чинит. Эскалация:
        //   1) reload → 2) reload → 3) recreate WebView → 4) recreate →
        //   5) честный оверлей «WebView не запустился» (вместо чёрного экрана).
        // Каждый шаг ждёт 6 сек на pageStartedReceived.
        val safetyNetJob = coroutineScope.launch(Dispatchers.Main) {
            val steps = listOf("reload", "reload", "recreate", "recreate", "fail")
            for ((idx, step) in steps.withIndex()) {
                kotlinx.coroutines.delay(6_000L)  // 6 сек — больше чем нормальный onPageStarted (~500мс-2с)
                // #BLACKSCREEN-RENDERER: isExchanging ВЫПИСАН из условия выхода.
                // На устройстве юзера remixsid лежит в куках → isExchanging=true
                // приходил мгновенно и SAFETY-NET никогда не эскалировал, хотя
                // renderer мёртв (JS не отвечает, onPageStarted не приходит).
                // Обмен без работающего renderer невозможен — эскалация нужна
                // ИМЕННО в этом состоянии. Выходим только по живой странице.
                if (pageStartedReceived || authSucceeded) return@launch
                when (step) {
                    "reload" -> {
                        AppLog.w(TAG, "#WEBVIEW-SAFETY-NET [${idx + 1}/${steps.size}]: onPageStarted не сработал за 6 сек — reload $startUrl")
                        val wv = webViewRef
                        if (wv != null) {
                            try {
                                wv.reload()
                            } catch (e: Exception) {
                                AppLog.w(TAG, "#WEBVIEW-SAFETY-NET: reload failed: ${e.message}")
                            }
                        } else {
                            AppLog.w(TAG, "#WEBVIEW-SAFETY-NET: webViewRef == null, reload невозможен")
                        }
                    }
                    "recreate" -> {
                        AppLog.w(TAG, "#WEBVIEW-SAFETY-NET [${idx + 1}/${steps.size}]: reload не помог — пересоздаём WebView (renderer не поднялся?)")
                        statusText = "WebView не отвечает, перезапускаем…"
                        authToast("WebView не отвечает, перезапускаем авторизацию…")
                        // Сброс page-state — иначе новый инстанс унаследует «живой» флаг
                        // и safety-net ложно решит, что всё ок.
                        pageStartedReceived = false
                        loading = true
                        webViewEpoch++
                    }
                    "fail" -> {
                        AppLog.e(TAG, "#WEBVIEW-SAFETY-NET: renderer WebView не поднялся после reload/recreate — показываем диагностику вместо чёрного экрана")
                        if (silentMode) {
                            // #AUTH-TOAST-STATUS: в silent чёрного экрана нет и быть не
                            // должно — закрываем невидимый экран, Main остаётся юзабельным.
                            authToast(
                                "WebView не работает: перезагрузите телефон или обновите " +
                                    "«Android System WebView» (Play Маркет), затем повторите вход",
                                long = true,
                            )
                            userClosing = true
                            handleClose()
                        } else {
                            rendererFailed = true
                        }
                    }
                }
            }
        }

        // onDispose: cancel polling + safety-net + detach/destroy WebView.
        // #WEBVIEW-DETACH-BEFORE-DESTROY: removeView BEFORE destroy — иначе
        // "WebView.destroy() called while still attached" → binder утечка.
        onDispose {
            pollJob.cancel()
            safetyNetJob.cancel()
            val reason = when {
                userClosing -> "user closed"
                authSucceeded -> "auth succeeded"
                else -> "system destroy"
            }
            val wv = webViewRef
            if (wv != null) {
                try {
                    val parent = wv.parent
                    if (parent is android.view.ViewGroup) {
                        parent.removeView(wv)
                    }
                } catch (e: Exception) {
                    AppLog.w(TAG, "onDispose removeView failed: ${e.message}")
                }
                try { wv.destroy() } catch (e: Exception) {
                    AppLog.w(TAG, "onDispose wv.destroy failed: ${e.message}")
                }
                AppLog.i(TAG, "onDispose: WebView destroyed ($reason)")
                webViewRef = null
            }
        }
    }

    val errorMessage = null  // Error overlay убран — кнопки Отмена/Офлайн достаточно.
    val hideWebView = isExchanging || silentMode

    val isDark = isSystemInDarkTheme()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        // MD3E x VKUI Panel Header
        if (!silentMode) {
            VkPanelHeader(
                modifier = Modifier.statusBarsPadding(),
                title = "Вход через VK ID",
                subtitle = if (isExchanging) {
                    "Получение токена сессии…"
                } else if (loading) {
                    "Загрузка страницы…"
                } else {
                    "Защищённая авторизация id.vk.ru"
                },
                before = {
                    IconButton(onClick = handleClose) {
                        Icon(
                            imageVector = VkIcons.ArrowBack24,
                            contentDescription = "Назад",
                            tint = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                },
                after = {
                    AuthDomainsSettingsIcon()
                },
                showSeparator = true,
            )

            // Плавный линейный прогресс загрузки прямо под шапкой
            AnimatedVisibility(
                visible = loading || isExchanging,
                enter = fadeIn(),
                exit = fadeOut(),
            ) {
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(2.5.dp),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = Color.Transparent,
                )
            }
        }

        // Основная область с WebView и оверлеями
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .navigationBarsPadding()
                .imePadding(),
        ) {
            // #BLACKSCREEN-NORENDER (2026-09-24): фон-подложка + лоадер ПОД WebView.
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surface),
                contentAlignment = Alignment.Center,
            ) {
                if (!pageStartedReceived && !isExchanging && !rendererFailed) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.padding(24.dp),
                    ) {
                        M3ELoadingIndicator(
                            size = 48.dp,
                            contained = true,
                        )
                        Spacer(Modifier.height(16.dp))
                        Text(
                            text = statusText,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            // #BLACKSCREEN-RENDERER: key(webViewEpoch) — инкремент epoch полностью
            // пересоздаёт инстанс WebView (новая попытка поднятия renderer'а).
            key(webViewEpoch) {
        AndroidView(
            modifier = Modifier
                .fillMaxSize()
                .then(if (hideWebView || !pageStartedReceived) Modifier.alpha(0f) else Modifier),
            factory = { ctx ->
                AppLog.i(TAG, "factory: создаём FixedInputWebView")
                FixedInputWebView(ctx).apply {
                    setBackgroundColor(android.graphics.Color.TRANSPARENT)
                    settings.apply {
                        javaScriptEnabled = true
                        domStorageEnabled = true
                        allowContentAccess = true
                        allowFileAccess = true
                        setSupportZoom(false)
                        builtInZoomControls = false
                        displayZoomControls = false
                        // #51: NORMAL layout — стабильный для input (курсор не сбрасывается).
                        layoutAlgorithm = WebSettings.LayoutAlgorithm.NORMAL
                        // #47: Chrome Mobile UA — динамический (system default), НЕ хардкод.
                        // P1-6 #AUTH-AUDIT: ранеe хардкод "HOTWAV Cyber 15 / Chrome/131.0.0.0"
                        // противоречил build.gradle.kts:46-50 (warn про error 15 на messages.*).
                        // System default UA = реальный Chrome Mobile на устройстве пользователя.
                        // P1-6 #AUTH-AUDIT: Chrome Mobile UA, НО БЕЗ WebView-маркера
                        // (Version/4.0). getDefaultUserAgent возвращает маркер → VK ID
                        // не запускает deep-link в офиц. VK app (#SSO-DEEPLINK-FIX).
                        userAgentString = chromeMobileUserAgent(ctx)
                        @Suppress("DEPRECATION")
                        saveFormData = false
                        javaScriptCanOpenWindowsAutomatically = true
                        cacheMode = WebSettings.LOAD_DEFAULT
                        @Suppress("DEPRECATION")
                        defaultTextEncodingName = "UTF-8"

                        // Интеграция с тёмной темой приложения
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            if (WebViewFeature.isFeatureSupported(WebViewFeature.ALGORITHMIC_DARKENING)) {
                                WebSettingsCompat.setAlgorithmicDarkeningAllowed(this, isDark)
                            } else if (WebViewFeature.isFeatureSupported(WebViewFeature.FORCE_DARK)) {
                                @Suppress("DEPRECATION")
                                WebSettingsCompat.setForceDark(
                                    this,
                                    if (isDark) WebSettingsCompat.FORCE_DARK_ON else WebSettingsCompat.FORCE_DARK_OFF,
                                )
                            }
                        }
                    }
                    // P1-5 #AUTH-AUDIT (ВОЗВРАЩЕНО): setLayerType(LAYER_TYPE_SOFTWARE).
                    // Я убрал его в коммите 8e60a07da думая что VkMintInputConnection справится
                    // один. Ошибка: без software rendering IME на некоторых устройствах шлёт
                    // composition events которые React обрабатывает неправильно → зеркальный
                    // ввод (Pluton240 → 042notulP). Тройной слой фикса:
                    //   1. setLayerType(SOFTWARE) — стабильный IME контекст
                    //   2. VkMintInputConnection — перехват setComposingText → commitText
                    //   3. VK_2FA_CURSOR_FIX_JS — forceEnd после input (React controlled inputs)
                    // Все 3 слоя нужны — убирание любого ломает ввод на части устройств.
                    setLayerType(View.LAYER_TYPE_SOFTWARE, null)
                    isFocusable = true
                    isFocusableInTouchMode = true
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        importantForAutofill = View.IMPORTANT_FOR_AUTOFILL_NO_EXCLUDE_DESCENDANTS
                    }
                    // P1-4 #AUTH-AUDIT: очищаем CookieManager перед loadUrl (только normal mode).
                    // Stale cookies от прошлой сессии (logout не вычистил) → polling мгновенно
                    // находит старый remixsid → onTokenExchange → silent refresh fails → Error →
                    // phase=LANDING. Пользователь не видит m.vk.ru, видит Landing снова.
                    // В silentMode НЕ очищаем — там как раз читаем существующий remixsid.
                    if (!silentMode) {
                        try {
                            CookieManager.getInstance().removeAllCookies(null)
                            CookieManager.getInstance().removeSessionCookies(null)
                            CookieManager.getInstance().flush()
                            AppLog.i(TAG, "CookieManager очищен перед loadUrl (normal mode, #AUTH-AUDIT P1-4)")
                        } catch (e: Exception) {
                            AppLog.w(TAG, "CookieManager cleanup failed: ${e.message}")
                        }
                    }
                    CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)
                    CookieManager.getInstance().setAcceptCookie(true)

                    // Fix #370 #LOGOUT-WEBTOKEN-CLEAR: one-shot флаг очистки —
                    // scoped к этому WebView-инстансу (factory выполняется один раз).
                    var webTokenCleanupDone = false

                    webViewClient = object : WebViewClient() {
                        override fun shouldOverrideUrlLoading(
                            view: WebView,
                            request: WebResourceRequest,
                        ): Boolean {
                            val url = request.url.toString()
                            val reqScheme = request.url.scheme
                            val scheme = if (reqScheme != null) reqScheme else ""
                            val reqHost = request.url.host
                            val host = if (reqHost != null) reqHost else ""
                            AppLog.d(TAG, "navigate: $url")

                            // Блокировка рекламы.
                            val adHosts = setOf(
                                "ad.mail.ru", "ads.vk.com", "ad.vk.com",
                                "rtb.vk.com", "an.yandex.ru", "mc.yandex.ru",
                                "mc.webvisor.org", "ad.doubleclick.net",
                                "googlesyndication.com", "googleadservices.com",
                                "googletagservices.com", "facebook.com",
                                "connect.facebook.net",
                            )
                            val isAdHost = adHosts.any { host == it || host.endsWith(".$it") }
                            if (isAdHost) {
                                AppLog.d(TAG, "Заблокирована реклама: $host")
                                return true
                            }

                            // intent:// → «Войти через приложение» (QR-SSO в VK app).
                            // #AUTH-SIMPLIFY (2026-08-15): НЕ запускаем VK app по QR.
                            // QR-SSO не работает в WebView: при уходе в VK app Chromium
                            // останавливает JS-таймеры (экономия батареи) → QR-polling
                            // id.vk.ru/auth прерывается → remixsid не ставится → цикл
                            // «новый QR → подтверждение → новый QR». Блокируем переход —
                            // пользователь остаётся на форме VK ID и входит логином/паролем.
                            if (scheme == "intent") {
                                AppLog.i(TAG, "intent:// заблокирован (#AUTH-SIMPLIFY: QR-SSO отключён) — остаёмся на форме VK ID")
                                return true
                            }
                            // Custom schemes.
                            if (scheme == "vkontakte" || scheme == "vk" || scheme == "vklink") {
                                AppLog.i(TAG, "custom scheme $scheme:// — VK app: $url")
                                return IntentLauncher.launchCustomScheme(url, scheme)
                            }
                            if (scheme == "market") {
                                AppLog.i(TAG, "market:// — Play Store: $url")
                                return IntentLauncher.launchMarketUrl(url)
                            }

                            // silent_token перехват (VK ID SDK).
                            val silentPair = SilentTokenExchanger.parseSilentToken(url)
                            if (silentPair != null) {
                                val (st, uuid) = silentPair
                                AppLog.i(TAG, "silent_token перехвачен (uuid=$uuid)")
                                SsoExchangeScope.launch {
                                    val appCtx = vk.mint.VkMintApp.get()
                                    val exchanger = SilentTokenExchanger(appCtx.httpClient)
                                    val deviceId = appCtx.exchangeStorage.deviceId()
                                    val result = exchanger.exchange(
                                        silentToken = st,
                                        silentTokenUuid = uuid,
                                        anonymousToken = null,
                                        // #SSO-PROVIDER-FIX: silent_token выдаётся для
                                        // app_id из vkIdLoginUrl() (= webClientId, 6287487),
                                        // а НЕ для VK_CLIENT_ID (2274003, официальный Android
                                        // client для Direct Auth). Раньше здесь был VK_CLIENT_ID —
                                        // auth.getAuthData отклонял silent_token как чужой
                                        // (token issued for другой app_id) → SSO не завершался.
                                        providerAppId = AuthDomainsConfig.webClientId(),
                                        deviceId = deviceId,
                                    )
                                    when (result) {
                                        is SilentTokenExchanger.Result.Success -> {
                                            AppLog.i(TAG, "silent_token exchange УСПЕШЕН → user_id=${result.userId}")
                                            PendingAuthResult.save(result.accessToken, result.userId)
                                            try {
                                                onSilentTokenExchanged(result.accessToken, result.userId)
                                            } catch (e: Exception) {
                                                AppLog.e(TAG, "onSilentTokenExchanged callback failed: ${e.message}", e)
                                            }
                                        }
                                        is SilentTokenExchanger.Result.TokenInvalid -> {
                                            AppLog.w(TAG, "silent_token exchange: токен невалиден — ${result.message}")
                                        }
                                        is SilentTokenExchanger.Result.Unavailable -> {
                                            AppLog.w(TAG, "silent_token exchange: сервер недоступен — ${result.message}")
                                        }
                                        is SilentTokenExchanger.Result.AllEndpointsFailed -> {
                                            AppLog.w(TAG, "silent_token exchange: все endpoints неудачны — ${result.errors}")
                                        }
                                    }
                                }
                                return true
                            }

                            // direct access_token (vk1.a.*) — без exchange.
                            val directToken = SilentTokenExchanger.parseDirectAccessToken(url)
                            if (directToken != null) {
                                val at = directToken.first
                                val uid = directToken.second
                                AppLog.i(TAG, "direct access_token перехвачен (user_id=$uid)")
                                PendingAuthResult.save(at, uid)
                                onSilentTokenExchanged(at, uid)
                                return true
                            }

                            // VK-домен allowlist.
                            val vkDomains = setOf(
                                "vk.com", "m.vk.com", "new.vk.com",
                                "login.vk.com", "login.vk.ru",
                                "oauth.vk.com", "oauth.vk.ru",
                                "id.vk.com", "id.vk.ru",
                                "connect.vk.com", "api.vk.com",
                                "web.api.vk.ru",
                                "static.vk.com", "st.vk.com", "st2.vk.com", "st3.vk.com",
                                "vkvideo.ru", "im.vk.com",
                                "passport.vk.com", "userapi.com",
                                "vk.ru", "m.vk.ru",
                            )
                            val isVkDomain = vkDomains.any { host == it || host.endsWith(".$it") }
                            return !isVkDomain
                        }

                        override fun onPageStarted(view: WebView, url: String, favicon: Bitmap?) {
                            super.onPageStarted(view, url, favicon)
                            loading = true
                            pageStartedReceived = true  // P0-3 fix: safety-net теперь не сработает ложно
                            AppLog.d(TAG, "onPageStarted: $url")
                            // P2-8 fix (восстановление): инжектим JS cursor fix + ad block
                            // на каждой загрузке страницы. IIFE-обёртка с `window.__vkmintCursorFix`
                            // / `window.__vkmintAdBlock` предотвращает дублирование.
                            // Без этого — зеркальный ввод в VK ID React forms (Pluton240 → 042notulP).
                            try {
                                view.evaluateJavascript(VK_2FA_CURSOR_FIX_JS, null)
                                view.evaluateJavascript(VK_INPUT_HARDENING_JS, null)
                            } catch (e: Exception) {
                                AppLog.w(TAG, "JS injection failed (onPageStarted): ${e.message}")
                            }
                        }

                        override fun onPageFinished(view: WebView, url: String?) {
                            super.onPageFinished(view, url)
                            loading = false
                            AppLog.i(TAG, "onPageFinished: $url")
                            statusText = "Войдите в VK, чтобы продолжить"
                            // Fix #370 #LOGOUT-WEBTOKEN-CLEAR: запуск после logout — на
                            // ПЕРВОЙ странице origin m.vk.ru/m.vk.com удаляем все
                            // *:web_token:login:auth ключи из localStorage (валидный
                            // web_token предыдущего аккаунта иначе остался бы и
                            // tryReadWebToken «молча» вернул бы его при следующем логине).
                            // РОВНО ОДИН раз — свежий токен новой сессии (появится после
                            // логина) не затрагиваем. Очистка строго ПОСЛЕ загрузки
                            // m.vk.ru: localStorage действует на origin текущего
                            // документа (на about:blank/id.vk.ru — другой storage).
                            if (clearStoredWebTokenOnLoad && !webTokenCleanupDone && url != null) {
                                val pageHost = Uri.parse(url).host
                                if (pageHost != null && (pageHost == "m.vk.ru" || pageHost == "m.vk.com")) {
                                    webTokenCleanupDone = true
                                    AppLog.i(TAG, "onPageFinished: reason=logout — чистим web_token из localStorage ($url)")
                                    try {
                                        vk.mint.auth.exchange.WebTokenAuth.clearAllWebTokenKeysNow(view)
                                    } catch (e: Exception) {
                                        AppLog.w(TAG, "clearAllWebTokenKeysNow failed: ${e.message}")
                                    }
                                }
                            }
                            // P2-8 fix (восстановление): повторная инъекция после полной загрузки.
                            // React может ре-рендерить input после navigation (SPA) —
                            // повторная инъекция гарантирует что listeners повешены.
                            try {
                                view.evaluateJavascript(VK_2FA_CURSOR_FIX_JS, null)
                                view.evaluateJavascript(VK_INPUT_HARDENING_JS, null)
                                // #CALLS-AUTOCLICK (временный): на id.vk.ru/auth автоматически
                                // нажимаем кнопку подтверждения входа (по тексту). Чтобы flow
                                // дошёл до onTokenReceived → getCallToken без ручного клика.
                                if (url?.contains("id.vk.ru/auth") == true) {
                                    val autoClickJs = """
                                        (function(){
                                          var btns = document.querySelectorAll('button, [role="button"], a');
                                          var targets = ['Продолжить','Продолжить как','Войти','Войти в VK','Продолжить вход'];
                                          for (var i=0;i<btns.length;i++){
                                            var t=(btns[i].textContent||'').trim();
                                            for (var j=0;j<targets.length;j++){
                                              if (t===targets[j]||t.indexOf(targets[j])===0){
                                                btns[i].click();
                                                return;
                                              }
                                            }
                                          }
                                        })();
                                    """.trimIndent()
                                    view.evaluateJavascript(autoClickJs, null)
                                    AppLog.i(TAG, "autoclick injected on id.vk.ru/auth")
                                }
                            } catch (e: Exception) {
                                AppLog.w(TAG, "JS injection failed (onPageFinished): ${e.message}")
                            }
                            // #FULL-COOKIE-CAPTURE: снимаем полный cookie-set при
                            // каждой загрузке страницы. VK ставит cookies на
                            // разные домены — getCookie(url) снимает для конкретного URL.
                            // Дополняем опросом всех VK-доменов ниже.
                            captureAndLogCookies(url)
                        }

                        override fun onReceivedError(
                            view: WebView,
                            request: WebResourceRequest?,
                            error: WebResourceError,
                        ) {
                            super.onReceivedError(view, request, error)
                            if (request != null && request.isForMainFrame) {
                                val msg = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                    error.description.toString()
                                } else {
                                    "error ${error.errorCode}"
                                }
                                AppLog.e(TAG, "onReceivedError: $msg (code=${error.errorCode}, url=${request.url})")
                                statusText = "Ошибка загрузки: $msg"
                            }
                        }

                        override fun onReceivedHttpError(
                            view: WebView,
                            request: WebResourceRequest?,
                            errorResponse: WebResourceResponse,
                        ) {
                            super.onReceivedHttpError(view, request, errorResponse)
                            if (request != null && request.isForMainFrame) {
                                AppLog.e(TAG, "onReceivedHttpError: ${errorResponse.statusCode} ${request.url}")
                            }
                        }

                        override fun onReceivedSslError(
                            view: WebView,
                            handler: android.webkit.SslErrorHandler,
                            error: android.net.http.SslError,
                        ) {
                            AppLog.e(TAG, "onReceivedSslError: ${error.primaryError} url=${error.url}")
                            handler.cancel()
                        }

                        override fun onRenderProcessGone(
                            view: WebView,
                            detail: android.webkit.RenderProcessGoneDetail,
                        ): Boolean {
                            // #BLACKSCREEN-RENDERER: дефолт (return false) УБИВАЕТ
                            // приложение при смерти renderer'а. Пересоздаём WebView.
                            val didCrash = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                detail.didCrash()
                            } else true
                            AppLog.e(TAG, "onRenderProcessGone: didCrash=$didCrash — recreate WebView")
                            rendererFailed = false
                            pageStartedReceived = false
                            loading = true
                            webViewEpoch++
                            return true
                        }
                    }

                    // P0-2 #AUTH-AUDIT: WebChromeClient — КРИТИЧНО для m.vk.ru SPA.
                    // БЕЗ WebChromeClient:
                    //   - onProgressChanged НЕ работает → UI не видит прогресс, нет fallback
                    //   - onJsAlert/Confirm/Prompt молча подавляются → VK ID SDK не получает ответа
                    //   - onCreateWindow НЕ работает → window.open() молча fail (VK ID QR-логин)
                    //   - onConsoleMessage НЕ работает → JS console output не виден в logcat
                    // Сравнение: OAuthWebViewActivity.kt:399 имеет setWebChromeClient (правильно),
                    // V2 — НЕ имел (бага). Это приводило к зависанию m.vk.ru при JS-диалогах.
                    setWebChromeClient(object : android.webkit.WebChromeClient() {
                        override fun onProgressChanged(view: WebView, newProgress: Int) {
                            AppLog.d(TAG, "onProgressChanged: $newProgress% (url=${view.url})")
                            // P0-3 #AUTH-AUDIT: safety-net через прогресс.
                            // Если прогресс остался 0 за 6 сек → chromium starvation → reload.
                            // (См. также safetyNetJob в DisposableEffect ниже — double coverage.)
                            if (newProgress >= 100) {
                                loading = false
                            }
                        }

                        override fun onJsAlert(
                            view: WebView,
                            url: String,
                            message: String,
                            result: android.webkit.JsResult,
                        ): Boolean {
                            AppLog.i(TAG, "JS alert: $message (url=$url)")
                            // Auto-confirm — не блокируем flow диалогами.
                            // Если нужно показать пользователю — тут должен быть AlertDialog.
                            result.confirm()
                            return true
                        }

                        override fun onJsConfirm(
                            view: WebView,
                            url: String,
                            message: String,
                            result: android.webkit.JsResult,
                        ): Boolean {
                            AppLog.i(TAG, "JS confirm: $message (url=$url)")
                            // Auto-confirm (VK ID SDK использует confirm для разрешений).
                            result.confirm()
                            return true
                        }

                        override fun onJsPrompt(
                            view: WebView,
                            url: String,
                            message: String,
                            defaultValue: String?,
                            result: android.webkit.JsPromptResult,
                        ): Boolean {
                            AppLog.i(TAG, "JS prompt: $message (url=$url, default=$defaultValue)")
                            // Auto-confirm с defaultValue — VK ID SDK использует prompt для ввода.
                            result.confirm(defaultValue ?: "")
                            return true
                        }

                        override fun onConsoleMessage(consoleMessage: android.webkit.ConsoleMessage): Boolean {
                            // Логируем JS console output — критично для отладки m.vk.ru.
                            AppLog.d(TAG, "JS [${consoleMessage.messageLevel()}]: ${consoleMessage.message()} at ${consoleMessage.sourceId()}:${consoleMessage.lineNumber()}")
                            return true
                        }

                        override fun onCreateWindow(
                            view: WebView,
                            isDialog: Boolean,
                            isUserGesture: Boolean,
                            resultMsg: android.os.Message,
                        ): Boolean {
                            // VK ID SDK может открывать window.open (например, для QR-логина).
                            // Создаём новый WebView в том же контексте с теми же настройками.
                            // API contract: resultMsg.obj — это WebView.WebViewTransport.
                            // Кладём в него новый WebView и sendToTarget().
                            AppLog.i(TAG, "onCreateWindow: isDialog=$isDialog, isUserGesture=$isUserGesture")
                            val newWebView = FixedInputWebView(view.context)
                            newWebView.settings.apply {
                                javaScriptEnabled = true
                                domStorageEnabled = true
                                userAgentString = chromeMobileUserAgent(view.context)
                                cacheMode = WebSettings.LOAD_DEFAULT
                            }
                            newWebView.webViewClient = view.webViewClient
                            newWebView.webChromeClient = view.webChromeClient
                            val transport = resultMsg.obj as? WebView.WebViewTransport
                            if (transport != null) {
                                transport.webView = newWebView
                            }
                            resultMsg.sendToTarget()
                            return true
                        }
                    })

                    AppLog.i(TAG, "loadUrl: $startUrl")
                    loadUrl(startUrl)
                    webViewRef = this
                }
            },
            update = { webView ->
                webViewRef = webView
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    if (WebViewFeature.isFeatureSupported(WebViewFeature.ALGORITHMIC_DARKENING)) {
                        WebSettingsCompat.setAlgorithmicDarkeningAllowed(webView.settings, isDark)
                    } else if (WebViewFeature.isFeatureSupported(WebViewFeature.FORCE_DARK)) {
                        @Suppress("DEPRECATION")
                        WebSettingsCompat.setForceDark(
                            webView.settings,
                            if (isDark) WebSettingsCompat.FORCE_DARK_ON else WebSettingsCompat.FORCE_DARK_OFF,
                        )
                    }
                }
            },
            onRelease = { wv ->
                // #BLACKSCREEN-RENDERER: destroy старого инстанса при recreate
                // (и при уходе с экрана — двойной destroy безвреден, guarded).
                try {
                    val parent = wv.parent
                    if (parent is android.view.ViewGroup) parent.removeView(wv)
                } catch (e: Exception) {
                    AppLog.w(TAG, "onRelease removeView failed: ${e.message}")
                }
                try { wv.destroy() } catch (e: Exception) {
                    AppLog.w(TAG, "onRelease wv.destroy failed: ${e.message}")
                }
                AppLog.i(TAG, "AndroidView onRelease: WebView destroyed (epoch=$webViewEpoch)")
            },
        )
        }

        // Оверлей обмена токена (M3E Expressive Dialog)
        if (isExchanging) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.92f))
                    .clickable(enabled = true, onClick = {}),
                contentAlignment = Alignment.Center,
            ) {
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    tonalElevation = 6.dp,
                    shadowElevation = 8.dp,
                    modifier = Modifier
                        .padding(32.dp)
                        .widthIn(max = 380.dp),
                ) {
                    Column(
                        modifier = Modifier.padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        M3ELoadingIndicator(
                            size = 52.dp,
                            contained = true,
                        )
                        Spacer(Modifier.height(20.dp))
                        Text(
                            text = "Вход через VK ID",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = "Получение токена сессии и синхронизация…",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }
        }

        // Оверлей ошибки рендеринга (VKUI x M3E Error Card)
        if (rendererFailed) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(20.dp),
                contentAlignment = Alignment.Center,
            ) {
                VkGroup(
                    mode = VkGroupMode.Card,
                    modifier = Modifier.widthIn(max = 440.dp),
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Warning,
                            contentDescription = null,
                            modifier = Modifier.size(44.dp),
                            tint = MaterialTheme.colorScheme.error,
                        )
                        Spacer(Modifier.height(14.dp))
                        Text(
                            text = "Не удалось загрузить страницу",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center,
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = "Системный компонент Android WebView не смог запустить процесс рендеринга страницы VK ID.\n\n" +
                                "1. Проверьте подключение к интернету или настройте прокси (шестерёнка вверху).\n" +
                                "2. Обновите «Android System WebView» в магазине приложений.\n" +
                                "3. Попробуйте повторить попытку.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Start,
                        )
                        Spacer(Modifier.height(20.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            VkButton(
                                onClick = {
                                    rendererFailed = false
                                    pageStartedReceived = false
                                    loading = true
                                    statusText = "Открываем VK ID…"
                                    webViewEpoch++
                                },
                                text = "Повторить",
                                mode = VkButtonMode.Primary,
                                size = VkButtonSize.Medium,
                                modifier = Modifier.weight(1f),
                            )
                            VkButton(
                                onClick = handleClose,
                                text = "Отмена",
                                mode = VkButtonMode.Secondary,
                                size = VkButtonSize.Medium,
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                }
            }
        }
    }
}
}

/**
 * Снимает полный cookie-set со всех VK-доменов и логирует найденные ключи.
 * Вызывается при onPageFinished — дополняет polling (который ловит только remixsid).
 */
private fun captureAndLogCookies(url: String?) {
    try {
        val cm = CookieManager.getInstance()
        // Список доменов, на которых VK ставит session cookies.
        val domains = listOf(
            "https://vk.ru",
            "https://m.vk.ru",
            "https://login.vk.ru",
            "https://id.vk.ru",
            "https://oauth.vk.com",
        )
        val found = mutableMapOf<String, String>()
        for (domain in domains) {
            val cookieHeader = cm.getCookie(domain)
            if (cookieHeader == null || cookieHeader.isBlank()) continue
            // cookieHeader = "name1=val1; name2=val2; ..."
            for (pair in cookieHeader.split("; ")) {
                val eqIdx = pair.indexOf('=')
                if (eqIdx <= 0) continue
                val name = pair.substring(0, eqIdx).trim()
                val value = pair.substring(eqIdx + 1).trim()
                if (name in SESSION_COOKIE_NAMES && value.isNotBlank()) {
                    found.putIfAbsent(name, value)  // первый найденный (приоритет .vk.ru)
                }
            }
        }
        AppLog.i(TAG, "onPageFinished cookie snapshot ($url): " +
            found.keys.joinToString(", ") { key ->
                val value = found[key]
                if (key == "remixsid" && value != null) "len=${value.length}" else "yes"
            })
    } catch (e: Exception) {
        AppLog.w(TAG, "captureAndLogCookies failed: ${e.message}")
    }
}

/**
 * Снимает полный cookie-set со всех VK-доменов для storage.
 * Возвращает SessionCookies со всеми найденными remix-ключами.
 *
 * Приоритет: .vk.ru (remixsid, httoken, remixnttpid, remixuacck, remixuas, remixdmgr)
 * > vk.ru host-only (remixnsid) > .login.vk.ru (p).
 */
private fun captureSessionCookies(): SessionCookies {
    val cm = CookieManager.getInstance()
    val domains = listOf(
        "https://vk.ru",
        "https://m.vk.ru",
        "https://login.vk.ru",
        "https://id.vk.ru",
        "https://oauth.vk.com",
    )
    val found = mutableMapOf<String, String>()
    for (domain in domains) {
        val cookieHeader = cm.getCookie(domain)
        if (cookieHeader == null || cookieHeader.isBlank()) continue
        for (pair in cookieHeader.split("; ")) {
            val eqIdx = pair.indexOf('=')
            if (eqIdx <= 0) continue
            val name = pair.substring(0, eqIdx).trim()
            val value = pair.substring(eqIdx + 1).trim()
            if (name in SESSION_COOKIE_NAMES && value.isNotBlank()) {
                found.putIfAbsent(name, value)
            }
        }
    }
    return SessionCookies(
        remixsid = found["remixsid"],
        p = found["p"],
        remixnsid = found["remixnsid"],
        httoken = found["httoken"],
        remixnttpid = found["remixnttpid"],
        remixuacck = found["remixuacck"],
        remixuas = found["remixuas"],
        remixdmgr = found["remixdmgr"],
        remixmvkFp = found["remixmvk-fp"],
    )
}

/**
 * 9 session cookies для submitOAuthToken. Все nullable — если cookie не захвачен,
 * не передаём (VK примет то, что есть).
 */
data class SessionCookies(
    val remixsid: String?,
    val p: String?,
    val remixnsid: String?,
    val httoken: String?,
    val remixnttpid: String?,
    val remixuacck: String?,
    val remixuas: String?,
    val remixdmgr: String?,
    val remixmvkFp: String?,
)

// P2-10 #AUTH-AUDIT: tryLaunchIntentUrl / tryLaunchCustomScheme / tryLaunchMarketUrl
// вынесены в [vk.mint.auth.IntentLauncher] object. Этот файл больше НЕ содержит
// private копий — shouldOverrideUrlLoading вызывает IntentLauncher.launchIntentUrl(url) и т.д.
