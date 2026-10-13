package vk.mint.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * VK Mint 2.0 typography — гибридная типографическая шкала **VKUI v8 × Material 3 Expressive (M3E)**.
 *
 * Базовые метрики выровнены по дизайн-системе ВКонтакте (`@vkontakte/vkui-tokens`):
 *  - Нулевой трекинг (`letterSpacing = 0.sp`) во всех рабочих кеглях (вместо разреженного M3 `0.25..0.5sp`),
 *    обеспечивающий фирменную плотную вёрстку VK Sans / Roboto при масштабе `1.0f` (`100%`).
 *  - Уплотнённый интерлиньяж (`lineHeight = 20.sp` для `15–16sp`, `18.sp` для `14sp`, `16.sp` для `13sp`).
 *  - Полная поддержка компаньон-токенов M3E (`*Emphasized`) для выразительных акцентов и заголовков.
 */
val VkMintTypography = Typography(
    displayLarge   = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold,     fontSize = 57.sp, lineHeight = 64.sp, letterSpacing = (-0.25).sp),
    displayMedium  = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold,     fontSize = 45.sp, lineHeight = 52.sp, letterSpacing = 0.sp),
    displaySmall   = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold, fontSize = 36.sp, lineHeight = 44.sp, letterSpacing = 0.sp),
    headlineLarge  = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold, fontSize = 32.sp, lineHeight = 38.sp, letterSpacing = 0.sp),
    headlineMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold, fontSize = 28.sp, lineHeight = 34.sp, letterSpacing = 0.sp),
    // VKUI Title 1 (24sp / 28sp, SemiBold 600)
    headlineSmall  = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold, fontSize = 24.sp, lineHeight = 28.sp, letterSpacing = 0.sp),
    // VKUI Title 2 (20sp / 24sp, SemiBold 600)
    titleLarge     = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold, fontSize = 20.sp, lineHeight = 24.sp, letterSpacing = 0.sp),
    // VKUI Headline 1 (16sp / 20sp, Medium 500)
    titleMedium    = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Medium,   fontSize = 16.sp, lineHeight = 20.sp, letterSpacing = 0.sp),
    // VKUI Headline 2 (15sp / 20sp, Medium 500)
    titleSmall     = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Medium,   fontSize = 15.sp, lineHeight = 20.sp, letterSpacing = 0.sp),
    // VKUI Text (16sp / 20sp, Regular 400)
    bodyLarge      = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Normal,   fontSize = 16.sp, lineHeight = 20.sp, letterSpacing = 0.sp),
    // VKUI Paragraph (15sp / 20sp, Regular 400 — основной текст постов и сообщений ВК)
    bodyMedium     = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Normal,   fontSize = 15.sp, lineHeight = 20.sp, letterSpacing = 0.sp),
    // VKUI Subhead (14sp / 18sp, Regular 400 — вторичные подписи SimpleCell, время, метаданные)
    bodySmall      = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Normal,   fontSize = 14.sp, lineHeight = 18.sp, letterSpacing = 0.sp),
    // VKUI Subhead Medium / Кнопки (14sp / 18sp, Medium 500)
    labelLarge     = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Medium,   fontSize = 14.sp, lineHeight = 18.sp, letterSpacing = 0.sp),
    // VKUI Footnote (13sp / 16sp, Regular 400 — счётчики лайков, секционные шапки, подписи)
    labelMedium    = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Normal,   fontSize = 13.sp, lineHeight = 16.sp, letterSpacing = 0.sp),
    // VKUI Caption 1 (12sp / 14sp, Regular 400 — мелкие бейджи, таймстемпы)
    labelSmall     = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Normal,   fontSize = 12.sp, lineHeight = 14.sp, letterSpacing = 0.sp),
)

/**
 * Material 3 Expressive (M3E) Emphasized Typography Tokens (адаптированные под сетку VKUI v8).
 *
 * Позволяют повышать визуальный вес заголовков, активных табов и бейджей без разрушения
 * компактного интерлиньяжа VKUI.
 */
val Typography.displayLargeEmphasized: TextStyle
    get() = displayLarge.copy(fontWeight = FontWeight.Bold)

val Typography.displayMediumEmphasized: TextStyle
    get() = displayMedium.copy(fontWeight = FontWeight.Bold)

val Typography.displaySmallEmphasized: TextStyle
    get() = displaySmall.copy(fontWeight = FontWeight.Bold)

val Typography.headlineLargeEmphasized: TextStyle
    get() = headlineLarge.copy(fontWeight = FontWeight.Bold)

val Typography.headlineMediumEmphasized: TextStyle
    get() = headlineMedium.copy(fontWeight = FontWeight.Bold)

val Typography.headlineSmallEmphasized: TextStyle
    get() = headlineSmall.copy(fontWeight = FontWeight.Bold)

val Typography.titleLargeEmphasized: TextStyle
    get() = titleLarge.copy(fontWeight = FontWeight.Bold)

/** Соответствует VKUI Title 3 (17sp / 22sp, SemiBold 600) или усиленному Headline 1. */
val Typography.titleMediumEmphasized: TextStyle
    get() = titleMedium.copy(fontWeight = FontWeight.SemiBold)

val Typography.titleSmallEmphasized: TextStyle
    get() = titleSmall.copy(fontWeight = FontWeight.SemiBold)

val Typography.bodyLargeEmphasized: TextStyle
    get() = bodyLarge.copy(fontWeight = FontWeight.Medium, letterSpacing = 0.sp)

val Typography.bodyMediumEmphasized: TextStyle
    get() = bodyMedium.copy(fontWeight = FontWeight.Medium)

val Typography.bodySmallEmphasized: TextStyle
    get() = bodySmall.copy(fontWeight = FontWeight.Medium)

val Typography.labelLargeEmphasized: TextStyle
    get() = labelLarge.copy(fontWeight = FontWeight.SemiBold)

val Typography.labelMediumEmphasized: TextStyle
    get() = labelMedium.copy(fontWeight = FontWeight.SemiBold)

val Typography.labelSmallEmphasized: TextStyle
    get() = labelSmall.copy(fontWeight = FontWeight.Medium)

// ═══════════════════════════════════════════════════════════════════════════
//  Семантические токены типографики VKUI v8 (прямой маппинг vkui.io)
// ═══════════════════════════════════════════════════════════════════════════

/** VKUI Title 1 — `24sp / 28sp`, SemiBold (600). Крупные заголовки экранов и модалок. */
val Typography.vkTitle1: TextStyle
    get() = headlineSmall

/** VKUI Title 2 — `20sp / 24sp`, SemiBold (600). Заголовки `PanelHeader` и `Group`. */
val Typography.vkTitle2: TextStyle
    get() = titleLarge

/** VKUI Title 3 — `17sp / 22sp`, SemiBold (600). Подзаголовки карточек и диалогов. */
val Typography.vkTitle3: TextStyle
    get() = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 17.sp,
        lineHeight = 22.sp,
        letterSpacing = 0.sp,
    )

/** VKUI Headline 1 — `16sp / 20sp`, Medium (500). Имена в `SimpleCell`, названия чатов и треков. */
val Typography.vkHeadline1: TextStyle
    get() = titleMedium

/** VKUI Headline 2 — `15sp / 20sp`, Medium (500). Имена авторов постов, компактные заголовки. */
val Typography.vkHeadline2: TextStyle
    get() = titleSmall

/** VKUI Text — `16sp / 20sp`, Regular (400). Основной текст ячеек и полей ввода. */
val Typography.vkText: TextStyle
    get() = bodyLarge

/** VKUI Paragraph — `15sp / 20sp`, Regular (400). Текст постов ленты, комментариев и сообщений. */
val Typography.vkParagraph: TextStyle
    get() = bodyMedium

/** VKUI Subhead — `14sp / 18sp`, Regular (400). Вторичные подписи `subtitle`, время, артисты. */
val Typography.vkSubhead: TextStyle
    get() = bodySmall

/** VKUI Subhead Medium — `14sp / 18sp`, Medium (500). Кнопки, табы и выделенные подписи. */
val Typography.vkSubheadMedium: TextStyle
    get() = labelLarge

/** VKUI Footnote — `13sp / 16sp`, Regular (400). Счётчики реакций, `Header` секций, подсказки. */
val Typography.vkFootnote: TextStyle
    get() = labelMedium

/** VKUI Footnote Caps — `13sp / 16sp`, SemiBold (600). Верхний регистр секционных заголовков. */
val Typography.vkFootnoteCaps: TextStyle
    get() = labelMediumEmphasized

/** VKUI Caption 1 — `12sp / 14sp`, Regular (400). Компактные бейджи и метаданные. */
val Typography.vkCaption1: TextStyle
    get() = labelSmall

/** VKUI Caption 2 — `11sp / 14sp`, Medium (500). Подписи таббара (`TabbarItem`) и микро-бейджи. */
val Typography.vkCaption2: TextStyle
    get() = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 14.sp,
        letterSpacing = 0.sp,
    )

// ═══════════════════════════════════════════════════════════════════════════
//  Top-level Convenience Accessors (VKUI Typography)
// ═══════════════════════════════════════════════════════════════════════════

val vkTitle1: TextStyle get() = VkMintTypography.vkTitle1
val vkTitle2: TextStyle get() = VkMintTypography.vkTitle2
val vkTitle3: TextStyle get() = VkMintTypography.vkTitle3
val vkHeadline1: TextStyle get() = VkMintTypography.vkHeadline1
val vkHeadline2: TextStyle get() = VkMintTypography.vkHeadline2
val vkText: TextStyle get() = VkMintTypography.vkText
val vkParagraph: TextStyle get() = VkMintTypography.vkParagraph
val vkSubhead: TextStyle get() = VkMintTypography.vkSubhead
val vkSubheadMedium: TextStyle get() = VkMintTypography.vkSubheadMedium
val vkFootnote: TextStyle get() = VkMintTypography.vkFootnote
val vkFootnoteCaps: TextStyle get() = VkMintTypography.vkFootnoteCaps
val vkCaption1: TextStyle get() = VkMintTypography.vkCaption1
val vkCaption2: TextStyle get() = VkMintTypography.vkCaption2


