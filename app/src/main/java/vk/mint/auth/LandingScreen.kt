package vk.mint.auth

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import vk.mint.auth.exchange.AuthState

/**
 * Landing screen — вход по логину и паролю.
 * Единственный поддерживаемый способ входа; делегирует в [DirectAuthForm].
 */
@Composable
internal fun LandingScreen(
    state: AuthState,
    onSubmit: (login: String, password: String) -> Unit = { _, _ -> },
    onBack: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    DirectAuthForm(
        state = state,
        onSubmit = onSubmit,
        onBack = onBack,
        modifier = modifier,
    )
}

/**
 * Вспомогательный парсер access_token + user_id из строки (утилита).
 * Делегирует единому валидатору [OAuthTokenUtils.parse].
 */
internal fun parseExternalBrowserToken(input: String): ParsedToken? {
    val token = OAuthTokenUtils.parse(input) ?: return null
    return ParsedToken(
        accessToken = token.accessToken,
        userId = token.userId,
        expiresIn = token.expiresIn,
    )
}

/** Результат парсинга токена. */
internal data class ParsedToken(
    val accessToken: String,
    val userId: Long,
    val expiresIn: Long,
)
