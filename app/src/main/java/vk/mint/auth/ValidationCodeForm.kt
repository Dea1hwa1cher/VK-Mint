package vk.mint.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import vk.mint.auth.exchange.AuthState
import vk.mint.auth.exchange.ValidationType
import vk.mint.ui.components.VkButton
import vk.mint.ui.components.VkButtonMode
import vk.mint.ui.components.VkButtonSize
import vk.mint.ui.components.VkGroup
import vk.mint.ui.components.VkGroupMode
import vk.mint.ui.components.VkInputField
import vk.mint.ui.icons.VkIcons
import vk.mint.ui.theme.vkSubhead
import vk.mint.ui.theme.vkTitle3

/**
 * 2FA code input form (SMS / Push / Email / IVR / Call / Telegram / Messenger / Passkey).
 *
 * Переведено в каноничную оболочку **Material 3 Expressive (MD3E)**:
 *  - Островной контейнер [VkGroup] со скруглением `16.dp` на фоне `surface`.
 *  - Поле ввода кода [VkInputField] с центрированным крупным шрифтом, пружинным фокус-морфингом и очисткой.
 *  - Тактильные кнопки [VkButton] с пружинным морфингом радиусов ([VkButtonMode.Primary], [VkButtonMode.Secondary], [VkButtonMode.Tonal]).
 *  - Деликатный тональный блок отображения ошибок сервера.
 *
 * @param validation данные 2FA из AuthState.NeedValidation (тип, phone mask, allowed ways)
 * @param state текущий AuthState (для Loading/Error отображения)
 * @param onSubmit callback ввода кода (CodeFormatter.parse(codeRaw))
 * @param onResend callback re-send кода (PUSH/SMS/EMAIL)
 * @param onWithoutPassword callback «Подтвердить через Push» (grant_type=without_password)
 * @param onBack callback «Назад» (возврат на LandingScreen)
 */
@Composable
internal fun ValidationCodeForm(
    validation: AuthState.NeedValidation?,
    state: AuthState,
    onSubmit: (String) -> Unit,
    onResend: (ValidationType) -> Unit,
    onWithoutPassword: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var codeRaw by rememberSaveable { mutableStateOf("") }
    val isLoading = state is AuthState.Loading
    val errorMessage = (state as? AuthState.Error)?.message?.let(::humanizeError)
    val hint = if (validation != null)
        validationHint(validation.validationType, validation.phoneMask)
    else "Введите код подтверждения."

    // Allowed re-send ways from VK (e.g. if SMS was sent, user can re-send via push).
    val allowedWays = validation?.allowedWays ?: emptyList()
    val canResendViaPush = allowedWays.any { it == ValidationType.PUSH }
    val canResendViaSms = allowedWays.any { it == ValidationType.SMS } || allowedWays.isEmpty()
    val canResendViaEmail = allowedWays.any { it == ValidationType.EMAIL }

    val keyboard = LocalSoftwareKeyboardController.current
    val codeFocus = remember { FocusRequester() }
    val canSubmit = !isLoading && codeRaw.length >= 4

    LaunchedEffect(Unit) {
        codeFocus.requestFocus()
    }

    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 24.dp)
            .widthIn(max = 440.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Spacer(Modifier.height(16.dp))

        VkGroup(
            mode = VkGroupMode.Card,
            containerColor = MaterialTheme.colorScheme.surface,
            contentPadding = PaddingValues(20.dp),
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                // Иконка безопасности в тональном круге M3E
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = VkIcons.Verified16,
                        contentDescription = null,
                        modifier = Modifier.size(28.dp),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }

                Spacer(Modifier.height(16.dp))

                Text(
                    text = "Подтверждение входа",
                    style = MaterialTheme.typography.vkTitle3,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                )

                Spacer(Modifier.height(6.dp))

                Text(
                    text = hint,
                    style = MaterialTheme.typography.vkSubhead,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )

                Spacer(Modifier.height(20.dp))

                VkInputField(
                    value = codeRaw,
                    onValueChange = { typed ->
                        codeRaw = typed.filter { it.isLetterOrDigit() }.take(8)
                    },
                    label = "Код подтверждения",
                    placeholder = "123456",
                    singleLine = true,
                    enabled = !isLoading,
                    isError = errorMessage != null,
                    textStyle = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 4.sp,
                        textAlign = TextAlign.Center,
                    ),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Done,
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            if (canSubmit) {
                                keyboard?.hide()
                                onSubmit(CodeFormatter.parse(codeRaw))
                            }
                        },
                    ),
                    trailingIcon = if (codeRaw.isNotEmpty() && !isLoading) {
                        {
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .clickable { codeRaw = "" },
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    imageVector = VkIcons.Clear16,
                                    contentDescription = "Очистить",
                                    modifier = Modifier.size(16.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    } else null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(codeFocus),
                )

                if (errorMessage != null) {
                    Spacer(Modifier.height(12.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f))
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = errorMessage,
                            style = MaterialTheme.typography.vkSubhead,
                            color = MaterialTheme.colorScheme.error,
                            textAlign = TextAlign.Center,
                        )
                    }
                }

                Spacer(Modifier.height(20.dp))

                // Кнопка «Подтвердить»
                VkButton(
                    onClick = {
                        keyboard?.hide()
                        onSubmit(CodeFormatter.parse(codeRaw))
                    },
                    text = if (isLoading) null else "Подтвердить",
                    mode = VkButtonMode.Primary,
                    size = VkButtonSize.Large,
                    enabled = canSubmit,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary,
                        )
                    }
                }

                Spacer(Modifier.height(10.dp))

                // Кнопка «Назад»
                VkButton(
                    onClick = onBack,
                    text = "Назад",
                    mode = VkButtonMode.Secondary,
                    size = VkButtonSize.Large,
                    enabled = !isLoading,
                    modifier = Modifier.fillMaxWidth(),
                )

                // Кнопка «Подтвердить через Push в VK»
                if (canResendViaPush && !isLoading) {
                    Spacer(Modifier.height(10.dp))
                    VkButton(
                        onClick = onWithoutPassword,
                        text = "Подтвердить через Push в VK",
                        mode = VkButtonMode.Tonal,
                        size = VkButtonSize.Medium,
                        enabled = !isLoading,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                // Опции повторной отправки кода
                if (allowedWays.isNotEmpty() && !isLoading) {
                    val resendOptions = buildList {
                        if (canResendViaPush) add(ValidationType.PUSH to "Отправить Push")
                        if (canResendViaSms) add(ValidationType.SMS to "Отправить SMS")
                        if (canResendViaEmail) add(ValidationType.EMAIL to "Отправить Email")
                    }
                    if (resendOptions.isNotEmpty()) {
                        Spacer(Modifier.height(16.dp))
                        Text(
                            text = "Получить код другим способом:",
                            style = MaterialTheme.typography.vkSubhead,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                        )
                        Spacer(Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                        ) {
                            resendOptions.forEach { (type, label) ->
                                VkButton(
                                    onClick = { onResend(type) },
                                    text = label,
                                    mode = VkButtonMode.Tonal,
                                    size = VkButtonSize.Small,
                                    enabled = !isLoading,
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(24.dp))
    }
}

/**
 * Convert raw server error messages into short human-readable Russian strings.
 * Hides HTML responses, stack traces, and long HTTP bodies from the UI.
 *
 * P2-12 #AUTH-AUDIT: вынесен из AuthActivity.kt (был private → internal).
 */
internal fun humanizeError(raw: String): String {
    val s = raw.trim()
    return when {
        // VK edge-шлюз вернул ошибку (больше не актуально для localStorage flow,
        // но оставляем на случай если ошибка придёт из другого места).
        s.contains("wrong origin", ignoreCase = true) ||
        s.contains("wrong_origin", ignoreCase = true) ->
            "VK отклонил запрос на получение токена. " +
            "Возможно, m.vk.ru не завершил авторизацию. Попробуйте войти заново."

        // Fix #331: VK отключил парольный вход (Direct Auth) для сторонних
        // клиентов. Вместо нечитаемого «1117 Access token has expired»
        // показываем понятное объяснение и подсказку использовать WebView.
        s.contains("VK отключил парольный вход", ignoreCase = true) ||
        s.contains("отклонён ВКонтакте", ignoreCase = true) ||
        s.contains("сторонних приложений", ignoreCase = true) ->
            s  // уже человекочитаемое сообщение из AuthState.Error

        // Fix #331: 1117 «Access token has expired» приходит от API-методов
        // сразу после Direct Auth (парольный вход). VK misleading — на самом
        // деле это не истечение, а rejecting third-party web-токена.
        s.contains("1117", ignoreCase = true) ||
        s.contains("Access token has expired", ignoreCase = true) ->
            "VK отклонил токен входа по паролю. Это известное ограничение VK — " +
            "парольный вход отключён для сторонних приложений. " +
            "Войдите через кнопку «Войти через Яндекс / Chrome» выше — " +
            "это официальный и поддерживаемый способ."

        // Flood control — частая проблема при парольном входе на client_id=2274003.
        // VK блокирует на ~15 минут после 3-5 попыток.
        s.contains("too_many_attempts", ignoreCase = true) ||
        s.contains("Слишком много попыток", ignoreCase = true) ->
            "Слишком много попыток входа. VK временно заблокировал парольный вход " +
            "(~15 минут). Войдите через браузер VK — кнопка ниже."

        s.contains("rate_limit", ignoreCase = true) ->
            "Превышен лимит запросов. Войдите через браузер VK — кнопка ниже."

        s.contains("need_captcha", ignoreCase = true) ->
            "VK требует капчу. Войдите через браузер VK — кнопка ниже " +
            "(капча будет показана прямо в форме VK)."

        s.contains("<html", ignoreCase = true) ||
        s.contains("<!DOCTYPE", ignoreCase = true) ||
        s.contains("<title>", ignoreCase = true) ->
            "Сервер VK вернул HTML-ошибку. Endpoint недоступен — проверьте client_id/secret."

        s.startsWith("HTTP ", ignoreCase = true) ->
            "Ошибка сети: ${s.take(80)}"

        s.contains("Unable to resolve host", ignoreCase = true) ||
        s.contains("UnknownHost", ignoreCase = true) ->
            "Нет соединения с интернетом."

        s.contains("timeout", ignoreCase = true) ||
        s.contains("timed out", ignoreCase = true) ->
            "Превышен таймаут запроса. Попробуйте ещё раз."

        s.length > 120 -> s.take(120) + "…"

        // VK API error 3: Unknown method passed — auth.getExchangeToken
        // недоступен для данного client_id или версии API. Не фатально.
        s.contains("Unknown method", ignoreCase = true) ->
            "VK API метод недоступен (error 3). Войдите через браузер VK."

        else -> s
    }
}

/**
 * Подсказка для пользователя в зависимости от типа 2FA.
 *
 * P2-12 #AUTH-AUDIT: вынесен из AuthActivity.kt (был private → internal).
 */
internal fun validationHint(type: ValidationType, phoneMask: String?): String {
    val phoneSuffix = phoneMask?.takeIf { it.isNotBlank() }?.let { " ($it)" } ?: ""
    return when (type) {
        ValidationType.SMS,
        ValidationType.SMS_INBOX,
        ValidationType.LIBVERIFY ->
            "На ваш телефон$phoneSuffix отправлен SMS-код. Введите его ниже."
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
