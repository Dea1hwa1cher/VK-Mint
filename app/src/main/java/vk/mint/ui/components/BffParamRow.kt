package vk.mint.ui.components

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import vk.mint.data.model.SettingsParam

/**
 * Единый компонент рендеринга BFF-параметра настроек (settingsGeneral.getNotifySettings / getPrivacySettings).
 *
 * Консолидирует дублировавшиеся реализации из NotificationSettingsScreen, PrivacySettingsScreen и SettingsScreen.
 * Поддерживает:
 *  - "toggle", "custom_toggle" -> ExpressiveSwitch (или CircularProgressIndicator при saving = true)
 *  - "select", "radio" -> DropdownMenu с опциями (или read-only если options пуст)
 *  - "button" -> TextButton с честным toast
 *  - "warning" -> Amber Card
 *  - default -> read-only текстовая строка
 */
@Composable
fun BffParamRow(
    param: SettingsParam,
    saving: Boolean = false,
    onToggle: (SettingsParam, Boolean) -> Unit,
    onSelect: (SettingsParam, String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var dropdownExpanded by remember { mutableStateOf(false) }
    val context = LocalContext.current
    when (param.type) {
        "toggle", "custom_toggle" -> {
            Row(
                modifier = modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp)
                    .defaultMinSize(minHeight = 44.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    param.title?.let { Text(it) }
                    param.description?.let {
                        Text(
                            it,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 13.sp,
                            modifier = Modifier.padding(top = 2.dp),
                        )
                    }
                }
                if (saving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                    )
                } else {
                    ExpressiveSwitch(
                        checked = param.isChecked == true,
                        onCheckedChange = { onToggle(param, it) },
                    )
                }
            }
        }
        "select", "radio" -> {
            // Select/radio БЕЗ options — рендер значения БЕЗ шеврона/кликабельности/dropdown.
            val canEdit = param.options.isNotEmpty()
            Box(modifier = modifier) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                        .defaultMinSize(minHeight = 44.dp)
                        .clip(MaterialTheme.shapes.small)
                        .then(
                            if (canEdit && !saving) {
                                Modifier.clickable { dropdownExpanded = true }
                            } else {
                                Modifier // read-only: options нет — dropdown не открываем
                            },
                        ),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        param.title?.let { Text(it) }
                        param.description?.let {
                            Text(
                                it,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 13.sp,
                                modifier = Modifier.padding(top = 2.dp),
                            )
                        }
                        val selectedLabel = param.options
                            .firstOrNull { it.value == param.value }?.label
                            ?: param.value ?: "—"
                        Text(
                            selectedLabel,
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(top = 4.dp),
                        )
                    }
                    if (saving) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                        )
                    } else if (canEdit) {
                        Icon(Icons.Default.ArrowDropDown, contentDescription = "Выбрать")
                    }
                }
                if (canEdit && !saving) {
                    DropdownMenu(
                        expanded = dropdownExpanded,
                        onDismissRequest = { dropdownExpanded = false },
                    ) {
                        param.options.forEach { opt ->
                            DropdownMenuItem(
                                text = { Text(opt.label) },
                                onClick = {
                                    dropdownExpanded = false
                                    onSelect(param, opt.value)
                                },
                            )
                        }
                    }
                }
            }
        }
        "button" -> {
            Row(
                modifier = modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                param.description?.let {
                    Text(
                        it,
                        Modifier.weight(1f),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 13.sp,
                    )
                }
                TextButton(
                    onClick = {
                        Toast.makeText(
                            context,
                            "Действие «${param.title ?: param.key}» недоступно",
                            Toast.LENGTH_SHORT,
                        ).show()
                    },
                    modifier = Modifier.heightIn(min = 44.dp),
                ) {
                    Text(param.title ?: "Действие")
                }
            }
        }
        "warning" -> {
            Card(
                modifier = modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFFFFC107).copy(alpha = 0.15f),
                ),
            ) {
                Column(Modifier.padding(16.dp)) {
                    param.title?.let { Text(it, fontWeight = FontWeight.SemiBold) }
                    param.description?.let {
                        Text(
                            it,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 13.sp,
                            modifier = Modifier.padding(top = 4.dp),
                        )
                    }
                }
            }
        }
        else -> {
            param.title?.let {
                Text(
                    it,
                    modifier = modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                )
            }
        }
    }
}
