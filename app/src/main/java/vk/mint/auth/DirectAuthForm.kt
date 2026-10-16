package vk.mint.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import vk.mint.auth.exchange.AuthState
import vk.mint.ui.components.VkButton
import vk.mint.ui.components.VkButtonMode
import vk.mint.ui.components.VkButtonSize
import vk.mint.ui.components.VkGroup
import vk.mint.ui.components.VkGroupMode
import vk.mint.ui.components.VkInputField
import vk.mint.ui.icons.VkIcons
import vk.mint.ui.theme.vkSubhead
import vk.mint.ui.theme.vkTitle3

/**
 * Форма прямого входа по логину и паролю (Direct Auth) в MD3E оболочке:
 *  - Островной контейнер [VkGroup] (mode = [VkGroupMode.Card], 16dp).
 *  - Поле ввода логина/телефона [VkInputField] с иконкой профиля и очисткой.
 *  - Поле ввода пароля [VkInputField] с маскированием и переключателем видимости.
 *  - Тактильные кнопки [VkButton] («Войти» и «Назад»).
 *  - Деликатный тональный блок отображения серверных ошибок.
 */
@Composable
internal fun DirectAuthForm(
    state: AuthState,
    onSubmit: (login: String, password: String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var login by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var passwordVisible by rememberSaveable { mutableStateOf(false) }

    val isLoading = state is AuthState.Loading
    val errorMessage = (state as? AuthState.Error)?.message?.let(::humanizeError)

    val keyboard = LocalSoftwareKeyboardController.current
    val passwordFocus = remember { FocusRequester() }

    val canSubmit = !isLoading && login.isNotBlank() && password.isNotBlank()

    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 24.dp)
            .widthIn(max = 440.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Spacer(Modifier.height(16.dp))

        VkGroup(
            mode = VkGroupMode.Card,
            containerColor = MaterialTheme.colorScheme.surface,
            contentPadding = PaddingValues(20.dp),
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                // Иконка замка в тональном контейнере
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Lock,
                        contentDescription = null,
                        modifier = Modifier.size(28.dp),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }

                Spacer(Modifier.height(16.dp))

                Text(
                    text = "Вход по логину и паролю",
                    style = MaterialTheme.typography.vkTitle3,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                )

                Spacer(Modifier.height(6.dp))

                Text(
                    text = "Введите номер телефона или email и пароль от страницы ВКонтакте",
                    style = MaterialTheme.typography.vkSubhead,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )

                Spacer(Modifier.height(20.dp))

                // Поле Логин / Телефон
                VkInputField(
                    value = login,
                    onValueChange = { login = it },
                    label = "Телефон или email",
                    placeholder = "+7 999 123-45-67 или email",
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Outlined.Person,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp),
                        )
                    },
                    trailingIcon = if (login.isNotEmpty() && !isLoading) {
                        {
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .clickable { login = "" },
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
                    singleLine = true,
                    enabled = !isLoading,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Email,
                        imeAction = ImeAction.Next,
                    ),
                    keyboardActions = KeyboardActions(
                        onNext = { passwordFocus.requestFocus() },
                    ),
                    modifier = Modifier.fillMaxWidth(),
                )

                Spacer(Modifier.height(14.dp))

                // Поле Пароль
                VkInputField(
                    value = password,
                    onValueChange = { password = it },
                    label = "Пароль",
                    placeholder = "Введите пароль",
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Outlined.Lock,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp),
                        )
                    },
                    trailingIcon = {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .clickable { passwordVisible = !passwordVisible },
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = if (passwordVisible) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                                contentDescription = if (passwordVisible) "Скрыть пароль" else "Показать пароль",
                                modifier = Modifier.size(20.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    },
                    singleLine = true,
                    enabled = !isLoading,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Done,
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            if (canSubmit) {
                                keyboard?.hide()
                                onSubmit(login.trim(), password)
                            }
                        },
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(passwordFocus),
                )

                if (errorMessage != null) {
                    Spacer(Modifier.height(12.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f))
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = errorMessage,
                            style = MaterialTheme.typography.vkSubhead,
                            color = MaterialTheme.colorScheme.error,
                            textAlign = TextAlign.Center,
                        )
                    }
                }

                Spacer(Modifier.height(20.dp))

                // Кнопка Войти
                VkButton(
                    onClick = {
                        keyboard?.hide()
                        onSubmit(login.trim(), password)
                    },
                    text = if (isLoading) null else "Войти",
                    mode = VkButtonMode.Primary,
                    size = VkButtonSize.Large,
                    enabled = canSubmit,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary,
                        )
                    }
                }

                Spacer(Modifier.height(10.dp))

                // Кнопка Назад
                VkButton(
                    onClick = onBack,
                    text = "Назад",
                    mode = VkButtonMode.Secondary,
                    size = VkButtonSize.Large,
                    enabled = !isLoading,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        Spacer(Modifier.height(24.dp))
    }
}
