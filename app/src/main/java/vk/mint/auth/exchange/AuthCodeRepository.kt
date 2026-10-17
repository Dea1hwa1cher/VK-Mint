package vk.mint.auth.exchange

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import vk.mint.BuildConfig
import vk.mint.util.AppLog
import vk.mint.util.ExponentialBackoff

/**
 * Auth-code flow (VK ID «Direct Auth») — оркестрация шагов 0..4.
 *
 * Отличие от [ExchangeAuthRepository.signIn]: пользователю не показывается
 * зашитый порядок «пароль → код». Сначала спрашиваем у VK, что делать дальше
 * ([validateAccount]), и рисуем именно тот экран, который он попросил.
 *
 * Референс: Fenrir-for-VK, `DirectAuthIdVKPresenter` (Kotlin/MVVM).
 *
 * ## Границы модуля
 * Здесь только шаги ДО получения токена. Финальный обмен на access_token
 * живёт в [ExchangeAuthRepository] — он уже умеет сохранять результат,
 * обновлять exchange_token и поднимать LongPoll. Дублировать его здесь
 * было бы ошибкой.
 *
 * ## Почему `sakVersion` по умолчанию null
 * `sak_version` — это заявление «я официальный клиент VK Android такой-то
 * версии». Fenrir шлёт `15.0.2` + подменённые `VK_ANDROID_APP_VERSION_NAME`
 * (см. `Constants.kt:17-21` в его репозитории) — это антифрод-обманка.
 * Текущий `grant_type=password` у нас работает и без неё, поэтому слепо
 * копировать чужие version-строки мы не будем: параметр есть, но выключен.
 * Включать — только если VK начнёт отклонять запросы без него.
 */
class AuthCodeRepository(
    private val api: ExchangeAuthApi,
    private val storage: ExchangeTokenStorage,
) {

    // =====================================================================
    // Шаг 0 — анонимный токен
    // =====================================================================

    /**
     * Актуальный анонимный токен: из кэша, иначе запросить у VK.
     *
     * Токен короткоживущий, поэтому сверяем `expired_at` с запасом в минуту
     * (см. [AnonymToken.isValid]) — иначе можно отправить в `validateAccount`
     * токен, протухший прямо в полёте.
     *
     * @throws Exception с human-readable сообщением, если VK отказал или сеть недоступна.
     */
    suspend fun anonymToken(forceRefresh: Boolean = false): AnonymToken {
        if (!forceRefresh) {
            storage.anonymToken()?.let { if (it.isValid()) return it }
        }
        val json = withContext(Dispatchers.IO) {
            ExponentialBackoff.retryOnTransient(
                strategy = ExponentialBackoff.AUTH_DEFAULT,
                tag = "get_anonym_token",
            ) {
                api.getAnonymToken(storage.deviceId())
            } ?: throw IllegalStateException("get_anonym_token: VK не ответил")
        }
        val parsed = AuthCodeParser.anonymToken(json)
            ?: throw IllegalStateException("get_anonym_token: в ответе нет токена")
        storage.saveAnonymToken(parsed)
        AppLog.i(TAG, "get_anonym_token: получен, expires_at=${parsed.expiresAt}")
        return parsed
    }

    // =====================================================================
    // Шаг 1 — «что делать дальше»
    // =====================================================================

    /**
     * Спрашивает у VK следующий шаг входа.
     *
     * @param login телефон, email или логин.
     * @return `flow_name` + `sid` + `next_step` — либо `null`, если ответ не распознан.
     * @throws Exception если VK вернул ошибку или сеть недоступна.
     */
    suspend fun validateAccount(login: String): ValidateAccountResult {
        val token = anonymToken()
        val json = withContext(Dispatchers.IO) {
            api.validateAccount(
                login = login,
                anonymToken = token.token,
                deviceId = storage.deviceId(),
            )
        }
        val result = AuthCodeParser.validateAccount(json)
        if (result == null) {
            AppLog.e(TAG, "auth.validateAccount: нераспознанный ответ ${json.toString().take(300)}")
            throw IllegalStateException("auth.validateAccount: не удалось разобрать ответ VK")
        }
        AppLog.i(
            TAG,
            "auth.validateAccount: flow=${result.flowName} next=${result.nextStepMethod} " +
                "alternatives=${result.hasAnotherMethods} sid=${result.sid.take(6)}…",
        )
        return result
    }

    // =====================================================================
    // Шаг 2 — способы подтверждения
    // =====================================================================

    /**
     * Список способов подтверждения, отсортированный по приоритету VK.
     *
     * Возвращает пустой список, если метод недоступен или ответ пуст —
     * вызывающий код тогда показывает один `next_step.verification_method`
     * вместо экрана выбора.
     */
    suspend fun verificationMethods(sid: String): List<VerificationMethod> {
        val token = anonymToken()
        val json = withContext(Dispatchers.IO) {
            api.getEcosystemVerificationMethods(sid, token.token, storage.deviceId())
        }
        val methods = AuthCodeParser.verificationMethods(json)
        AppLog.i(TAG, "ecosystem.getVerificationMethods: ${methods.map { it.name }}")
        return methods
    }

    // =====================================================================
    // Шаг 3 — отправка кода
    // =====================================================================

    /** Отправить код выбранным способом (`sms`, `push`, `callreset`, `email`, …). */
    suspend fun sendOtp(method: String, sid: String): OtpSendResult {
        val token = anonymToken()
        val json = withContext(Dispatchers.IO) {
            api.sendEcosystemOtp(method, sid, token.token, storage.deviceId())
        }
        val result = AuthCodeParser.otpSend(json)
        AppLog.i(
            TAG,
            "ecosystem.sendOtp${api.ecosystemMethodSuffix(method)}: ok, " +
                "code_length=${result.codeLength} info=${result.info.orEmpty()}",
        )
        return result
    }

    // =====================================================================
    // Шаг 4 — проверка кода
    // =====================================================================

    /**
     * Шаг 4 — проверить введённый код.
     *
     * Возвращает новый `sid` (он меняет форму — см. [AuthCodeSession]) и
     * `can_skip_password`: если `true`, пароль спрашивать не нужно и можно
     * сразу обменивать токен (шаг 5).
     */
    suspend fun checkOtp(
        method: String,
        code: String,
        session: AuthCodeSession,
    ): OtpCheckResult {
        val token = anonymToken()
        val json = withContext(Dispatchers.IO) {
            api.checkEcosystemOtp(
                method = method,
                code = code,
                sid = session.sidForNextCall(),
                anonymToken = token.token,
                deviceId = storage.deviceId(),
            )
        }
        val result = AuthCodeParser.otpCheck(json)
        AppLog.i(
            TAG,
            "ecosystem.checkOtp: ok, can_skip_password=${result.canSkipPassword} " +
                "auth_hash=${result.authHash?.take(8)}…",
        )
        return result
    }

    // =====================================================================
    // Шаг 5 — финальный обмен на access_token
    // =====================================================================

    /**
     * Обменяет результат проверки на access_token.
     *
     * `grant_type` выбирается по тому, что сервер уже подтвердил:
     *  - [GrantType.PASSWORD] — пользователь ввёл пароль (flow `need_password*`);
     *  - [GrantType.WITHOUT_PASSWORD] — код принят и `can_skip_password=true`;
     *  - [GrantType.PHONE_CONFIRMATION_SID] — код принят, но VK требует пароль.
     *
     * Возвращает сырой результат: успешный токен обязан сохранить
     * [ExchangeAuthRepository] — он умеет ещё exchange_token, LongPoll и
     * верификацию через users.get, дублировать это здесь нельзя.
     */
    suspend fun exchangeToken(
        grantType: String,
        login: String,
        session: AuthCodeSession,
        password: String? = null,
        code: String? = null,
    ): AuthCodeTokenResult {
        val token = anonymToken()
        // Диагностика: печатаем длину, НЕ сам пароль. Пустой или обрезанный
        // пароль VK трактует как неверный и увеличивает счётчик bruteforce.
        AppLog.i(
            TAG,
            "oauth/token: grant=$grantType sid=${session.sidForNextCall().take(10)}… " +
                "pass=${if (password.isNullOrEmpty()) "ПУСТО" else "len=${password.length}"} " +
                "auth_hash=${if (session.authHash.isNullOrBlank()) "нет" else "есть"} " +
                "sak=${BuildConfig.VK_SAK_VERSION}",
        )
        val json = withContext(Dispatchers.IO) {
            api.authCodeToken(
                grantType = grantType,
                login = login,
                anonymToken = token.token,
                session = session,
                deviceId = storage.deviceId(),
                password = password,
                code = code,
            )
        }
        val result = AuthCodeParser.authCodeToken(json)
        if (result.isSuccess) {
            AppLog.i(TAG, "oauth/token: токен получен, user_id=${result.userId}")
        } else {
            AppLog.w(
                TAG,
                "oauth/token: отказ — ${result.error} type=${result.errorType} " +
                    "grant=$grantType sak=${if (BuildConfig.VK_SAK_VERSION.isBlank()) "нет" else BuildConfig.VK_SAK_VERSION} " +
                    "${result.errorDescription.orEmpty()}",
            )
        }
        return result
    }

    /** Значения `grant_type` финального обмена (совпадают с легаси-названиями). */
    object GrantType {
        const val PASSWORD = "password"
        const val WITHOUT_PASSWORD = "without_password"
        const val PHONE_CONFIRMATION_SID = "phone_confirmation_sid"
    }

    /** Сбросить кэш анонимного токена — например, после logout. */
    fun clearAnonymToken() = storage.clearAnonymToken()

    private companion object {
        const val TAG = "AuthCodeRepo"
    }
}
