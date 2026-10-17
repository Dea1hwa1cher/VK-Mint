package vk.mint.auth

import android.webkit.WebView
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import vk.mint.auth.exchange.AuthCodeRepository
import vk.mint.auth.exchange.AuthCodeSession
import vk.mint.auth.exchange.AuthCodeTokenResult
import vk.mint.auth.exchange.AuthErrorKind
import vk.mint.auth.exchange.VkAuthApiException
import vk.mint.auth.exchange.VerificationMethod
import vk.mint.auth.exchange.AuthResult
import vk.mint.auth.exchange.AuthState
import vk.mint.auth.exchange.ExchangeAuthRepository
import vk.mint.auth.exchange.LongPollCredentials
import vk.mint.auth.exchange.ValidationType
import vk.mint.auth.exchange.WebTokenAuth
import vk.mint.util.AppLog

/**
 * UI state + intent handler for [AuthActivity].
 *
 * Fully mirrors decompiled VK 8.178 auth flow:
 *   - AuthModel (interface) → coordinates auth state transitions
 *   - VkAuthState builders → grant_type selection
 *   - AuthByExchangeToken → actual HTTP calls
 *
 * State machine:
 *   Idle → Loading → Success | NeedValidation | Error
 *   NeedValidation → Loading → Success | Error | NeedValidation (re-send)
 *
 * On app start, tries trusted_hash re-login (mirrors VK's auto-login).
 * After Success, prefetches LongPoll credentials for messenger.
 */
class AuthViewModel(
    private val repo: ExchangeAuthRepository,
) : ViewModel() {

    private val _state = MutableStateFlow<AuthState>(AuthState.Idle)
    val state: StateFlow<AuthState> = _state.asStateFlow()

    /** Phone captured from step 1 — needed for 2FA and re-send. */
    private var lastPhone: String = ""
    private var lastPassword: String = ""
    private var pendingSid: String = ""
    private var pendingValidationType: ValidationType? = null

    /**
     * #SESSION-WEB-MECHANISM: флаг «скрытый web-refresh при старте уже пытались».
     * Одна попытка на жизнь ViewModel — как в вебе: cookies есть → молча вошли;
     * cookies мертвы → форма логина (не дёргаем WebView на каждый resume).
     */
    private var webRefreshAttempted: Boolean = false

    /**
     * §51 #WEB-TOKEN-DEAD-SESSION-CLEAR (2026-08-05):
     * Флаг: чистили ли уже dead session (cookies+storage) в этом submitWebToken
     * вызове. Гарантирует только ОДНА чистка — иначе при каждой неудаче
     * (например ручной логин который занимает >25 сек) чистили бы снова.
     * Сбрасывается в false при новом submitWebToken вызове (новый AuthActivity).
     */
    private var cookiesClearedForRetry: Boolean = false

    /** Fix #113: Текущая асинхронная операция (submitWebToken / submitCredentials / …).
     *  Сохраняется чтобы [cancel] мог прервать реально идущий network-запрос, а не
     *  только сбросить state. Без этого submitWebToken продолжает работать в фоне
     *  даже после cancel() → state возвращается в Loading/Success и UI «подвисает»
     *  — кнопка офлайн-режима остаётся disabled, пользователь не может войти. */
    private var currentJob: kotlinx.coroutines.Job? = null

    // =====================================================================
    // Auto-login (trusted hash)
    // =====================================================================

    /**
     * Attempt silent login via trusted_hash on app start.
     * Mirrors VK's VkAutoLoginComponent / silent auth flow.
     * If this succeeds, the user never sees the login form.
     *
     * Uses tryTrustedHashLoginFullState() which returns the complete AuthState
     * with all fields properly parsed and persisted (exchange_token, scope, etc.).
     */
    /**
     * Авто-вход при старте — веб-механизм (#SESSION-WEB-MECHANISM, 2026-09-24).
     *
     * Как в веб-версии: cookies живы → «перепосещение» в скрытом WebView молча
     * обновляет токен (VK узнаёт пользователя по `p`/`remixsid`); cookies
     * мертвы → сразу форма логина. Никаких хранённых паролей/trusted_hash.
     *
     * Триггер повторной попытки при появлении сети — как раньше: флаг НЕ
     * выставляется в offline-ветке, AuthActivity/NetworkObserver перезапустит.
     */
    fun tryAutoLogin() {
        if (webRefreshAttempted) return
        if (repo.isSignedIn()) {
            AppLog.i(TAG, "Already signed in, skipping auto-login")
            return
        }

        // #NETWORK-RESILIENCE: OfflineWithCache — offline-first вход (без изменений).
        val offlineState = repo.offlineWithCacheState()
        if (offlineState != null) {
            AppLog.i(TAG, "tryAutoLogin: OFFLINE + cached session " +
                "(user=${offlineState.cachedUserId}) → OfflineWithCache")
            _state.value = offlineState
            return
        }

        // Веб-семантика: silent-вход возможен ТОЛЬКО если web-сессия (cookies)
        // есть в CookieManager. Нет cookies → форма логина, WebView не дёргаем.
        if (!repo.hasSilentReloginMeans()) {
            AppLog.d(TAG, "tryAutoLogin: нет web-сессии (cookies) в CookieManager — форма логина")
            return
        }
        webRefreshAttempted = true
        _state.value = AuthState.Loading
        currentJob = viewModelScope.launch {
            val token = withContext(Dispatchers.IO) {
                try {
                    repo.ensureFreshToken(force = true)
                } catch (e: Exception) {
                    AppLog.w(TAG, "tryAutoLogin: hidden refresh failed: ${e.message}")
                    null
                }
            }
            if (token != null) {
                val expiresAt = repo.expiresAt()
                _state.value = AuthState.Success(
                    AuthResult(
                        accessToken = token,
                        exchangeToken = repo.exchangeToken(),
                        userId = repo.userId(),
                        expiresIn = if (expiresAt > 0L)
                            (expiresAt - System.currentTimeMillis()) / 1000 else 0L,
                        scope = repo.scope(),
                    )
                )
                prefetchLongPoll()
            } else {
                AppLog.i(TAG, "tryAutoLogin: web-сессия не сработала — показываем форму логина")
                _state.value = AuthState.Idle
            }
        }
    }

    // =====================================================================
    // Auth-code flow (VK ID «Direct Auth») — шаги 0..5
    //
    // Отличие от submitCredentials ниже: здесь сначала спрашиваем у VK, что
    // делать дальше, и UI рисует нужный экран по ответу, вместо того чтобы
    // жёстко идти «пароль → код».
    // Референс: Fenrir-for-VK `DirectAuthIdVKPresenter`.
    // =====================================================================

    /**
     * Состояние auth-code сессии между шагами.
     *
     * Хранится отдельно от [_state]: тот перезаписывается каждым ответом VK,
     * а нам нужны обе формы `sid` и хеши для финального обмена.
     */
    private var codeSession = AuthCodeSession()

    /**
     * Попадали ли мы на экран пароля ПОСЛЕ успешного `ecosystem.checkOtp`.
     *
     * Определяет `grant_type` финального обмена — см.
     * [exchangeAuthCodeTokenWithPassword]. Аналог `backToPasswordOnError`
     * в Fenrir.
     */
    private var passwordAfterOtp: Boolean = false

    /**
     * До какого момента локально запрещаем повторять обмен по паролю.
     *
     * Заполняется на `error_type = password_bruteforce_attempt`. Нужен потому,
     * что каждая попытка продлевает счётчик у VK: без этого пользователь,
     * нажимающий «Войти» в цикле, только отдаляет разблокировку. Срок условный —
     * VK точной длительности не сообщает.
     */
    private var passwordBlockedUntilMs: Long = 0L

    /**
     * Шаг 0+1: получить анонимный токен и спросить у VK следующий шаг.
     *
     * На выходе [_state] = [AuthState.NeedValidation], в котором лежит
     * `flowName` / `nextStepMethod` / `supportedMethods` — по ним UI решает,
     * показать пароль, поле кода или экран выбора способа.
     */
    fun beginAccountValidation(login: String) {
        val clean = login.trim()
        if (clean.isBlank()) {
            _state.value = AuthState.Error(AuthErrorKind.INVALID_CREDENTIALS, "Введите телефон или почту")
            return
        }
        lastPhone = clean
        // Не даём фоновому ensureFreshToken каскадить релогин в скрытом WebView,
        // пока пользователь вводит учётные данные (см. nativeAuthInProgress).
        repo.nativeAuthInProgress = true
        codeSession = AuthCodeSession()
        passwordAfterOtp = false
        _state.value = AuthState.Loading
        currentJob = viewModelScope.launch {
            try {
                val code = repo.authCode
                val result = withContext(Dispatchers.IO) { code.validateAccount(clean) }
                if (result.flowName == "need_registration") {
                    _state.value = AuthState.Error(
                        AuthErrorKind.NEED_SIGNUP,
                        "Аккаунт не зарегистрирован в VK",
                    )
                    return@launch
                }
                _state.value = buildNeedValidation(result)
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                AppLog.w(TAG, "beginAccountValidation failed: ${e.message}")
                _state.value = AuthState.Error(AuthErrorKind.UNKNOWN, e.message ?: "Ошибка validateAccount")
            }
        }
    }

    /** Шаг 3: отправить код выбранным способом (обновляет `sid` и подсказку). */
    fun sendOtp(method: String) {
        if (codeSession.sidRaw.isBlank()) return
        _state.value = AuthState.Loading
        currentJob = viewModelScope.launch {
            try {
                val sent = withContext(Dispatchers.IO) {
                    repo.authCode.sendOtp(method, codeSession.sidForNextCall())
                }
                // sendOtp может вернуть sid в другой форме — принимаем как есть,
                // но полную форму сохраняем на случай если вернётся короткая.
                codeSession = codeSession.copy(
                    sidRaw = sent.sid?.takeIf { it.isNotBlank() } ?: codeSession.sidRaw,
                    method = method,
                )
                pendingSid = codeSession.sidForNextCall()
                val prev = _state.value as? AuthState.NeedValidation
                _state.value = (prev ?: defaultNeedValidation(method))
                    .copy(
                        validationSid = pendingSid,
                        nextStepMethod = method,
                        otpInfo = sent.info,
                        otpCodeLength = sent.codeLength,
                    )
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.value = AuthState.Error(AuthErrorKind.UNKNOWN, e.message ?: "Не удалось отправить код")
            }
        }
    }

    /**
     * Шаг 4: проверить код.
     *
     * При успехе либо сразу обмениваем токен (`canSkipPassword`), либо
     * оставляем пользователя на экране пароля — как решит сервер.
     */
    fun checkOtp(method: String, code: String) {
        val clean = code.trim()
        if (codeSession.sidRaw.isBlank() || clean.isBlank()) {
            _state.value = AuthState.Error(AuthErrorKind.INVALID_CREDENTIALS, "Введите код")
            return
        }
        _state.value = AuthState.Loading
        currentJob = viewModelScope.launch {
            try {
                val session = codeSession.copy(method = method)
                val checked = withContext(Dispatchers.IO) {
                    repo.authCode.checkOtp(method, clean, session)
                }
                // sid из checkOtp может прийти БЕЗ префикса loginvalidate_
                // (наблюдалось на живом прогоне) — сохраняем ровно как вернул
                // сервер, не нормализуя, иначе следующий вызов отвергнут.
                codeSession = session.copy(
                    sidAfterCheck = checked.sid,
                    authHash = checked.authHash ?: session.authHash,
                )
                pendingSid = codeSession.sidForNextCall()
                val prev = _state.value as? AuthState.NeedValidation
                val merged = (prev ?: defaultNeedValidation(method)).copy(
                    validationSid = pendingSid,
                    canSkipPassword = checked.canSkipPassword,
                    nextStepMethod = if (checked.canSkipPassword) null else ValidationMethodNames.PASSWORD,
                )
                if (checked.canSkipPassword) {
                    // Сервер сказал: пароль не нужен, обмениваем сразу (шаг 5).
                    exchangeAuthCodeToken(AuthCodeRepository.GrantType.WITHOUT_PASSWORD)
                } else {
                    // Пароль спросили после кода → финальный grant_type будет
                    // phone_confirmation_sid, а не password (см. grant_typeChoice).
                    passwordAfterOtp = true
                    _state.value = merged
                }
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.value = AuthState.Error(AuthErrorKind.INVALID_CREDENTIALS, vkAuthErrorText(e))
            }
        }
    }

    // =====================================================================
    // Шаг 5 — финальный обмен (auth-code)
    // =====================================================================

    /**
     * Обмен на access_token через `{api}/oauth/token`.
     *
     * Отличие от легаси [submitCredentials]: уходит с `anonymous_token`,
     * `flow_type=tg_flow` и sid, принятым последним вызовом `ecosystem.*`.
     *
     * Пароль передаётся для всех grant_type, где VK его ждёт, — и для
     * `password`, и для `phone_confirmation_sid` (в последнем случае sid
     * уже проверен кодом, пароль идёт как подтверждение, а не как новая
     * попытка входа).
     */
    fun exchangeAuthCodeToken(grantType: String) {
        if (codeSession.sidRaw.isBlank()) {
            _state.value = AuthState.Error(AuthErrorKind.UNKNOWN, "Сессия входа потеряна, начните заново")
            return
        }
        _state.value = AuthState.Loading
        currentJob = viewModelScope.launch {
            try {
                val result = withContext(Dispatchers.IO) {
                    repo.authCode.exchangeToken(
                        grantType = grantType,
                        login = lastPhone,
                        session = codeSession,
                        password = lastPassword.takeIf { it.isNotBlank() },
                    )
                }
                handleTokenExchangeResult(result)
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                AppLog.w(TAG, "auth-code token exchange failed: ${e.message}")
                _state.value = AuthState.Error(AuthErrorKind.UNKNOWN, e.message ?: "Не удалось получить токен")
            }
        }
    }

    /**
     * Экран пароля внутри auth-code сессии: сохраняет пароль и делает обмен (шаг 5).
     *
     * `grant_type` выбирается по ТОМУ, как мы попали на экран пароля —
     * это критично, и неочевидно:
     *
     *  - Пароль спросили сразу, без кода (`validateAccount` вернул
     *    `next_step == null` или `verification_method == "password"`) →
     *    [AuthCodeRepository.GrantType.PASSWORD]. Ни одного `ecosystem.*` вызова
     *    не было, подтверждать нечем.
     *  - Пароль спросили ПОСЛЕ успешного `ecosystem.checkOtp` →
     *    [AuthCodeRepository.GrantType.PHONE_CONFIRMATION_SID]. Sid уже
     *    проверен сервером, личность подтверждена кодом, и обмен идёт
     *    по sid-ветке.
     *
     * Почему не всегда `password`: ветка `grant_type=password` у VK защищена
     * `password_bruteforce_attempt`. На живом прогоне (17.10, 18.10) она
     * стабильно отвечала `HTTP 401 {"error":"9;Flood control"}` — счётчик
     * попыток по паролю у аккаунта исчерпан. Sid-ветка тем же запросом
     * проходит, потому что пароль там не проверяется «с нуля».
     *
     * Референс: `DirectAuthIdVKPresenter.doAuth()` — `grantType` выбирается
     * из `validateMethod?.name == "password"` / `backToPasswordOnError`.
     */
    fun exchangeAuthCodeTokenWithPassword(password: String) {
        val waitMs = passwordBlockedUntilMs - System.currentTimeMillis()
        if (waitMs > 0) {
            _state.value = AuthState.Error(
                AuthErrorKind.TOO_MANY_ATTEMPTS,
                "VK заблокировал попытки входа по паролю. Повтор доступен через " +
                    "${(waitMs + 59_999) / 60_000} мин — чаще пробовать не стоит, " +
                    "счётчик продлевается.",
            )
            return
        }
        lastPassword = password
        val grant = if (passwordAfterOtp) {
            AuthCodeRepository.GrantType.PHONE_CONFIRMATION_SID
        } else {
            AuthCodeRepository.GrantType.PASSWORD
        }
        AppLog.i(TAG, "#AUTH-CODE-FLOW: обмен паролем, grant_type=$grant (после OTP=$passwordAfterOtp)")
        exchangeAuthCodeToken(grant)
    }

    /** Разбор ответа `{api}/oauth/token`: успех сохраняем, отказы показываем. */
    private suspend fun handleTokenExchangeResult(result: AuthCodeTokenResult) {
        if (result.isSuccess) {
            val token = result.accessToken!!
            _state.value = withContext(Dispatchers.IO) {
                repo.saveOAuthToken(token, result.userId, null)
            }
            return
        }
        val error = result.error.orEmpty()
        // need_validation после обмена — VK всё ещё хоет код (например 2fa_libverify).
        if (error.contains("need_validation", ignoreCase = true)) {
            _state.value = AuthState.NeedValidation(
                validationType = ValidationType.UNKNOWN,
                validationSid = result.validationSid?.takeIf { it.isNotBlank() }
                    ?: codeSession.sidForNextCall(),
                phoneMask = null,
                nextStepMethod = ValidationMethodNames.SMS,
            )
            return
        }
        _state.value = when {
            result.isPasswordBruteforce -> {
                // Счётчик парольных попыток у аккаунта. Каждая новая попытка его
                // продлевает, поэтому гасим локальный повтор на [_PASSWORD_COOLDOWN_MS] —
                // иначе пользователь, тыкающий «Войти», только отдаляет разблокировку.
                passwordBlockedUntilMs = System.currentTimeMillis() + PASSWORD_COOLDOWN_MS
                AppLog.w(
                    TAG,
                    "#AUTH-CODE-FLOW: password_bruteforce_attempt — локальный повтор " +
                        "заблокирован на ${PASSWORD_COOLDOWN_MS / 60_000} мин",
                )
                AuthState.Error(
                    AuthErrorKind.TOO_MANY_ATTEMPTS,
                    "VK заблокировал попытки входа по паролю для этого аккаунта. " +
                        "Подождите, воспользуйтесь восстановлением пароля или войдите позже.",
                )
            }
            error.contains("captcha", true) ->
                AuthState.Error(AuthErrorKind.TOO_MANY_ATTEMPTS, "Требуется подтверждение капчи")
            error.contains("too_many", true) || error.contains("flood", true) ->
                AuthState.Error(
                    AuthErrorKind.TOO_MANY_ATTEMPTS,
                    "Слишком много попыток входа. VK временно ограничил доступ.",
                )
            error.contains("invalid_client", true) ->
                AuthState.Error(AuthErrorKind.UNKNOWN, "VK отклонил приложение. Проверьте client_id.")
            else -> AuthState.Error(
                AuthErrorKind.INVALID_CREDENTIALS,
                humanizeError(result.errorDescription ?: error.ifBlank { "Вход не удался" }),
            )
        }
    }

    /**
 * Человеческий текст для ошибки шага auth-code.
 *
 * VK отдаёт коды без расшифровки («Incorrect code: invalid code for phone»),
 * поэтому разбираем их здесь. Неизвестный код показываем с текстом VK —
 * так диагностика не теряется.
 */
    private fun vkAuthErrorText(e: Exception): String {
        if (e !is VkAuthApiException) {
            return humanizeError(e.message ?: "Неверный код подтверждения")
        }
        AppLog.w(TAG, "#AUTH-CODE-FLOW: VK отклонил шаг — code=${e.code} subcode=${e.subcode} ${e.message}")
        return when (e.code) {
            1110 -> "Неверный код подтверждения. Проверьте раскладку и попробуйте снова."
            1116 -> "Требуется подтверждение капчи."
            9 -> "VK временно ограничил число попыток входа. Подождите и повторите позже."
            1117 -> "Пароль неверный или вход временно заблокирован."
            -1 -> humanizeError(e.message ?: "VK отклонил запрос")
            else -> humanizeError(e.message ?: "Ошибка VK (${e.code})")
        }
    }

    /**
     * Шаг 2: список способов подтверждения (по приоритету VK).
     *
     * Вызывается, когда `validateAccount` сказал «способов больше одного», но
     * список пришёл отдельным вызовом. До этого пользователь видит WAITING_STEP.
     *
     * Страховка от залипания: если список не пришёл (пустой или ошибка), мы НЕ
     * оставляем его на индикаторе — показываем хотя бы `next_step`-метод, который
     * VK уже назвал в `validateAccount`. Раньше пустой список держал экран
     * «Проверяем аккаунт» бесконечно (воспроизведено18.10 00:55).
     */
    fun loadVerificationMethods() {
        val sid = codeSession.sidRaw
        if (sid.isBlank()) return
        viewModelScope.launch {
            var loaded = emptyList<VerificationMethod>()
            try {
                loaded = withContext(Dispatchers.IO) { repo.authCode.verificationMethods(sid) }
            } catch (e: Exception) {
                AppLog.w(TAG, "loadVerificationMethods failed: ${e.message}")
            }
            val methods = loaded.ifEmpty {
                // Откат на next_step: один способ подтверждения точно рабочий.
                listOfNotNull(
                    (_state.value as? AuthState.NeedValidation)?.nextStepMethod
                        ?.takeIf { it != ValidationMethodNames.PASSWORD }
                        ?.let { name ->
                            VerificationMethod(name = name, priority = 0, title = null, info = null)
                        },
                )
            }
            if (methods.isEmpty()) {
                AppLog.w(TAG, "loadVerificationMethods: ни список, ни next_step — снимаем WAITING_STEP")
                val prev = _state.value as? AuthState.NeedValidation ?: return@launch
                _state.value = prev.copy(hasAnotherMethods = false)
                return@launch
            }
            val prev = _state.value as? AuthState.NeedValidation
            _state.value = (prev ?: defaultNeedValidation(null)).copy(supportedMethods = methods)
        }
    }

    private fun buildNeedValidation(result: vk.mint.auth.exchange.ValidateAccountResult):
        AuthState.NeedValidation {
        pendingSid = result.sid
        // sid нормализуем к полной форме: ecosystem.* ждёт именно её, пока
        // checkOtp не вернёт короткую.
        codeSession = AuthCodeSession(
            sidRaw = AuthCodeSession.normalizeFull(result.sid),
            rememberHash = result.rememberHash,
        )
        val nextMethod = result.nextStepMethod
        return defaultNeedValidation(nextMethod).copy(
            validationSid = pendingSid,
            flowName = result.flowName,
            nextStepMethod = nextMethod,
            phoneMask = result.phoneMask,
            canSkipPassword = result.canSkipPassword,
            hasAnotherMethods = result.hasAnotherMethods,
        )
    }

    private fun defaultNeedValidation(method: String?): AuthState.NeedValidation =
        AuthState.NeedValidation(
            validationType = ValidationType.UNKNOWN,
            validationSid = pendingSid,
            phoneMask = lastPhoneMask(),
            nextStepMethod = method,
        )

    private fun lastPhoneMask(): String? = lastPhone.takeIf { it.length >= 4 }?.let {
        it.take(1) + "******" + it.takeLast(2)
    }

    /** Имена способов подтверждения, которые VK использует в полях. */
    object ValidationMethodNames {
        const val PASSWORD = "password"
        const val SMS = "sms"
        const val PUSH = "push"
        const val EMAIL = "email"
        const val CALLRESET = "callreset"
        const val RESERVE_CODE = "reserve_code"
        const val CODEGEN = "codegen"
    }

    // =====================================================================
    // Step 1 — phone + password (legacy direct auth)
    // =====================================================================

    val savedPhone: String get() = lastPhone

    /** Submit phone + password. Mirrors VkAuthState.b(sid=null). */
    fun submitCredentials(phone: String, password: String) {
        val trimmed = phone.trim()
        val cleaned = if (!trimmed.contains("@")) trimmed.replace(Regex("[\\s\\-\\(\\)]"), "") else trimmed
        if (cleaned.isBlank() || password.isBlank()) {
            _state.value = AuthState.Error(AuthErrorKind.INVALID_CREDENTIALS, "Введите телефон и пароль")
            return
        }
        lastPhone = cleaned
        lastPassword = password
        _state.value = AuthState.Loading
        currentJob = viewModelScope.launch {
            val result = withContext(Dispatchers.IO) { repo.signIn(cleaned, password) }
            handleAuthResult(result)
        }
    }

    // =====================================================================
    // Step 2 — 2FA code
    // =====================================================================

    /** Submit 2FA code. Mirrors VkAuthState.b(sid!=null) or VkAuthState.d(z=false). */
    fun submit2FaCode(code: String) {
        if (code.isBlank() || pendingSid.isBlank()) {
            _state.value = AuthState.Error(AuthErrorKind.INVALID_CREDENTIALS, "Введите код")
            return
        }
        _state.value = AuthState.Loading
        currentJob = viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                repo.submit2FaCode(lastPhone, pendingSid, code)
            }
            handleAuthResult(result)
        }
    }

    // =====================================================================
    // Push-approved login (no password)
    // =====================================================================

    /**
     * Login without password — push was already approved.
     * Mirrors VkAuthState.d(z=true) → grant_type=without_password.
     */
    fun submitWithoutPassword() {
        if (pendingSid.isBlank()) {
            _state.value = AuthState.Error(AuthErrorKind.UNKNOWN, "Нет sid для push-авторизации")
            return
        }
        _state.value = AuthState.Loading
        currentJob = viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                repo.authWithoutPassword(lastPhone, pendingSid)
            }
            handleAuthResult(result)
        }
    }

    // =====================================================================
    // OAuth WebView token (from OAuthWebViewActivity, bypasses flood control)
    // =====================================================================

    /**
     * Submit token obtained via OAuth WebView (oauth.vk.com/authorize).
     * The token comes from VK's own login page, so no flood control.
     * We try to get exchange_token via auth.getExchangeToken for refresh support.
     *
     * #REMIXSID-CAPTURE (§41.22): [remixsid] — опциональный remixsid из
     * CookieManager, захваченный в [vk.mint.auth.OAuthWebViewActivity]
     * после in-app web-логина. Если передан (non-null) → сохраняется
     * напрямую, Path 1.5 (silentRefreshViaRemixsid) включается.
     *
     * Если null (external browser flow, или WebView не нашёл cookie) →
     * после сохранения токена CookieManager.flush() фиксирует cookie-set как
     * best-effort: скрытый WebView пытается silent OAuth sign-in и
     * захватить remixsid. Работает только если CookieManager уже имеет
     * VK session от предыдущей in-app WebView сессии.
     *
     * §55 #SSO-FULL-COOKIE-SET / §57 #COOKIE-CAPTURE-UNIFY: расширено до
     * 9 кук браузерного набора. p/remixnsid критичны для cross-IP silent
     * refresh, httoken/remixnttpid/remixuacck/remixuas/remixdmgr/remixmvk-fp —
     * anti-CSRF/anti-fraud, VK часто отвергает silent refresh без них (SSO loop §54).
     * Все 6 доп. кук опциональны (null = не переданы, storage не патчит).
     */
    fun submitOAuthToken(
        accessToken: String,
        userId: Long,
        remixsid: String? = null,
        pCookie: String? = null,
        remixnsid: String? = null,
        // §55 #SSO-FULL-COOKIE-SET: 6 доп. кук браузерного набора.
        httoken: String? = null,
        remixnttpid: String? = null,
        remixuacck: String? = null,
        remixuas: String? = null,
        remixdmgr: String? = null,
        remixmvkFp: String? = null,
    ) {
        _state.value = AuthState.Loading
        currentJob = viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                repo.saveOAuthToken(
                    accessToken, userId, remixsid, pCookie, remixnsid,
                    httoken, remixnttpid, remixuacck, remixuas, remixdmgr, remixmvkFp,
                )
            }
            handleAuthResult(result)

            // #SESSION-WEB-MECHANISM (2026-09-24): RemixsidCapturer-фоллбэк с
            // сохранением копий в storage удалён — cookies уже в CookieManager
            // (их поставила страница логина). Только flush на диск (#DOZE-COOKIE-FLUSH).
            if (result is AuthState.Success) {
                withContext(Dispatchers.Main) {
                    runCatching { android.webkit.CookieManager.getInstance().flush() }
                }
            }
        }
    }

    /**
     * PRIMARY auth method — чтение web_token из m.vk.ru localStorage.
     *
     * Запускается после того, как AuthActivity обнаружил remixsid в CookieManager.
     * m.vk.ru JS автоматически обменял remixsid на токен через login.vk.com
     * и сохранил его в localStorage. Мы читаем оттуда через evaluateJavascript().
     *
     * @param remixsid Значение remixsid (для сохранения в storage и refresh)
     * @param webView  WebView instance на m.vk.ru
     */
    fun submitWebToken(remixsid: String, webView: WebView) {
        _state.value = AuthState.Loading
        // §51 #WEB-TOKEN-DEAD-SESSION-CLEAR: сбрасываем флаг на новом вызове
        // (новый AuthActivity lifecycle — даём шанс ещё одной чистке если понадобится).
        cookiesClearedForRetry = false
        currentJob = viewModelScope.launch {
            // Fix #104: retry-цикл для истёкших токенов.
            //
            // WebTokenAuth.fullAuthFlow (Fix #103) уже отбраковывает истёкший
            // токен и удаляет его из localStorage, после чего m.vk.ru JS должен
            // получить свежий. Но JS может не сработать с первого раза (нужен
            // re-load страницы, либо сессия login.vk.com тоже устарела).
            //
            // Стратегия:
            //   1. Пытаемся fullAuthFlow. Если Success — сохраняем и выходим.
            //   2. Если saveWebTokenResult вернул EXPIRED (Fix #105) —
            //      перезагружаем m.vk.ru и пробуем снова.
            //   3. Максимум MAX_EXPIRED_RETRIES попыток, затем показываем
            //      пользователю экран логина (не зависаем на splash).
            //
            // §51 #WEB-TOKEN-DEAD-SESSION-CLEAR (2026-08-05):
            //   4. Если fullAuthFlow упал с НЕ-EXPIRED ошибкой (токен не появился
            //      за 25 сек — remixsid есть, но VK не обменивает) — чистим
            //      dead remixsid из CookieManager+storage (clearDeadSessionForRetry).
            //      Это разрывает auto-relogin loop: без чистки MainActivity
            //      перезапустит AuthActivity → снова найдёт мёртвый remixsid →
            //      цикл, пользователь никогда не увидит форму логина с 2FA.
            //      С чисткой следующий запуск AuthActivity покажет форму логина
            //      (remixsid нет) → пользователь введёт phone+pass → 2FA →
            //      новый валидный токен.
            //      НЕ делаем retry внутри submitWebToken — после очистки WebView
            //      показывает форму логина, и пользователю нужно >25 сек на
            //      ручной ввод (таймаут fullAuthFlow). Просто возвращаем Error,
            //      AuthActivity/MainActivity перезапустится с чистой сессией.
            //      Флаг cookiesClearedForRetry гарантирует только ОДНА чистка
            //      (иначе чистили бы при каждой неудаче, в т.ч. при нормальном
            //      ручном логине который занимает время).
            val maxRetries = 2
            var result: AuthState
            var attempt = 0
            do {
                attempt++
                result = withContext(Dispatchers.Main) {
                    val webResult = WebTokenAuth.fullAuthFlow(webView)
                    if (webResult.isFailure) {
                        val err = webResult.exceptionOrNull()?.message ?: "unknown error"
                        AppLog.e(TAG, "WebTokenAuth failed (attempt $attempt/$maxRetries): $err")
                        // Если ошибка содержит "истёк" — это EXPIRED, пробуем ещё раз.
                        // Иначе — другая ошибка (network, JS error), отдаём наверх.
                        val isExpired = err.contains("истёк", ignoreCase = true) ||
                            err.contains("expired", ignoreCase = true)
                        return@withContext if (isExpired && attempt < maxRetries) {
                            AuthState.Error(AuthErrorKind.EXPIRED, err)
                        } else {
                            AuthState.Error(
                                AuthErrorKind.UNKNOWN,
                                "Не удалось получить токен: $err"
                            )
                        }
                    }
                    val token = webResult.getOrThrow()
                    AppLog.i(TAG, "WebTokenAuth success — saving token for user_id=${token.userId}")

                    // Fix #WEB-TOKEN-FALLBACK (2026-08-04): remixsid может быть
                    // пустым если web_token найден через localStorage polling
                    // (CookieManager не видит remixsid на Android 7+). В этом
                    // случае передаём null чтобы НЕ перезаписать существующий
                    // remixsid в storage (если он был сохранён ранее).
                    val remixsidToSave = remixsid.takeIf { it.isNotBlank() }
                    repo.saveWebTokenResult(
                        accessToken = token.accessToken,
                        userId = token.userId,
                        expiresAt = token.expiresAt,
                        satToken = token.satToken,
                        logoutHash = token.logoutHash,
                        remixsid = remixsidToSave,
                    )
                }

                // Если токен истёк — reload m.vk.ru и retry.
                // saveWebTokenResult вернёт AuthState.Error(EXPIRED, ...) (Fix #105).
                val isExpired = result is AuthState.Error &&
                    result.kind == AuthErrorKind.EXPIRED
                if (isExpired && attempt < maxRetries) {
                    AppLog.w(TAG, "submitWebToken: токен истёк (attempt $attempt/$maxRetries) — " +
                        "reload m.vk.ru и retry")
                    withContext(Dispatchers.Main) {
                        // Очищаем localStorage чтобы m.vk.ru JS точно получил свежий токен
                        webView.evaluateJavascript(
                            "localStorage.removeItem('7879029:web_token:login:auth')",
                            null
                        )
                        // Перезагружаем страницу — это заставит m.vk.ru JS
                        // переинициализироваться и запросить новый токен.
                        webView.loadUrl("https://m.vk.ru")
                    }
                    // Даём m.vk.ru время на load + JS-обмен.
                    kotlinx.coroutines.delay(3000L)
                }

                // §51 #WEB-TOKEN-DEAD-SESSION-CLEAR: не-EXPIRED failure на
                // последней попытке — чистим dead session (CookieManager+storage),
                // чтобы следующий запуск AuthActivity показал форму логина вместо
                // auto-relogin с мёртвым remixsid. НЕ retry внутри submitWebToken —
                // после очистки пользователю нужно >25 сек на ручной ввод.
                //
                // §58 #2FA-SESSION-WIPE-FIX (2026-08-05, лог 19:47-19:48):
                // ПЕРЕД чисткой кук попробуем Path 1.5 (silentRefreshViaRemixsid).
                // КОРНЕВАЯ ПРИЧИНА сброса 2FA: после VK app SSO remixsid ВАЛИДЕН
                // (88 символов, только что получен), но m.vk.ru/feed НЕ запускает
                // VK ID SDK → web_token не появляется в localStorage → 25 сек
                // таймаут → clearDeadSessionForRetry → УДАЛЯЕТ ВСЕ КУКИ (вкл.
                // валидный remixsid) → 2FA сбрасывается, пользователь вводит заново.
                //
                // ФИКС: Path 1.5 делает HTTP POST на login.vk.ru/?act=web_token с
                // Cookie: remixsid=... → VK вернёт access_token за 1-2 сек, БЕЗ
                // сброса 2FA сессии. ensureFreshToken(force=true) сохраняет токен
                // в storage. Если Path 1.5 успешен — возвращаем Success, куки НЕ
                // трогаем. Если Path 1.5 тоже упал (remixsid действительно мёртв) —
                // тогда чистим dead session как раньше.
                val isUnknownError = result is AuthState.Error &&
                    result.kind == AuthErrorKind.UNKNOWN
                if (isUnknownError && !cookiesClearedForRetry) {
                    // #AUTH-LOOP-FIX (2026-08-07): перед Path 1.5 fallback,
                    // извлекаем userId из cookie remixsid_user и сохраняем в storage.
                    // Path 1.5 (silentRefreshViaRemixsid) требует userId для
                    // `remixsid_user=<userId>` cookie header. Без userId Path 1.5
                    // падает → clearDeadSessionForRetry → loop (пользователь видит
                    // форму логина снова и снова).
                    //
                    // Сценарий: m.vk.ru login → remixsid валиден, но WebTokenAuth
                    // таймаутит (m.vk.ru/feed редиректит, VK ID SDK не init).
                    // remixsid_user cookie УЖЕ есть в CookieManager (VK ставит его
                    // при логине) — извлекаем и сохраняем в storage.
                    if (repo.userId() == 0L) {
                        val cookieUserId = withContext(Dispatchers.Main) {
                            vk.mint.auth.getUserIdFromCookieManager()
                        }
                        var extractedUserId = cookieUserId
                        // #SSO-USERID-EXTRACT: remixsid_user cookie нет в SSO-flow.
                        // Извлекаем userId из window.init VK ID SDK через JS.
                        if (extractedUserId <= 0L) {
                            extractedUserId = try {
                                vk.mint.auth.exchange.WebTokenAuth.readUserIdFromWindowInit(webView)
                            } catch (e: Exception) {
                                AppLog.w(TAG, "submitWebToken: readUserIdFromWindowInit failed: ${e.message}")
                                0L
                            }
                        }
                        if (extractedUserId > 0L) {
                            AppLog.i(TAG, "submitWebToken: #AUTH-LOOP-FIX — извлёк userId=$extractedUserId " +
                                "(${if (cookieUserId > 0L) "cookie" else "window.init"}), сохраняю в storage перед Path 1.5")
                            withContext(Dispatchers.IO) {
                                repo.setUserId(extractedUserId)
                            }
                        } else {
                            AppLog.w(TAG, "submitWebToken: #AUTH-LOOP-FIX — userId=0 в storage и " +
                                "ни в cookie remixsid_user, ни в window.init. Path 1.5 скорее " +
                                "всего упадёт (нет userId для remixsid_user cookie header).")
                        }
                    }
                    // §58: Path 1.5 fallback ПЕРЕД чисткой кук.
                    val path15Token = withContext(Dispatchers.IO) {
                        try {
                            repo.ensureFreshToken(force = true)
                        } catch (e: Exception) {
                            AppLog.w(TAG, "submitWebToken: §58 Path 1.5 fallback exception: ${e.message}")
                            null
                        }
                    }
                    if (path15Token != null) {
                        // Path 1.5 УСПЕШЕН — remixsid обменян на access_token.
                        // ensureFreshToken уже сохранил токен в storage.
                        // Возвращаем Success, куки НЕ трогаем (2FA сессия сохранена).
                        val p15UserId = repo.userId()
                        val p15ExpiresAt = repo.expiresAt()
                        AppLog.i(TAG, "submitWebToken: §58 #2FA-SESSION-WIPE-FIX — Path 1.5 " +
                            "WEB-MECHANISM refresh SUCCESS — cookies обменяны на " +
                            "access_token через HTTP (user_id=$p15UserId), куки НЕ очищены " +
                            "(2FA сессия сохранена, повторный ввод не требуется)")
                        result = AuthState.Success(
                            AuthResult(
                                accessToken = path15Token,
                                exchangeToken = repo.exchangeToken(),
                                userId = p15UserId,
                                expiresIn = if (p15ExpiresAt > 0L)
                                    (p15ExpiresAt - System.currentTimeMillis()) / 1000 else 0L,
                                scope = repo.scope(),
                            )
                        )
                    } else {
                        // Path 1.5 тоже упал. Различаем две причины:
                        //
                        // #VKID-SESSION-WIPE-GUARD (логкэт 2026-08-07 16:48:31):
                        //   - wasLastSilentRefreshDefinitivelyDead()=true → remixsid
                        //     ОДНОЗНАЧНО мёртв (VK вернул явный auth rejection).
                        //     Чистим dead session — единственный шанс = re-login с 2FA.
                        //   - wasLastSilentRefreshDefinitivelyDead()=false → CONTRACT
                        //     failure (wrong origin / unauthorized / parsing bug / network).
                        //     remixsid может быть ВАЛИДЕН (VK реально вернул токен, но мы
                        //     не распарсили). НЕ чистим сессию — иначе уничтожим рабочую
                        //     2FA-сессию и заставим пользователя перелогиниваться без нужды.
                        //     Возвращаем Error — пользователь может нажать "повторить".
                        val definitivelyDead = withContext(Dispatchers.IO) {
                            try { repo.wasLastSilentRefreshDefinitivelyDead() } catch (_: Exception) { false }
                        }
                        if (definitivelyDead) {
                            AppLog.w(TAG, "submitWebToken: §58 Path 1.5 упал — remixsid " +
                                "definitively dead (VK explicit rejection) — clearing dead " +
                                "remixsid (CookieManager+storage). Next AuthActivity launch " +
                                "will show login form → 2FA.")
                            cookiesClearedForRetry = true
                            withContext(Dispatchers.IO) {
                                repo.clearDeadSessionForRetry()
                            }
                            // Чистим localStorage WebView от старых web_token ключей,
                            // чтобы следующий запуск не подобрал истёкший токен.
                            withContext(Dispatchers.Main) {
                                webView.evaluateJavascript(
                                    "(function(){Object.keys(localStorage)" +
                                        ".filter(function(k){return k.indexOf(':web_token:login:auth')>-1})" +
                                        ".forEach(function(k){localStorage.removeItem(k)})})();",
                                    null
                                )
                                webView.clearCache(true)
                            }
                        } else {
                            AppLog.w(TAG, "submitWebToken: §58 Path 1.5 упал по CONTRACT failure " +
                                "(wrong origin / unauthorized / parsing / network) — remixsid " +
                                "МОЖЕТ быть валиден. НЕ чистим сессию (#VKID-SESSION-WIPE-GUARD). " +
                                "Возвращаем Error — пользователь может повторить.")
                            // НЕ вызываем clearDeadSessionForRetry — сессия сохранена.
                            // НЕ чистим localStorage — web_token может появиться при retry.
                            // Возвращаем Error с понятным сообщением.
                            result = AuthState.Error(
                                AuthErrorKind.UNKNOWN,
                                "Не удалось получить токен (контракт VK изменился). " +
                                "Сессия сохранена — попробуйте ещё раз."
                            )
                        }
                    }
                }
            } while (isExpired && attempt < maxRetries)

            handleAuthResult(result)
        }
    }

    /** Show OAuth WebView error in the UI. */
    fun setOAuthError(message: String) {
        _state.value = AuthState.Error(AuthErrorKind.UNKNOWN, message)
    }

    // =====================================================================
    // Re-send 2FA code
    // =====================================================================

    /**
     * Re-send 2FA code via a different method.
     * VK allows switching between SMS, push, email, IVR.
     */
    fun resendCode(via: ValidationType) {
        if (pendingSid.isBlank()) return
        _state.value = AuthState.Loading
        currentJob = viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                repo.resendValidationCode(lastPhone, lastPassword, pendingSid, via)
            }
            // After resend, VK returns a new NeedValidation with updated sid.
            if (result is AuthState.NeedValidation) {
                pendingSid = result.validationSid
                pendingValidationType = result.validationType
            }
            _state.value = result
        }
    }

    // =====================================================================
    // Cancel
    // =====================================================================

    /** Reset back to Idle (e.g. user taps "Back" from 2FA screen или «Отмена»).
     *  Fix #113: отменяем реально идущий network-запрос, иначе coroutine
     *  продолжит работу и перезапишет state после cancel() — UI снова уйдёт
     *  в Loading и все кнопки (включая офлайн-режим) заблокируются. */
    fun cancel() {
        currentJob?.cancel()
        currentJob = null
        _state.value = AuthState.Idle
        pendingSid = ""
        lastPassword = ""
        codeSession = AuthCodeSession()
        repo.nativeAuthInProgress = false
    }

    override fun onCleared() {
        // ViewModel умер вместе с AuthActivity — фоновой refresh должен
        // снова взвесиваться, иначе приложение останется без обновления токена.
        repo.nativeAuthInProgress = false
        super.onCleared()
    }

    // =====================================================================
    // Internal
    // =====================================================================

    private fun handleAuthResult(result: AuthState) {
        if (result is AuthState.NeedValidation) {
            pendingSid = result.validationSid
            pendingValidationType = result.validationType
        }
        _state.value = result
        if (result is AuthState.Success) {
            prefetchLongPoll()
        }
    }

    /**
     * Pre-fetch LongPoll credentials in the background so the messenger
     * screen has them ready. Mirrors VK's post-auth LongPoll init.
     */
    private fun prefetchLongPoll() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val creds: LongPollCredentials? = repo.fetchLongPoll()
                if (creds != null) {
                    AppLog.i(TAG, "LongPoll prefetched (ts=${creds.ts})")
                } else {
                    AppLog.w(TAG, "LongPoll prefetch returned null — IM will retry on demand")
                }
            } catch (e: Exception) {
                AppLog.w(TAG, "LongPoll prefetch error: ${e.message}")
            }
        }
    }

    private companion object {
        const val TAG = "AuthViewModel"

        /**
         * Локальный запрет повторов после `password_bruteforce_attempt`.
         *
         * 30 минут — оценочное значение: VK длительность окна не сообщает,
         * а точная цифра была бы выдумкой. Задача — не дать продлить счётчик
         * серией нажатий, а не угадать срок разблокировки.
         */
        const val PASSWORD_COOLDOWN_MS = 30 * 60 * 1000L
    }
}