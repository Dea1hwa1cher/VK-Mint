package vk.mint.auth

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.outlined.Call
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Key
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.MailOutline
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material.icons.outlined.Sms
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.material.icons.outlined.VpnKey
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import vk.mint.auth.exchange.AuthDomainsSettingsIcon
import vk.mint.auth.exchange.AuthState
import vk.mint.auth.exchange.ValidationType
import vk.mint.auth.exchange.VerificationMethod
import vk.mint.ui.anim.M3ExpressiveMotion
import vk.mint.ui.components.M3ELoadingIndicator
import vk.mint.ui.components.VkSeparator
import vk.mint.ui.icons.VkIcons
import vk.mint.ui.theme.rememberM3EMorphingShape
import vk.mint.ui.theme.vkHeadline1
import vk.mint.ui.theme.vkSubhead
import vk.mint.ui.theme.vkText
import vk.mint.ui.theme.vkTitle1

// ═══════════════════════════════════════════════════════════════════════════
//  Нативные экраны авторизации VK Mint (референс — VK X)
//
//  Геометрия и композиция повторяют мобильный авторизационный флоу VK X:
//  welcome-экран с центральным бейджем и полноширинными CTA, затем принцип
//  «одно действие — один экран» (телефон → пароль → код 2FA), где каждый шаг
//  оформлен шапкой «назад + тематическая иконка», центрированным заголовком с
//  подзаголовком, контурным полем ввода, CTA с иконкой справа и — на первом
//  шаге — карточкой «Ваш аккаунт в безопасности».
//
//  Все цвета берутся из токенов темы (`MaterialTheme.colorScheme`), поэтому
//  экраны уважают выбранный пользователем акцент, Material You и режим
//  светлая/тёмная тема — и остаются едиными с остальным приложением
//  (дизайн-система VKUI × M3E).
// ═══════════════════════════════════════════════════════════════════════════

// ───────────────────────────────────────────────────────────────────────────
//  Геометрия экранов
// ───────────────────────────────────────────────────────────────────────────

private object AuthMetrics {
    /** Боковое поле экрана — вертикальная направляющая всех блоков. */
    val ScreenPadding: Dp = 20.dp

    /** Высота верхней панели «назад + иконка шага». */
    val TopBarHeight: Dp = 56.dp

    /** Высота контурного поля ввода. */
    val FieldHeight: Dp = 56.dp

    /** Радиус скругления поля ввода. */
    val FieldCorner: Dp = 12.dp

    /** Высота полноширинной CTA-кнопки. */
    val CtaHeight: Dp = 52.dp

    /** Радиус скругления CTA-кнопки (пружинно сжимается при нажатии). */
    val CtaCorner: Dp = 20.dp

    /** Радиус скругления информационной карточки. */
    val CardCorner: Dp = 20.dp

    /** Диаметр центрального бейджа на welcome-экране. */
    val BadgeSize: Dp = 88.dp
}

// ───────────────────────────────────────────────────────────────────────────
//  1. Примитивы
// ───────────────────────────────────────────────────────────────────────────

/** Вариант заливки полноширинной CTA-кнопки. */
private enum class AuthCtaMode { Filled, Outline }

/**
 * Выравнивание содержимого CTA-кнопки.
 *
 * [Center] — как на welcome-экране: текст по центру, иконка перед текстом.
 * [LabelStart] — как на экранах форм: подпись слева, тематическая иконка справа.
 */
private enum class AuthCtaAlign { Center, LabelStart }

/**
 * Шапка шага авторизации: кнопка «назад» слева и тематическая иконка шага
 * по центру (иконка меняется от шага к шагу: аккаунт → облачко SMS → ключ).
 */
@Composable
private fun AuthStageTopBar(
    onBack: () -> Unit,
    icon: ImageVector,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(AuthMetrics.TopBarHeight)
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .clickable(onClick = onBack),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = VkIcons.ArrowBack24,
                contentDescription = "Назад",
                modifier = Modifier.size(24.dp),
                tint = MaterialTheme.colorScheme.onSurface,
            )
        }
        Box(
            modifier = Modifier
                .weight(1f)
                .height(AuthMetrics.TopBarHeight),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(26.dp),
                tint = MaterialTheme.colorScheme.onSurface,
            )
        }
        // Симметричный отступ — визуальный баланс относительно иконки шага.
        Spacer(modifier = Modifier.size(48.dp))
    }
}

/** Центрированный заголовок шага с поясняющим подзаголовком. */
@Composable
private fun AuthStageTitle(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.vkTitle1,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = subtitle,
            style = MaterialTheme.typography.vkSubhead,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

/**
 * Контурное поле ввода с постоянным акцентным бордером (в отличие от
 * [vk.mint.ui.components.VkInputField], где рамка появляется только в фокусе).
 *
 * Пружинно насыщается и утолщается при получении фокуса, краснеет при ошибке.
 */
@Composable
private fun AuthTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isError: Boolean = false,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    trailingIcon: (@Composable () -> Unit)? = null,
    textStyle: TextStyle = MaterialTheme.typography.vkText,
    focusRequester: FocusRequester? = null,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val shape = RoundedCornerShape(AuthMetrics.FieldCorner)

    val borderColor by animateColorAsState(
        targetValue = when {
            isError -> MaterialTheme.colorScheme.error
            isFocused -> MaterialTheme.colorScheme.primary
            else -> MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)
        },
        animationSpec = M3ExpressiveMotion.fastEffects(),
        label = "AuthFieldBorder",
    )
    val borderWidth by animateDpAsState(
        targetValue = if (isFocused || isError) 1.5.dp else 1.dp,
        animationSpec = M3ExpressiveMotion.fastEffects(),
        label = "AuthFieldBorderWidth",
    )
    val containerColor by animateColorAsState(
        targetValue = if (enabled) {
            MaterialTheme.colorScheme.surface
        } else {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        },
        animationSpec = M3ExpressiveMotion.fastEffects(),
        label = "AuthFieldBg",
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(AuthMetrics.FieldHeight)
            .clip(shape)
            .background(containerColor)
            .border(width = borderWidth, color = borderColor, shape = shape)
            .padding(start = 16.dp, end = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier.weight(1f),
            contentAlignment = Alignment.CenterStart,
        ) {
            if (value.isEmpty()) {
                Text(
                    text = placeholder,
                    style = textStyle,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .then(
                        if (focusRequester != null) {
                            Modifier.focusRequester(focusRequester)
                        } else {
                            Modifier
                        },
                    ),
                enabled = enabled,
                singleLine = true,
                textStyle = textStyle.copy(
                    color = if (enabled) {
                        MaterialTheme.colorScheme.onSurface
                    } else {
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                    },
                ),
                cursorBrush = SolidColor(
                    if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                ),
                keyboardOptions = keyboardOptions,
                keyboardActions = keyboardActions,
                visualTransformation = visualTransformation,
                interactionSource = interactionSource,
            )
        }
        if (trailingIcon != null) {
            Spacer(modifier = Modifier.width(8.dp))
            trailingIcon()
        }
    }
}

/**
 * Полноширинная CTA-кнопка авторизации.
 *
 * Пружинный морфинг радиуса и лёгкий масштаб при нажатии ([rememberM3EMorphingShape]),
 * три состояния правого слота:
 *  - `loading` → морфящийся индикатор M3E;
 *  - `!enabled` → замок (подсказка «нужно заполнить поле»);
 *  - иначе → [trailingIcon] (стрелка «дальше»).
 */
@Composable
private fun AuthCtaButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false,
    mode: AuthCtaMode = AuthCtaMode.Filled,
    align: AuthCtaAlign = AuthCtaAlign.Center,
    leadingIcon: ImageVector? = null,
    trailingIcon: ImageVector? = null,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val shape = rememberM3EMorphingShape(
        pressed = isPressed,
        restCorner = AuthMetrics.CtaCorner,
        pressedCorner = AuthMetrics.CtaCorner - 4.dp,
    )
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.98f else 1f,
        animationSpec = M3ExpressiveMotion.fastSpatial(),
        label = "AuthCtaScale",
    )

    val isFilled = mode == AuthCtaMode.Filled
    // Во время загрузки кнопка остаётся акцентной (как в референсе VK X) —
    // просто некликабельная: блокируются нажатия через Surface(enabled = false).
    val looksActive = enabled || loading
    val containerColor = when {
        isFilled && looksActive -> MaterialTheme.colorScheme.primary
        isFilled -> MaterialTheme.colorScheme.surfaceContainerHigh
        else -> Color.Transparent
    }
    val contentColor = when {
        isFilled && looksActive -> MaterialTheme.colorScheme.onPrimary
        looksActive -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
    }
    val borderColor = when {
        looksActive -> MaterialTheme.colorScheme.outline
        else -> MaterialTheme.colorScheme.outlineVariant
    }

    Surface(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(AuthMetrics.CtaHeight)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            },
        enabled = enabled,
        shape = shape,
        color = containerColor,
        contentColor = contentColor,
        border = if (isFilled) null else BorderStroke(1.dp, borderColor),
        interactionSource = interactionSource,
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = if (align == AuthCtaAlign.Center) {
                Arrangement.Center
            } else {
                Arrangement.Start
            },
        ) {
            if (leadingIcon != null) {
                Icon(
                    imageVector = leadingIcon,
                    contentDescription = null,
                    modifier = Modifier.size(22.dp),
                    tint = contentColor,
                )
                Spacer(modifier = Modifier.width(10.dp))
            }
            Text(
                text = text,
                style = MaterialTheme.typography.vkHeadline1,
                fontWeight = FontWeight.Medium,
                color = contentColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (align == AuthCtaAlign.LabelStart) {
                Spacer(modifier = Modifier.weight(1f))
            }
            when {
                loading -> M3ELoadingIndicator(
                    modifier = Modifier.size(22.dp),
                    size = 22.dp,
                    contained = false,
                    indicatorColor = contentColor,
                )

                !enabled -> Icon(
                    imageVector = Icons.Outlined.Lock,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = contentColor,
                )

                trailingIcon != null -> Icon(
                    imageVector = trailingIcon,
                    contentDescription = null,
                    modifier = Modifier.size(22.dp),
                    tint = contentColor,
                )
            }
        }
    }
}

/** Текстовое действие под разделителем («Восстановить пароль», «Забыли пароль?»). */
@Composable
private fun AuthTextAction(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    trailingIcon: ImageVector? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 18.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.vkHeadline1,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center,
        )
        if (trailingIcon != null) {
            Spacer(modifier = Modifier.width(8.dp))
            Icon(
                imageVector = trailingIcon,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

/**
 * Компактная группа ссылок-действий в одну строку — используется для
 * альтернативных способов повторной отправки кода 2FA.
 */
@Composable
private fun AuthLinkRow(
    labels: List<String>,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(20.dp, Alignment.CenterHorizontally),
    ) {
        labels.forEachIndexed { index, label ->
            Text(
                text = label,
                style = MaterialTheme.typography.vkSubhead,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.clickable { onSelect(index) },
            )
        }
    }
}

/** Карточка «Ваш аккаунт в безопасности» под формой входа. */
@Composable
private fun AuthSecurityCard(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(AuthMetrics.CardCorner))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .padding(20.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Icon(
            imageVector = Icons.Outlined.Lock,
            contentDescription = null,
            modifier = Modifier.size(22.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Ваш аккаунт в безопасности",
                style = MaterialTheme.typography.vkHeadline1,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "VK Mint только отправляет ваши данные на сервера VK " +
                    "и хранит их в зашифрованной форме.",
                style = MaterialTheme.typography.vkSubhead,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** Тональный блок с текстом серверной ошибки (раньше ошибки в UI не попадали). */
@Composable
private fun AuthErrorBanner(message: String, modifier: Modifier = Modifier) {
    Text(
        text = message,
        style = MaterialTheme.typography.vkSubhead,
        color = MaterialTheme.colorScheme.error,
        textAlign = TextAlign.Center,
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f))
            .padding(horizontal = 14.dp, vertical = 10.dp),
    )
}

/**
 * Общий каркас экрана-шага: шапка «назад + иконка», прокручиваемое тело с
 * заголовком и полями и опциональный прижатый к низу блок (например,
 * «Забыли пароль?» на экране пароля).
 */
@Composable
private fun AuthFormStage(
    onBack: () -> Unit,
    icon: ImageVector,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    footer: (@Composable ColumnScope.() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding(),
    ) {
        AuthStageTopBar(onBack = onBack, icon = icon)

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .imePadding()
                .padding(horizontal = AuthMetrics.ScreenPadding),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            AuthStageTitle(title = title, subtitle = subtitle)

            Spacer(modifier = Modifier.height(28.dp))

            content()

            Spacer(modifier = Modifier.height(24.dp))
        }

        if (footer != null) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .imePadding()
                    .padding(horizontal = AuthMetrics.ScreenPadding),
                horizontalAlignment = Alignment.CenterHorizontally,
                content = footer,
            )
        }
    }
}

// ───────────────────────────────────────────────────────────────────────────
//  2. Экран приветствия
// ───────────────────────────────────────────────────────────────────────────

/**
 * Welcome-экран авторизации: центральный бейдж приложения с заголовком и
 * способами входа, прижатыми к низу.
 *
 * @param onSignIn переход к нативной форме входа (телефон → пароль → код 2FA).
 * @param onOpenVkId переход к веб-форме VK ID (существующий WebView-флоу).
 * @param onRestorePassword переход к восстановлению пароля через VK ID.
 *
 * > Слот для «Войти через QR-код» (третье действие в оригинальном VK X)
 * > намеренно не отрисован — генератора QR-кодов в проекте пока нет.
 * > Когда он появится, экран уже готов принять третью CTA-кнопку: место
 * > зарезервировано между [onOpenVkId] и разделителем.
 */
@Composable
internal fun AuthWelcomeScreen(
    onSignIn: () -> Unit,
    onOpenVkId: () -> Unit,
    onRestorePassword: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding(),
    ) {
        // Служебная шестерёнка доменов VK. Раньше WebView был единственным
        // экраном авторизации и иконка была доступна всегда; после появления
        // welcome-экрана поднимаем её сюда, чтобы смена заблокированных
        // доменов осталась доступна ДО попытки входа (иначе пользователь
        // не сможет открыть страницу логина и застрянет).
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(AuthMetrics.TopBarHeight),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AuthDomainsSettingsIcon()
        }

        // Центральный блок: бейдж + заголовок + подзаголовок.
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = AuthMetrics.ScreenPadding),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Box(
                modifier = Modifier
                    .size(AuthMetrics.BadgeSize)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = VkIcons.MusicFill28,
                    contentDescription = null,
                    modifier = Modifier.size(40.dp),
                    tint = MaterialTheme.colorScheme.onSurface,
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "Добро пожаловать в VK Mint",
                style = MaterialTheme.typography.vkTitle1,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center,
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Революция в мире музыки.",
                style = MaterialTheme.typography.vkSubhead,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }

        // Нижний блок действий.
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = AuthMetrics.ScreenPadding),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            AuthCtaButton(
                text = "Войти через аккаунт VK",
                onClick = onSignIn,
                mode = AuthCtaMode.Filled,
                align = AuthCtaAlign.Center,
            )

            Spacer(modifier = Modifier.height(12.dp))

            AuthCtaButton(
                text = "Войти через VK ID",
                onClick = onOpenVkId,
                mode = AuthCtaMode.Outline,
                align = AuthCtaAlign.Center,
                leadingIcon = Icons.Outlined.Public,
            )

            Spacer(modifier = Modifier.height(8.dp))

            VkSeparator(insetStart = 0.dp, insetEnd = 0.dp)

            AuthTextAction(
                text = "Восстановить пароль",
                onClick = onRestorePassword,
                trailingIcon = Icons.AutoMirrored.Outlined.OpenInNew,
            )
        }
    }
}

// ───────────────────────────────────────────────────────────────────────────
//  3. Шаг 1 — телефон или почта
// ───────────────────────────────────────────────────────────────────────────

/**
 * Шаг входа: логин (телефон или email) → переход к вводу пароля.
 *
 * @param errorMessage текст серверной ошибки (показывается под полем), либо null.
 * @param onContinue переход к шагу ввода пароля.
 * @param onOpenVkId запасной путь — веб-форма VK ID (обход flood control).
 */
@Composable
internal fun AuthLoginScreen(
    login: String,
    onLoginChange: (String) -> Unit,
    onContinue: () -> Unit,
    onOpenVkId: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    errorMessage: String? = null,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    focusRequester: FocusRequester? = null,
) {
    val canContinue = login.isNotBlank()

    AuthFormStage(
        onBack = onBack,
        icon = Icons.Outlined.Person,
        title = "Войти в VK Mint",
        subtitle = "используя ваш аккаунт VK",
        modifier = modifier,
    ) {
        AuthTextField(
            value = login,
            onValueChange = onLoginChange,
            placeholder = "Номер телефона или почта",
            isError = errorMessage != null,
            focusRequester = focusRequester,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Next,
            ),
            keyboardActions = keyboardActions,
        )

        Spacer(modifier = Modifier.height(16.dp))

        AuthCtaButton(
            text = "Войти",
            onClick = onContinue,
            enabled = canContinue,
            align = AuthCtaAlign.LabelStart,
            trailingIcon = Icons.AutoMirrored.Outlined.ArrowForward,
        )

        if (errorMessage != null) {
            Spacer(modifier = Modifier.height(12.dp))
            AuthErrorBanner(message = errorMessage)
        }

        Spacer(modifier = Modifier.height(24.dp))

        AuthSecurityCard()

        Spacer(modifier = Modifier.height(4.dp))

        AuthTextAction(
            text = "Войти через VK ID",
            onClick = onOpenVkId,
            trailingIcon = Icons.AutoMirrored.Outlined.OpenInNew,
        )
    }
}

// ───────────────────────────────────────────────────────────────────────────
//  4. Шаг 2 — пароль
// ───────────────────────────────────────────────────────────────────────────

/**
 * Шаг ввода пароля от аккаунта VK ID.
 *
 * @param onSubmit отправка credentials в [AuthViewModel.submitCredentials].
 * @param onForgotPassword переход к восстановлению пароля через VK ID.
 */
@Composable
internal fun AuthPasswordScreen(
    password: String,
    onPasswordChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onBack: () -> Unit,
    onForgotPassword: () -> Unit,
    modifier: Modifier = Modifier,
    loading: Boolean = false,
    errorMessage: String? = null,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    focusRequester: FocusRequester? = null,
) {
    val canSubmit = password.isNotBlank() && !loading
    var passwordVisible by remember { mutableStateOf(false) }

    AuthFormStage(
        onBack = onBack,
        icon = Icons.Outlined.VpnKey,
        title = "Введите пароль",
        subtitle = "Чтобы продолжить, введите ваш пароль для входа в аккаунт VK ID",
        modifier = modifier,
        footer = {
            VkSeparator(insetStart = 0.dp, insetEnd = 0.dp)
            AuthTextAction(
                text = "Забыли пароль?",
                onClick = onForgotPassword,
                trailingIcon = Icons.AutoMirrored.Outlined.OpenInNew,
            )
        },
    ) {
        AuthTextField(
            value = password,
            onValueChange = onPasswordChange,
            placeholder = "Пароль",
            enabled = !loading,
            isError = errorMessage != null,
            focusRequester = focusRequester,
            visualTransformation = if (passwordVisible) {
                VisualTransformation.None
            } else {
                PasswordVisualTransformation()
            },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Done,
            ),
            keyboardActions = keyboardActions,
            trailingIcon = {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .clickable(enabled = !loading) { passwordVisible = !passwordVisible },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = if (passwordVisible) {
                            Icons.Outlined.VisibilityOff
                        } else {
                            Icons.Outlined.Visibility
                        },
                        contentDescription = if (passwordVisible) {
                            "Скрыть пароль"
                        } else {
                            "Показать пароль"
                        },
                        modifier = Modifier.size(22.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            },
        )

        Spacer(modifier = Modifier.height(16.dp))

        AuthCtaButton(
            text = "Продолжить",
            onClick = onSubmit,
            enabled = canSubmit,
            loading = loading,
            align = AuthCtaAlign.LabelStart,
            trailingIcon = Icons.AutoMirrored.Outlined.ArrowForward,
        )

        if (errorMessage != null) {
            Spacer(modifier = Modifier.height(12.dp))
            AuthErrorBanner(message = errorMessage)
        }
    }
}

// ───────────────────────────────────────────────────────────────────────────
//  5. Шаг 3 — код подтверждения
// ───────────────────────────────────────────────────────────────────────────

/**
 * Шаг подтверждения входа кодом.
 *
 * Работает по именам способов от VK (`sms`, `push`, `callreset`, …), а не по
 * enum — так экран одинаково обслуживает и auth-code флоу
 * ([vk.mint.auth.exchange.AuthCodeRepository]), и легаси `need_validation`.
 *
 * @param validation данные из [AuthState.NeedValidation] — заголовок, подсказка
 *   от VK (`otpInfo`), длина кода и доступные способы повторной отправки.
 * @param method способ подтверждения, который сейчас проверяем.
 * @param onResend запрос повторной отправки выбранным способом.
 */
@Composable
internal fun AuthCodeScreen(
    code: String,
    onCodeChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onResend: (String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    method: String? = null,
    validation: AuthState.NeedValidation? = null,
    loading: Boolean = false,
    errorMessage: String? = null,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    focusRequester: FocusRequester? = null,
) {
    // Длину кода знает только VK (важно для callreset: 4, не 6).
    val minLength = validation?.otpCodeLength?.takeIf { it in 1..8 } ?: 4
    val canSubmit = code.count { it.isLetterOrDigit() } >= minLength && !loading
    val resendWays = remember(validation) { validation?.resendWays().orEmpty() }
        .filter { it != method }
        .ifEmpty { listOfNotNull(method) }

    AuthFormStage(
        onBack = onBack,
        icon = Icons.Outlined.ChatBubbleOutline,
        title = method.codeTitle(),
        subtitle = codeSubtitle(validation, method),
        modifier = modifier,
    ) {
        AuthTextField(
            value = code,
            onValueChange = { typed ->
                onCodeChange(typed.filter { it.isLetterOrDigit() }.take(8))
            },
            placeholder = if (minLength > 4) "Код из $minLength цифр" else "Код",
            enabled = !loading,
            isError = errorMessage != null,
            focusRequester = focusRequester,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Number,
                imeAction = ImeAction.Done,
            ),
            keyboardActions = keyboardActions,
        )

        Spacer(modifier = Modifier.height(16.dp))

        AuthCtaButton(
            text = "Продолжить",
            onClick = onSubmit,
            enabled = canSubmit,
            loading = loading,
            align = AuthCtaAlign.LabelStart,
            trailingIcon = Icons.AutoMirrored.Outlined.ArrowForward,
        )

        if (errorMessage != null) {
            Spacer(modifier = Modifier.height(12.dp))
            AuthErrorBanner(message = errorMessage)
        }

        // Способы повторной отправки, разрешённые сервером.
        if (resendWays.isNotEmpty()) {
            Spacer(modifier = Modifier.height(20.dp))
            Text(
                text = "Не пришёл код?",
                style = MaterialTheme.typography.vkSubhead,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(8.dp))
            AuthLinkRow(
                labels = resendWays.map { it.resendLabel() },
                onSelect = { index -> onResend(resendWays[index]) },
            )
        }
    }
}

// ───────────────────────────────────────────────────────────────────────────
//  6. Вспомогательное
// ───────────────────────────────────────────────────────────────────────────

/**
 * Подзаголовок шага кода в стиле VK X: «Код был отправлен на 7\*\*\*\*\*\*06».
 *
 * Приоритет источников: собственный текст VK (`otpInfo`) → маска номера с
 * названием канала → текстовая подсказка [validationHint].
 */
private fun codeSubtitle(validation: AuthState.NeedValidation?, method: String?): String {
    if (validation == null) return "Введите код подтверждения."
    validation.otpInfo?.takeIf { it.isNotBlank() }?.let { return it }
    val mask = validation.phoneMask?.takeIf { it.isNotBlank() }
    val channel = when (method ?: validation.nextStepMethod) {
        "sms" -> "SMS"
        "callreset" -> "звонка"
        "email" -> "email"
        "push" -> "Push-уведомления"
        "reserve_code" -> "резервного кода"
        "codegen" -> "генератора кодов"
        else -> when (validation.validationType) {
            ValidationType.SMS, ValidationType.SMS_INBOX, ValidationType.LIBVERIFY -> "SMS"
            ValidationType.EMAIL -> "email"
            ValidationType.PUSH -> "Push-уведомления"
            else -> null
        }
    }
    return when {
        channel != null && mask != null -> "Код был отправлен на $mask"
        mask != null -> "Код отправлен на $mask"
        channel != null -> "Код отправлен. Проверьте $channel."
        else -> validationHint(validation.validationType, validation.phoneMask)
    }
}

/**
 * Способы повторной отправки кода, которые сервер разрешил.
 *
 * Берём именованные способы из auth-code флоу ([supportedMethods]); если их
 * нет — откатываемся на легаси `ValidationType`, конвертируя его в то же
 * строковое пространство имён.
 */
private fun AuthState.NeedValidation.resendWays(): List<String> {
    if (supportedMethods.isNotEmpty()) {
        return supportedMethods.map { it.name }
            .filter { it != AuthViewModel.ValidationMethodNames.PASSWORD }
    }
    val base: List<ValidationType> = if (allowedWays.isNotEmpty()) allowedWays else listOf(validationType)
    return base.filter { it != ValidationType.PASSKEY }.map { it.methodName() }
}

/** Заголовок шага кода — зависит от способа, который проверяем. */
private fun String?.codeTitle(): String = when (this) {
    "callreset" -> "Введите код из звонка"
    "email" -> "Введите код из письма"
    "push" -> "Подтвердите вход"
    "reserve_code" -> "Введите резервный код"
    "codegen" -> "Введите код из приложения"
    else -> "Введите код из SMS"
}

/** Короткая подпись способа отправки для компактных действий. */
private fun String.resendLabel(): String = when (this) {
    "sms" -> "Отправить SMS"
    "callreset" -> "Позвонить ещё раз"
    "email" -> "Отправить Email"
    "push" -> "Отправить Push"
    "reserve_code" -> "Другой способ"
    "codegen" -> "Код из приложения"
    "messenger" -> "В мессенджер"
    "telegram" -> "В Telegram"
    else -> "Отправить повторно"
}

/** Легаси `ValidationType` → имя способа в auth-code флоу. */
private fun ValidationType.methodName(): String = when (this) {
    ValidationType.SMS, ValidationType.SMS_INBOX, ValidationType.LIBVERIFY -> "sms"
    ValidationType.PUSH -> "push"
    ValidationType.EMAIL -> "email"
    ValidationType.IVR, ValidationType.CALL_RESET -> "callreset"
    ValidationType.MESSENGER -> "messenger"
    ValidationType.TELEGRAM -> "telegram"
    else -> "sms"
}

// ───────────────────────────────────────────────────────────────────────────
//  7. Экран ожидания решения VK
// ───────────────────────────────────────────────────────────────────────────

/**
 * Пока идёт `auth.validateAccount`, показать индикатор с возможностью отмены.
 *
 * Отдельный экран нужен, чтобы не держать нарисованную форму входа: VK может
 * ответить через несколько секунд, и пользователь не должен видеть «пустое»
 * поле, которое ещё ничего не отправило.
 */
@Composable
internal fun AuthWaitingScreen(
    message: String,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = AuthMetrics.ScreenPadding),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier.widthIn(max = 440.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            M3ELoadingIndicator(size = 56.dp, contained = true)
            Spacer(modifier = Modifier.height(20.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.vkSubhead,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Отменить",
                style = MaterialTheme.typography.vkHeadline1,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .clickable(onClick = onCancel)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
            )
        }
    }
}

// ───────────────────────────────────────────────────────────────────────────
//  8. Экран выбора способа подтверждения
// ───────────────────────────────────────────────────────────────────────────

/**
 * Экран выбора способа подтверждения.
 *
 * Появляется, когда `auth.validateAccount` вернул `has_another_verification_methods`
 * и `ecosystem.getVerificationMethods` — то есть VK разрешил больше одного
 * варианта. Порядок и подписи приходят от сервера (сортировка по `priority`
 * делается в [vk.mint.auth.exchange.AuthCodeRepository.verificationMethods]).
 *
 * Это то место, где наш UI перестаёт быть зашитым под «телефон → пароль → код»
 * и начинает следовать за решением VK.
 */
@Composable
internal fun AuthMethodSelectScreen(
    methods: List<VerificationMethod>,
    onSelect: (String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    loading: Boolean = false,
    errorMessage: String? = null,
) {
    AuthFormStage(
        onBack = onBack,
        icon = Icons.Outlined.MailOutline,
        title = "Как подтвердить вход?",
        subtitle = "Выберите удобный способ получения кода",
        modifier = modifier,
    ) {
        methods.forEachIndexed { index, method ->
            if (index > 0) Spacer(modifier = Modifier.height(8.dp))
            AuthMethodRow(
                method = method,
                enabled = !loading,
                onClick = { onSelect(method.name) },
            )
        }

        if (errorMessage != null) {
            Spacer(modifier = Modifier.height(12.dp))
            AuthErrorBanner(message = errorMessage)
        }
    }
}

/** Строка выбора способа — остров VKUI × M3E с пружинным морфингом. */
@Composable
private fun AuthMethodRow(
    method: VerificationMethod,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val shape = rememberM3EMorphingShape(
        pressed = isPressed,
        restCorner = AuthMetrics.CardCorner,
        pressedCorner = AuthMetrics.CardCorner - 4.dp,
    )

    Surface(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        enabled = enabled,
        shape = shape,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        contentColor = MaterialTheme.colorScheme.onSurface,
        interactionSource = interactionSource,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = method.name.methodIcon(),
                contentDescription = null,
                modifier = Modifier.size(22.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = method.title ?: method.name.resendLabel(),
                    style = MaterialTheme.typography.vkHeadline1,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                if (!method.info.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = method.info,
                        style = MaterialTheme.typography.vkSubhead,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Icon(
                imageVector = VkIcons.ChevronRight20,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private fun String.methodIcon(): ImageVector = when (this) {
    "email" -> Icons.Outlined.MailOutline
    "push" -> Icons.Outlined.NotificationsNone
    "callreset" -> Icons.Outlined.Call
    "reserve_code" -> Icons.Outlined.Key
    "codegen" -> Icons.Outlined.VerifiedUser
    else -> Icons.Outlined.Sms
}
