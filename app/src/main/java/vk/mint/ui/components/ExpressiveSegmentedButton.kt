package vk.mint.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Material 3 Expressive (M3E) Single Choice Segmented Button Row.
 *
 * Provides a tactile, connected pill-shaped segmented button row for choosing
 * among 2 to 5 discrete mutually exclusive options (e.g. audio format, bitrates,
 * grouping modes, preview modes), satisfying M3 Expressive component specifications.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T> ExpressiveSegmentedButtonRow(
    items: List<T>,
    selectedItem: T,
    onItemSelected: (T) -> Unit,
    modifier: Modifier = Modifier.fillMaxWidth(),
    enabled: Boolean = true,
    showIcon: Boolean = false,
    label: @Composable (T) -> Unit = { item -> Text(item.toString()) },
) {
    SingleChoiceSegmentedButtonRow(
        modifier = modifier,
    ) {
        items.forEachIndexed { index, item ->
            val isSelected = item == selectedItem
            SegmentedButton(
                selected = isSelected,
                onClick = { onItemSelected(item) },
                shape = SegmentedButtonDefaults.itemShape(index = index, count = items.size),
                enabled = enabled,
                icon = {
                    if (showIcon) {
                        SegmentedButtonDefaults.Icon(active = isSelected)
                    }
                },
                label = { label(item) },
            )
        }
    }
}
