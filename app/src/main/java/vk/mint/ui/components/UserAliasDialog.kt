package vk.mint.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

/**
 * Диалог установки/редактирования локального псевдонима пользователя.
 *
 * @param userId ID пользователя VK
 * @param defaultName Исходное имя пользователя
 * @param currentAlias Текущий псевдоним (если уже был установлен)
 * @param onDismiss Закрыть диалог
 * @param onSave Сохранить псевдоним (пустая строка означает удаление)
 */
@Composable
fun EditUserAliasDialog(
    userId: Long,
    defaultName: String,
    currentAlias: String?,
    onDismiss: () -> Unit,
    onSave: (alias: String) -> Unit,
) {
    var text by remember { mutableStateOf(currentAlias ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Локальный псевдоним") },
        text = {
            Column {
                Text(
                    text = "Задайте персональное имя для «$defaultName». Оно будет отображаться только у вас во всех разделах приложения.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    label = { Text("Псевдоним") },
                    placeholder = { Text(defaultName) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onSave(text.trim())
                    onDismiss()
                }
            ) {
                Text("Сохранить")
            }
        },
        dismissButton = {
            Row {
                if (!currentAlias.isNullOrBlank()) {
                    TextButton(
                        onClick = {
                            onSave("")
                            onDismiss()
                        }
                    ) {
                        Text("Удалить", color = MaterialTheme.colorScheme.error)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                }
                TextButton(onClick = onDismiss) {
                    Text("Отмена")
                }
            }
        }
    )
}

/**
 * Диалог добавления псевдонима по ID пользователя (для настроек).
 */
@Composable
fun AddUserAliasByIdDialog(
    onDismiss: () -> Unit,
    onSave: (userId: Long, alias: String) -> Unit,
) {
    var idText by remember { mutableStateOf("") }
    var aliasText by remember { mutableStateOf("") }
    var errorText by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Новый псевдоним") },
        text = {
            Column {
                Text(
                    text = "Введите числовой ID пользователя VK и желаемый локальный псевдоним.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.height(14.dp))
                OutlinedTextField(
                    value = idText,
                    onValueChange = {
                        idText = it
                        errorText = null
                    },
                    label = { Text("ID пользователя (число)") },
                    placeholder = { Text("123456789") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    isError = errorText != null,
                    modifier = Modifier.fillMaxWidth(),
                )
                if (errorText != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = errorText ?: "",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = aliasText,
                    onValueChange = { aliasText = it },
                    label = { Text("Псевдоним") },
                    placeholder = { Text("Например: Друг Саша") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val uid = idText.trim().toLongOrNull()
                    if (uid == null || uid <= 0L) {
                        errorText = "Укажите корректный положительный ID пользователя"
                        return@TextButton
                    }
                    if (aliasText.trim().isBlank()) {
                        errorText = "Псевдоним не может быть пустым"
                        return@TextButton
                    }
                    onSave(uid, aliasText.trim())
                    onDismiss()
                }
            ) {
                Text("Добавить")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Отмена")
            }
        }
    )
}
