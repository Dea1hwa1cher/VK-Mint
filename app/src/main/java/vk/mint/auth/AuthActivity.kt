package vk.mint.auth

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import android.webkit.CookieManager
import android.webkit.WebView
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import vk.mint.R
import vk.mint.VkMintApp
import vk.mint.auth.exchange.AuthDomainsConfig
import vk.mint.auth.exchange.AuthState
import vk.mint.auth.exchange.ExternalBrowserAuth
import vk.mint.auth.exchange.RemixsidCapturer
import vk.mint.auth.exchange.WebTokenAuth
import vk.mint.ui.components.DraggableLogFab
import vk.mint.ui.components.LogDialogState
import vk.mint.ui.components.LogViewerDialog
import vk.mint.ui.theme.VkMintTheme
import vk.mint.util.AppLog

/**
 * Auth activity — WebView m.vk.ru (primary) + Direct Auth (deep fallback).
 *
 * **Основной flow:** WebView грузит `m.vk.ru` — пользователь логинится
 * через стандартную форму VK (включая 2FA через VK ID). CookieManager получает
 * `remixsid`. JavaScript m.vk.ru автоматически обменивает remixsid на токен
 * через login.vk.com и сохраняет его в localStorage.
 * Затем [WebTokenAuth.fullAuthFlow] читает токен из localStorage через
 * evaluateJavascript() — никаких прямых HTTP-запросов к login.vk.com.
 *
 * **Deep fallback:** Direct Auth — phone+password через POST
 * oauth.vk.com/access_token с grant_type=password, client_id=2274003 (VK Android).
 * Возвращает access_token + user_secret → sig. Подвержен flood_control.
 * Доступен через "Вход по паролю" на landing screen.
 *
 * См. [WebTokenAuth] (localStorage token reading),
 *     [AuthViewModel.submitWebToken] (обработка результата),
 *     [ExchangeAuthRepository.signIn] (Direct Auth deep fallback),
 *     [OAuthWebViewActivity] (устаревший OAuth flow).
 */
class AuthActivity : ComponentActivity() {

    @Suppress("DEPRECATION")
    private val viewModel: AuthViewModel by viewModels {
        viewModelFactory {
            initializer {
                val app = VkMintApp.get(this@AuthActivity)
                AuthViewModel(app.exchangeAuthRepository)
            }
        }
    }

    /**
     * Fix #BROWSER-FINISH: BroadcastReceiver для внешнего сигнала «токен получен».
     *
     * Когда пользователь нажимает «Поделиться» → VK Mint в браузере, система
     * поднимает MainActivity (root task) через onNewIntent. MainActivity сохраняет
     * токен через saveOAuthTokenFromPayload и затем рассылает ACTION_AUTH_SUCCESS.
     * AuthActivity ловит этот broadcast и завершает себя с RESULT_OK — иначе она
     * остаётся в back-стеке над MainActivity и пользователь видит экран авторизации
     * вместо главного экрана.
     *
     * Аналогичная логика применяется при clipboard-detection в onWindowFocusChanged
     * (если tokenStorage уже заполнен — сигнал пришёл быстрее).
     */
    private val authSuccessReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == ACTION_AUTH_SUCCESS) {
                AppLog.i(TAG, "authSuccessReceiver: token saved externally (share/deep-link) — finishing with RESULT_OK (Fix #BROWSER-FINISH)")
                PendingAuthResult.clear()
                setResult(RESULT_OK)
                finish()
            }
        }
    }

    override fun onStart() {
        super.onStart()
        // Fix #BROWSER-FINISH: регистрируем receiver на время видимости Activity.
        // RECEIVER_NOT_EXPORTED: broadcast только внутри приложения (не exported).
        val filter = IntentFilter(ACTION_AUTH_SUCCESS)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(authSuccessReceiver, filter, RECEIVER_NOT_EXPORTED)
        } else {
            @Suppress("UnspecifiedRegisterReceiverFlag")
            registerReceiver(authSuccessReceiver, filter)
        }
        AppLog.d(TAG, "onStart — authSuccessReceiver registered (Fix #BROWSER-FINISH)")
    }

    override fun onStop() {
        super.onStop()
        try {
            unregisterReceiver(authSuccessReceiver)
        } catch (e: Exception) {
            AppLog.d(TAG, "onStop — authSuccessReceiver already unregistered: ${e.message}")
        }
    }

    /** ActivityResult контракт для запуска OAuthWebViewActivity.
     *  Возвращает access_token + user_id (success) или error message (cancel). */
    private val oauthWebViewLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val token = result.data?.getStringExtra(OAuthWebViewActivity.EXTRA_ACCESS_TOKEN)
            val userId = result.data?.getLongExtra(OAuthWebViewActivity.EXTRA_USER_ID, 0L) ?: 0L
            // #REMIXSID-CAPTURE / #SESSION-COOKIES (§41.22): session cookies из
            // CookieManager, захваченные в OAuthWebViewActivity после web-логина.
            // null если не найдены — AuthViewModel запустит RemixsidCapturer как
            // best-effort fallback.
            //
            // #SESSION-COOKIES: p + remixnsid критичны для cross-IP silent refresh.
            // §55 #SSO-FULL-COOKIE-SET / §57 #COOKIE-CAPTURE-UNIFY: 6 доп. кук
            // (httoken, remixnttpid, remixuacck, remixuas, remixdmgr, remixmvk-fp) —
            // VK login.vk.ru/?act=web_token валидирует полный cookie-set, без них
            // silent refresh часто падает (SSO loop §54).
            val remixsid = result.data?.getStringExtra(OAuthWebViewActivity.EXTRA_REMIXSID)
            val pCookie = result.data?.getStringExtra(OAuthWebViewActivity.EXTRA_P_COOKIE)
            val remixnsid = result.data?.getStringExtra(OAuthWebViewActivity.EXTRA_REMIXNSID)
            val httoken = result.data?.getStringExtra(OAuthWebViewActivity.EXTRA_HTTP_TOKEN)
            val remixnttpid = result.data?.getStringExtra(OAuthWebViewActivity.EXTRA_REMIX_NTTPID)
            val remixuacck = result.data?.getStringExtra(OAuthWebViewActivity.EXTRA_REMIX_UACCK)
            val remixuas = result.data?.getStringExtra(OAuthWebViewActivity.EXTRA_REMIX_UAS)
            val remixdmgr = result.data?.getStringExtra(OAuthWebViewActivity.EXTRA_REMIX_DMGR)
            val remixmvkFp = result.data?.getStringExtra(OAuthWebViewActivity.EXTRA_REMIX_MVK_FP)
            if (!token.isNullOrBlank() && userId > 0L) {
                AppLog.i(TAG, "OAuth WebView returned token for user_id=$userId, " +
                    "remixsid=${if (remixsid != null) "yes" else "no"}, " +
                    "p=${if (pCookie != null) "yes" else "no"}, " +
                    "remixnsid=${if (remixnsid != null) "yes" else "no"}, " +
                    "httoken=${if (httoken != null) "yes" else "no"}, " +
                    "nttpid=${if (remixnttpid != null) "yes" else "no"}, " +
                    "uacck=${if (remixuacck != null) "yes" else "no"}, " +
                    "uas=${if (remixuas != null) "yes" else "no"}, " +
                    "dmgr=${if (remixdmgr != null) "yes" else "no"}, " +
                    "mvkfp=${if (remixmvkFp != null) "yes" else "no"}")
                viewModel.submitOAuthToken(
                    token, userId, remixsid, pCookie, remixnsid,
                    httoken, remixnttpid, remixuacck, remixuas, remixdmgr, remixmvkFp,
                )
            } else {
                AppLog.w(TAG, "OAuth WebView RESULT_OK but no token/user_id in extras")
                viewModel.setOAuthError("Вход через WebView завершился без токена. Попробуйте ещё раз.")
            }
        } else {
            val err = result.data?.getStringExtra(OAuthWebViewActivity.EXTRA_ERROR)
            if (!err.isNullOrBlank()) {
                AppLog.w(TAG, "OAuth WebView cancelled/error: $err")
                viewModel.setOAuthError("WebView: $err")
            } else {
                AppLog.d(TAG, "OAuth WebView cancelled by user")
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        // Fix #339: в silent mode применяем transparent theme ДО super.onCreate и
        // enableEdgeToEdge — иначе windowBackground из манифеста успеет нарисоваться.
        // windowIsTranslucent=true + прозрачный фон → под Activity виден предыдущий
        // кадр MainActivity, а WebView работает в невидимом слое.
        val silentMode = intent?.getBooleanExtra(EXTRA_SILENT_MODE, false) ?: false
        if (silentMode) {
            setTheme(R.style.Theme_VkMint_Silent)
            AppLog.i(TAG, "onCreate — SILENT mode (transparent theme, Fix #339)")
        }
        // Fix #370 #LOGOUT-WEBTOKEN-CLEAR: причина запуска. "logout" →
        // WebView-шаг 1 очистит старые web_token-ключи из localStorage m.vk.ru
        // до первого обмена (см. VkAuthWebViewScreenV2.clearStoredWebTokenOnLoad).
        val authIntent = intent
        val clearStoredWebToken = if (authIntent != null) {
            authIntent.getStringExtra(EXTRA_AUTH_REASON) == "logout"
        } else {
            false
        }
        if (clearStoredWebToken) {
            AppLog.i(TAG, "onCreate — reason=logout: web_token localStorage будет очищен в WebView-шаге 1")
        }
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        AppLog.i(TAG, "onCreate — WebView m.vk.ru (primary) + Direct Auth (deep fallback)")
        // #P2-8: VkAuthWebViewFactoryState.reset() удалён вместе с мёртвым V1.

        // Fix #176 УДАЛЕН (2026-08-06, #WEBVIEW-FIRST-LOAD-HANG):
        // evictAll() основан на ЛОЖНОЙ посылке «WebView использует OkHttp client».
        // На самом деле WebView использует собственный Chromium network stack
        // (нет shouldInterceptRequest → OkHttp не перехватывает запросы WebView).
        // evictAll бесполезен для загрузки m.vk.ru, но нагружает startup
        // (Dispatchers.IO + lifecycleScope) и может ломать in-flight запросы
        // ExchangeAuthRepository (который ДЕЙСТВИТЕЛЬНО использует OkHttp).
        // Реальный фикс первого зависания — reload safety-net в polling loop
        // (см. #WEBVIEW-FIRST-LOAD-HANG ниже): если onPageStarted не сработал
        // за 6 сек (cr_ChildProcessConn failed), вызываем reload().

        // Fix #197: защита от авто-входа после logout через stale clipboard.
        // Защита активна ТОЛЬКО при явном logout (clearStoredWebToken = true).
        // При обычном старте или пересоздании AuthActivity после внешнего браузера
        // НЕ блокируем свежий токен, который пользователь только что скопировал в браузере.
        if (clearStoredWebToken) {
            try {
                val cm = getSystemService(android.content.ClipboardManager::class.java)
                if (cm != null && cm.hasPrimaryClip()) {
                    val text = cm.primaryClip?.getItemAt(0)?.coerceToText(this)?.toString()
                    if (text != null && containsOAuthToken(text)) {
                        lastProcessedClipFingerprint = text.hashCode()
                        AppLog.i(TAG, "onCreate: logout mode — clipboard already contains OAuth token — marked as processed (Fix #197).")
                    }
                }
            } catch (e: Exception) {
                AppLog.d(TAG, "onCreate: clipboard pre-check failed: ${e.message}")
            }
        }

        setContent {
            // Читаем prefs до VkMintTheme, чтобы экраны авторизации синхронизировались
            // с Material You, пользовательским акцентом и масштабом (Fix #AUTH-THEME).
            val app = VkMintApp.get(this)
            val snapshot by app.prefs.data.collectAsState(initial = null)
            val snap = snapshot

            VkMintTheme(
                darkTheme = snap?.themeDark ?: isSystemInDarkTheme(),
                dynamicColor = snap?.themeDynamic ?: false,
                monetHybrid = snap?.themeMonetHybrid ?: true,
                accentIndex = snap?.themeAccentIndex ?: 0,
                fontScale = snap?.fontScale ?: 100,
                uiScale = snap?.uiScale ?: vk.mint.data.local.VkMintPrefs.UI_SCALE_DEFAULT,
                avatarShape = snap?.avatarShape ?: vk.mint.data.model.AvatarShape.CIRCLE,
            ) {
                // Fix #107: silent re-login режим — запускается MainActivity при
                // tokenInvalidationTick, если в storage есть remixsid.
                val silentMode = intent?.getBooleanExtra(EXTRA_SILENT_MODE, false) ?: false
                if (silentMode) {
                    AppLog.i(TAG, "Silent re-login mode (Fix #107) — auto WebView, no landing")
                }
                // #LOG-FAB-DEFAULT-OFF (14.09): фолбэк до загрузки snap — false.
                val showLogFab = snap?.showLogFab ?: false
                Box(modifier = Modifier.fillMaxSize()) {
                    Surface(modifier = Modifier.fillMaxSize()) {
                        AuthScreen(
                            viewModel = viewModel,
                            silentMode = silentMode,
                            // Fix #370 #LOGOUT-WEBTOKEN-CLEAR: запуск после logout —
                            // WebView-шаг 1 очистит старые web_token-ключи.
                            clearStoredWebToken = clearStoredWebToken,
                            onSuccess = {
                                AppLog.i(TAG, "Auth success — finishing with RESULT_OK")
                                PendingAuthResult.clear()
                                setResult(RESULT_OK)
                                finish()
                            },
                            onCancel = {
                                AppLog.i(TAG, "Auth cancelled")
                                PendingAuthResult.clear()
                                setResult(RESULT_CANCELED)
                                finish()
                            },
                            onLaunchWebView = {
                                AppLog.i(TAG, "Launching OAuth WebView (vk.com web client_id=6287487)")
                                oauthWebViewLauncher.launch(
                                    Intent(this@AuthActivity, OAuthWebViewActivity::class.java)
                                )
                            },
                            // Fix #187/#190: запуск внешнего браузера (Chrome/Яндекс) для OAuth.
                            // Fix #190: redirect_uri=blank.html (vkmint://oauth НЕ работает с
                            // client_id=6287487). После входа юзер копирует URL из адресной
                            // строки браузера и вставляет в поле на LandingScreen.
                            onLaunchExternalBrowser = {
                                AppLog.i(TAG, "Launching external browser for OAuth (blank.html redirect, Fix #190)")
                                vk.mint.auth.exchange.ExternalBrowserLauncher.launch(this@AuthActivity)
                            },
                        )
                    }
                    // Floating log button — управляется настройкой showLogFab.
                    if (showLogFab) {
                        DraggableLogFab(onClick = { LogDialogState.show() })
                    }
                }
                // Global log viewer dialog — overlays the auth screen.
                LogViewerDialog()
                vk.mint.ui.components.CaptchaDialog()
            }
        }
    }

    /**
     * Fix #191: onResume — проверка токена (scenario 1).
     *
     * Сценарий 1: пользователь нажал «Войти через Яндекс/Chrome», выбрал
     * браузер, в браузере нажал «Поделиться» → VK Mint. MainActivity получил
     * share intent, сохранил токен. AuthActivity была в фоне. При возврате
     * (Recent → VK Mint) — onResume видит токен → finish(RESULT_OK).
     *
     * Сценарий 2 (clipboard auto-detection) перенесён в onWindowFocusChanged
     * (Fix #195b) — там app точно foreground и ClipboardService разрешит чтение.
     *
     * Сценарий 3 (fallback): ручная вставка через поле на LandingScreen.
     */
    override fun onResume() {
        super.onResume()
        val app = VkMintApp.get(this)

        // Сценарий 1: токен уже сохранён (через share intent в MainActivity).
        if (app.tokenStorage.hasValidToken()) {
            AppLog.i(TAG, "onResume: token already present (likely from share intent) — finishing with RESULT_OK")
            PendingAuthResult.clear()
            setResult(RESULT_OK)
            finish()
            return
        }

        // #VK-SSO-PENDING-RESULT: токен мог быть получен через shouldOverrideUrlLoading
        // пока AuthActivity была уничтожена (старый viewModel cleared → submitOAuthToken
        // не выполнился). Проверяем singleton — если есть токен, обрабатываем на НОВОМ viewModel.
        PendingAuthResult.consume()?.let { result ->
            val token = result.first
            val uid = result.second
            AppLog.i(TAG, "onResume: PendingAuthResult found — processing token (user_id=$uid) on fresh viewModel")
            viewModel.submitOAuthToken(token, uid)
            return
        }
    }

    /**
     * Safety-net очистка PendingAuthResult при finish().
     * (PendingAuthResult хранит токен SSO-входа — silent_token / direct access_token
     * из shouldOverrideUrlLoading; чистим чтобы не было авто-входа после logout.)
     */
    override fun onDestroy() {
        super.onDestroy()
        if (isFinishing) {
            AppLog.i(TAG, "onDestroy(isFinishing=true) — clearing PendingAuthResult")
            PendingAuthResult.clear()
        }
    }

    /**
     * Fix #195b: clipboard auto-detection в onWindowFocusChanged.
     *
     * Раньше (Fix #191) чтение буфера было в onResume — но Android 10+
     * блокирует чтение буфера для приложений НЕ в foreground: «ClipboardService:
     * Denying clipboard access to vk.mint.debug, application is not in focus».
     * onResume вызывается ДО того как окно получает focus → отказ.
     *
     * onWindowFocusChanged(hasFocus=true) срабатывает когда окно РЕАЛЬНО
     * получило focus — app foreground, ClipboardService разрешит чтение.
     * На Android 12+ покажется тост «App pasted from clipboard» — это
     * нормальное поведение, не ошибка.
     *
     * На Android 10+ нет permission READ_CLIPBOARD для сторонних приложений —
     * foreground-статус это и есть «разрешение» (implicit, системное).
     */
    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (!hasFocus) return

        val app = VkMintApp.get(this)

        // Сценарий 1: токен уже сохранён (через share intent в MainActivity).
        if (app.tokenStorage.hasValidToken()) {
            AppLog.i(TAG, "onWindowFocusChanged: token already present — finishing with RESULT_OK")
            setResult(RESULT_OK)
            finish()
            return
        }

        // Сценарий 2: clipboard auto-detection.
        try {
            val clipboard = getSystemService(android.content.ClipboardManager::class.java)
            if (clipboard != null && clipboard.hasPrimaryClip()) {
                val clip = clipboard.primaryClip
                if (clip != null && clip.itemCount > 0) {
                    val text = clip.getItemAt(0)?.coerceToText(this)?.toString()
                    if (text != null && containsOAuthToken(text)) {
                        // Проверяем что этот буфер мы ещё не обрабатывали.
                        val fingerprint = text.hashCode()
                        if (lastProcessedClipFingerprint != fingerprint) {
                            lastProcessedClipFingerprint = fingerprint
                            AppLog.i(TAG, "onWindowFocusChanged: OAuth token found in clipboard — auto-saving (Fix #191/#195b)")
                            val parsed = OAuthTokenUtils.parse(text)
                            if (parsed != null) {
                                AppLog.i(TAG, "onWindowFocusChanged: clipboard token parsed — user_id=${parsed.userId}, at=${OAuthTokenUtils.maskTokenPrefix(parsed.accessToken)}")
                                viewModel.submitOAuthToken(parsed.accessToken, parsed.userId)
                                return
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            AppLog.w(TAG, "onWindowFocusChanged: clipboard check failed: ${e.message}")
        }
    }

    /**
     * Запоминаем fingerprint последнего обработанного clipboard-токена,
     * чтобы не входить повторно при следующем onWindowFocusChanged с тем же
     * буфером. Без этого каждый return в app (смена фокуса) вызывал бы вход.
     */
    private var lastProcessedClipFingerprint: Int = 0

    private fun containsOAuthToken(text: String): Boolean = OAuthTokenUtils.containsOAuthToken(text)

    private fun extractOAuthPayload(text: String): String? = OAuthTokenUtils.extractOAuthPayload(text)

    /** Возвращает (userId, accessToken) или null. */
    private fun parsePayloadToToken(payload: String): Pair<Long, String>? = OAuthTokenUtils.parsePayloadToToken(payload)

    companion object {
        const val TAG = "AuthActivity"

        /**
         * Fix #107: Extra для silent re-login режима.
         *
         * Когда MainActivity ловит tokenInvalidationTick (error 5/1117),
         * и в storage есть сохранённый remixsid (Fix #106 сохранил его) —
         * AuthActivity запускается с этим extra=true.
         *
         * В silent mode:
         *   - LANDING screen пропускается, сразу WEBVIEW
         *   - VkAuthWebViewScreen автоматически запускает submitWebToken
         *     при обнаружении remixsid в CookieManager
         *   - Пользователь НЕ видит форму логина — всё происходит в фоне
         *
         * Если silent re-login не удаётся (remixsid тоже устарел) —
         * AuthActivity возвращается в LANDING для ручного входа.
         */
        const val EXTRA_SILENT_MODE = "silent_mode"

        /**
         * Fix #370 #LOGOUT-WEBTOKEN-CLEAR: причина запуска AuthActivity.
         * MainActivity ставит "logout" в logout-пути (launchAuth). При
         * reason="logout" WebView-шаг 1 (VkAuthWebViewScreenV2) удаляет ВСЕ
         * ключи *:web_token:login:auth из localStorage m.vk.ru ДО первого
         * обмена — иначе валидный web_token предыдущего аккаунта оставался
         * в localStorage и следующий логин мог «молча» вернуть токен старого
         * аккаунта (WebTokenAuth.tryReadWebToken берёт первый валидный ключ).
         */
        const val EXTRA_AUTH_REASON = "auth_reason"

        /**
         * Fix #BROWSER-FINISH: action для broadcast-сигнала «OAuth токен получен».
         *
         * MainActivity рассылает этот broadcast после успешного сохранения токена
         * из share-intent или deep-link. AuthActivity слушает его через
         * authSuccessReceiver и вызывает finish(RESULT_OK), освобождая стек.
         *
         * Broadcast приватный (не exported, отправляется через sendBroadcast
         * с явным packageName) — внешние приложения не могут его имитировать.
         */
        const val ACTION_AUTH_SUCCESS = "vk.mint.action.AUTH_SUCCESS"

        fun launch(activity: Activity) {
            activity.startActivity(Intent(activity, AuthActivity::class.java))
        }
    }
}

private enum class AuthPhase { LANDING, WEBVIEW, TWO_FA, CREDENTIALS, SUCCESS }

/** Результат проверки внешнего браузера — определяется один раз при показе LandingScreen. */
private data class BrowserAuthCheck(
    val found: Boolean,
    val remixsid: String?,
    val source: String?,
)

// WebView m.vk.ru — PRIMARY auth flow.
// Пользователь логинится в WebView → CookieManager получает remixsid →
// m.vk.ru JS автоматически получает web_token через login.vk.com и
// сохраняет его в localStorage → WebTokenAuth.fullAuthFlow() читает
// токен из localStorage → токен работает со всеми VK API methods без sig.
//
// Direct Auth (phone+password → oauth.vk.com/access_token, client_id=2274003) —
// DEEP FALLBACK, доступен через "Вход по паролю" на landing screen.

@Composable
private fun AuthScreen(
    viewModel: AuthViewModel,
    onSuccess: () -> Unit,
    onCancel: () -> Unit,
    onLaunchWebView: () -> Unit,
    silentMode: Boolean = false,
    // Fix #370 #LOGOUT-WEBTOKEN-CLEAR: запуск после logout (EXTRA_AUTH_REASON=
    // "logout") — WebView-шаг 1 удаляет старые *:web_token:login:auth ключи
    // из localStorage m.vk.ru до первого обмена (см. VkAuthWebViewScreenV2).
    clearStoredWebToken: Boolean = false,
    // Fix #187: запуск внешнего браузера (Chrome/Яндекс) для OAuth.
    // AuthActivity передаёт сюда лямбду, которая вызывает ExternalBrowserLauncher.
    onLaunchExternalBrowser: () -> Unit = {},
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current

    // Fix #190: показываем поле «Вставьте ссылку» после запуска внешнего
    // браузера. Пользователь копирует URL из адресной строки браузера
    // (https://oauth.vk.com/blank.html#access_token=...) и вставляет сюда.
    var showTokenPaste by rememberSaveable { mutableStateOf(false) }
    var tokenPasteError by rememberSaveable { mutableStateOf<String?>(null) }
    // Fix #191: ручная вставка — свёрнутая секция (fallback, если Share не сработал).
    var showManualPaste by rememberSaveable { mutableStateOf(false) }

    // Fix #107: в silent mode пропускаем LANDING — сразу WEBVIEW.
    // MainActivity запускает AuthActivity с EXTRA_SILENT_MODE=true когда
    // tokenInvalidationTick сработал и есть сохранённый remixsid (Fix #106).
    var phase by rememberSaveable(silentMode) {
        mutableStateOf(if (silentMode) AuthPhase.WEBVIEW else AuthPhase.LANDING)
    }
    var lastValidation by remember { mutableStateOf<AuthState.NeedValidation?>(null) }
    val scope = rememberCoroutineScope()

    // Auto-check CookieManager на remixsid — если пользователь ранее логинился
    // в WebView внутри VK Mint, сессия найдётся → авто-переход в WEBVIEW для обмена.
    // На Android 7+ почти всегда false (cookies изолированы от настоящих браузеров).
    var browserAuth by remember {
        mutableStateOf<BrowserAuthCheck?>(BrowserAuthCheck(found = false, remixsid = null, source = null))
    }
    LaunchedEffect(Unit) {
        // Fix #107: в silent mode пропускаем — phase уже WEBVIEW, remixsid в storage.
        if (silentMode) {
            AppLog.d("AuthActivity", "Silent mode — skipping browser auth check (already in WEBVIEW phase)")
            return@LaunchedEffect
        }
        val result = withContext(Dispatchers.IO) {
            val ext = ExternalBrowserAuth.tryFindExistingAuth()
            BrowserAuthCheck(ext.found, ext.remixsid, ext.source)
        }
        browserAuth = result
        if (result.found) {
            AppLog.i("AuthActivity", "Сессия VK найдена в CookieManager (${result.source}) — автоматически входим через WebView")
            phase = AuthPhase.WEBVIEW
        } else {
            AppLog.d("AuthActivity", "CookieManager не содержит remixsid — это нормально на Android 7+ (cookies изолированы).")
        }
    }


    LaunchedEffect(state) {
        when (state) {
            is AuthState.NeedValidation -> {
                lastValidation = state as AuthState.NeedValidation
                phase = AuthPhase.TWO_FA
            }
            is AuthState.Success -> {
                phase = AuthPhase.SUCCESS
                onSuccess()
            }
            is AuthState.Idle -> {
                // User tapped "Back" from 2FA or Credentials -> return to landing.
                if (phase == AuthPhase.TWO_FA || phase == AuthPhase.CREDENTIALS) phase = AuthPhase.LANDING
            }
            is AuthState.Error -> {
                // Fix #107 → #AUTH-TOAST-STATUS (2026-09-24): silent re-login не удался
                // (remixsid устарел, либо renderer WebView мёртв — см. #BLACKSCREEN-RENDERER).
                // Раньше: fallback to LANDING — при мёртвом renderer это вечный чёрный
                // экран (WebView видимый, страницы нет). Теперь: Toast со статусом +
                // закрытие невидимой AuthActivity — юзер остаётся в Main с боковой
                // панелью/навигацией (guest-режим), вход вручную — из меню.
                if (silentMode && phase == AuthPhase.WEBVIEW) {
                    val errKind = (state as AuthState.Error).kind
                    AppLog.w("AuthActivity", "Silent re-login failed ($errKind) — Toast + close (Main остаётся юзабельным)")
                    try {
                        android.widget.Toast.makeText(
                            context,
                            "Не удалось обновить авторизацию ($errKind) — войдите вручную из бокового меню",
                            android.widget.Toast.LENGTH_LONG,
                        ).show()
                    } catch (e: Exception) {
                        AppLog.w("AuthActivity", "Toast display failed: ${e.message}")
                    }
                    onCancel()
                }
            }
            is AuthState.OfflineWithCache -> {
                // #NETWORK-RESILIENCE (2026-08-04): offline-first вход.
                // Токен протух, но сеть недоступна → silent refresh невозможен.
                // НЕ показываем LANDING (бесполезно — нет сети для логина).
                // Завершаем AuthActivity с RESULT_OK в offline-режиме —
                // MainActivity откроется, покажет кэшированную ленту + баннер.
                // При появлении сети — AuthActivity будет перезапущена (см.
                // NetworkObserver listener в MainActivity) и silent refresh
                // обновит токен без участия пользователя.
                val offline = state as AuthState.OfflineWithCache
                AppLog.i("AuthActivity", "OfflineWithCache — entering offline mode " +
                    "(user=${offline.cachedUserId}, tokenExpiredAt=${offline.tokenExpiredAt}). " +
                    "AuthActivity finishing with RESULT_OK; MainActivity will show cached data.")
                phase = AuthPhase.SUCCESS
                onSuccess()
            }
            else -> { /* Loading: keep current phase */ }
        }
    }

    // Fix #384 #AUTH-SILENT-STEALTH: в SILENT-режиме во время автоматической
    // WEBVIEW-фазы весь контент (Surface + WebView + статус-оверлей «Сессия
    // найдена, получаем токен…») рисуется с alpha=0 — юзер видит предыдущий
    // кадр MainActivity/сплеш, а не чёрный экран.
    // Root-cause чёрного фона: прозрачная тема Fix #339 (Theme.VkMint.Silent)
    // делала ОКНО прозрачным, но M3 Surface внутри setContent красил весь
    // экран colorScheme.surface (тёмная тема ≈ чёрный), а WebView + оверлей
    // статуса рисовались поверх — юзер часами смотрел на чёрный экран с
    // «Сессия найдена…» (логкат 13:09:35→13:11:19, 104 секунды тишины).
    // Alpha=0 держит дерево ЖИВЫМ (WebView грузит m.vk.ru, cookies/JS работают,
    // polling remixsid идёт), но полностью невидимым.
    // Провал silent (timeout 30с / AuthState.Error) → phase=LANDING → alpha=1
    // → юзер видит экран ручного входа. TWO_FA тоже виден. Успех → finish().
    val silentStealth = silentMode && phase == AuthPhase.WEBVIEW
    Box(modifier = Modifier.fillMaxSize().imePadding().alpha(if (silentStealth) 0f else 1f)) {
        when (phase) {
            // Fix #199: systemBarsPadding — иначе контент LandingScreen (и
            // шестерёнка в TopEnd) «ныряет» под status bar при edge-to-edge,
            // и тап по шестерёнке перехватывается системной панелью.
            AuthPhase.LANDING -> Box(modifier = Modifier.fillMaxSize().systemBarsPadding()) {
                LandingScreen(
                    state = state,
                    // #VKAUTH-V2 Primary CTA: переход к WebView m.vk.ru.
                    onStartWebView = { phase = AuthPhase.WEBVIEW },
                    // Fix #190: при запуске внешнего браузера показываем подсказку
                    // «Поделиться → VK Mint» (Fix #191 — основной способ).
                    onLaunchExternalBrowser = {
                        onLaunchExternalBrowser()
                        showTokenPaste = true
                        tokenPasteError = null
                        showManualPaste = false
                    },
                    // Fix #190/#191: подсказка Share + свёрнутая ручная вставка.
                    showTokenPaste = showTokenPaste,
                    tokenPasteError = tokenPasteError,
                    onPasteToken = { pasted ->
                        val parsed = parseExternalBrowserToken(pasted)
                        if (parsed == null) {
                            tokenPasteError = "Не удалось найти access_token и user_id в вставленном тексте.\n" +
                                "Скопируйте полный URL из адресной строки браузера " +
                                "(начинается с https://oauth.vk.com/blank.html#...)."
                        } else {
                            tokenPasteError = null
                            AppLog.i("AuthActivity", "External browser token parsed: user_id=${parsed.userId}, token=${parsed.accessToken.take(12)}...")
                            viewModel.submitOAuthToken(parsed.accessToken, parsed.userId)
                        }
                    },
                    onCancelTokenPaste = {
                        showTokenPaste = false
                        tokenPasteError = null
                        showManualPaste = false
                    },
                    showManualPaste = showManualPaste,
                    onShowManualPaste = { showManualPaste = true },
                    onStartDirectAuth = { phase = AuthPhase.CREDENTIALS },
                    modifier = Modifier.fillMaxSize(),
                )
                // Fix #189: шестерёнка с настройками VK доменов — доступна
                // ДО авторизации. Размещена в правом верхнем углу LandingScreen.
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 8.dp, end = 4.dp),
                ) {
                    vk.mint.auth.exchange.AuthDomainsSettingsIcon()
                }
            }
            AuthPhase.CREDENTIALS -> DirectAuthForm(
                state = state,
                onSubmit = { login, password -> viewModel.submitCredentials(login, password) },
                onBack = {
                    viewModel.cancel()
                    phase = AuthPhase.LANDING
                },
                modifier = Modifier.fillMaxSize(),
            )
            AuthPhase.WEBVIEW -> VkAuthWebViewScreenV2(
                isLoading = state is AuthState.Loading,
                // #VKID-ONLY (vk.id.md F-apply): кнопка «Войти через VK» → старт
                // WebView сразу на VK ID entry (m.vk.ru/login?app_id=6287487).
                // VK ID SDK делает silent exchange если есть remixsid, иначе показывает
                // форму входа VK ID. Авторизация ТОЛЬКО через VK ID.
                startUrl = AuthDomainsConfig.vkIdLoginUrl(),
                onTokenExchange = { remixsid, cookies, webView ->
                    // #SESSION-WEB-MECHANISM (2026-09-24): cookie-set живёт в
                    // CookieManager (единственный источник сессии) — storage-копии
                    // не ведём. Только flush на диск (#DOZE-COOKIE-FLUSH): иначе
                    // незаflush'енные ротации теряются при смерти WebView-процесса.
                    runCatching {
                        CookieManager.getInstance().flush()
                    }.onFailure { e ->
                        AppLog.w("AuthActivity", "#DOZE-COOKIE-FLUSH: flush failed: ${e.message}")
                    }
                    // remixsid найден → m.vk.ru JS получил токен. Читаем web_token.
                    viewModel.submitWebToken(remixsid, webView)
                },
                onBack = {
                    phase = AuthPhase.LANDING
                },
                onCancel = {
                    viewModel.cancel()
                    phase = AuthPhase.LANDING
                },
                onSilentTokenExchanged = { accessToken, userId ->
                    AppLog.i("AuthActivity", "silent_token exchange УСПЕШЕН → user_id=$userId, сохраняем OAuth token")
                    // #SESSION-HOLD: silent_token (VK ID/SSO) даёт токен БЕЗ remixsid.
                    // Пытаемся захватить cookie-set из CookieManager — если WebView уже
                    // установил сессию (remixsid), передаём в submitOAuthToken, чтобы
                    // Path 1.5 (silentRefreshViaRemixsid) работала при смене сети.
                    val captured = RemixsidCapturer.snapshotCookies()
                    if (captured != null) {
                        AppLog.i("AuthActivity",
                            "silent_token: cookie-set захвачен (remixsid len=${captured.remixsid.length}, " +
                                "p=${if (captured.pCookie != null) "yes" else "no"}) — Path 1.5 enabled")
                        viewModel.submitOAuthToken(
                            accessToken, userId,
                            remixsid = captured.remixsid,
                            pCookie = captured.pCookie,
                            remixnsid = captured.remixnsid,
                            httoken = captured.httoken,
                            remixnttpid = captured.remixnttpid,
                            remixuacck = captured.remixuacck,
                            remixuas = captured.remixuas,
                            remixdmgr = captured.remixdmgr,
                            remixmvkFp = captured.remixmvkFp,
                        )
                    } else {
                        AppLog.w("AuthActivity",
                            "silent_token: cookie-set НЕ найден в CookieManager — " +
                                "Path 1.5 недоступна (нужен in-app WebView login для silent network switch)")
                        viewModel.submitOAuthToken(accessToken, userId)
                    }
                },
                silentMode = silentMode,
                // Fix #370 #LOGOUT-WEBTOKEN-CLEAR: проброс причины "logout" в
                // WebView-шаг 1 — очистка старых web_token-ключей localStorage.
                clearStoredWebTokenOnLoad = clearStoredWebToken,
                modifier = Modifier.fillMaxSize(),
            )
            AuthPhase.TWO_FA -> ValidationCodeForm(
                validation = lastValidation,
                state = state,
                onSubmit = { code -> viewModel.submit2FaCode(code) },
                onResend = { via -> viewModel.resendCode(via) },
                onWithoutPassword = { viewModel.submitWithoutPassword() },
                onBack = { viewModel.cancel() },
                modifier = Modifier.fillMaxSize(),
            )
            AuthPhase.SUCCESS -> {
                // Brief splash before the activity finishes — gives the user feedback.
                Column(
                    modifier = Modifier.fillMaxSize().padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text("Вход выполнен", style = MaterialTheme.typography.headlineMedium)
                    Spacer(modifier = Modifier.height(16.dp))
                    CircularProgressIndicator()
                }
            }
        }
        // DraggableLogFab рендерится снаружи в setContent, поверх всего.
    }
}


