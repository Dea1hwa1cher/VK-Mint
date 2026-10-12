package vk.mint.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance

// VK Mint 2.0 color palette — B&W minimalist base + VK accents + 10 accent colors.
//
// Mirrors the original VK Mint accent set (VkMintBlack, VkMintRed, etc.)
// but rebuilt with Compose Color values.
//
// P0.21 #VK-ACCENTS (2026-10): добавлены VK-фирменные акценты в НАЧАЛО списка —
// становятся первыми при переключении акцента цвета в настройках (slider).
// Историческая палитра VK (от старой к современной):
//   - VK Classic 2016-2022: #4A76A8 (классический «вкотый» синий)
//   - VK Modern 2022+: #0077FF (ярко-синий, «цифровой»)
//   - VK Pink/Red (редкий акцент из VK Donut/Pro): #FF3347
// Фирменные цвета выбраны по официальной VK Brand Guidelines (vk.design).
object VkMintColors {

    // Base B&W (always present)
    val Black   = Color(0xFF000000)
    val White   = Color(0xFFFFFFFF)
    val Gray    = Color(0xFF888888)
    val Light   = Color(0xFFEEEEEE)
    val Dark    = Color(0xFF111111)

    // P0.21: VK-фирменные акценты в НАЧАЛЕ списка (indices 0-2).
    // Это первые цвета при переключении акцента в настройках (slider start).
    val VK_MODERN   = Color(0xFF0077FF)  // VK 2022+ «цифровой» синий (основной акцент VK)
    val VK_CLASSIC  = Color(0xFF4A76A8)  // VK 2016-2022 классический «вкотый» синий
    val VK_PINK     = Color(0xFFE033AC)  // VK Pink/Pro акцент (Donut/Pro highlights)

    // 13 accent colors: 3 VK + 10 original VK Mint (matches VK Mint 1.2.1 + VK).
    val accents: List<Color> = listOf(
        Color(0xFF0077FF), // 0: VK Modern (P0.21 — primary VK accent, default)
        Color(0xFF4A76A8), // 1: VK Classic (вкотый синий, 2016-2022)
        Color(0xFFE033AC), // 2: VK Pink (Pro/Donut)
        Color(0xFF000000), // 3: Pure Black (старый default, сдвинут с 0 на 3)
        Color(0xFFE53935), // 4: Red
        Color(0xFF1E88E5), // 5: Blue
        Color(0xFF43A047), // 6: Green
        Color(0xFFFB8C00), // 7: Orange
        Color(0xFF8E24AA), // 8: Purple
        Color(0xFF00ACC1), // 9: Cyan
        Color(0xFF6D4C41), // 10: Brown
        Color(0xFFC0CA33), // 11: Lime
        Color(0xFFEC407A), // 12: Pink
    )

    val accentNames: List<String> = listOf(
        "VK Modern", "VK Classic", "VK Pink",
        "Black", "Red", "Blue", "Green", "Orange",
        "Purple", "Cyan", "Brown", "Lime", "Pink",
    )
}

/**
 * Пресет палитры сообщений (HEX-цвета исходящих и входящих баблов).
 */
data class BubbleColorPreset(
    val name: String,
    val outBgHex: String,
    val outTextHex: String,
    val inBgHex: String,
    val inTextHex: String,
)

object BubbleColorPresets {
    val PRESETS: List<BubbleColorPreset> = listOf(
        BubbleColorPreset(
            name = "VK Classic",
            outBgHex = "#4A76A8",
            outTextHex = "#FFFFFF",
            inBgHex = "#2E3642",
            inTextHex = "#ECEFF4",
        ),
        BubbleColorPreset(
            name = "VK Modern",
            outBgHex = "#0077FF",
            outTextHex = "#FFFFFF",
            inBgHex = "#26292E",
            inTextHex = "#FFFFFF",
        ),
        BubbleColorPreset(
            name = "Telegram Dark",
            outBgHex = "#2B5278",
            outTextHex = "#FFFFFF",
            inBgHex = "#182533",
            inTextHex = "#F5F5F5",
        ),
        BubbleColorPreset(
            name = "Telegram Light",
            outBgHex = "#EEFFDE",
            outTextHex = "#1C242B",
            inBgHex = "#FFFFFF",
            inTextHex = "#1C242B",
        ),
        BubbleColorPreset(
            name = "Tokyo Night",
            outBgHex = "#7AA2F7",
            outTextHex = "#1A1B26",
            inBgHex = "#24283B",
            inTextHex = "#C0CAF5",
        ),
        BubbleColorPreset(
            name = "Nord",
            outBgHex = "#88C0D0",
            outTextHex = "#2E3440",
            inBgHex = "#3B4252",
            inTextHex = "#ECEFF4",
        ),
        BubbleColorPreset(
            name = "Catppuccin Mocha",
            outBgHex = "#89B4FA",
            outTextHex = "#11111B",
            inBgHex = "#313244",
            inTextHex = "#CDD6F4",
        ),
        BubbleColorPreset(
            name = "Emerald Mint",
            outBgHex = "#00B074",
            outTextHex = "#FFFFFF",
            inBgHex = "#202A25",
            inTextHex = "#E0F2E9",
        ),
        BubbleColorPreset(
            name = "Sunset Orange",
            outBgHex = "#FF6B4A",
            outTextHex = "#FFFFFF",
            inBgHex = "#2E2421",
            inTextHex = "#FFEBE5",
        ),
        BubbleColorPreset(
            name = "Monochrome",
            outBgHex = "#383838",
            outTextHex = "#FFFFFF",
            inBgHex = "#1E1E1E",
            inTextHex = "#E0E0E0",
        ),
    )

    /**
     * Безопасный парсинг HEX-строки цвета (#RRGGBB, #AARRGGBB, #RGB).
     * Возвращает null при невалидном формате или пустой строке.
     */
    fun parseHexColorOrNull(hex: String?): Color? {
        if (hex.isNullOrBlank()) return null
        val clean = hex.trim().removePrefix("#")
        return try {
            when (clean.length) {
                6 -> Color(clean.toLong(16) or 0xFF000000L)
                8 -> Color(clean.toLong(16))
                3 -> {
                    val r = clean[0].toString().repeat(2)
                    val g = clean[1].toString().repeat(2)
                    val b = clean[2].toString().repeat(2)
                    Color("$r$g$b".toLong(16) or 0xFF000000L)
                }
                else -> null
            }
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Расчёт контрастного цвета текста на основе относительной яркости (luminance) фона.
     */
    fun contrastTextColor(
        bgColor: Color,
        defaultDark: Color = Color(0xFF1E1E1E),
        defaultLight: Color = Color.White,
    ): Color {
        return if (bgColor.luminance() > 0.45f) defaultDark else defaultLight
    }

    /**
     * Нормализация строки HEX (добавление решётки при необходимости).
     */
    fun normalizeHex(hex: String): String {
        val trimmed = hex.trim()
        if (trimmed.isEmpty()) return ""
        return if (trimmed.startsWith("#")) trimmed.uppercase() else "#${trimmed.uppercase()}"
    }

    /**
     * Проверка валидности введённого HEX-кода.
     */
    fun isValidHex(hex: String): Boolean {
        val clean = hex.trim().removePrefix("#")
        return (clean.length == 3 || clean.length == 6 || clean.length == 8) &&
                clean.all { it in '0'..'9' || it in 'a'..'f' || it in 'A'..'F' }
    }
}
