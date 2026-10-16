package vk.mint.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import vk.mint.util.toCountString
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
fun VkSubnavigationBar(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
    content: @Composable androidx.compose.foundation.layout.RowScope.() -> Unit,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(contentPadding),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        content = content,
    )
}

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
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun VkReactionPill(
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    count: Int = 0,
    active: Boolean = false,
    enabled: Boolean = true,
    activeContainerColor: Color = VkMintColors.VkLikeRed.copy(alpha = 0.14f),
    activeContentColor: Color = VkMintColors.VkLikeRed,
    contentDescription: String? = null,
    onLongClick: (() -> Unit)? = null,
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
            .combinedClickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                onClick = onClick,
                onLongClick = onLongClick,
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
fun formatVkCount(count: Int): String = count.toCountString()

// ═══════════════════════════════════════════════════════════════════════════
//  7. VkPanelHeader — Шапка экрана (VKUI PanelHeader × M3E TopBar)
// ═══════════════════════════════════════════════════════════════════════════

/**
 * Шапка экрана `PanelHeader` по стандарту **VKUI v8 × M3E**:
 *  - Фиксированная высота `56.dp` (мобильный канон VKUI v8).
 *  - Слот [before]: навигационная иконка «Назад» или бургер-меню.
 *  - Центральная зона: `title` (`vkTitle3`), опциональный `subtitle` (`vkSubhead` / `vkFootnote`).
 *  - Слот [after]: кнопки быстрых действий (поиск, шаринг, контекстное меню).
 *  - Разделитель [showSeparator]: нижний `VkSeparator` при необходимости.
 */
@Composable
fun VkPanelHeader(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    before: (@Composable () -> Unit)? = null,
    after: (@Composable RowScope.() -> Unit)? = null,
    containerColor: Color = MaterialTheme.colorScheme.surface,
    contentColor: Color = MaterialTheme.colorScheme.onSurface,
    showSeparator: Boolean = false,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = containerColor,
        contentColor = contentColor,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (before != null) {
                    Box(
                        modifier = Modifier.size(48.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        before()
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                } else {
                    Spacer(modifier = Modifier.width(12.dp))
                }

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.vkTitle3,
                        color = contentColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (!subtitle.isNullOrBlank()) {
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.vkSubhead,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }

                if (after != null) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.End,
                        content = after,
                    )
                } else {
                    Spacer(modifier = Modifier.width(12.dp))
                }
            }
            if (showSeparator) {
                VkSeparator(insetStart = 0.dp, insetEnd = 0.dp)
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════
//  8. VkButton — Кнопка действия (VKUI Button × M3E Shape Morphing)
// ═══════════════════════════════════════════════════════════════════════════

enum class VkButtonMode {
    /** Основная заливка (`primary` фон, контрастный текст). */
    Primary,
    /** Вторичная тональная кнопка (`surfaceContainerHigh` / `secondaryContainer`). */
    Secondary,
    /** Тональная кнопка акцента (`primaryContainer`). */
    Tonal,
    /** Контурная кнопка (`outline` бордер, прозрачный фон). */
    Outline,
}

enum class VkButtonSize {
    /** Компактная кнопка `32.dp` (внутренние списки, чипы). */
    Small,
    /** Стандартная кнопка `38.dp` (карточки, модалки, подписки). */
    Medium,
    /** Полноразмерная кнопка `44.dp` (CTA, главные действия). */
    Large,
}

@Composable
fun VkButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    text: String? = null,
    mode: VkButtonMode = VkButtonMode.Primary,
    size: VkButtonSize = VkButtonSize.Medium,
    enabled: Boolean = true,
    before: (@Composable () -> Unit)? = null,
    after: (@Composable () -> Unit)? = null,
    content: (@Composable RowScope.() -> Unit)? = null,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val height = when (size) {
        VkButtonSize.Small -> 32.dp
        VkButtonSize.Medium -> 38.dp
        VkButtonSize.Large -> 44.dp
    }

    val restCorner = when (size) {
        VkButtonSize.Small -> 10.dp
        VkButtonSize.Medium -> 12.dp
        VkButtonSize.Large -> 14.dp
    }
    val pressedCorner = (restCorner - 4.dp).coerceAtLeast(6.dp)

    val shape = rememberM3EMorphingShape(
        pressed = isPressed,
        restCorner = restCorner,
        pressedCorner = pressedCorner,
    )

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.97f else 1f,
        animationSpec = M3ExpressiveMotion.fastSpatial(),
        label = "VkButtonScale",
    )

    val containerColor = when (mode) {
        VkButtonMode.Primary -> if (enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
        VkButtonMode.Secondary -> if (enabled) MaterialTheme.colorScheme.surfaceContainerHigh else MaterialTheme.colorScheme.surfaceContainerLow
        VkButtonMode.Tonal -> if (enabled) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceContainer
        VkButtonMode.Outline -> Color.Transparent
    }

    val contentColor = when (mode) {
        VkButtonMode.Primary -> if (enabled) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
        VkButtonMode.Secondary -> if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
        VkButtonMode.Tonal -> if (enabled) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
        VkButtonMode.Outline -> if (enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
    }

    val textStyle = when (size) {
        VkButtonSize.Small -> MaterialTheme.typography.vkFootnote
        VkButtonSize.Medium -> MaterialTheme.typography.vkSubheadMedium
        VkButtonSize.Large -> MaterialTheme.typography.vkHeadline1
    }

    Surface(
        onClick = onClick,
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .defaultMinSize(minHeight = height),
        enabled = enabled,
        shape = shape,
        color = containerColor,
        contentColor = contentColor,
        border = if (mode == VkButtonMode.Outline) {
            BorderStroke(
                1.dp,
                if (enabled) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.outlineVariant,
            )
        } else null,
        interactionSource = interactionSource,
    ) {
        Row(
            modifier = Modifier
                .height(height)
                .padding(horizontal = if (size == VkButtonSize.Small) 10.dp else 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            if (before != null) {
                before()
                Spacer(modifier = Modifier.width(6.dp))
            }
            if (text != null) {
                Text(
                    text = text,
                    style = textStyle,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (content != null) {
                content()
            }
            if (after != null) {
                Spacer(modifier = Modifier.width(6.dp))
                after()
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════
//  9. VkInputField — Текстовое поле ввода (VKUI Input × M3E Focus Morph)
// ═══════════════════════════════════════════════════════════════════════════

/**
 * Тактильное поле ввода по стандарту **VKUI v8 × M3E**:
 *  - Высота от `52.dp` (эргономичный размер для мобильного ввода).
 *  - Заливка `--vkui--color_field_background` (`surfaceVariant`: `#F2F3F5` в Light и `#232324` в Dark).
 *  - При фокусе: пружинный морфинг радиуса (`12.dp -> 16.dp`) через [M3ExpressiveMotion.fastSpatial],
 *    акцентная подсветка контура `primary` и плавный переход заливки в `surfaceContainerHighest`.
 *  - При ошибке: рамка и текст ошибки в цвете `error`.
 *  - Слоты [leadingIcon] (слева) и [trailingIcon] (справа — например, очистка или глазок пароля).
 */
@Composable
fun VkInputField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    label: String? = null,
    leadingIcon: (@Composable () -> Unit)? = null,
    trailingIcon: (@Composable () -> Unit)? = null,
    visualTransformation: androidx.compose.ui.text.input.VisualTransformation = androidx.compose.ui.text.input.VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    singleLine: Boolean = true,
    maxLines: Int = 1,
    minLines: Int = 1,
    enabled: Boolean = true,
    isError: Boolean = false,
    errorMessage: String? = null,
    textStyle: androidx.compose.ui.text.TextStyle = MaterialTheme.typography.vkText,
    containerColor: Color = MaterialTheme.colorScheme.surfaceVariant,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    val cornerRadius by animateDpAsState(
        targetValue = if (isFocused) 16.dp else 12.dp,
        animationSpec = M3ExpressiveMotion.fastSpatial(),
        label = "VkInputFieldCorner",
    )

    val bgColor by animateColorAsState(
        targetValue = when {
            !enabled -> containerColor.copy(alpha = 0.5f)
            isFocused -> MaterialTheme.colorScheme.surfaceContainerHighest
            else -> containerColor
        },
        animationSpec = M3ExpressiveMotion.fastEffects(),
        label = "VkInputFieldBg",
    )

    val borderColor by animateColorAsState(
        targetValue = when {
            isError -> MaterialTheme.colorScheme.error
            isFocused -> MaterialTheme.colorScheme.primary
            else -> Color.Transparent
        },
        animationSpec = M3ExpressiveMotion.fastEffects(),
        label = "VkInputFieldBorder",
    )

    Column(modifier = modifier.fillMaxWidth()) {
        if (!label.isNullOrBlank()) {
            Text(
                text = label,
                style = MaterialTheme.typography.vkSubheadMedium,
                color = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 4.dp, bottom = 6.dp),
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = 52.dp)
                .clip(RoundedCornerShape(cornerRadius))
                .background(bgColor)
                .then(
                    if (isFocused || isError) {
                        Modifier.border(
                            width = if (isFocused) 1.5.dp else 1.dp,
                            color = borderColor,
                            shape = RoundedCornerShape(cornerRadius),
                        )
                    } else Modifier
                )
                .padding(horizontal = 14.dp, vertical = if (singleLine) 0.dp else 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (leadingIcon != null) {
                Box(
                    modifier = Modifier.size(24.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    leadingIcon()
                }
                Spacer(modifier = Modifier.width(10.dp))
            }

            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.CenterStart,
            ) {
                if (value.isEmpty() && placeholder != null) {
                    Text(
                        text = placeholder,
                        style = textStyle,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f),
                        maxLines = maxLines,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = enabled,
                    singleLine = singleLine,
                    maxLines = maxLines,
                    minLines = minLines,
                    textStyle = textStyle.copy(
                        color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
                    ),
                    cursorBrush = SolidColor(if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary),
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

        if (!errorMessage.isNullOrBlank()) {
            Text(
                text = errorMessage,
                style = MaterialTheme.typography.vkCaption1,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(start = 6.dp, top = 4.dp),
            )
        }
    }
}

