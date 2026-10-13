package vk.mint.data.model

/**
 * Форма аватарок пользователей, сообществ и чатов в приложении.
 */
enum class AvatarShape(val value: String, val title: String) {
    CIRCLE("circle", "Круг"),
    ROUNDED_SQUARE("rounded_square", "Скруглённый"),
    SQUARE("square", "Квадрат"),
    COOKIE("cookie", "Печенька"),
    CLOVER("clover", "Клевер"),
    HEXAGON("hexagon", "Гексагон"),
    ARCH("arch", "Арка");

    companion object {
        fun fromValue(v: String?): AvatarShape = when (v?.lowercase()) {
            "rounded_square", "squircle" -> ROUNDED_SQUARE
            "square" -> SQUARE
            "cookie", "sunny" -> COOKIE
            "clover" -> CLOVER
            "hexagon" -> HEXAGON
            "arch" -> ARCH
            else -> CIRCLE
        }
    }
}
