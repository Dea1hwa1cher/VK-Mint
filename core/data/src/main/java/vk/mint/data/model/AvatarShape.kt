package vk.mint.data.model

/**
 * Форма аватарок пользователей, сообществ и чатов в приложении.
 */
enum class AvatarShape(val value: String, val title: String) {
    CIRCLE("circle", "Круг"),
    ROUNDED_SQUARE("rounded_square", "Скруглённый квадрат"),
    SQUARE("square", "Квадрат");

    companion object {
        fun fromValue(v: String?): AvatarShape = when (v?.lowercase()) {
            "rounded_square", "squircle" -> ROUNDED_SQUARE
            "square" -> SQUARE
            else -> CIRCLE
        }
    }
}
