package vk.mint.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ContentPaste
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import kotlinx.coroutines.launch
import vk.mint.data.local.ProxyPingResult
import vk.mint.data.model.ProxyConfig
import vk.mint.data.model.ProxyType

/**
 * Карточка прокси-сервера в списке Менеджера прокси.
 */
@Composable
fun ProxyItemCard(
    proxy: ProxyConfig,
    isActive: Boolean,
    pingResult: ProxyPingResult?,
    isTesting: Boolean,
    onSelect: () -> Unit,
    onPing: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val borderColor = if (isActive) MaterialTheme.colorScheme.primary else Color.Transparent
    val cardColors = if (isActive) {
        CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f))
    } else {
        CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .border(if (isActive) 1.5.dp else 0.dp, borderColor, RoundedCornerShape(14.dp))
            .clickable { onSelect() },
        shape = RoundedCornerShape(14.dp),
        colors = cardColors,
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RadioButton(
                    selected = isActive,
                    onClick = onSelect,
                    modifier = Modifier.size(24.dp),
                )
                Spacer(Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = proxy.displayTitle,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                        if (isActive) {
                            Spacer(Modifier.width(6.dp))
                            Surface(
                                color = MaterialTheme.colorScheme.primary,
                                shape = RoundedCornerShape(6.dp),
                            ) {
                                Text(
                                    "АКТИВЕН",
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                )
                            }
                        }
                    }
                    Text(
                        text = "${proxy.type.displayName} • ${proxy.formattedAddress}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                IconButton(onClick = onPing, modifier = Modifier.size(36.dp)) {
                    if (isTesting) {
                        CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
                    } else {
                        Icon(Icons.Outlined.Speed, contentDescription = "Проверить задержку", modifier = Modifier.size(20.dp))
                    }
                }
                IconButton(onClick = onEdit, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Outlined.Edit, contentDescription = "Редактировать", modifier = Modifier.size(20.dp))
                }
                IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                    Icon(
                        Icons.Outlined.Delete,
                        contentDescription = "Удалить",
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.error,
                    )
                }
            }

            // Дополнительная строка: авторизация и пинг-бейдж
            if (proxy.authEnabled || pingResult != null) {
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    if (proxy.authEnabled) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Outlined.Lock,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = "Логин: ${proxy.username}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    } else {
                        Spacer(Modifier.weight(1f))
                    }

                    if (pingResult != null) {
                        when (pingResult) {
                            is ProxyPingResult.Success -> {
                                val pingColor = when {
                                    pingResult.latencyMs < 250 -> Color(0xFF4CAF50)
                                    pingResult.latencyMs < 700 -> Color(0xFFFFB300)
                                    else -> Color(0xFFFF7043)
                                }
                                Surface(
                                    color = pingColor.copy(alpha = 0.15f),
                                    shape = RoundedCornerShape(6.dp),
                                ) {
                                    Text(
                                        text = "⚡ ${pingResult.latencyMs} мс",
                                        color = pingColor,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    )
                                }
                            }
                            is ProxyPingResult.Error -> {
                                Surface(
                                    color = MaterialTheme.colorScheme.error.copy(alpha = 0.15f),
                                    shape = RoundedCornerShape(6.dp),
                                ) {
                                    Text(
                                        text = "✕ ${pingResult.message}",
                                        color = MaterialTheme.colorScheme.error,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Диалог добавления или редактирования прокси-сервера.
 */
@Composable
fun AddEditProxyDialog(
    initialProxy: ProxyConfig? = null,
    onDismissRequest: () -> Unit,
    onSave: (ProxyConfig) -> Unit,
    onTest: suspend (ProxyConfig) -> ProxyPingResult,
) {
    @Suppress("DEPRECATION")
    val clipboardManager = LocalClipboardManager.current
    val scope = rememberCoroutineScope()

    var title by remember { mutableStateOf(initialProxy?.title.orEmpty()) }
    var type by remember { mutableStateOf(initialProxy?.type ?: ProxyType.HTTP) }
    var host by remember { mutableStateOf(initialProxy?.host.orEmpty()) }
    var port by remember { mutableStateOf(initialProxy?.port?.toString() ?: "8080") }
    var authEnabled by remember { mutableStateOf(initialProxy?.authEnabled ?: false) }
    var username by remember { mutableStateOf(initialProxy?.username.orEmpty()) }
    var password by remember { mutableStateOf(initialProxy?.password.orEmpty()) }
    var passwordVisible by remember { mutableStateOf(false) }

    var isTesting by remember { mutableStateOf(false) }
    var testResult by remember { mutableStateOf<ProxyPingResult?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    fun buildConfig(): ProxyConfig? {
        val cleanHost = host.trim()
        val cleanPort = port.trim().toIntOrNull()
        if (cleanHost.isBlank() || cleanPort == null || cleanPort !in 1..65535) {
            errorMessage = "Укажите корректный хост и порт (1..65535)"
            return null
        }
        return ProxyConfig(
            id = initialProxy?.id ?: java.util.UUID.randomUUID().toString(),
            title = title.trim(),
            type = type,
            host = cleanHost,
            port = cleanPort,
            authEnabled = authEnabled,
            username = username.trim(),
            password = password,
        )
    }

    Dialog(onDismissRequest = onDismissRequest) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = if (initialProxy == null) "Добавить прокси" else "Редактировать прокси",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                    )

                    // Кнопка вставки из буфера
                    TextButton(
                        onClick = {
                            val clip = clipboardManager.getText()?.text.orEmpty()
                            val parsed = ProxyConfig.parseFromString(clip)
                            if (parsed != null) {
                                if (parsed.title.isNotBlank()) title = parsed.title
                                type = parsed.type
                                host = parsed.host
                                port = parsed.port.toString()
                                authEnabled = parsed.authEnabled
                                username = parsed.username
                                password = parsed.password
                                testResult = null
                                errorMessage = null
                            } else {
                                errorMessage = "Не удалось распознать прокси из буфера"
                            }
                        },
                    ) {
                        Icon(Icons.Outlined.ContentPaste, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Из буфера", fontSize = 12.sp)
                    }
                }

                // Переключатель типа HTTP / SOCKS5
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    FilterChip(
                        selected = type == ProxyType.HTTP,
                        onClick = { type = ProxyType.HTTP; testResult = null },
                        label = { Text("HTTP / HTTPS") },
                        leadingIcon = if (type == ProxyType.HTTP) {
                            { Icon(Icons.Outlined.Check, null, modifier = Modifier.size(16.dp)) }
                        } else null,
                        modifier = Modifier.weight(1f),
                    )
                    FilterChip(
                        selected = type == ProxyType.SOCKS,
                        onClick = { type = ProxyType.SOCKS; testResult = null },
                        label = { Text("SOCKS5") },
                        leadingIcon = if (type == ProxyType.SOCKS) {
                            { Icon(Icons.Outlined.Check, null, modifier = Modifier.size(16.dp)) }
                        } else null,
                        modifier = Modifier.weight(1f),
                    )
                }

                // Название
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Название (необязательно)") },
                    placeholder = { Text("Например: Нидерланды SOCKS") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )

                // Хост и порт
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    OutlinedTextField(
                        value = host,
                        onValueChange = { host = it; testResult = null; errorMessage = null },
                        label = { Text("Хост / IP") },
                        placeholder = { Text("192.168.1.1") },
                        singleLine = true,
                        modifier = Modifier.weight(2.2f),
                    )
                    OutlinedTextField(
                        value = port,
                        onValueChange = { port = it; testResult = null; errorMessage = null },
                        label = { Text("Порт") },
                        placeholder = { Text(if (type == ProxyType.SOCKS) "1080" else "8080") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1.3f),
                    )
                }

                // Авторизация
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { authEnabled = !authEnabled }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column {
                        Text("Авторизация", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                        Text(
                            "Требуются логин и пароль",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Switch(checked = authEnabled, onCheckedChange = { authEnabled = it })
                }

                if (authEnabled) {
                    OutlinedTextField(
                        value = username,
                        onValueChange = { username = it; testResult = null },
                        label = { Text("Имя пользователя (логин)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it; testResult = null },
                        label = { Text("Пароль") },
                        singleLine = true,
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    if (passwordVisible) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                                    contentDescription = null,
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                // Ошибка валидации
                if (errorMessage != null) {
                    Text(
                        text = errorMessage.orEmpty(),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }

                // Проверка соединения
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    OutlinedButton(
                        onClick = {
                            val cfg = buildConfig() ?: return@OutlinedButton
                            isTesting = true
                            testResult = null
                            scope.launch {
                                testResult = onTest(cfg)
                                isTesting = false
                            }
                        },
                        enabled = !isTesting && host.isNotBlank() && port.isNotBlank(),
                    ) {
                        if (isTesting) {
                            CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Проверка…")
                        } else {
                            Icon(Icons.Outlined.Speed, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Проверить")
                        }
                    }

                    if (testResult != null) {
                        when (val r = testResult) {
                            is ProxyPingResult.Success -> {
                                Text(
                                    "⚡ Доступен (${r.latencyMs} мс)",
                                    color = Color(0xFF4CAF50),
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 12.sp,
                                )
                            }
                            is ProxyPingResult.Error -> {
                                Text(
                                    "✕ ${r.message}",
                                    color = MaterialTheme.colorScheme.error,
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 12.sp,
                                )
                            }
                            else -> {}
                        }
                    }
                }

                Spacer(Modifier.height(4.dp))

                // Кнопки Отмена / Сохранить
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TextButton(onClick = onDismissRequest) {
                        Text("Отмена")
                    }
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val cfg = buildConfig() ?: return@Button
                            onSave(cfg)
                            onDismissRequest()
                        },
                    ) {
                        Text("Сохранить")
                    }
                }
            }
        }
    }
}

/**
 * Диалог подтверждения удаления прокси.
 */
@Composable
fun DeleteProxyConfirmDialog(
    proxy: ProxyConfig,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Удалить прокси?") },
        text = { Text("Сервер «${proxy.displayTitle}» (${proxy.formattedAddress}) будет удалён из списка.") },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm()
                    onDismiss()
                },
            ) {
                Text("Удалить")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Отмена")
            }
        },
    )
}
