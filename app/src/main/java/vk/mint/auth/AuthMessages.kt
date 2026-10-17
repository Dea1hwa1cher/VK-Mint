package vk.mint.auth

import vk.mint.auth.exchange.ValidationType

/**
 * Текстовые подсказки для нативных экранов авторизации.
 *
 * Раньше эти функции жили рядом с мёртвым композаблом `ValidationCodeForm`;
 * теперь они используются экранами [AuthCodeScreen] и [AuthPasswordScreen].
 */

/**
 * Convert raw server error messages into short human-readable Russian strings.
 * Hides HTML responses, stack traces, and long HTTP bodies from the UI.
 *
 * Используется [AuthActivity] для показа баннера ошибки под полем.
 */
internal fun humanizeError(raw: String): String {
    val s = raw.trim()
    return when {
        // VK edge-шлюз отклонил запрос (неактуально для localStorage flow,
        // но остаётся на случай ошибки с другого пути).
        s.contains("wrong origin", ignoreCase = true) ||
            s.contains("wrong_origin", ignoreCase = true) ->
            "VK отклонил запрос на получение токена. " +
                "Возможно, m.vk.ru не завершил авторизацию. Попробуйте войти заново."

        // Ошибки валидации 2FA-кода.
        s.contains("wrong code", ignoreCase = true) ||
            s.contains("invalid code", ignoreCase = true) ||
            s.contains("неверный код", ignoreCase = true) ||
            s.contains("code expired", ignoreCase = true) ||
            s.contains("код устарел", ignoreCase = true) ->
            "Неверный или устаревший код подтверждения. Попробуйте ещё раз."

        // Статусные ошибки VK.
        s.contains("отклонён ВКонтакте", ignoreCase = true) ||
            s.contains("сторонних приложений", ignoreCase = true) ->
            s

        // 1117 / отклонённый токен.
        s.contains("1117", ignoreCase = true) ||
            s.contains("Access token has expired", ignoreCase = true) ->
            "VK временно ограничил доступ по паролю. Подождите пару минут и попробуйте снова."

        // Flood control.
        s.contains("too_many_attempts", ignoreCase = true) ||
            s.contains("Слишком много попыток", ignoreCase = true) ->
            "Слишком много попыток входа. VK временно ограничил доступ (~15 минут). Подождите немного."

        s.contains("rate_limit", ignoreCase = true) ->
            "Превышен лимит запросов. Подождите пару минут и попробуйте снова."

        s.contains("need_captcha", ignoreCase = true) ->
            "Требуется подтверждение капчи."

        s.contains("<html", ignoreCase = true) ||
            s.contains("<!DOCTYPE", ignoreCase = true) ||
            s.contains("<title>", ignoreCase = true) ->
            "Сервер VK вернул ошибку сервиса. Проверьте подключение и повторите попытку."

        s.startsWith("HTTP ", ignoreCase = true) ->
            "Ошибка сети: ${s.take(80)}"

        s.contains("Unable to resolve host", ignoreCase = true) ||
            s.contains("UnknownHost", ignoreCase = true) ->
            "Нет соединения с интернетом."

        s.contains("timeout", ignoreCase = true) ||
            s.contains("timed out", ignoreCase = true) ->
            "Превышен таймаут запроса. Попробуйте ещё раз."

        s.length > 120 -> s.take(120) + "…"

        else -> s
    }
}

/**
 * Подсказка для пользователя в зависимости от типа 2FA.
 *
 * Фолбэк для [AuthCodeScreen], когда VK не прислал маску номера и подзаголовок
 * вида «Код был отправлен на 7\*\*\*\*\*\*06» собрать не из чего.
 */
internal fun validationHint(type: ValidationType, phoneMask: String?): String {
    val phoneSuffix = phoneMask?.takeIf { it.isNotBlank() }?.let { " ($it)" } ?: ""
    return when (type) {
        ValidationType.SMS,
        ValidationType.SMS_INBOX,
        ValidationType.LIBVERIFY ->
            "На ваш телефон$phoneSuffix отправлен SMS-код. Введите его ниже."

        ValidationType.APP ->
            "Введите код из приложения-генератора кодов (VK ID, Google Authenticator) или запросите SMS ниже."

        ValidationType.PUSH ->
            "На ваше устройство отправлено push-уведомление. Подтвердите вход в приложении VK, либо введите код из push ниже."

        ValidationType.EMAIL ->
            "На вашу электронную почту отправлен код подтверждения."

        ValidationType.IVR ->
            "Вам поступит звонок-робот. Введите последние 4 цифры номера входящего звонка."

        ValidationType.CALL_RESET ->
            "Вам поступит звонок. Последние 4 цифры номера — это код подтверждения."

        ValidationType.TELEGRAM ->
            "Код отправлен через Telegram-бот VK."

        ValidationType.MESSENGER ->
            "Код отправлен через мессенджер VK."

        ValidationType.PASSKEY ->
            "Подтвердите вход с помощью Passkey."

        ValidationType.UNKNOWN ->
            "Введите код подтверждения$phoneSuffix."
    }
}
