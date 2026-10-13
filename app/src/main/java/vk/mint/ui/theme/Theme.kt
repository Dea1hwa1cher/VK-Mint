package vk.mint.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import vk.mint.data.model.AvatarShape

private val LocalVkMintAccent = compositionLocalOf { VkMintColors.Black }

/**
 * Генерирует гармоничный M3E secondary цвет из базового accent:
 * приглушённая насыщенность (×0.55) с небольшим сдвигом тона (+8°).
 */
private fun deriveM3ESecondary(accent: Color, darkTheme: Boolean): Color {
    if (accent == VkMintColors.Black) return if (darkTheme) Color(0xFFB8BCC2) else Color(0xFF45474A)
    val hsv = FloatArray(3)
    android.graphics.Color.colorToHSV(accent.toArgb(), hsv)
    hsv[0] = (hsv[0] + 8f) % 360f
    hsv[1] = (hsv[1] * 0.55f).coerceIn(0.15f, 0.65f)
    hsv[2] = if (darkTheme) hsv[2].coerceAtLeast(0.82f) else hsv[2].coerceIn(0.35f, 0.55f)
    return Color(android.graphics.Color.HSVToColor(hsv))
}

/**
 * Генерирует выразительный M3E tertiary цвет из базового accent:
 * смежный сдвиг оттенка (+35°) по стандарту M3 Expressive / Tonal Spot.
 */
private fun deriveM3ETertiary(accent: Color, darkTheme: Boolean): Color {
    if (accent == VkMintColors.Black) return if (darkTheme) Color(0xFFD0D4DA) else Color(0xFF2E3133)
    val hsv = FloatArray(3)
    android.graphics.Color.colorToHSV(accent.toArgb(), hsv)
    hsv[0] = (hsv[0] + 35f) % 360f
    hsv[1] = (hsv[1] * 0.78f).coerceIn(0.25f, 0.85f)
    hsv[2] = if (darkTheme) hsv[2].coerceAtLeast(0.86f) else hsv[2].coerceIn(0.38f, 0.62f)
    return Color(android.graphics.Color.HSVToColor(hsv))
}

/**
 * Fix #UISCALE (2026-10-02): масштаб ВСЕГО интерфейса (кнопки/панели/текст).
 *
 * Локальный CompositionLocal-коэффициент. Base-дефолт 0.85 (−15%) при стандартной
 * настройке. Значение поставляется из VkMintTheme (параметр uiScale, источник —
 * VkMintPrefs.uiScale, варианты 0.75/0.85/1.0/1.15/1.3) и растёт вместе с настройкой —
 * интерфейс не «замерзает» на −15%.
 *
 * Рычаги применения:
 *  - ТЕКСТ (sp): VkMintTheme умножает LocalDensity.fontScale на uiScale (и на
 *    существующий fontScale-параметр) — все sp-значения масштабируются глобально.
 *  - ПАНЕЛИ/КНОПКИ (dp): явно через helper [UiScale.scaled] (Dp.scaled()), который
 *    умножает размер на LocalUiScale и применяется в ключевых панелях (TopAppBar,
 *    панель ввода чата, лента, уведомления) и в компонентах, где нужен масштаб dp.
 *
 * Почему NOT глобальный Density.density: умножение density умножает ВСЕ dp-литералы
 * (включая внутренние отступы Material-компонентов, border'ы, псевдотачи) и может
 * ломать клик-зоны/выравнивание. Поэтому dp-масштаб сделан явным — безопасный
 * скелет: охвачены ключевые панели, остальное остается номинальных размеров.
 */
val LocalUiScale = compositionLocalOf { 1f }

/** Доступ к текущему масштабу интерфейса из любой composable. */
object UiScale {
    /** Текущий коэффициент масштаба (0.75..1.3). 1f = без масштаба. */
    val current: Float
        @ReadOnlyComposable @Composable get() = LocalUiScale.current

    /** Масштабировать Dp-размер текущим коэффициентом интерфейса. */
    @ReadOnlyComposable @Composable
    fun scaled(value: Dp): Dp = value * current
}

/** Modifier-хелпер: применяет LocalUiScale к dp-размеру (высоте/ширине/отступу). */
@ReadOnlyComposable @Composable
fun Modifier.scaled(size: Dp): Modifier = this.then(Modifier.requiredSize(size * LocalUiScale.current))

/**
 * VK Mint 2.0 theme — B&W minimalist base, swappable accent color,
 * optional Material You dynamic colors (M3E 2025 compliant).
 */
@Composable
fun VkMintTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    monetHybrid: Boolean = false,
    accentIndex: Int = 0,
    fontScale: Int = 100,
    uiScale: Float = 0.85f,
    avatarShape: AvatarShape = AvatarShape.CIRCLE,
    content: @Composable () -> Unit,
) {
    val accent = VkMintColors.accents.getOrElse(accentIndex) { VkMintColors.Black }
    val white = Color.White

    // #WCAG-CONTRAST: динамический расчет контрастного onAccent.
    val onAccent = if (accent.luminance() > 0.4f) VkMintColors.Black else white
    val secondaryRole = deriveM3ESecondary(accent, darkTheme)
    val onSecondaryRole = if (secondaryRole.luminance() > 0.4f) VkMintColors.Black else white
    val tertiaryRole = deriveM3ETertiary(accent, darkTheme)
    val onTertiaryRole = if (tertiaryRole.luminance() > 0.4f) VkMintColors.Black else white

    // Непрозрачные (opaque) тональные контейнеры по стандарту M3E 2025:
    // исключают просвечивание FAB, активных карточек и таблеток навигации при скролле.
    val darkSurfaceBase = Color(0xFF1E1E1E)
    val primaryContainerDark = if (accent == VkMintColors.Black) Color(0xFF2C2C2C) else lerp(darkSurfaceBase, accent, 0.28f)
    val onPrimaryContainerDark = if (accent == VkMintColors.Black) white else lerp(accent, white, 0.72f)
    val secondaryContainerDark = if (accent == VkMintColors.Black) Color(0xFF2C2C2C) else lerp(darkSurfaceBase, secondaryRole, 0.26f)
    val onSecondaryContainerDark = if (accent == VkMintColors.Black) white else lerp(secondaryRole, white, 0.78f)
    val tertiaryContainerDark = if (accent == VkMintColors.Black) Color(0xFF242424) else lerp(darkSurfaceBase, tertiaryRole, 0.25f)
    val onTertiaryContainerDark = if (accent == VkMintColors.Black) white else lerp(tertiaryRole, white, 0.75f)

    val primaryContainerLight = if (accent == VkMintColors.Black) Color(0xFFE8E8E8) else lerp(white, accent, 0.16f)
    val onPrimaryContainerLight = if (accent == VkMintColors.Black) VkMintColors.Black else lerp(accent, VkMintColors.Black, 0.55f)
    val secondaryContainerLight = if (accent == VkMintColors.Black) Color(0xFFE2E7EE) else lerp(white, secondaryRole, 0.18f)
    val onSecondaryContainerLight = if (accent == VkMintColors.Black) VkMintColors.Black else lerp(secondaryRole, VkMintColors.Black, 0.60f)
    val tertiaryContainerLight = if (accent == VkMintColors.Black) Color(0xFFF0F0F0) else lerp(white, tertiaryRole, 0.16f)
    val onTertiaryContainerLight = if (accent == VkMintColors.Black) VkMintColors.Black else lerp(tertiaryRole, VkMintColors.Black, 0.55f)

    val canUseDynamicColor = dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

    val colorScheme = when {
        canUseDynamicColor -> {
            val ctx = LocalContext.current
            val dyn = if (darkTheme) dynamicDarkColorScheme(ctx) else dynamicLightColorScheme(ctx)
            if (monetHybrid) {
                dyn.copy(
                    primary = accent,
                    onPrimary = onAccent,
                    primaryContainer = if (darkTheme) primaryContainerDark else primaryContainerLight,
                    onPrimaryContainer = if (darkTheme) onPrimaryContainerDark else onPrimaryContainerLight,
                    secondary = secondaryRole,
                    onSecondary = onSecondaryRole,
                    secondaryContainer = if (darkTheme) secondaryContainerDark else secondaryContainerLight,
                    onSecondaryContainer = if (darkTheme) onSecondaryContainerDark else onSecondaryContainerLight,
                    tertiary = tertiaryRole,
                    onTertiary = onTertiaryRole,
                    tertiaryContainer = if (darkTheme) tertiaryContainerDark else tertiaryContainerLight,
                    onTertiaryContainer = if (darkTheme) onTertiaryContainerDark else onTertiaryContainerLight,
                )
            } else {
                dyn
            }
        }
        darkTheme -> darkColorScheme(
            primary = accent,
            onPrimary = onAccent,
            primaryContainer = primaryContainerDark,
            onPrimaryContainer = onPrimaryContainerDark,
            secondary = secondaryRole,
            onSecondary = onSecondaryRole,
            secondaryContainer = secondaryContainerDark,
            onSecondaryContainer = onSecondaryContainerDark,
            tertiary = tertiaryRole,
            onTertiary = onTertiaryRole,
            tertiaryContainer = tertiaryContainerDark,
            onTertiaryContainer = onTertiaryContainerDark,
            background = Color(0xFF121212),
            onBackground = white,
            surface = darkSurfaceBase,
            onSurface = white,
            surfaceVariant = Color(0xFF2A2A2A),
            onSurfaceVariant = Color(0xFF999999),
            surfaceDim = Color(0xFF121212),
            surfaceBright = Color(0xFF38383A),
            surfaceContainerLowest = Color(0xFF0F0F10),
            surfaceContainerLow = Color(0xFF1A1A1A),
            surfaceContainer = Color(0xFF1E1E1E),
            surfaceContainerHigh = Color(0xFF262626),
            surfaceContainerHighest = Color(0xFF333333),
            outline = Color(0xFF444444),
            outlineVariant = Color(0xFF333333),
            error = Color(0xFFCF6679),
            onError = Color(0xFF1E1E1E),
            errorContainer = Color(0xFF93000A),
            onErrorContainer = Color(0xFFFFDAD6),
            inverseSurface = Color(0xFFE2E2E6),
            inverseOnSurface = Color(0xFF1B1B1F),
            inversePrimary = if (accent == VkMintColors.Black) white else accent,
        )
        else -> lightColorScheme(
            primary = accent,
            onPrimary = onAccent,
            primaryContainer = primaryContainerLight,
            onPrimaryContainer = onPrimaryContainerLight,
            secondary = secondaryRole,
            onSecondary = onSecondaryRole,
            secondaryContainer = secondaryContainerLight,
            onSecondaryContainer = onSecondaryContainerLight,
            tertiary = tertiaryRole,
            onTertiary = onTertiaryRole,
            tertiaryContainer = tertiaryContainerLight,
            onTertiaryContainer = onTertiaryContainerLight,
            background = Color(0xFFF2F4F7),
            onBackground = VkMintColors.Black,
            surface = white,
            onSurface = VkMintColors.Black,
            surfaceVariant = Color(0xFFF0F2F5),
            onSurfaceVariant = Color(0xFF555555),
            surfaceDim = Color(0xFFDADCE0),
            surfaceBright = white,
            surfaceContainerLowest = white,
            surfaceContainerLow = Color(0xFFF7F9FB),
            surfaceContainer = Color(0xFFECEFF3),
            surfaceContainerHigh = Color(0xFFE6E9EE),
            surfaceContainerHighest = Color(0xFFE0E3E8),
            outline = Color(0xFFCCCCCC),
            outlineVariant = Color(0xFFE0E0E0),
            error = Color(0xFFBA1A1A),
            onError = white,
            errorContainer = Color(0xFFFFDAD6),
            onErrorContainer = Color(0xFF410002),
            inverseSurface = Color(0xFF2E3133),
            inverseOnSurface = Color(0xFFF1F0F4),
            inversePrimary = white,
        )
    }

    // Масштаб шрифта: переопределяем LocalDensity.fontScale ОДИН раз.
    // Compose автоматически умножает все .sp (включая VkMintTypography) на LocalDensity.fontScale,
    // поэтому повторное умножение через scaleTypography давало квадратичное сжатие (scale^2).
    val original = LocalDensity.current
    val effectiveTextScale = (fontScale / 100f) * uiScale
    val scaledDensity = if (effectiveTextScale == 1f) {
        original
    } else {
        Density(
            density = original.density,
            fontScale = original.fontScale * effectiveTextScale,
        )
    }

    val uiScaleValue = uiScale.coerceIn(0.75f, 1.3f)
    val currentAvatarShape = AvatarShapes.forShape(avatarShape)

    CompositionLocalProvider(
        LocalVkMintAccent provides accent,
        LocalDensity provides scaledDensity,
        LocalUiScale provides uiScaleValue,
        LocalAvatarShape provides currentAvatarShape,
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = VkMintTypography,
            shapes = VkMintShapes,
            content = content,
        )
    }
}

