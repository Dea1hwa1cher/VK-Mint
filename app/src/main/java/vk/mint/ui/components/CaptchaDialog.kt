// File: ui/components/CaptchaDialog.kt
package vk.mint.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil3.compose.AsyncImage
import vk.mint.VkMintApp
import vk.mint.ui.icons.VkIcons
import vk.mint.ui.theme.M3ExpressiveShapes
import vk.mint.ui.theme.vkSubhead
import vk.mint.ui.theme.vkTitle3

/**
 * Sprint 1, P0-3 (#76) + MD3E Redesign:
 * Диалог ввода VK Captcha в каноничной оболочке **Material 3 Expressive (MD3E)**:
 *  - Контейнер: тональный остров `surfaceContainerHigh` со скруглением `28.dp` ([M3ExpressiveShapes.extraLarge]).
 *  - Картинка капчи в карточке со скруглением `12.dp` и контуром `outlineVariant`.
 *  - Тактильное поле ввода [VkInputField] с фокусным пружинным морфингом, центрированным текстом и кнопкой очистки.
 *  - Экспрессивные кнопки [VkButton] (`Primary` / `Secondary`) с морфингом углов при нажатии.
 */
@Composable
fun CaptchaDialog() {
    val app = VkMintApp.getOrNull() ?: return
    val challengeState by app.captchaHandler.challenge.collectAsState()
    val challenge = challengeState

    if (challenge == null) return  // нет активной captcha — не показываем

    var input by remember(challenge.sid) { mutableStateOf("") }
    var submitting by remember(challenge.sid) { mutableStateOf(false) }

    Dialog(
        onDismissRequest = {
            // Запрещаем dismiss по тапу вне диалога — нужно явное решение (ввести код или «Отмена»).
        },
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false,
        ),
    ) {
        Surface(
            modifier = Modifier
                .padding(horizontal = 24.dp)
                .widthIn(max = 380.dp)
                .fillMaxWidth(),
            shape = M3ExpressiveShapes.extraLarge,
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            tonalElevation = 6.dp,
            shadowElevation = 8.dp,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                // Иконка щита / безопасности в тональном круге
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = VkIcons.Verified16,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }

                Spacer(Modifier.height(14.dp))

                Text(
                    text = "Код с картинки",
                    style = MaterialTheme.typography.vkTitle3,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                )

                Spacer(Modifier.height(4.dp))

                Text(
                    text = "Подтвердите, что вы не робот. Введите символы с картинки ниже:",
                    style = MaterialTheme.typography.vkSubhead,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )

                Spacer(Modifier.height(16.dp))

                // Картинка captcha. VK отдаёт ~130x50 px (aspect ratio ~2.6:1).
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(2.6f)
                        .clip(M3ExpressiveShapes.medium)
                        .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                        .border(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.outlineVariant,
                            shape = M3ExpressiveShapes.medium,
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    AsyncImage(
                        model = challenge.img,
                        contentDescription = "Капча",
                        modifier = Modifier.fillMaxWidth(),
                        contentScale = ContentScale.Fit,
                    )
                }

                Spacer(Modifier.height(16.dp))

                VkInputField(
                    value = input,
                    onValueChange = { input = it.trim().take(10) },
                    placeholder = "Код",
                    label = "Символы с картинки",
                    enabled = !submitting,
                    singleLine = true,
                    textStyle = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp,
                        textAlign = TextAlign.Center,
                    ),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            val key = input.trim()
                            if (key.isNotBlank() && !submitting) {
                                submitting = true
                                challenge.submit(key)
                            }
                        },
                    ),
                    trailingIcon = if (input.isNotEmpty() && !submitting) {
                        {
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .clickable { input = "" },
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    imageVector = VkIcons.Clear16,
                                    contentDescription = "Очистить",
                                    modifier = Modifier.size(16.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    } else null,
                )

                if (submitting) {
                    Spacer(Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "Проверяем…",
                            style = MaterialTheme.typography.vkSubhead,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                Spacer(Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    VkButton(
                        onClick = {
                            submitting = false
                            challenge.cancel()
                        },
                        text = "Отмена",
                        mode = VkButtonMode.Secondary,
                        size = VkButtonSize.Medium,
                        enabled = !submitting,
                        modifier = Modifier.weight(1f),
                    )

                    VkButton(
                        onClick = {
                            val key = input.trim()
                            if (key.isBlank()) return@VkButton
                            submitting = true
                            challenge.submit(key)
                        },
                        text = "Отправить",
                        mode = VkButtonMode.Primary,
                        size = VkButtonSize.Medium,
                        enabled = input.isNotBlank() && !submitting,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}
