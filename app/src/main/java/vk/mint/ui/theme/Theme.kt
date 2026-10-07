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
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.sp

private val LocalVkMintAccent = compositionLocalOf { VkMintColors.Black }

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
 * optional Material You dynamic colors.
 *
 * Inspired by VK's ColorScheme system (VK Light / VK Dark / VK Blue),
 * but preconfigured for the VK Mint "B&W + accent" look.
 *
 * fontScale: целочисленный масштаб шрифта в процентах (70..150).
 *   100 = системный размер, <100 = мельче, >100 = крупнее.
 *   Реализован через переопределение LocalDensity (fontScale множитель)
 *   И scaled-копию VkMintTypography (на случай прямых .sp без MaterialTheme).
 *
 * uiScale (Fix #UISCALE): масштаб ВСЕГО интерфейса (0.75..1.3, дефолт 0.85 = −15%).
 *   Умножает fontScale (текст масштабируется глобально через LocalDensity) и
 *   поставляется в LocalUiScale — key-панели/кнопки масштабируют dp-размеры
 *   через UiScale.scaled(). Растёт с настройкой (интерфейс не замерзает на −15%).
 *
 * #MONET-DYNAMIC-COLOR: Material You / Monet — адаптивная цветовая тема,
 * доступна ТОЛЬКО на Android 12+ (API 31, S). На более старых версиях
 * Android система НЕ извлекает цвета из обоев — SystemColors API
 * отсутствует. Compose Material3 dynamicLightColorScheme/dynamicDarkColorScheme
 * помечены @RequiresApi(Build.VERSION_CODES.S), поэтому вызов ниже API 31
 * либо невозможен (если не защищён), либо вернёт fallback colorScheme
 * (НЕ настоящие dynamic colors).
 *
 * На Android < 12 мы игнорируем dynamicColor=true и используем статичную
 * B&W схему с пользовательским accent color. Пользователь видит в настройках
 * что Material You переключатель доступен, но если он на старом Android —
 * переключатель бесполезен (можно отключить визуально, но это усложнит UI;
 * лучше оставить включаемым с silent fallback).
 *
 * Источник: https://developer.android.com/develop/ui/views/theming/dynamic-colors
 */
@Composable
fun VkMintTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    monetHybrid: Boolean = false,
    accentIndex: Int = 0,
    fontScale: Int = 100,
    uiScale: Float = 0.85f,
    content: @Composable () -> Unit,
) {
    val accent = VkMintColors.accents.getOrElse(accentIndex) { VkMintColors.Black }
    val white = Color.White

    // #WCAG-CONTRAST: динамический расчет контрастного onAccent.
    // Для ярких/светлых акцентов (luminance > 0.4f, например Lime, Orange)
    // черный текст обеспечивает WCAG AA/AAA контраст (>= 4.5:1).
    // Для темных и насыщенных (VK Modern, VK Classic, Red, Blue, Black) — белый текст.
    val onAccent = if (accent.luminance() > 0.4f) VkMintColors.Black else white

    // #MONET-DYNAMIC-COLOR: dynamic colors только на Android 12+ (API 31, S).
    // На более старых версиях dynamicLightColorScheme/dynamicDarkColorScheme
    // либо crash (нет @RequiresApi guard), либо возвращают fallback. Мы
    // явно проверяем SDK_INT — на Android < 12 dynamicColor silent-отключается.
    val canUseDynamicColor = dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

    // #MONET-HYBRID: гибридный режим Material You.
    // Проблема: dynamicLightColorScheme/dynamicDarkColorScheme возвращают
    //   ПОЛНОСТЬЮ обоевую схему — primary/secondary/tertiary = цвета из обоев,
    //   а НЕ пользовательский accent. Все Material3-компоненты (Switch, Button,
    //   FAB, ProgressIndicator и т.п. через MaterialTheme.colorScheme.primary)
    //   перекрашиваются под обои, и выбранный accent теряется.
    // Гибрид: берём dynamic-схему, но .copy() оверрайдим accent-роли (primary,
    //   secondary, tertiary, onPrimary, onSecondary, onTertiary, container-токены) = accent.
    //   Surface/background/surfaceVariant остаются от обоев. Так активные
    //   элементы (кнопки, свитчи, прогресс) = accent, а подложка = обои.
    // При monetHybrid=false — чистый Monet (все роли = обои), как раньше.
    val colorScheme = when {
        canUseDynamicColor -> {
            val ctx = LocalContext.current
            val dyn = if (darkTheme) dynamicDarkColorScheme(ctx) else dynamicLightColorScheme(ctx)
            if (monetHybrid) {
                val primaryContainerColor = if (darkTheme) {
                    if (accent == VkMintColors.Black) Color(0xFF2C2C2C) else accent.copy(alpha = 0.22f)
                } else {
                    if (accent == VkMintColors.Black) Color(0xFFE8E8E8) else accent.copy(alpha = 0.12f)
                }
                val onPrimaryContainerColor = if (darkTheme) {
                    white
                } else {
                    if (accent == VkMintColors.Black || accent.luminance() > 0.4f) VkMintColors.Black else accent
                }
                dyn.copy(
                    primary = accent,
                    onPrimary = onAccent,
                    primaryContainer = primaryContainerColor,
                    onPrimaryContainer = onPrimaryContainerColor,
                    secondary = accent,
                    onSecondary = onAccent,
                    secondaryContainer = if (darkTheme) Color(0xFF2C2C2C) else Color(0xFFE2E7EE),
                    onSecondaryContainer = if (darkTheme) white else VkMintColors.Black,
                    tertiary = accent,
                    onTertiary = onAccent,
                    tertiaryContainer = if (darkTheme) {
                        if (accent == VkMintColors.Black) Color(0xFF242424) else accent.copy(alpha = 0.15f)
                    } else {
                        if (accent == VkMintColors.Black) Color(0xFFF0F0F0) else accent.copy(alpha = 0.08f)
                    },
                    onTertiaryContainer = onPrimaryContainerColor,
                    // inversePrimary оставляем от обоев — для inverse-поверхностей.
                )
            } else {
                dyn
            }
        }
        darkTheme -> darkColorScheme(
            primary = accent,
            onPrimary = onAccent,
            primaryContainer = if (accent == VkMintColors.Black) Color(0xFF2C2C2C) else accent.copy(alpha = 0.22f),
            onPrimaryContainer = white,
            secondary = accent,
            onSecondary = onAccent,
            secondaryContainer = Color(0xFF2C2C2C),
            onSecondaryContainer = white,
            tertiary = accent,
            onTertiary = onAccent,
            tertiaryContainer = if (accent == VkMintColors.Black) Color(0xFF242424) else accent.copy(alpha = 0.15f),
            onTertiaryContainer = white,
            background = Color(0xFF121212),       // VK desktop: page background
            onBackground = white,
            surface = Color(0xFF1E1E1E),          // VK desktop: card/surface
            onSurface = white,
            surfaceVariant = Color(0xFF2A2A2A),   // VK: slightly lighter surface
            onSurfaceVariant = Color(0xFF999999),  // VK: secondary text
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
            primaryContainer = if (accent == VkMintColors.Black) Color(0xFFE8E8E8) else accent.copy(alpha = 0.12f),
            onPrimaryContainer = if (accent == VkMintColors.Black || accent.luminance() > 0.4f) VkMintColors.Black else accent,
            secondary = accent,
            onSecondary = onAccent,
            secondaryContainer = Color(0xFFE2E7EE),
            onSecondaryContainer = VkMintColors.Black,
            tertiary = accent,
            onTertiary = onAccent,
            tertiaryContainer = if (accent == VkMintColors.Black) Color(0xFFF0F0F0) else accent.copy(alpha = 0.08f),
            onTertiaryContainer = if (accent == VkMintColors.Black || accent.luminance() > 0.4f) VkMintColors.Black else accent,
            background = Color(0xFFF2F4F7),       // Modern neutral page background (separates white cards)
            onBackground = VkMintColors.Black,
            surface = white,                      // Clean white card / surface
            onSurface = VkMintColors.Black,
            surfaceVariant = Color(0xFFF0F2F5),
            onSurfaceVariant = Color(0xFF555555),
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

    // Масштаб шрифта: переопределяем LocalDensity, чтобы fontScale применился
    // ко всем sp-значениям глобально (включая явные .sp вне MaterialTheme.typography).
    // Важно: сохраняем исходный density (x dpi), меняем только fontScale.
    // Fix #UISCALE: дополнительно умножаем fontScale НА uiScale — так текст
    // масштабируется и «Размером текста» (fontScale, отдельный), и «Масштабом
    // интерфейса» (uiScale, −15% база + рост с настройкой).
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
    val scaledTypography = if (effectiveTextScale == 1f) VkMintTypography
        else scaleTypography(VkMintTypography, (effectiveTextScale * 100f).toInt())

    // Fix #UISCALE: прокидываем коэффициент масштаба в LocalUiScale, чтобы
    // key-панели/кнопки могли явно масштабировать dp-размеры через UiScale.scaled().
    val uiScaleValue = uiScale.coerceIn(0.75f, 1.3f)

    CompositionLocalProvider(
        LocalVkMintAccent provides accent,
        LocalDensity provides scaledDensity,
        LocalUiScale provides uiScaleValue,
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = scaledTypography,
            shapes = VkMintShapes,
            content = content,
        )
    }
}

/**
 * Возвращает копию Typography с пересчитанными fontSize/lineHeight.
 * Нужна как страховка для компонентов, которые берут TextStyle напрямую
 * из MaterialTheme.typography, а не через LocalDensity (редкие случаи).
 */
private fun scaleTypography(src: Typography, scalePercent: Int): Typography {
    val k = scalePercent / 100f
    fun TextStyle.scale() = copy(
        fontSize = (fontSize.value * k).sp,
        lineHeight = (lineHeight.value * k).sp,
    )
    return Typography(
        displayLarge   = src.displayLarge.scale(),
        displayMedium  = src.displayMedium.scale(),
        displaySmall   = src.displaySmall.scale(),
        headlineLarge  = src.headlineLarge.scale(),
        headlineMedium = src.headlineMedium.scale(),
        headlineSmall  = src.headlineSmall.scale(),
        titleLarge     = src.titleLarge.scale(),
        titleMedium    = src.titleMedium.scale(),
        titleSmall     = src.titleSmall.scale(),
        bodyLarge      = src.bodyLarge.scale(),
        bodyMedium     = src.bodyMedium.scale(),
        bodySmall      = src.bodySmall.scale(),
        labelLarge     = src.labelLarge.scale(),
        labelMedium    = src.labelMedium.scale(),
        labelSmall     = src.labelSmall.scale(),
    )
}

