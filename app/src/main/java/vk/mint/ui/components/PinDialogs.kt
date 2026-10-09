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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material3.AlertDialog
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
import vk.mint.locker.LockerActivity
import vk.mint.util.AppLog

/**
 * Круглая клавиша PIN-пада (3x4 сетка).
 */
@Composable
fun PinPadKey(key: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(64.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        if (key == "del") {
            Icon(Icons.AutoMirrored.Filled.Backspace, contentDescription = "Удалить")
        } else {
            Text(key, fontSize = 22.sp, fontWeight = FontWeight.Medium)
        }
    }
}

/**
 * Диалог создания / подтверждения PIN-кода.
 * Двухэтапный ввод: ввод (0..3) -> повтор (0..3).
 * При совпадении возвращает SHA-256 хэш через onPinSet.
 */
@Composable
fun PinSetupDialog(
    title: String = "Задайте PIN-код",
    subtitle: String = "4 цифры для блокировки приложения",
    onDismiss: () -> Unit,
    onPinSet: (hash: String) -> Unit,
) {
    val context = LocalContext.current
    var pin by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    val step = if (pin.length < 4) 0 else 1 // 0=ввод, 1=подтверждение
    val activeValue = if (step == 0) pin else confirm

    fun onKey(k: String) {
        if (k == "del") {
            error = null
            if (step == 0) {
                if (pin.isNotEmpty()) pin = pin.dropLast(1)
            } else {
                if (confirm.isNotEmpty()) confirm = confirm.dropLast(1)
            }
            return
        }
        if (activeValue.length >= 4) return
        if (step == 0) {
            pin = pin + k
        } else {
            confirm = confirm + k
            if (confirm.length == 4) {
                if (confirm == pin) {
                    onPinSet(LockerActivity.hashPin(pin))
                } else {
                    error = "PIN-коды не совпадают — придумайте новый"
                    LockerActivity.vibrate(context)
                    pin = ""
                    confirm = ""
                }
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (step == 0) title else "Подтвердите PIN-код") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    if (step == 0) subtitle else "Повторите PIN-код",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(16.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    repeat(4) { i ->
                        val filled = i < activeValue.length
                        Box(
                            modifier = Modifier
                                .size(16.dp)
                                .clip(CircleShape)
                                .background(if (filled) MaterialTheme.colorScheme.primary else Color.Transparent)
                                .border(2.dp, MaterialTheme.colorScheme.primary, CircleShape),
                        )
                    }
                }
                val err = error
                if (err != null) {
                    Spacer(Modifier.height(8.dp))
                    Text(err, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
                Spacer(Modifier.height(16.dp))
                val keys = listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "del", "0", "")
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    keys.chunked(3).forEach { rowKeys ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                        ) {
                            rowKeys.forEach { k ->
                                if (k.isEmpty()) {
                                    Box(modifier = Modifier.size(64.dp))
                                } else {
                                    PinPadKey(key = k, onClick = { onKey(k) })
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Отмена") }
        },
    )
}

/**
 * Диалог верификации сохранённого PIN-кода.
 * Одноэтапный ввод: 4 цифры -> сверка хэша.
 */
@Composable
fun PinVerifyDialog(
    storedHash: String,
    title: String,
    subtitle: String,
    onDismiss: () -> Unit,
    onVerified: () -> Unit,
) {
    val context = LocalContext.current
    var pin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    fun onKey(k: String) {
        if (k == "del") {
            error = null
            if (pin.isNotEmpty()) pin = pin.dropLast(1)
            return
        }
        if (pin.length >= 4) return
        pin = pin + k
        if (pin.length == 4) {
            val hash = LockerActivity.hashPin(pin)
            if (hash == storedHash) {
                onVerified()
            } else {
                AppLog.w("PinDialog", "PIN verify failed: wrong PIN (dialog='$title')")
                error = "Неверный PIN-код"
                LockerActivity.vibrate(context)
                pin = ""
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(16.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    repeat(4) { i ->
                        val filled = i < pin.length
                        Box(
                            modifier = Modifier
                                .size(16.dp)
                                .clip(CircleShape)
                                .background(if (filled) MaterialTheme.colorScheme.primary else Color.Transparent)
                                .border(2.dp, MaterialTheme.colorScheme.primary, CircleShape),
                        )
                    }
                }
                val err = error
                if (err != null) {
                    Spacer(Modifier.height(8.dp))
                    Text(err, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
                Spacer(Modifier.height(16.dp))
                val keys = listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "del", "0", "")
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    keys.chunked(3).forEach { rowKeys ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                        ) {
                            rowKeys.forEach { k ->
                                if (k.isEmpty()) {
                                    Box(modifier = Modifier.size(64.dp))
                                } else {
                                    PinPadKey(key = k, onClick = { onKey(k) })
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Отмена") }
        },
    )
}
