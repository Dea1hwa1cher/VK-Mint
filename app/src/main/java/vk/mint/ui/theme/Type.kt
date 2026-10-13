package vk.mint.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * VK Mint 2.0 typography — system sans-serif, Material 3 Expressive (M3E) type scale.
 */
val VkMintTypography = Typography(
    displayLarge   = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Normal,   fontSize = 57.sp, lineHeight = 64.sp, letterSpacing = (-0.25).sp),
    displayMedium  = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Normal,   fontSize = 45.sp, lineHeight = 52.sp, letterSpacing = 0.sp),
    displaySmall   = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Normal,   fontSize = 36.sp, lineHeight = 44.sp, letterSpacing = 0.sp),
    headlineLarge  = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold, fontSize = 32.sp, lineHeight = 40.sp, letterSpacing = 0.sp),
    headlineMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold, fontSize = 28.sp, lineHeight = 36.sp, letterSpacing = 0.sp),
    headlineSmall  = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold, fontSize = 24.sp, lineHeight = 32.sp, letterSpacing = 0.sp),
    titleLarge     = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold, fontSize = 22.sp, lineHeight = 28.sp, letterSpacing = 0.sp),
    titleMedium    = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Medium,   fontSize = 16.sp, lineHeight = 24.sp, letterSpacing = 0.15.sp),
    titleSmall     = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Medium,   fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = 0.1.sp),
    bodyLarge      = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Normal,   fontSize = 16.sp, lineHeight = 24.sp, letterSpacing = 0.5.sp),
    bodyMedium     = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Normal,   fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = 0.25.sp),
    bodySmall      = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Normal,   fontSize = 12.sp, lineHeight = 16.sp, letterSpacing = 0.4.sp),
    labelLarge     = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Medium,   fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = 0.1.sp),
    labelMedium    = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Medium,   fontSize = 12.sp, lineHeight = 16.sp, letterSpacing = 0.5.sp),
    labelSmall     = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Medium,   fontSize = 11.sp, lineHeight = 16.sp, letterSpacing = 0.5.sp),
)

/**
 * Material 3 Expressive (M3E) Emphasized Typography Tokens.
 *
 * M3E introduces emphasized typography companion styles alongside standard styles
 * to provide unmistakable visual hierarchy, tactile personality, and elevated contrast.
 * Emphasized styles feature heavier weights (Bold / SemiBold / Medium) tuned for
 * display, headers, titles, and interactive labels.
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

val Typography.titleMediumEmphasized: TextStyle
    get() = titleMedium.copy(fontWeight = FontWeight.SemiBold)

val Typography.titleSmallEmphasized: TextStyle
    get() = titleSmall.copy(fontWeight = FontWeight.SemiBold)

val Typography.bodyLargeEmphasized: TextStyle
    get() = bodyLarge.copy(fontWeight = FontWeight.Medium, letterSpacing = 0.15.sp)

val Typography.bodyMediumEmphasized: TextStyle
    get() = bodyMedium.copy(fontWeight = FontWeight.Medium)

val Typography.bodySmallEmphasized: TextStyle
    get() = bodySmall.copy(fontWeight = FontWeight.Medium)

val Typography.labelLargeEmphasized: TextStyle
    get() = labelLarge.copy(fontWeight = FontWeight.SemiBold)

val Typography.labelMediumEmphasized: TextStyle
    get() = labelMedium.copy(fontWeight = FontWeight.SemiBold)

val Typography.labelSmallEmphasized: TextStyle
    get() = labelSmall.copy(fontWeight = FontWeight.SemiBold)

