package vk.mint.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * VK Mint 2.0 / Material Design 3 Expressive (M3E) Shape scale.
 *
 * Follows the Material 3 Expressive corner rounding hierarchy:
 * - extraSmall (4.dp): Snackbars, text input fields, small badges, inline tags.
 * - small (8.dp): Chips (FilterChip, AssistChip), segmented buttons, dropdown menus, compact tooltips.
 * - medium (16.dp): Content cards (Feed, Profile, Community, Settings), list item containers, media cards.
 * - large (24.dp): Dialog surfaces, prominent feature cards, popup sheets, modal call panels.
 * - extraLarge (28.dp): Modal bottom sheets, date/time pickers, large floating panels.
 */
val VkMintShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(28.dp),
)
