package vk.mint.auth

import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import vk.mint.auth.exchange.AuthState
import vk.mint.ui.components.VkButton
import vk.mint.ui.components.VkButtonMode
import vk.mint.ui.components.VkButtonSize
import vk.mint.ui.components.VkGroup
import vk.mint.ui.components.VkGroupMode
import vk.mint.ui.components.VkInputField
import vk.mint.ui.theme.M3ExpressiveShapes
import vk.mint.ui.theme.vkSubhead
import vk.mint.ui.theme.vkTitle3

/**
 * Landing screen — экран входа с кнопками «Войти через VK» (WebView),
 * «Войти через Яндекс / Chrome» (внешний браузер, OAuth implicit)
 * и «Офлайн-режим».
 *
 * P2-12 #AUTH-AUDIT: вынесен из AuthActivity.kt в отдельный файл.
 * Функции [parseExternalBrowserToken], [ParsedToken], [parseParams],
 * [urlDecode], [ExternalBrowserShareHintSection], [ManualPasteBlock]
 * также перенесены сюда.
 */

// ═══════════════════════════════════════════════════════════════════
// #41: LANDING SCREEN — начальный экран с кнопкой «Войти через Яндекс / Chrome»
// ═══════════════════════════════════════════════════════════════════

@Composable
internal fun LandingScreen(
    state: AuthState,
    // #VKAUTH-V2 Primary CTA: запуск встроенного WebView (m.vk.ru).
    onStartWebView: () -> Unit = {},
    // Fix #187: вход через РЕАЛЬНЫЙ внешний браузер (Chrome/Яндекс) —
    // OAuth implicit flow с redirect_uri=https://oauth.vk.com/blank.html
    // (Fix #190: vkmint://oauth не работает с client_id=6287487).
    // В отличие от onBrowserSessionLogin (который читает CookieManager и
    // не работает для настоящих браузеров) — этот вариант открывает
    // системный браузер, где пользователь уже может быть залогинен в VK.
    onLaunchExternalBrowser: () -> Unit = {},
    // Fix #190: показываем подсказку «Поделиться → VK Mint» после запуска
    // внешнего браузера. Fix #191: Share — основной способ, ручная вставка —
    // свёрнута (раскрывается по кнопке «Вставить вручную»).
    showTokenPaste: Boolean = false,
    tokenPasteError: String? = null,
    onPasteToken: (String) -> Unit = {},
    onCancelTokenPaste: () -> Unit = {},
    showManualPaste: Boolean = false,
    onShowManualPaste: () -> Unit = {},
    onStartDirectAuth: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val isLoading = state is AuthState.Loading
    val errorMessage = (state as? AuthState.Error)?.message?.let(::humanizeError)

    // Fix #194b: диалог подтверждения перед запуском внешнего браузера.
    // Пользователь должен успеть прочитать инструкцию (выбрать браузер, где
    // залогинен; после входа — Поделиться → VK Mint) ДО того, как браузер
    // перекроет экран приложения.
    var showBrowserConfirmDialog by rememberSaveable { mutableStateOf(false) }

    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 28.dp, vertical = 32.dp)
            .widthIn(max = 480.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Spacer(Modifier.height(40.dp))

        // Логотип / иконка
        Surface(
            shape = androidx.compose.foundation.shape.CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier.size(96.dp),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = "VK",
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        Text(
            text = "VK Mint",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onBackground,
        )

        Text(
            text = "Вход через VK — музыка, сообщения, друзья,\n" +
                   "группы, фото, стена. Все методы работают.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )

        Spacer(Modifier.height(20.dp))

        // #VKAUTH-V2 #VKID-ONLY Primary CTA — «Войти через VK» (встроенный WebView,
        // VK ID flow). Грузит m.vk.ru/login?app_id=6287487 → VK ID SDK делает
        // silent exchange remixsid→web_token (или показывает форму входа VK ID).
        // Полный cookie capture (9 remix-ключей) → submitWebToken. Path 1.5
        // silentRefreshViaRemixsid удерживает сессию при смене IP (remixsid +
        // httoken + remixnsid + p cookie ... в storage). Авторизация ТОЛЬКО через VK ID.
        VkButton(
            onClick = onStartWebView,
            text = "Войти через VK",
            mode = VkButtonMode.Primary,
            size = VkButtonSize.Large,
            enabled = !isLoading,
            modifier = Modifier.fillMaxWidth(),
            before = {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Login,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                )
            },
        )

        Text(
            text = "Вход через VK ID. Сессия удерживается при смене сети — " +
                   "remixsid, p cookie и все cookies сохраняются автоматически.",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            textAlign = TextAlign.Center,
        )

        Spacer(Modifier.height(12.dp))

        // Secondary — «Войти через Яндекс / Chrome» (внешний браузер, OAuth implicit).
        // Fix #187 + Fix #188: вход через РЕАЛЬНЫЙ внешний браузер
        // (Chrome/Яндекс/Samsung). OAuth implicit flow.
        // Fix #190: redirect_uri=https://oauth.vk.com/blank.html. После входа
        // браузер покажет пустую страницу с access_token в URL — пользователь
        // «Поделиться» → VK Mint.
        VkButton(
            onClick = { showBrowserConfirmDialog = true },
            text = "Войти через Яндекс / Chrome",
            mode = VkButtonMode.Secondary,
            size = VkButtonSize.Large,
            enabled = !isLoading,
            modifier = Modifier.fillMaxWidth(),
            before = {
                Icon(
                    imageVector = Icons.Outlined.Public,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                )
            },
        )

        Text(
            text = "Вход через браузер, где вы уже залогинены в VK. " +
                   "Перед запуском покажем инструкцию. " +
                   "Внимание: этот способ не удерживает сессию — при смене сети " +
                   "(Wi-Fi ↔ мобильные данные) потребуется повторный вход.",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            textAlign = TextAlign.Center,
        )

        // Tertiary — «Войти по логину и паролю» (Direct Auth)
        VkButton(
            onClick = onStartDirectAuth,
            text = "Войти по логину и паролю",
            mode = VkButtonMode.Tonal,
            size = VkButtonSize.Small,
            enabled = !isLoading,
        )

        // Fix #191: показываем «Поделиться»-инструкцию после запуска браузера.
        // Это основной способ — юзер жмёт Share в браузере → выбирает VK Mint.
        if (showTokenPaste) {
            ExternalBrowserShareHintSection(
                isLoading = isLoading,
                onShowManualPaste = onShowManualPaste,
                showManualPaste = showManualPaste,
                manualPasteError = tokenPasteError,
                onManualPaste = onPasteToken,
                onCancel = onCancelTokenPaste,
            )
        }
    }

    // Fix #194b: диалог подтверждения перед запуском внешнего браузера.
    // Выполнен в каноничной MD3E оболочке:
    //  - Контейнер: surfaceContainerHigh со скруглением extraLarge (28dp).
    //  - Иконка в тональном круге, заголовок vkTitle3.
    //  - Экспрессивные кнопки VkButton (Primary / Secondary) с тактильным откликом.
    if (showBrowserConfirmDialog) {
        Dialog(
            onDismissRequest = { showBrowserConfirmDialog = false },
            properties = DialogProperties(usePlatformDefaultWidth = false),
        ) {
            Surface(
                modifier = Modifier
                    .padding(horizontal = 24.dp)
                    .widthIn(max = 420.dp)
                    .fillMaxWidth(),
                shape = M3ExpressiveShapes.extraLarge,
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                tonalElevation = 6.dp,
                shadowElevation = 8.dp,
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Public,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(26.dp),
                        )
                    }

                    Spacer(Modifier.height(14.dp))

                    Text(
                        text = "Вход через внешний браузер",
                        style = MaterialTheme.typography.vkTitle3,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center,
                    )

                    Spacer(Modifier.height(10.dp))

                    Text(
                        text = "Сейчас откроется выбор браузера:\n\n" +
                               "1. Выберите браузер, где вы уже залогинились в VK (Яндекс, Chrome и др.).\n" +
                               "2. Если вы залогинены — ВК предложит «Продолжить как …», нажмите её.\n" +
                               "3. После входа нажмите «Поделиться» (⤴) в меню браузера.\n" +
                               "4. Выберите VK Mint в списке — вход произойдёт автоматически.",
                        style = MaterialTheme.typography.vkSubhead,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )

                    Spacer(Modifier.height(12.dp))

                    // Fix #196: примечание про 405
                    Surface(
                        color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            text = "⚠ Если после входа браузер покажет «405 Method Not Allowed» или пустую страницу — это нормально. Токен уже получен, просто вернитесь в VK Mint (системная кнопка Back или переключатель приложений) — вход произойдёт автоматически.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onTertiaryContainer,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        )
                    }

                    Spacer(Modifier.height(20.dp))

                    VkButton(
                        onClick = {
                            showBrowserConfirmDialog = false
                            onLaunchExternalBrowser()
                        },
                        text = "Продолжить",
                        mode = VkButtonMode.Primary,
                        size = VkButtonSize.Large,
                        modifier = Modifier.fillMaxWidth(),
                    )

                    Spacer(Modifier.height(8.dp))

                    VkButton(
                        onClick = { showBrowserConfirmDialog = false },
                        text = "Отмена",
                        mode = VkButtonMode.Secondary,
                        size = VkButtonSize.Large,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════
// Fix #191: ExternalBrowserShareHintSection + parseExternalBrowserToken
// ═══════════════════════════════════════════════════════════════════

/**
 * Fix #191: секция-подсказка «Поделиться → VK Mint» для external browser auth.
 * Оформлена в MD3E остров [VkGroupMode.Card].
 */
@Composable
private fun ExternalBrowserShareHintSection(
    isLoading: Boolean,
    showManualPaste: Boolean,
    onShowManualPaste: () -> Unit,
    manualPasteError: String?,
    onManualPaste: (String) -> Unit,
    onCancel: () -> Unit,
) {
    VkGroup(
        mode = VkGroupMode.Card,
        containerColor = MaterialTheme.colorScheme.surface,
        contentPadding = PaddingValues(16.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = "Шаг 2: Поделиться → VK Mint",
                style = MaterialTheme.typography.vkTitle3,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = "В браузере открылась страница входа VK.\n" +
                       "После авторизации (или сразу, если вы залогинены):\n\n" +
                       "1. Нажмите «Поделиться» (иконка ⤴ в меню браузера)\n" +
                       "2. Выберите VK Mint в списке приложений\n" +
                       "3. Вход произойдёт автоматически",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            // Fix #196: примечание про 405 — VK сервер нестабильно отдаёт
            // 405 на /blank.html, хотя токен уже в URL fragment. Юзер видит
            // «405 Method Not Allowed» и пугается. Объясняем что это нормально
            // и что делать (вернуться в VK Mint — токен уже в буфере).
            Surface(
                color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.4f),
                shape = MaterialTheme.shapes.small,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = "⚠ Видите «405» или пустую страницу? Это нормально — токен уже получен.\n" +
                           "Вернитесь в VK Mint (Back или переключатель приложений) — вход произойдёт сам.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                )
            }

            if (isLoading) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                    )
                    Text(
                        text = "Входим…",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }

            if (manualPasteError != null) {
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer,
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = manualPasteError,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                    )
                }
            }

            // Deep fallback: ручная вставка (раскрывается по тапу).
            if (showManualPaste) {
                ManualPasteBlock(
                    isLoading = isLoading,
                    onSubmit = onManualPaste,
                )
            } else {
                VkButton(
                    onClick = onShowManualPaste,
                    text = "Не сработало? Вставить ссылку вручную",
                    mode = VkButtonMode.Tonal,
                    size = VkButtonSize.Small,
                    enabled = !isLoading,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            VkButton(
                onClick = onCancel,
                text = "Отмена",
                mode = VkButtonMode.Secondary,
                size = VkButtonSize.Small,
                enabled = !isLoading,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

/**
 * Fix #191: блок ручной вставки URL (deep fallback).
 * Показывается когда Share и clipboard не сработали.
 */
@Composable
private fun ManualPasteBlock(
    isLoading: Boolean,
    onSubmit: (String) -> Unit,
) {
    var pastedText by rememberSaveable { mutableStateOf("") }
    // Fix #193: LocalClipboardManager deprecated в Compose 1.8+ (BOM 2025.06.00)
    // в пользу LocalClipboard с suspend-функциями. Чтобы не тянуть coroutine scope
    // в onClick и не рисковать с точным API новой abstraction — читаем буфер
    // через платформенный android.content.ClipboardManager (не deprecated,
    // работает синхронно, не зависит от Compose versioning).
    val context = LocalContext.current
    val clipboardManager = remember(context) {
        context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        VkInputField(
            value = pastedText,
            onValueChange = { pastedText = it },
            label = "Ссылка из адресной строки",
            placeholder = "https://oauth.vk.com/blank.html#access_token=...",
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading,
            minLines = 2,
            maxLines = 4,
            singleLine = false,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Text,
                imeAction = ImeAction.Done,
            ),
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            VkButton(
                onClick = {
                    val clip = clipboardManager.primaryClip
                    val text = clip?.getItemAt(0)?.coerceToText(context)?.toString()
                    if (!text.isNullOrEmpty()) pastedText = text
                },
                text = "Из буфера",
                mode = VkButtonMode.Secondary,
                size = VkButtonSize.Medium,
                enabled = !isLoading,
                modifier = Modifier.weight(1f),
            )
            VkButton(
                onClick = { onSubmit(pastedText) },
                text = "Войти",
                mode = VkButtonMode.Primary,
                size = VkButtonSize.Medium,
                enabled = !isLoading && pastedText.isNotBlank(),
                modifier = Modifier.weight(1f),
            )
        }
    }
}

/**
 * Fix #190: парсит access_token + user_id из вставленной строки.
 *
 * Делегирует единому валидатору [OAuthTokenUtils.parse].
 * Принимает любой из форматов (полный URL, фрагмент # или %23, query ?, raw params, текст со ссылкой).
 */
internal fun parseExternalBrowserToken(input: String): ParsedToken? {
    val token = OAuthTokenUtils.parse(input) ?: return null
    return ParsedToken(
        accessToken = token.accessToken,
        userId = token.userId,
        expiresIn = token.expiresIn,
    )
}


/** Результат парсинга вставленного токена. */
internal data class ParsedToken(
    val accessToken: String,
    val userId: Long,
    val expiresIn: Long,
)

