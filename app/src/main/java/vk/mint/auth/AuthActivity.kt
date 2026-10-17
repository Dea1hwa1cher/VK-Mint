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

private enum class AuthPhase { CREDENTIALS, TWO_FA, VK_ID_WEB, SUCCESS }

@Composable
private fun AuthScreen(
    viewModel: AuthViewModel,
    onSuccess: () -> Unit,
    onCancel: () -> Unit,
    silentMode: Boolean = false,
    clearStoredWebToken: Boolean = false,
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current

    var phase by rememberSaveable(silentMode) {
        mutableStateOf(AuthPhase.CREDENTIALS)
    }
    var lastValidation by remember { mutableStateOf<AuthState.NeedValidation?>(null) }

    LaunchedEffect(Unit) {
        if (silentMode) {
            AppLog.i("AuthActivity", "Silent mode — attempting silent auto-login")
            viewModel.tryAutoLogin()
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
                if (phase == AuthPhase.TWO_FA) phase = AuthPhase.CREDENTIALS
            }
            is AuthState.Error -> {
                if (silentMode) {
                    val errKind = (state as AuthState.Error).kind
                    AppLog.w("AuthActivity", "Silent re-login failed ($errKind) — Toast + close")
                    try {
                        android.widget.Toast.makeText(
                            context,
                            "Не удалось обновить авторизацию ($errKind) — войдите вручную",
                            android.widget.Toast.LENGTH_LONG,
                        ).show()
                    } catch (e: Exception) {
                        AppLog.w("AuthActivity", "Toast display failed: ${e.message}")
                    }
                    onCancel()
                }
            }
            is AuthState.OfflineWithCache -> {
                val offline = state as AuthState.OfflineWithCache
                AppLog.i("AuthActivity", "OfflineWithCache — entering offline mode " +
                    "(user=${offline.cachedUserId}, tokenExpiredAt=${offline.tokenExpiredAt}).")
                phase = AuthPhase.SUCCESS
                onSuccess()
            }
            else -> { /* Loading: keep current phase */ }
        }
    }

    Box(modifier = Modifier.fillMaxSize().imePadding().systemBarsPadding()) {
        when (phase) {
            AuthPhase.CREDENTIALS -> {
                DirectAuthForm(
                    state = state,
                    onSubmit = { login, password -> viewModel.submitCredentials(login, password) },
                    onBack = onCancel,
                    onOpenVkId = { phase = AuthPhase.VK_ID_WEB },
                    initialLogin = viewModel.savedPhone,
                    modifier = Modifier.fillMaxSize(),
                )
                // Шестерёнка с настройками VK доменов / прокси
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 8.dp, end = 4.dp),
                ) {
                    vk.mint.auth.exchange.AuthDomainsSettingsIcon()
                }
            }
            AuthPhase.TWO_FA -> ValidationCodeForm(
                validation = lastValidation,
                state = state,
                onSubmit = { code -> viewModel.submit2FaCode(code) },
                onResend = { via -> viewModel.resendCode(via) },
                onWithoutPassword = { viewModel.submitWithoutPassword() },
                onBack = {
                    viewModel.cancel()
                    phase = AuthPhase.CREDENTIALS
                },
                onOpenVkId = { phase = AuthPhase.VK_ID_WEB },
                modifier = Modifier.fillMaxSize(),
            )
            AuthPhase.VK_ID_WEB -> VkAuthWebViewScreenV2(
                isLoading = state is AuthState.Loading,
                startUrl = AuthDomainsConfig.vkIdLoginUrl(),
                onTokenExchange = { remixsid, _, webView ->
                    runCatching {
                        CookieManager.getInstance().flush()
                    }.onFailure { e ->
                        AppLog.w("AuthActivity", "#DOZE-COOKIE-FLUSH: flush failed: ${e.message}")
                    }
                    viewModel.submitWebToken(remixsid, webView)
                },
                onBack = {
                    phase = AuthPhase.CREDENTIALS
                },
                onCancel = {
                    viewModel.cancel()
                    phase = AuthPhase.CREDENTIALS
                },
                onSilentTokenExchanged = { accessToken, userId ->
                    AppLog.i("AuthActivity", "silent_token exchange УСПЕШЕН → user_id=$userId, сохраняем OAuth token")
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
                clearStoredWebTokenOnLoad = clearStoredWebToken,
                modifier = Modifier.fillMaxSize(),
            )
            AuthPhase.SUCCESS -> {
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
    }
}


