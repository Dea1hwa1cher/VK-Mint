package vk.mint.auth.exchange

import com.google.gson.JsonObject

// ═══════════════════════════════════════════════════════════════════════════
//  Auth-code flow (VK ID «Direct Auth»)
// ───────────────────────────────────────────────────────────────────────────
//  Референс: Fenrir-for-VK, `DirectAuthIdVKPresenter` + `IAuthService`.
//  Отличие от старого Direct Auth (`grant_type=password` в лоб): сначала
//  спрашиваем у VK, что делать дальше, и только потом вводим пароль/код.
//
//  Порядок вызовов:
//    1. get_anonym_token            → анонимный токен (обязателен для шагов 2..4)
//    2. auth.validateAccount        → flow_name + sid + next_step
//    3. ecosystem.getVerificationMethods (опционально) → список способов
//    4. ecosystem.sendOtp<M>        → отправка кода выбранным способом
//    5. ecosystem.checkOtp          → проверка кода → sid + can_skip_password
//    6. POST /oauth/token           → access_token (grant_type=password/
//                                     without_password/phone_confirmation_sid)
// ═══════════════════════════════════════════════════════════════════════════

/**
 * Анонимный токен (`POST {api}/oauth/get_anonym_token`).
 *
 * Живёт недолго и кэшируется — без него `validateAccount` и `ecosystem.*`
 * отвечают ошибкой. Выдан на пару с `client_secret`, поэтому в логи не пишем.
 */
data class AnonymToken(
    val token: String,
    /** Unix-секунды; после этого токен надо перезапросить. */
    val expiresAt: Long,
) {
    /** Запас 60 секунд, чтобы не слать заведомо протухший токен. */
    fun isValid(now: Long = System.currentTimeMillis() / 1000L): Boolean =
        token.isNotBlank() && expiresAt - 60L > now
}

/**
 * Способ подтверждения (`ecosystem.getVerificationMethods`).
 *
 * [priority] — VK сам ранжирует способы; UI обязан сортировать по нему,
 * а не по порядку ответа.
 */
data class VerificationMethod(
    val name: String,
    val priority: Int,
    val title: String? = null,
    val info: String? = null,
) {
    /** Способы, код от которых вводится вручную в поле «Код». */
    val needsCode: Boolean get() = name in CODE_METHODS

    companion object {
        val CODE_METHODS = setOf(
            "sms", "callreset", "email", "push", "reserve_code", "codegen",
        )
    }
}

/**
 * Ответ `auth.validateAccount` — сервер решает, что показывать дальше.
 *
 * @param flowName     `need_password` / `need_password_and_validation` /
 *                     `need_registration` / …
 * @param sid          идентификатор сессии проверки, нужен всем ecosystem.*
 * @param nextStepMethod `next_step.verification_method` — что VK просит сначала.
 * @param hasAnotherMethods есть ли альтернативные способы (нужен
 *                     `ecosystem.getVerificationMethods`).
 */
data class ValidateAccountResult(
    val flowName: String?,
    val sid: String,
    val rememberHash: String?,
    val nextStepMethod: String?,
    val hasAnotherMethods: Boolean,
    val canSkipPassword: Boolean,
    val phoneMask: String?,
) {
    // Раньше здесь было свойство needsPasswordFlow, которое возвращало true для
    // flow=need_password_and_validation независимо от next_step. Из-за этого
    // подсказка «сначала пароль» противоречила серверу, который просил код.
    // Маршрутизация живёт в routeOf() (AuthActivity) и смотрит только на
    // nextStepMethod — то есть на то, что VK назвал следующим шагом.
    companion object {
        const val METHOD_PASSWORD = "password"
    }
}

/**
 * Состояние auth-code сессии между шагами.
 *
 * Существует из-за неочевидной детали формата `sid`: после `checkOtp` VK начинает
 * ожидать sid БЕЗ префикса `loginvalidate_`, поэтому простое «перезаписать последним
 * ответом» ломает следующий вызов. Плюс два хеша для финального обмена токена.
 */
data class AuthCodeSession(
    /** Sid в полной форме — как вернул `validateAccount` / `sendOtp`. */
    val sidRaw: String = "",
    /** Sid после `checkOtp` — без префикса `loginvalidate_`, если так вернул сервер. */
    val sidAfterCheck: String? = null,
    /** Из `ecosystem.checkOtp` — участвует в обмене токена (шаг 5). */
    val authHash: String? = null,
    /** Из `auth.validateAccount` — участвует в обмене токена (шаг 5). */
    val rememberHash: String? = null,
    /** Способ подтверждения, которым сейчас проверяем. */
    val method: String? = null,
) {
    /**
     * Какой `sid` слать дальше.
     *
     * После `checkOtp` приоритет у формы без префикса — её вернул сам сервер и её же
     * будет ждать следующий вызов; до него используется полная форма.
     */
    fun sidForNextCall(): String = sidAfterCheck?.takeIf { it.isNotBlank() } ?: sidRaw

    companion object {
        const val SID_PREFIX = "loginvalidate_"

        /** Приводит sid к полной форме, если сервер вернул короткую. */
        fun normalizeFull(sid: String): String =
            if (sid.startsWith(SID_PREFIX)) sid else SID_PREFIX + sid
    }
}

/** Ответ `ecosystem.checkOtp` — код принят. */
data class OtpCheckResult(
    val sid: String?,
    val canSkipPassword: Boolean,
    val profileExist: Boolean,
    val photo200: String?,
    /** Хеш для финального обмена токена (шаг 5). */
    val authHash: String?,
)

/**
 * Ответ финального обмена — `POST {api}/oauth/token`.
 *
 * Тот же контракт, что у легаси `/access_token`, но с `anonymous_token`,
 * `auth_hash` и `remember_hash`.
 */
data class AuthCodeTokenResult(
    val accessToken: String?,
    val userId: Long,
    val expiresIn: Int,
    val error: String?,
    val errorDescription: String?,
    val validationType: String?,
    val validationSid: String?,
    /**
     * Поле `error_type` из ответа VK.
     *
     * Содержит машинное имя лимитера: `password_bruteforce_attempt`,
     * `captcha_needed` и т.п. Без него баннер рисуется по догадке.
     */
    val errorType: String? = null,
) {
    val isSuccess: Boolean get() = !accessToken.isNullOrBlank() && error.isNullOrBlank()

    /** Счётчик парольных попыток у аккаунта исчерпан. */
    val isPasswordBruteforce: Boolean
        get() = errorType?.contains("password_bruteforce", true) == true
}

/**
 * Ответ `ecosystem.sendOtp*`.
 *
 * @param info        текст от VK («Код отправлен на +7******06») — показываем as-is.
 * @param codeLength  длина кода для callreset/reserve_code — подсказка в поле ввода.
 */
data class OtpSendResult(
    val sid: String?,
    val info: String?,
    val codeLength: Int,
)

// ───────────────────────────────────────────────────────────────────────────
//  Парсинг ответов VK
// ───────────────────────────────────────────────────────────────────────────

/**
 * Ошибка ответа VK в конверте `{"error":{...}}`.
 *
 * @param code    `error_code` VK (1110 — неверный код, 9 — flood control, …).
 * @param subcode `error_subcode`, уточняет код (1122 — код не для этого телефона).
 */
class VkAuthApiException(
    val code: Int,
    val subcode: Int?,
    message: String,
) : Exception(message)

internal object AuthCodeParser {

    /**
     * Достаёт `response` или бросает [VkAuthApiException].
     *
     * Раньше здесь стояло `json.getAsJsonObject("response") ?: json`, и это была
     * тихая катастрофа: VK на отказ отвечает HTTP 200 с конвертом
     * `{"error":{"error_code":1110,…,"error_msg":"Incorrect code"}}` — поля
     * `response` там нет, поэтому «успешный» разбор возвращал объект из одних
     * значений по умолчанию. Приложение показывало «код принят», уводило на
     * пароль, и уже там VK отвечал `9;Flood control` — на сессии, которую
     * никто не подтверждал. Живой прогон 18.10 00:51 это воспроизвёл.
     */
    private fun unwrap(json: JsonObject): JsonObject {
        json.get("error")?.let { err ->
            if (err.isJsonNull) return json.getAsJsonObject("response") ?: json
            if (err.isJsonPrimitive) {
                // Формат oauth/token: {"error":"9;Flood control","error_description":"…"}
                throw VkAuthApiException(
                    code = err.asString.toIntOrNull() ?: -1,
                    subcode = null,
                    message = json.str("error_description")
                        ?.takeIf { it.isNotBlank() }
                        ?: err.asString,
                )
            }
            if (err.isJsonObject) {
                val o = err.asJsonObject
                throw VkAuthApiException(
                    code = o.int("error_code"),
                    subcode = o.int("error_subcode").takeIf { it != 0 },
                    message = o.str("error_msg") ?: o.str("error_text") ?: "VK отклонил запрос",
                )
            }
        }
        return json.getAsJsonObject("response") ?: json
    }

    private fun JsonObject.str(vararg names: String): String? {
        for (n in names) {
            val v = get(n)
            if (v != null && !v.isJsonNull && v.isJsonPrimitive) {
                val s = v.asString
                if (s.isNotBlank()) return s
            }
        }
        return null
    }

    private fun JsonObject.int(vararg names: String): Int {
        for (n in names) {
            val v = get(n)
            if (v != null && !v.isJsonNull && v.isJsonPrimitive) {
                return runCatching { v.asInt }.getOrDefault(0)
            }
        }
        return 0
    }

    private fun JsonObject.bool(vararg names: String): Boolean {
        for (n in names) {
            val v = get(n)
            if (v != null && !v.isJsonNull && v.isJsonPrimitive) {
                return runCatching { v.asBoolean }.getOrDefault(false)
            }
        }
        return false
    }

    /** `{"response":{"token":"...","expired_at":123}}` */
    fun anonymToken(json: JsonObject): AnonymToken? {
        val resp = unwrap(json)
        val token = resp.str("token", "anonymous_token") ?: return null
        return AnonymToken(token, resp.long("expired_at", "expires_at", "expires_in_epoch"))
    }

    /**
     * `{"response":{"flow_name":"need_password","sid":"...","next_step":{
     *    "verification_method":"sms","has_another_verification_methods":true},
     *  "can_skip_password":false}}`
     */
    fun validateAccount(json: JsonObject): ValidateAccountResult? {
        // unwrap уже возвращает содержимое `response`.
        val resp = unwrap(json)
        if (resp.str("flow_name") == null && resp.str("sid", "validation_sid") == null) {
            return null
        }
        val next = resp.getAsJsonObject("next_step")
        return ValidateAccountResult(
            flowName = resp.str("flow_name"),
            sid = resp.str("sid", "validation_sid").orEmpty(),
            rememberHash = resp.str("remember_hash"),
            nextStepMethod = next?.str("verification_method"),
            hasAnotherMethods = next?.bool("has_another_verification_methods") ?: false,
            canSkipPassword = resp.bool("can_skip_password"),
            phoneMask = resp.str("phone_mask", "phoneMask"),
        )
    }

    /** `{"response":{"sid":"...","methods":[{"name":"sms","priority":1}, ...]}}` */
    fun verificationMethods(json: JsonObject): List<VerificationMethod> {
        // unwrap уже отдаёт содержимое `response` — второй раз по конверту
        // НЕ ищем (иначе список молча становится пустым и флоу зависает).
        val arr = unwrap(json).getAsJsonArray("methods") ?: return emptyList()
        return arr.mapNotNull { el ->
            if (!el.isJsonObject) return@mapNotNull null
            val o = el.asJsonObject
            val name = o.str("name") ?: return@mapNotNull null
            VerificationMethod(
                name = name,
                priority = o.int("priority"),
                title = o.str("title"),
                info = o.str("info"),
            )
        }.sortedBy { it.priority }
    }

    /** `{"response":{"sid":"...","info":"...","code_length":4}}` */
    fun otpSend(json: JsonObject): OtpSendResult {
        val resp = unwrap(json)
        return OtpSendResult(
            sid = resp.str("sid", "validation_sid"),
            info = resp.str("info"),
            codeLength = resp.int("code_length", "codeLength"),
        )
    }

    /** `{"response":{"sid":"...","can_skip_password":true,"profile_exist":true,"auth_hash":"...","profile":{"photo_200":"..."}}}` */
    fun otpCheck(json: JsonObject): OtpCheckResult {
        val resp = unwrap(json)
        // Ответ без sid — это не «код принят, но sid нет», а отказ.
        // Раньше такое молча превращалось в успех с пустыми полями.
        resp.str("sid", "validation_sid")
            ?: throw VkAuthApiException(-1, null, "VK не вернул sid после проверки кода")
        val profile = resp.getAsJsonObject("profile")
        return OtpCheckResult(
            sid = resp.str("sid", "validation_sid"),
            canSkipPassword = resp.bool("can_skip_password"),
            profileExist = resp.bool("profile_exist"),
            photo200 = profile?.str("photo_200"),
            authHash = resp.str("auth_hash"),
        )
    }

    /**
     * Ответ `POST {api}/oauth/token`.
     *
     * Формат тот же, что у легаси `/access_token`:
     * `{"access_token":"vk1.a.…","user_id":123,"expires_in":0}`
     * либо `{"error":"need_validation","validation_type":"2fa_sms","validation_sid":"…"}`.
     */
    fun authCodeToken(json: JsonObject): AuthCodeTokenResult {
        // Здесь ошибку НЕ бросаем: отказ токена — это штатный исход шага 5,
        // ViewModel разбирает его по кодам (need_validation / captcha / flood).
        // Но конверт `{"error":{...}}` тоже надо учесть — VK отдаёт его на
        // отказах по паролю/sid, и раньше он молча превращался в «пустой успех».
        val resp = json.getAsJsonObject("response")
            ?: json.get("error")?.takeIf { it.isJsonObject }?.asJsonObject
            ?: json
        return AuthCodeTokenResult(
            accessToken = resp.str("access_token"),
            userId = resp.long("user_id"),
            expiresIn = resp.int("expires_in"),
            error = resp.str("error") ?: json.str("error"),
            errorDescription = resp.str("error_description") ?: resp.str("error_msg"),
            validationType = resp.str("validation_type"),
            validationSid = resp.str("validation_sid"),
            errorType = resp.str("error_type") ?: json.str("error_type"),
        )
    }

    private fun JsonObject.long(vararg names: String): Long {
        for (n in names) {
            val v = get(n)
            if (v != null && !v.isJsonNull && v.isJsonPrimitive) {
                runCatching { return v.asLong }
            }
        }
        return 0L
    }}
