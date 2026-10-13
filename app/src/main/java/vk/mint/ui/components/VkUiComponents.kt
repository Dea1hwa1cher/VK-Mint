package vk.mint.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import vk.mint.ui.anim.M3ExpressiveMotion
import vk.mint.ui.icons.VkIcons
import vk.mint.ui.theme.VkMintColors
import vk.mint.ui.theme.rememberM3EMorphingShape
import vk.mint.ui.theme.vkCaption1
import vk.mint.ui.theme.vkCaption2
import vk.mint.ui.theme.vkFootnote
import vk.mint.ui.theme.vkFootnoteCaps
import vk.mint.ui.theme.vkHeadline1
import vk.mint.ui.theme.vkSubhead
import vk.mint.ui.theme.vkSubheadMedium
import vk.mint.ui.theme.vkText
import vk.mint.ui.theme.vkTitle3

// ═══════════════════════════════════════════════════════════════════════════
//  1. VkGroup — Островной контейнер секции (VKUI Group × M3E Surface)
// ═══════════════════════════════════════════════════════════════════════════

enum class VkGroupMode {
    /** Островная карточка со скруглением 16dp (`--vkui--size_border_radius--regular`) на фоне подложки. */
    Card,
    /** Плоская секция на всю ширину с тонким разделителем `Separator` снизу. */
    Plain,
}

/**
 * Гибридный контейнер секции `Group` по стандарту **VKUI v8 × M3E**:
 *  - В режиме [VkGroupMode.Card] рисует «остров» (`surface` = `#FFFFFF` Light / `#19191A` Dark)
 *    со скруглением `16.dp` поверх контрастной подложки экрана (`background` = `#EBEDF0` / `#0A0A0A`).
 *  - Поддерживает встроенный заголовок [header] (`VkHeader`) и подвал [footer].
 */
@Composable
fun VkGroup(
    modifier: Modifier = Modifier,
    mode: VkGroupMode = VkGroupMode.Card,
    header: (@Composable () -> Unit)? = null,
    footer: (@Composable () -> Unit)? = null,
    contentPadding: PaddingValues = PaddingValues(vertical = 4.dp),
    containerColor: Color = MaterialTheme.colorScheme.surface,
    content: @Composable ColumnScope.() -> Unit,
) {
    val shape: Shape = when (mode) {
        VkGroupMode.Card -> RoundedCornerShape(16.dp)
        VkGroupMode.Plain -> RoundedCornerShape(0.dp)
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = shape,
        color = containerColor,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            if (header != null) {
                header()
            }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(contentPadding),
                content = content,
            )
            if (footer != null) {
                footer()
            }
            if (mode == VkGroupMode.Plain) {
                VkSeparator(insetStart = 12.dp, insetEnd = 12.dp)
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════
//  2. VkHeader — Заголовок группы/секции (VKUI Header)
// ═══════════════════════════════════════════════════════════════════════════

enum class VkHeaderMode {
    /** Крупный заголовок секции: `Title 3` (`17sp/22sp SemiBold`, основной цвет текста). */
    Primary,
    /** Компактный заголовок в верхнем регистре: `Footnote Caps` (`13sp/16sp SemiBold`, вторичный цвет). */
    Secondary,
    /** Подзаголовок среднего веса: `Subhead Medium` (`14sp/18sp Medium`, вторичный цвет). */
    Tertiary,
}

/**
 * Официальный заголовок секции `Header` из дизайн-системы **VKUI v8**:
 *  - [title]: текст заголовка.
 *  - [subtitle]: опциональный подзаголовок под основным текстом.
 *  - [indicator]: счётчик или бейдж справа от названия (например, число элементов в группе).
 *  - [aside]: правый слот действия (например, кнопка «Показать все» или иконка).
 */
@Composable
fun VkHeader(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    mode: VkHeaderMode = VkHeaderMode.Primary,
    indicator: (@Composable () -> Unit)? = null,
    aside: (@Composable RowScope.() -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(
                start = 12.dp,
                end = 12.dp,
                top = if (mode == VkHeaderMode.Primary) 10.dp else 8.dp,
                bottom = 4.dp,
            ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                val displayTitle = if (mode == VkHeaderMode.Secondary) title.uppercase() else title
                val textStyle = when (mode) {
                    VkHeaderMode.Primary -> MaterialTheme.typography.vkTitle3
                    VkHeaderMode.Secondary -> MaterialTheme.typography.vkFootnoteCaps
                    VkHeaderMode.Tertiary -> MaterialTheme.typography.vkSubheadMedium
                }
                val textColor = when (mode) {
                    VkHeaderMode.Primary -> MaterialTheme.colorScheme.onSurface
                    VkHeaderMode.Secondary, VkHeaderMode.Tertiary -> MaterialTheme.colorScheme.onSurfaceVariant
                }
                Text(
                    text = displayTitle,
                    style = textStyle,
                    color = textColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (indicator != null) {
                    indicator()
                }
            }
            if (!subtitle.isNullOrBlank()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.vkFootnote,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 1.dp),
                )
            }
        }
        if (aside != null) {
            Spacer(modifier = Modifier.width(8.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                content = aside,
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════
//  3. VkSimpleCell — Универсальная ячейка списка (VKUI SimpleCell × M3E Morph)
// ═══════════════════════════════════════════════════════════════════════════

/**
 * Каноническая ячейка списка `SimpleCell` по стандарту **VKUI v8 × M3E**:
 *  - Высота от `48.dp`, внутренние отступы `12.dp` по горизонтали, `8.dp` по вертикали.
 *  - Слот [before]: аватарка (`Avatar`), обложка трека или иконка `28dp`/`24dp`.
 *  - Центральная колонка: `title` (`vkHeadline1` `16sp/20sp Medium`) + [badgeAfterTitle] (`Verified16` / `Mute16`),
 *    `subtitle` (`vkSubhead` `14sp/18sp Regular`), `extraSubtitle` (`vkFootnote` `13sp/16sp`).
 *  - Слот [after]: переключатель `ExpressiveSwitch`, счётчик `VkCounterBadge`, время или шеврон `ChevronRight20`.
 *  - **M3E тактильность**: при нажатии плавно морфирует радиус подсветки (`12dp -> 8dp`) и масштаб (`0.985f`)
 *    через пружину [M3ExpressiveMotion.fastSpatial].
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun VkSimpleCell(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    extraSubtitle: String? = null,
    before: (@Composable () -> Unit)? = null,
    badgeAfterTitle: (@Composable () -> Unit)? = null,
    after: (@Composable RowScope.() -> Unit)? = null,
    chevron: Boolean = false,
    selected: Boolean = false,
    enabled: Boolean = true,
    morphOnPress: Boolean = true,
    titleColor: Color = MaterialTheme.colorScheme.onSurface,
    subtitleColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    onClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val cellShape = if (morphOnPress && (onClick != null || onLongClick != null)) {
        rememberM3EMorphingShape(
            pressed = isPressed,
            restCorner = 12.dp,
            pressedCorner = 8.dp,
        )
    } else {
        RoundedCornerShape(12.dp)
    }

    val scale by animateFloatAsState(
        targetValue = if (morphOnPress && isPressed) 0.985f else 1f,
        animationSpec = M3ExpressiveMotion.fastSpatial(),
        label = "VkSimpleCellScale",
    )

    val containerColor by animateColorAsState(
        targetValue = when {
            selected -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
            isPressed -> MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.65f)
            else -> Color.Transparent
        },
        animationSpec = M3ExpressiveMotion.fastEffects(),
        label = "VkSimpleCellBg",
    )

    val clickModifier = when {
        onClick != null && onLongClick != null -> Modifier.combinedClickable(
            interactionSource = interactionSource,
            indication = null,
            enabled = enabled,
            onClick = onClick,
            onLongClick = onLongClick,
        )
        onClick != null -> Modifier.clickable(
            interactionSource = interactionSource,
            indication = null,
            enabled = enabled,
            onClick = onClick,
        )
        else -> Modifier
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(cellShape)
            .background(containerColor)
            .then(clickModifier)
            .defaultMinSize(minHeight = 48.dp)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (before != null) {
            Box(contentAlignment = Alignment.Center) {
                before()
            }
            Spacer(modifier = Modifier.width(12.dp))
        }

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.Center,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.vkHeadline1,
                    color = if (enabled) titleColor else titleColor.copy(alpha = 0.45f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                if (badgeAfterTitle != null) {
                    badgeAfterTitle()
                }
            }
            if (!subtitle.isNullOrBlank()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.vkSubhead,
                    color = if (enabled) subtitleColor else subtitleColor.copy(alpha = 0.45f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
            if (!extraSubtitle.isNullOrBlank()) {
                Text(
                    text = extraSubtitle,
                    style = MaterialTheme.typography.vkFootnote,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = if (enabled) 0.8f else 0.4f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 1.dp),
                )
            }
        }

        if (after != null || chevron) {
            Spacer(modifier = Modifier.width(8.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                if (after != null) {
                    after()
                }
                if (chevron) {
                    Icon(
                        imageVector = VkIcons.ChevronRight20,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.outline,
                    )
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════
//  4. VkSubnavigationBar — Горизонтальная лента пилюль-фильтров (VKUI × M3E)
// ═══════════════════════════════════════════════════════════════════════════

/**
 * Горизонтальная панель навигационных пилюль `SubnavigationBar` + `SubnavigationButton`
 * по стандарту **VKUI v8 × M3E**:
 *  - Заменяет громоздкие `FilterChip` с галочками и `PrimaryScrollableTabRow` с нижним подчерком
 *    в папках чатов, фильтрах ленты, вкладках музыки и сообществ.
 *  - **M3E Shape Morphing**: неактивная пилюля имеет капсульную форму (`18.dp`), при выборе или
 *    нажатии пружинно морфирует в тактильный сквиркл (`10.dp`) с заливкой `primaryContainer` / `primary`.
 */
@Composable
fun <T> VkSubnavigationBar(
    items: List<T>,
    selectedItem: T,
    onItemSelected: (T) -> Unit,
    label: (T) -> String,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
    icon: ((T) -> ImageVector?)? = null,
    count: ((T) -> Int)? = null,
    highContrastSelected: Boolean = false,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(contentPadding),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        items.forEach { item ->
            val isSelected = item == selectedItem
            VkSubnavigationButton(
                text = label(item),
                selected = isSelected,
                onClick = { onItemSelected(item) },
                leadingIcon = icon?.invoke(item),
                badgeCount = count?.invoke(item) ?: 0,
                highContrastSelected = highContrastSelected,
            )
        }
    }
}

@Composable
fun VkSubnavigationButton(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leadingIcon: ImageVector? = null,
    badgeCount: Int = 0,
    highContrastSelected: Boolean = false,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val cornerRadius by animateDpAsState(
        targetValue = when {
            isPressed -> 8.dp
            selected -> 10.dp
            else -> 18.dp
        },
        animationSpec = M3ExpressiveMotion.fastSpatial(),
        label = "VkSubnavCorner",
    )

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = M3ExpressiveMotion.fastSpatial(),
        label = "VkSubnavScale",
    )

    val containerColor by animateColorAsState(
        targetValue = when {
            selected && highContrastSelected -> MaterialTheme.colorScheme.primary
            selected -> MaterialTheme.colorScheme.primaryContainer
            else -> MaterialTheme.colorScheme.surfaceContainerHigh
        },
        animationSpec = M3ExpressiveMotion.fastEffects(),
        label = "VkSubnavBg",
    )

    val contentColor by animateColorAsState(
        targetValue = when {
            selected && highContrastSelected -> MaterialTheme.colorScheme.onPrimary
            selected -> MaterialTheme.colorScheme.onPrimaryContainer
            else -> MaterialTheme.colorScheme.onSurface
        },
        animationSpec = M3ExpressiveMotion.fastEffects(),
        label = "VkSubnavFg",
    )

    Row(
        modifier = modifier
            .height(36.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(RoundedCornerShape(cornerRadius))
            .background(containerColor)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            )
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        if (leadingIcon != null) {
            Icon(
                imageVector = leadingIcon,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = contentColor,
            )
        }
        Text(
            text = text,
            style = MaterialTheme.typography.vkSubheadMedium,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
            color = contentColor,
            maxLines = 1,
        )
        if (badgeCount > 0) {
            VkCounterBadge(
                count = badgeCount,
                size = VkCounterSize.Small,
                mode = if (selected) VkCounterMode.Primary else VkCounterMode.Secondary,
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════
//  5. VkSearch — Компактное поле поиска (VKUI Search × M3E Focus Morph)
// ═══════════════════════════════════════════════════════════════════════════

/**
 * Компактная строка поиска `Search` по стандарту **VKUI v8 × M3E**:
 *  - Высота `36.dp` (вместо громоздкого `56.dp` `OutlinedTextField`), без жёсткой рамки.
 *  - Заливка `--vkui--color_field_background` (`MaterialTheme.colorScheme.surfaceVariant`:
 *    `#F2F3F5` в Light и `#232324` в Dark).
 *  - При фокусе плавно морфирует радиус скругления (`10.dp -> 14.dp`) через пружину M3E.
 *  - Снабжена иконкой `VkIcons.Search20` и анимированной кнопкой сброса `VkIcons.Clear16`.
 */
@Composable
fun VkSearch(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "Поиск",
    leadingIcon: ImageVector = VkIcons.Search20,
    trailingAction: (@Composable () -> Unit)? = null,
    onSearch: (() -> Unit)? = null,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    val cornerRadius by animateDpAsState(
        targetValue = if (isFocused) 14.dp else 10.dp,
        animationSpec = M3ExpressiveMotion.fastSpatial(),
        label = "VkSearchCorner",
    )

    val bgColor by animateColorAsState(
        targetValue = if (isFocused) {
            MaterialTheme.colorScheme.surfaceContainerHighest
        } else {
            MaterialTheme.colorScheme.surfaceVariant
        },
        animationSpec = M3ExpressiveMotion.fastEffects(),
        label = "VkSearchBg",
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(36.dp)
            .clip(RoundedCornerShape(cornerRadius))
            .background(bgColor)
            .padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = leadingIcon,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
            tint = if (isFocused) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.width(8.dp))
        Box(
            modifier = Modifier.weight(1f),
            contentAlignment = Alignment.CenterStart,
        ) {
            if (value.isEmpty()) {
                Text(
                    text = placeholder,
                    style = MaterialTheme.typography.vkText,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                textStyle = MaterialTheme.typography.vkText.copy(
                    color = MaterialTheme.colorScheme.onSurface,
                ),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                interactionSource = interactionSource,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { onSearch?.invoke() }),
                modifier = Modifier.fillMaxWidth(),
            )
        }
        AnimatedVisibility(
            visible = value.isNotEmpty(),
            enter = scaleIn(animationSpec = M3ExpressiveMotion.fastSpatial()) + fadeIn(),
            exit = scaleOut(animationSpec = M3ExpressiveMotion.fastSpatial()) + fadeOut(),
        ) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .clickable { onValueChange("") },
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
        if (trailingAction != null) {
            Spacer(modifier = Modifier.width(4.dp))
            trailingAction()
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════
//  6. VkCounterBadge, VkReactionPill и VkSeparator (VKUI Counter / Post Pill)
// ═══════════════════════════════════════════════════════════════════════════

enum class VkCounterMode {
    /** Акцентный бейдж (`primary` фон, белый текст) — непрочитанные чаты с включённым звуком. */
    Primary,
    /** Приглушённый серо-стальной бейдж — заглушённые чаты (`muted`) и вторичные счётчики. */
    Secondary,
    /** Ярко-алый бейдж (`#FF3347`) — важные уведомления и отметки. */
    Prominent,
}

enum class VkCounterSize {
    /** Стандартный бейдж `20.dp` высотой (`Caption 1 12sp Medium`). */
    Medium,
    /** Компактный бейдж `16.dp` высотой (`Caption 2 11sp Medium`) для табов и иконок. */
    Small,
}

/**
 * Капсульный счётчик `Counter` по стандарту **VKUI v8**:
 *  - Автоматически растягивается из круга в пилюлю при двузначных/трёхзначных числах.
 *  - Поддерживает режимы [VkCounterMode.Primary], [VkCounterMode.Secondary] (для muted-чатов)
 *    и [VkCounterMode.Prominent] (`#FF3347`).
 */
@Composable
fun VkCounterBadge(
    count: Int,
    modifier: Modifier = Modifier,
    mode: VkCounterMode = VkCounterMode.Primary,
    size: VkCounterSize = VkCounterSize.Medium,
    maxCount: Int = 999,
) {
    if (count <= 0) return
    val text = if (count > maxCount) "${maxCount}+" else count.toString()

    val bgColor = when (mode) {
        VkCounterMode.Primary -> MaterialTheme.colorScheme.primary
        VkCounterMode.Secondary -> MaterialTheme.colorScheme.surfaceContainerHighest
        VkCounterMode.Prominent -> VkMintColors.VkLikeRed
    }
    val fgColor = when (mode) {
        VkCounterMode.Primary -> MaterialTheme.colorScheme.onPrimary
        VkCounterMode.Secondary -> MaterialTheme.colorScheme.onSurfaceVariant
        VkCounterMode.Prominent -> Color.White
    }

    val height = if (size == VkCounterSize.Medium) 20.dp else 16.dp
    val hPadding = if (text.length == 1) 0.dp else 6.dp
    val textStyle = if (size == VkCounterSize.Medium) {
        MaterialTheme.typography.vkCaption1.copy(fontWeight = FontWeight.Medium)
    } else {
        MaterialTheme.typography.vkCaption2.copy(fontWeight = FontWeight.SemiBold)
    }

    Box(
        modifier = modifier
            .defaultMinSize(minWidth = height, minHeight = height)
            .height(height)
            .clip(CircleShape)
            .background(bgColor)
            .padding(horizontal = hPadding),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = textStyle,
            color = fgColor,
            maxLines = 1,
        )
    }
}

/**
 * Фирменная пилюля реакции под постом ВКонтакте (`Like`, `Comment`, `Repost`)
 * в гибриде **VKUI v8 × M3E**:
 *  - Высота `32.dp`, форма `CircleShape`, иконка `20.dp` (`VkIcons.*20`) + счётчик `vkFootnote` (`13sp Medium`).
 *  - В неактивном состоянии использует фон `--vkui--color_background_secondary` (`surfaceContainer`).
 *  - В активном состоянии (например, лайк) окрашивается в деликатный тинт `#FF3347` с пружинным
 *    отскоком масштаба [M3ExpressiveMotion.fastSpatial].
 */
@Composable
fun VkReactionPill(
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    count: Int = 0,
    active: Boolean = false,
    activeContainerColor: Color = VkMintColors.VkLikeRed.copy(alpha = 0.14f),
    activeContentColor: Color = VkMintColors.VkLikeRed,
    contentDescription: String? = null,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = when {
            isPressed -> 0.90f
            active -> 1.0f
            else -> 1.0f
        },
        animationSpec = M3ExpressiveMotion.fastSpatial(),
        label = "VkReactionPillScale",
    )

    val containerColor by animateColorAsState(
        targetValue = if (active) activeContainerColor else MaterialTheme.colorScheme.surfaceContainer,
        animationSpec = M3ExpressiveMotion.fastEffects(),
        label = "VkReactionPillBg",
    )

    val contentColor by animateColorAsState(
        targetValue = if (active) activeContentColor else MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = M3ExpressiveMotion.fastEffects(),
        label = "VkReactionPillFg",
    )

    Row(
        modifier = modifier
            .height(32.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(CircleShape)
            .background(containerColor)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            )
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            modifier = Modifier.size(20.dp),
            tint = contentColor,
        )
        if (count > 0) {
            Text(
                text = formatVkCount(count),
                style = MaterialTheme.typography.vkFootnote.copy(fontWeight = FontWeight.Medium),
                color = contentColor,
                maxLines = 1,
            )
        }
    }
}

/**
 * Тонкий разделитель `Separator` по стандарту **VKUI v8** (`0.5.dp`, `--vkui--color_separator_primary`
 * с боковыми отступами `12.dp` по умолчанию).
 */
@Composable
fun VkSeparator(
    modifier: Modifier = Modifier,
    insetStart: Dp = 12.dp,
    insetEnd: Dp = 12.dp,
    color: Color = MaterialTheme.colorScheme.outlineVariant,
) {
    HorizontalDivider(
        modifier = modifier.padding(start = insetStart, end = insetEnd),
        thickness = 0.5.dp,
        color = color,
    )
}

/** Форматирование счётчиков реакций/просмотров в стиле ВКонтакте (`1.2K`, `15K`, `1.4M`). */
fun formatVkCount(count: Int): String = when {
    count >= 1_000_000 -> "%.1fM".format(java.util.Locale.US, count / 1_000_000f).let {
        if (it.endsWith(".0M")) it.removeSuffix(".0M") + "M" else it
    }
    count >= 10_000 -> "${count / 1_000}K"
    count >= 1_000 -> "%.1fK".format(java.util.Locale.US, count / 1_000f).let {
        if (it.endsWith(".0K")) it.removeSuffix(".0K") + "K" else it
    }
    else -> count.toString()
}
