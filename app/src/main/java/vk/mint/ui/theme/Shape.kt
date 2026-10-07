package vk.mint.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * VK Mint 2.0 / Material Design 3 Shape scale.
 *
 * Follows the Material 3 Expressive corner rounding hierarchy:
 * - extraSmall (4.dp): Snackbars, text input fields, small badges.
 * - small (8.dp): Chips (FilterChip, AssistChip), dropdown menus, compact tooltips.
 * - medium (12.dp): Content cards (Feed, Profile, Community), small popups.
 * - large (16.dp): FABs, standard dialog surfaces, modal pickers, drawer items.
 * - extraLarge (28.dp): Modal bottom sheets, date/time pickers, large floating panels.
 */
val VkMintShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(28.dp),
)
