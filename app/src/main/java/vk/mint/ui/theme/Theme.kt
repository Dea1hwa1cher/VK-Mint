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
 * Fix #UISCALE (2026-10-02) / VKUI v8 Hybrid (2026-10-13):
 * Масштаб ВСЕГО интерфейса (кнопки/панели/текст).
 *
 * Локальный CompositionLocal-коэффициент. По стандарту VKUI v8 базовый дефолт равен `1.0f` (`100%`),
 * так как типографическая сетка VKUI (`15sp` Paragraph, `16sp` Headline 1, `13sp` Footnote с `0.sp`
 * трекингом) изначально спроектирована компактной без искусственного сжатия.
 * Пользователю доступны дискретные пресеты `0.75 / 0.85 / 1.0 / 1.15 / 1.3` в Настройках.
 *
 * Рычаги применения:
 *  - ТЕКСТ (sp): VkMintTheme умножает LocalDensity.fontScale на uiScale (и на
 *    существующий fontScale-параметр) — все sp-значения масштабируются глобально.
 *  - ПАНЕЛИ/КНОПКИ (dp): явно через helper [UiScale.scaled] (Dp.scaled()), который
 *    умножает размер на LocalUiScale и применяется в ключевых панелях (TopAppBar,
 *    панель ввода чата, лента, уведомления) и в компонентах, где нужен масштаб dp.
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
 * VK Mint 2.0 theme — гибридная палитра **VKUI v8 (`vkBase` / `vkBaseDark`) × Material 3 Expressive (M3E)**:
 *  - Официальные фоны подложек и карточек-островов (`#EBEDF0`/`#FFFFFF` в Light, `#0A0A0A`/`#19191A` в Dark).
 *  - Фирменные серо-голубые вторичные тексты и иконки ВКонтакте (`#818C99` / `#909499`).
 *  - Непрозрачные (opaque) тональные контейнеры M3E (`primaryContainer`, `secondaryContainer`, `tertiaryContainer`),
 *    вычисляемые через `lerp` от базовой поверхности `Group` и выбранного акцента.
 */
@Composable
fun VkMintTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    monetHybrid: Boolean = false,
    accentIndex: Int = 0,
    fontScale: Int = 100,
    uiScale: Float = 1.0f,
    avatarShape: AvatarShape = AvatarShape.CIRCLE,
    content: @Composable () -> Unit,
) {
    val rawAccent = VkMintColors.accents.getOrElse(accentIndex) { VkMintColors.VK_MODERN }
    // В тёмной теме vkBaseDark фирменный синий #0077FF автоматически осветляется до #529EF4
    // (--vkui--color_background_accent / --vkui--color_text_accent) для соблюдения WCAG AA на фоне #19191A.
    val accent = if (darkTheme && rawAccent == VkMintColors.VK_MODERN) {
        VkMintColors.VkDarkAccentBlue
    } else {
        rawAccent
    }
    val white = Color.White

    // #WCAG-CONTRAST: для фирменного синего ВК (и в Light #0077FF, и в Dark #529EF4)
    // текст на акцентных кнопках всегда чисто белый (--vkui--color_text_contrast).
    val onAccent = when {
        rawAccent == VkMintColors.VK_MODERN -> white
        accent.luminance() > 0.42f -> VkMintColors.Black
        else -> white
    }
    val secondaryRole = deriveM3ESecondary(accent, darkTheme)
    val onSecondaryRole = if (secondaryRole.luminance() > 0.42f) VkMintColors.Black else white
    val tertiaryRole = deriveM3ETertiary(accent, darkTheme)
    val onTertiaryRole = if (tertiaryRole.luminance() > 0.42f) VkMintColors.Black else white

    // Непрозрачные (opaque) тональные контейнеры по стандарту M3E 2025 поверх островной поверхности VKUI:
    // исключают просвечивание FAB, активных карточек и таблеток навигации при скролле.
    val darkSurfaceBase = VkMintColors.VkDarkContent // #19191A (--vkui--color_background_content)
    val primaryContainerDark = if (rawAccent == VkMintColors.Black) Color(0xFF2C2D2E) else lerp(darkSurfaceBase, accent, 0.26f)
    val onPrimaryContainerDark = if (rawAccent == VkMintColors.Black) VkMintColors.VkDarkTextPrimary else lerp(accent, white, 0.72f)
    val secondaryContainerDark = if (rawAccent == VkMintColors.Black) Color(0xFF28292A) else lerp(darkSurfaceBase, secondaryRole, 0.24f)
    val onSecondaryContainerDark = if (rawAccent == VkMintColors.Black) VkMintColors.VkDarkTextPrimary else lerp(secondaryRole, white, 0.78f)
    val tertiaryContainerDark = if (rawAccent == VkMintColors.Black) Color(0xFF232324) else lerp(darkSurfaceBase, tertiaryRole, 0.24f)
    val onTertiaryContainerDark = if (rawAccent == VkMintColors.Black) VkMintColors.VkDarkTextPrimary else lerp(tertiaryRole, white, 0.75f)

    val primaryContainerLight = if (rawAccent == VkMintColors.Black) Color(0xFFE8EAED) else lerp(white, rawAccent, 0.14f)
    val onPrimaryContainerLight = if (rawAccent == VkMintColors.Black) VkMintColors.Black else lerp(rawAccent, VkMintColors.Black, 0.45f)
    val secondaryContainerLight = if (rawAccent == VkMintColors.Black) Color(0xFFE5E7EB) else lerp(white, secondaryRole, 0.16f)
    val onSecondaryContainerLight = if (rawAccent == VkMintColors.Black) VkMintColors.Black else lerp(secondaryRole, VkMintColors.Black, 0.55f)
    val tertiaryContainerLight = if (rawAccent == VkMintColors.Black) Color(0xFFEBEDF0) else lerp(white, tertiaryRole, 0.15f)
    val onTertiaryContainerLight = if (rawAccent == VkMintColors.Black) VkMintColors.Black else lerp(tertiaryRole, VkMintColors.Black, 0.50f)

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
            // Официальная тёмная схема VKUI v8 (vkBaseDark):
            background = VkMintColors.VkDarkBackground,             // #0A0A0A (--vkui--color_background)
            onBackground = VkMintColors.VkDarkTextPrimary,          // #E1E3E6 (--vkui--color_text_primary)
            surface = VkMintColors.VkDarkContent,                   // #19191A (--vkui--color_background_content)
            onSurface = VkMintColors.VkDarkTextPrimary,             // #E1E3E6 (--vkui--color_text_primary)
            surfaceVariant = VkMintColors.VkDarkSecondary,          // #232324 (--vkui--color_field_background)
            onSurfaceVariant = Color(0xFF909499),                   // Серо-стальной вторичный текст/иконки ВК (WCAG AA на #19191A)
            surfaceDim = VkMintColors.VkDarkBackground,             // #0A0A0A
            surfaceBright = Color(0xFF363738),
            surfaceContainerLowest = VkMintColors.VkDarkBackground, // #0A0A0A
            surfaceContainerLow = VkMintColors.VkDarkModal,         // #1C1D1E (--vkui--color_background_modal)
            surfaceContainer = VkMintColors.VkDarkTertiary,         // #202021 (--vkui--color_background_tertiary)
            surfaceContainerHigh = VkMintColors.VkDarkSecondary,    // #232324 (--vkui--color_background_secondary)
            surfaceContainerHighest = VkMintColors.VkDarkElevated,  // #2C2D2E
            outline = VkMintColors.VkDarkTextSecondary,             // #76787A (--vkui--color_icon_secondary)
            outlineVariant = VkMintColors.VkDarkSeparator,          // #363738 (--vkui--color_separator_primary)
            error = VkMintColors.VkNegativeDark,                    // #FF5C5C (--vkui--color_text_negative)
            onError = Color(0xFF19191A),
            errorContainer = Color(0xFF521818),
            onErrorContainer = Color(0xFFFFDAD6),
            inverseSurface = VkMintColors.VkDarkTextPrimary,
            inverseOnSurface = VkMintColors.VkDarkContent,
            inversePrimary = if (rawAccent == VkMintColors.Black) white else rawAccent,
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
            // Официальная светлая схема VKUI v8 (vkBase):
            background = VkMintColors.VkLightBackground,            // #EBEDF0 (--vkui--color_background)
            onBackground = VkMintColors.VkLightTextPrimary,         // #000000 (--vkui--color_text_primary)
            surface = VkMintColors.VkLightContent,                  // #FFFFFF (--vkui--color_background_content)
            onSurface = VkMintColors.VkLightTextPrimary,            // #000000 (--vkui--color_text_primary)
            surfaceVariant = VkMintColors.VkLightField,             // #F2F3F5 (--vkui--color_field_background)
            onSurfaceVariant = VkMintColors.VkLightTextSecondary,   // #818C99 (--vkui--color_text_secondary / icon_medium)
            surfaceDim = Color(0xFFE1E3E6),
            surfaceBright = VkMintColors.VkLightContent,            // #FFFFFF
            surfaceContainerLowest = VkMintColors.VkLightContent,   // #FFFFFF
            surfaceContainerLow = Color(0xFFF9F9FA),
            surfaceContainer = VkMintColors.VkLightSecondary,       // #F5F5F5 (--vkui--color_background_secondary)
            surfaceContainerHigh = VkMintColors.VkLightField,       // #F2F3F5 (--vkui--color_field_background)
            surfaceContainerHighest = Color(0xFFE5E7EB),
            outline = VkMintColors.VkLightIconSecondary,            // #99A2AD (--vkui--color_icon_secondary)
            outlineVariant = VkMintColors.VkLightSeparator,         // #D7D8D9 (--vkui--color_separator_primary)
            error = VkMintColors.VkNegative,                        // #E64646 (--vkui--color_background_negative)
            onError = white,
            errorContainer = Color(0xFFFFECEC),
            onErrorContainer = Color(0xFF5C1010),
            inverseSurface = Color(0xFF232324),
            inverseOnSurface = Color(0xFFF2F3F5),
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

