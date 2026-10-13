package vk.mint.ui.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/**
 * Каталог векторных иконок в стилистике `@vkontakte/icons` (VKUI v8) для гибрида VKUI × MD3E.
 *
 * Ключевые отличия от стандартных иконок Material (MD2):
 *  - Капсы и соединения линий всегда скруглённые ([StrokeCap.Round], [StrokeJoin.Round]).
 *  - Оптическая сетка привязана к стандарту VKUI (`16dp`, `20dp`, `24dp`, `28dp`).
 *  - Все векторы лениво кэшируются в памяти (0 аллокаций при рекомпозиции в горячем пути списков).
 */
object VkIcons {

    // ═══════════════════════════════════════════════════════════════════════
    //  1. НАВИГАЦИЯ И ТАББАР (Сетка 28dp — стандарт VKUI TabbarItem / Epic)
    // ═══════════════════════════════════════════════════════════════════════

    val NewsfeedOutline28: ImageVector
        get() = _newsfeedOutline28 ?: ImageVector.Builder(
            name = "VkIcons.NewsfeedOutline28",
            defaultWidth = 28.dp,
            defaultHeight = 28.dp,
            viewportWidth = 28f,
            viewportHeight = 28f,
        ).apply {
            path(
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                // Скруглённый контур новостной карточки
                moveTo(8.5f, 4.5f)
                lineTo(19.5f, 4.5f)
                curveTo(21.71f, 4.5f, 23.5f, 6.29f, 23.5f, 8.5f)
                lineTo(23.5f, 19.5f)
                curveTo(23.5f, 21.71f, 21.71f, 23.5f, 19.5f, 23.5f)
                lineTo(8.5f, 23.5f)
                curveTo(6.29f, 23.5f, 4.5f, 21.71f, 4.5f, 19.5f)
                lineTo(4.5f, 8.5f)
                curveTo(4.5f, 6.29f, 6.29f, 4.5f, 8.5f, 4.5f)
                close()
                // Верхний медиа-блок / шапка поста
                moveTo(8.5f, 9.5f)
                lineTo(19.5f, 9.5f)
                // Текстовые строки ленты
                moveTo(8.5f, 14f)
                lineTo(19.5f, 14f)
                moveTo(8.5f, 18.5f)
                lineTo(14.5f, 18.5f)
            }
        }.build().also { _newsfeedOutline28 = it }
    private var _newsfeedOutline28: ImageVector? = null

    val NewsfeedFill28: ImageVector
        get() = _newsfeedFill28 ?: ImageVector.Builder(
            name = "VkIcons.NewsfeedFill28",
            defaultWidth = 28.dp,
            defaultHeight = 28.dp,
            viewportWidth = 28f,
            viewportHeight = 28f,
        ).apply {
            path(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.EvenOdd,
            ) {
                moveTo(8.5f, 3.5f)
                lineTo(19.5f, 3.5f)
                curveTo(22.26f, 3.5f, 24.5f, 5.74f, 24.5f, 8.5f)
                lineTo(24.5f, 19.5f)
                curveTo(24.5f, 22.26f, 22.26f, 24.5f, 19.5f, 24.5f)
                lineTo(8.5f, 24.5f)
                curveTo(5.74f, 24.5f, 3.5f, 22.26f, 3.5f, 19.5f)
                lineTo(3.5f, 8.5f)
                curveTo(3.5f, 5.74f, 5.74f, 3.5f, 8.5f, 3.5f)
                close()
                // Вырезы строк
                moveTo(8.5f, 8.5f)
                curveTo(7.95f, 8.5f, 7.5f, 8.95f, 7.5f, 9.5f)
                curveTo(7.5f, 10.05f, 7.95f, 10.5f, 8.5f, 10.5f)
                lineTo(19.5f, 10.5f)
                curveTo(20.05f, 10.5f, 20.5f, 10.05f, 20.5f, 9.5f)
                curveTo(20.5f, 8.95f, 20.05f, 8.5f, 19.5f, 8.5f)
                close()
                moveTo(8.5f, 13f)
                curveTo(7.95f, 13f, 7.5f, 13.45f, 7.5f, 14f)
                curveTo(7.5f, 14.55f, 7.95f, 15f, 8.5f, 15f)
                lineTo(19.5f, 15f)
                curveTo(20.05f, 15f, 20.5f, 14.55f, 20.5f, 14f)
                curveTo(20.5f, 13.45f, 20.05f, 13f, 19.5f, 13f)
                close()
                moveTo(8.5f, 17.5f)
                curveTo(7.95f, 17.5f, 7.5f, 17.95f, 7.5f, 18.5f)
                curveTo(7.5f, 19.05f, 7.95f, 19.5f, 8.5f, 19.5f)
                lineTo(14.5f, 19.5f)
                curveTo(15.05f, 19.5f, 15.5f, 19.05f, 15.5f, 18.5f)
                curveTo(15.5f, 17.95f, 15.05f, 17.5f, 14.5f, 17.5f)
                close()
            }
        }.build().also { _newsfeedFill28 = it }
    private var _newsfeedFill28: ImageVector? = null

    val ServicesOutline28: ImageVector
        get() = _servicesOutline28 ?: ImageVector.Builder(
            name = "VkIcons.ServicesOutline28",
            defaultWidth = 28.dp,
            defaultHeight = 28.dp,
            viewportWidth = 28f,
            viewportHeight = 28f,
        ).apply {
            path(
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                // Top-left squircle
                moveTo(6.8f, 4.5f)
                lineTo(10.2f, 4.5f)
                curveTo(11.47f, 4.5f, 12.5f, 5.53f, 12.5f, 6.8f)
                lineTo(12.5f, 10.2f)
                curveTo(12.5f, 11.47f, 11.47f, 12.5f, 10.2f, 12.5f)
                lineTo(6.8f, 12.5f)
                curveTo(5.53f, 12.5f, 4.5f, 11.47f, 4.5f, 10.2f)
                lineTo(4.5f, 6.8f)
                curveTo(4.5f, 5.53f, 5.53f, 4.5f, 6.8f, 4.5f)
                close()
                // Top-right circle/pill (VK SuperApp Hub accent)
                moveTo(19.5f, 4.5f)
                curveTo(21.71f, 4.5f, 23.5f, 6.29f, 23.5f, 8.5f)
                curveTo(23.5f, 10.71f, 21.71f, 12.5f, 19.5f, 12.5f)
                curveTo(17.29f, 12.5f, 15.5f, 10.71f, 15.5f, 8.5f)
                curveTo(15.5f, 6.29f, 17.29f, 4.5f, 19.5f, 4.5f)
                close()
                // Bottom-left squircle
                moveTo(6.8f, 15.5f)
                lineTo(10.2f, 15.5f)
                curveTo(11.47f, 15.5f, 12.5f, 16.53f, 12.5f, 17.8f)
                lineTo(12.5f, 21.2f)
                curveTo(12.5f, 22.47f, 11.47f, 23.5f, 10.2f, 23.5f)
                lineTo(6.8f, 23.5f)
                curveTo(5.53f, 23.5f, 4.5f, 22.47f, 4.5f, 21.2f)
                lineTo(4.5f, 17.8f)
                curveTo(4.5f, 16.53f, 5.53f, 15.5f, 6.8f, 15.5f)
                close()
                // Bottom-right squircle
                moveTo(17.8f, 15.5f)
                lineTo(21.2f, 15.5f)
                curveTo(22.47f, 15.5f, 23.5f, 16.53f, 23.5f, 17.8f)
                lineTo(23.5f, 21.2f)
                curveTo(23.5f, 22.47f, 22.47f, 23.5f, 21.2f, 23.5f)
                lineTo(17.8f, 23.5f)
                curveTo(16.53f, 23.5f, 15.5f, 22.47f, 15.5f, 21.2f)
                lineTo(15.5f, 17.8f)
                curveTo(15.5f, 16.53f, 16.53f, 15.5f, 17.8f, 15.5f)
                close()
            }
        }.build().also { _servicesOutline28 = it }
    private var _servicesOutline28: ImageVector? = null

    val ServicesFill28: ImageVector
        get() = _servicesFill28 ?: ImageVector.Builder(
            name = "VkIcons.ServicesFill28",
            defaultWidth = 28.dp,
            defaultHeight = 28.dp,
            viewportWidth = 28f,
            viewportHeight = 28f,
        ).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(6.8f, 4f)
                lineTo(10.2f, 4f)
                curveTo(11.75f, 4f, 13f, 5.25f, 13f, 6.8f)
                lineTo(13f, 10.2f)
                curveTo(13f, 11.75f, 11.75f, 13f, 10.2f, 13f)
                lineTo(6.8f, 13f)
                curveTo(5.25f, 13f, 4f, 11.75f, 4f, 10.2f)
                lineTo(4f, 6.8f)
                curveTo(4f, 5.25f, 5.25f, 4f, 6.8f, 4f)
                close()
                moveTo(19.5f, 4f)
                curveTo(21.99f, 4f, 24f, 6.01f, 24f, 8.5f)
                curveTo(24f, 10.99f, 21.99f, 13f, 19.5f, 13f)
                curveTo(17.01f, 13f, 15f, 10.99f, 15f, 8.5f)
                curveTo(15f, 6.01f, 17.01f, 4f, 19.5f, 4f)
                close()
                moveTo(6.8f, 15f)
                lineTo(10.2f, 15f)
                curveTo(11.75f, 15f, 13f, 16.25f, 13f, 17.8f)
                lineTo(13f, 21.2f)
                curveTo(13f, 22.75f, 11.75f, 24f, 10.2f, 24f)
                lineTo(6.8f, 24f)
                curveTo(5.25f, 24f, 4f, 22.75f, 4f, 21.2f)
                lineTo(4f, 17.8f)
                curveTo(4f, 16.25f, 5.25f, 15f, 6.8f, 15f)
                close()
                moveTo(17.8f, 15f)
                lineTo(21.2f, 15f)
                curveTo(22.75f, 15f, 24f, 16.25f, 24f, 17.8f)
                lineTo(24f, 21.2f)
                curveTo(24f, 22.75f, 22.75f, 24f, 21.2f, 24f)
                lineTo(17.8f, 24f)
                curveTo(16.25f, 24f, 15f, 22.75f, 15f, 21.2f)
                lineTo(15f, 17.8f)
                curveTo(15f, 16.25f, 16.25f, 15f, 17.8f, 15f)
                close()
            }
        }.build().also { _servicesFill28 = it }
    private var _servicesFill28: ImageVector? = null

    val MessagesOutline28: ImageVector
        get() = _messagesOutline28 ?: ImageVector.Builder(
            name = "VkIcons.MessagesOutline28",
            defaultWidth = 28.dp,
            defaultHeight = 28.dp,
            viewportWidth = 28f,
            viewportHeight = 28f,
        ).apply {
            path(
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                moveTo(14f, 4.5f)
                curveTo(19.52f, 4.5f, 24f, 8.42f, 24f, 13.25f)
                curveTo(24f, 18.08f, 19.52f, 22f, 14f, 22f)
                curveTo(12.42f, 22f, 10.92f, 21.68f, 9.58f, 21.11f)
                lineTo(5.35f, 23.18f)
                curveTo(4.78f, 23.46f, 4.16f, 22.92f, 4.35f, 22.31f)
                lineTo(5.58f, 18.42f)
                curveTo(4.58f, 16.92f, 4f, 15.15f, 4f, 13.25f)
                curveTo(4f, 8.42f, 8.48f, 4.5f, 14f, 4.5f)
                close()
            }
        }.build().also { _messagesOutline28 = it }
    private var _messagesOutline28: ImageVector? = null

    val MessagesFill28: ImageVector
        get() = _messagesFill28 ?: ImageVector.Builder(
            name = "VkIcons.MessagesFill28",
            defaultWidth = 28.dp,
            defaultHeight = 28.dp,
            viewportWidth = 28f,
            viewportHeight = 28f,
        ).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(14f, 4f)
                curveTo(19.8f, 4f, 24.5f, 8.14f, 24.5f, 13.25f)
                curveTo(24.5f, 18.36f, 19.8f, 22.5f, 14f, 22.5f)
                curveTo(12.38f, 22.5f, 10.84f, 22.17f, 9.47f, 21.58f)
                lineTo(5.18f, 23.68f)
                curveTo(4.42f, 24.05f, 3.6f, 23.33f, 3.85f, 22.52f)
                lineTo(5.1f, 18.55f)
                curveTo(4.09f, 17f, 3.5f, 15.19f, 3.5f, 13.25f)
                curveTo(3.5f, 8.14f, 8.2f, 4f, 14f, 4f)
                close()
            }
        }.build().also { _messagesFill28 = it }
    private var _messagesFill28: ImageVector? = null

    val ClipsOutline28: ImageVector
        get() = _clipsOutline28 ?: ImageVector.Builder(
            name = "VkIcons.ClipsOutline28",
            defaultWidth = 28.dp,
            defaultHeight = 28.dp,
            viewportWidth = 28f,
            viewportHeight = 28f,
        ).apply {
            path(
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                // Вертикальный рилс-контейнер со скруглёнными углами
                moveTo(9f, 4f)
                lineTo(19f, 4f)
                curveTo(21.21f, 4f, 23f, 5.79f, 23f, 8f)
                lineTo(23f, 20f)
                curveTo(23f, 22.21f, 21.21f, 24f, 19f, 24f)
                lineTo(9f, 24f)
                curveTo(6.79f, 24f, 5f, 22.21f, 5f, 20f)
                lineTo(5f, 8f)
                curveTo(5f, 5.79f, 6.79f, 4f, 9f, 4f)
                close()
                // Мягкий треугольник Play внутри клипа
                moveTo(12f, 10.5f)
                lineTo(17.5f, 14f)
                lineTo(12f, 17.5f)
                close()
            }
        }.build().also { _clipsOutline28 = it }
    private var _clipsOutline28: ImageVector? = null

    val ClipsFill28: ImageVector
        get() = _clipsFill28 ?: ImageVector.Builder(
            name = "VkIcons.ClipsFill28",
            defaultWidth = 28.dp,
            defaultHeight = 28.dp,
            viewportWidth = 28f,
            viewportHeight = 28f,
        ).apply {
            path(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.EvenOdd,
            ) {
                moveTo(9f, 3.5f)
                lineTo(19f, 3.5f)
                curveTo(21.49f, 3.5f, 23.5f, 5.51f, 23.5f, 8f)
                lineTo(23.5f, 20f)
                curveTo(23.5f, 22.49f, 21.49f, 24.5f, 19f, 24.5f)
                lineTo(9f, 24.5f)
                curveTo(6.51f, 24.5f, 4.5f, 22.49f, 4.5f, 20f)
                lineTo(4.5f, 8f)
                curveTo(4.5f, 5.51f, 6.51f, 3.5f, 9f, 3.5f)
                close()
                moveTo(11.5f, 10.2f)
                lineTo(18f, 14f)
                lineTo(11.5f, 17.8f)
                close()
            }
        }.build().also { _clipsFill28 = it }
    private var _clipsFill28: ImageVector? = null

    val VideoOutline28: ImageVector
        get() = _videoOutline28 ?: ImageVector.Builder(
            name = "VkIcons.VideoOutline28",
            defaultWidth = 28.dp,
            defaultHeight = 28.dp,
            viewportWidth = 28f,
            viewportHeight = 28f,
        ).apply {
            path(
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                moveTo(7.5f, 5.5f)
                lineTo(20.5f, 5.5f)
                curveTo(22.71f, 5.5f, 24.5f, 7.29f, 24.5f, 9.5f)
                lineTo(24.5f, 18.5f)
                curveTo(24.5f, 20.71f, 22.71f, 22.5f, 20.5f, 22.5f)
                lineTo(7.5f, 22.5f)
                curveTo(5.29f, 22.5f, 3.5f, 20.71f, 3.5f, 18.5f)
                lineTo(3.5f, 9.5f)
                curveTo(3.5f, 7.29f, 5.29f, 5.5f, 7.5f, 5.5f)
                close()
                moveTo(12f, 10.8f)
                lineTo(17.2f, 14f)
                lineTo(12f, 17.2f)
                close()
            }
        }.build().also { _videoOutline28 = it }
    private var _videoOutline28: ImageVector? = null

    val VideoFill28: ImageVector
        get() = _videoFill28 ?: ImageVector.Builder(
            name = "VkIcons.VideoFill28",
            defaultWidth = 28.dp,
            defaultHeight = 28.dp,
            viewportWidth = 28f,
            viewportHeight = 28f,
        ).apply {
            path(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.EvenOdd,
            ) {
                moveTo(7.5f, 4.5f)
                lineTo(20.5f, 4.5f)
                curveTo(23.26f, 4.5f, 25.5f, 6.74f, 25.5f, 9.5f)
                lineTo(25.5f, 18.5f)
                curveTo(25.5f, 21.26f, 23.26f, 23.5f, 20.5f, 23.5f)
                lineTo(7.5f, 23.5f)
                curveTo(4.74f, 23.5f, 2.5f, 21.26f, 2.5f, 18.5f)
                lineTo(2.5f, 9.5f)
                curveTo(2.5f, 6.74f, 4.74f, 4.5f, 7.5f, 4.5f)
                close()
                moveTo(11.5f, 10.2f)
                lineTo(18f, 14f)
                lineTo(11.5f, 17.8f)
                close()
            }
        }.build().also { _videoFill28 = it }
    private var _videoFill28: ImageVector? = null

    val MusicOutline28: ImageVector
        get() = _musicOutline28 ?: ImageVector.Builder(
            name = "VkIcons.MusicOutline28",
            defaultWidth = 28.dp,
            defaultHeight = 28.dp,
            viewportWidth = 28f,
            viewportHeight = 28f,
        ).apply {
            path(
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                // Штили и перекладина восьмых нот (Icon28MusicOutline)
                moveTo(11f, 19.5f)
                lineTo(11f, 7.2f)
                curveTo(11f, 6.3f, 11.65f, 5.54f, 12.54f, 5.38f)
                lineTo(20.54f, 3.95f)
                curveTo(21.58f, 3.76f, 22.5f, 4.56f, 22.5f, 5.62f)
                lineTo(22.5f, 17.5f)
                // Нижняя перекладина
                moveTo(11f, 10.2f)
                lineTo(22.5f, 8.2f)
                // Левая головка ноты
                moveTo(8f, 22.5f)
                curveTo(9.66f, 22.5f, 11f, 21.16f, 11f, 19.5f)
                curveTo(11f, 17.84f, 9.66f, 16.5f, 8f, 16.5f)
                curveTo(6.34f, 16.5f, 5f, 17.84f, 5f, 19.5f)
                curveTo(5f, 21.16f, 6.34f, 22.5f, 8f, 22.5f)
                close()
                // Правая головка ноты
                moveTo(19.5f, 20.5f)
                curveTo(21.16f, 20.5f, 22.5f, 19.16f, 22.5f, 17.5f)
                curveTo(22.5f, 15.84f, 21.16f, 14.5f, 19.5f, 14.5f)
                curveTo(17.84f, 14.5f, 16.5f, 15.84f, 16.5f, 17.5f)
                curveTo(16.5f, 19.16f, 17.84f, 20.5f, 19.5f, 20.5f)
                close()
            }
        }.build().also { _musicOutline28 = it }
    private var _musicOutline28: ImageVector? = null

    val MusicFill28: ImageVector
        get() = _musicFill28 ?: ImageVector.Builder(
            name = "VkIcons.MusicFill28",
            defaultWidth = 28.dp,
            defaultHeight = 28.dp,
            viewportWidth = 28f,
            viewportHeight = 28f,
        ).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(20.36f, 3.0f)
                lineTo(12.36f, 4.43f)
                curveTo(10.99f, 4.68f, 10f, 5.87f, 10f, 7.26f)
                lineTo(10f, 16.05f)
                curveTo(9.4f, 15.7f, 8.72f, 15.5f, 8f, 15.5f)
                curveTo(5.79f, 15.5f, 4f, 17.29f, 4f, 19.5f)
                curveTo(4f, 21.71f, 5.79f, 23.5f, 8f, 23.5f)
                curveTo(10.21f, 23.5f, 12f, 21.71f, 12f, 19.5f)
                lineTo(12f, 11.04f)
                lineTo(21.5f, 9.35f)
                lineTo(21.5f, 14.05f)
                curveTo(20.9f, 13.7f, 20.22f, 13.5f, 19.5f, 13.5f)
                curveTo(17.29f, 13.5f, 15.5f, 15.29f, 15.5f, 17.5f)
                curveTo(15.5f, 19.71f, 17.29f, 21.5f, 19.5f, 21.5f)
                curveTo(21.71f, 21.5f, 23.5f, 19.71f, 23.5f, 17.5f)
                lineTo(23.5f, 5.62f)
                curveTo(23.5f, 3.98f, 21.98f, 2.71f, 20.36f, 3.0f)
                close()
            }
        }.build().also { _musicFill28 = it }
    private var _musicFill28: ImageVector? = null

    val ProfileOutline28: ImageVector
        get() = _profileOutline28 ?: ImageVector.Builder(
            name = "VkIcons.ProfileOutline28",
            defaultWidth = 28.dp,
            defaultHeight = 28.dp,
            viewportWidth = 28f,
            viewportHeight = 28f,
        ).apply {
            path(
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                // Голова
                moveTo(14f, 13.5f)
                curveTo(16.49f, 13.5f, 18.5f, 11.49f, 18.5f, 9f)
                curveTo(18.5f, 6.51f, 16.49f, 4.5f, 14f, 4.5f)
                curveTo(11.51f, 4.5f, 9.5f, 6.51f, 9.5f, 9f)
                curveTo(9.5f, 11.49f, 11.51f, 13.5f, 14f, 13.5f)
                close()
                // Плавная дуга плеч (VKUI Icon28UserOutline)
                moveTo(5.5f, 23f)
                curveTo(5.5f, 19.13f, 9.31f, 16.5f, 14f, 16.5f)
                curveTo(18.69f, 16.5f, 22.5f, 19.13f, 22.5f, 23f)
            }
        }.build().also { _profileOutline28 = it }
    private var _profileOutline28: ImageVector? = null

    val ProfileFill28: ImageVector
        get() = _profileFill28 ?: ImageVector.Builder(
            name = "VkIcons.ProfileFill28",
            defaultWidth = 28.dp,
            defaultHeight = 28.dp,
            viewportWidth = 28f,
            viewportHeight = 28f,
        ).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(14f, 4f)
                curveTo(16.76f, 4f, 19f, 6.24f, 19f, 9f)
                curveTo(19f, 11.76f, 16.76f, 14f, 14f, 14f)
                curveTo(11.24f, 14f, 9f, 11.76f, 9f, 9f)
                curveTo(9f, 6.24f, 11.24f, 4f, 14f, 4f)
                close()
                moveTo(14f, 16f)
                curveTo(19.25f, 16f, 23.5f, 18.85f, 23.5f, 23f)
                curveTo(23.5f, 23.83f, 22.83f, 24.5f, 22f, 24.5f)
                lineTo(6f, 24.5f)
                curveTo(5.17f, 24.5f, 4.5f, 23.83f, 4.5f, 23f)
                curveTo(4.5f, 18.85f, 8.75f, 16f, 14f, 16f)
                close()
            }
        }.build().also { _profileFill28 = it }
    private var _profileFill28: ImageVector? = null

    val UsersOutline28: ImageVector
        get() = _usersOutline28 ?: ImageVector.Builder(
            name = "VkIcons.UsersOutline28",
            defaultWidth = 28.dp,
            defaultHeight = 28.dp,
            viewportWidth = 28f,
            viewportHeight = 28f,
        ).apply {
            path(
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                moveTo(10.5f, 13f)
                curveTo(12.71f, 13f, 14.5f, 11.21f, 14.5f, 9f)
                curveTo(14.5f, 6.79f, 12.71f, 5f, 10.5f, 5f)
                curveTo(8.29f, 5f, 6.5f, 6.79f, 6.5f, 9f)
                curveTo(6.5f, 11.21f, 8.29f, 13f, 10.5f, 13f)
                close()
                moveTo(3.5f, 22.5f)
                curveTo(3.5f, 19.19f, 6.63f, 16.5f, 10.5f, 16.5f)
                curveTo(14.37f, 16.5f, 17.5f, 19.19f, 17.5f, 22.5f)
                // Второй человек справа
                moveTo(18f, 5.5f)
                curveTo(19.93f, 6.0f, 21.3f, 7.65f, 21.3f, 9.6f)
                curveTo(21.3f, 11.55f, 19.93f, 13.2f, 18f, 13.7f)
                moveTo(20f, 16.8f)
                curveTo(22.65f, 17.55f, 24.5f, 19.75f, 24.5f, 22.5f)
            }
        }.build().also { _usersOutline28 = it }
    private var _usersOutline28: ImageVector? = null

    val UsersFill28: ImageVector
        get() = _usersFill28 ?: ImageVector.Builder(
            name = "VkIcons.UsersFill28",
            defaultWidth = 28.dp,
            defaultHeight = 28.dp,
            viewportWidth = 28f,
            viewportHeight = 28f,
        ).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(10.5f, 4.5f)
                curveTo(12.99f, 4.5f, 15f, 6.51f, 15f, 9f)
                curveTo(15f, 11.49f, 12.99f, 13.5f, 10.5f, 13.5f)
                curveTo(8.01f, 13.5f, 6f, 11.49f, 6f, 9f)
                curveTo(6f, 6.51f, 8.01f, 4.5f, 10.5f, 4.5f)
                close()
                moveTo(10.5f, 16f)
                curveTo(14.92f, 16f, 18.5f, 18.91f, 18.5f, 22.5f)
                curveTo(18.5f, 23.33f, 17.83f, 24f, 17f, 24f)
                lineTo(4f, 24f)
                curveTo(3.17f, 24f, 2.5f, 23.33f, 2.5f, 22.5f)
                curveTo(2.5f, 18.91f, 6.08f, 16f, 10.5f, 16f)
                close()
                moveTo(18.5f, 5.2f)
                curveTo(20.78f, 5.2f, 22.6f, 7.02f, 22.6f, 9.3f)
                curveTo(22.6f, 11.58f, 20.78f, 13.4f, 18.5f, 13.4f)
                curveTo(17.95f, 13.4f, 17.43f, 13.28f, 16.96f, 13.08f)
                curveTo(17.62f, 11.9f, 18f, 10.5f, 18f, 9f)
                curveTo(18f, 7.62f, 17.67f, 6.32f, 17.08f, 5.42f)
                curveTo(17.53f, 5.28f, 18.0f, 5.2f, 18.5f, 5.2f)
                close()
                moveTo(20.2f, 16.2f)
                curveTo(23.25f, 16.9f, 25.5f, 19.4f, 25.5f, 22.5f)
                curveTo(25.5f, 23.33f, 24.83f, 24f, 24f, 24f)
                lineTo(20.2f, 24f)
                curveTo(20.4f, 23.52f, 20.5f, 23.02f, 20.5f, 22.5f)
                curveTo(20.5f, 19.95f, 19.25f, 17.7f, 17.3f, 16.25f)
                curveTo(18.25f, 16.05f, 19.24f, 16.0f, 20.2f, 16.2f)
                close()
            }
        }.build().also { _usersFill28 = it }
    private var _usersFill28: ImageVector? = null

    val NotificationsOutline28: ImageVector
        get() = _notificationsOutline28 ?: ImageVector.Builder(
            name = "VkIcons.NotificationsOutline28",
            defaultWidth = 28.dp,
            defaultHeight = 28.dp,
            viewportWidth = 28f,
            viewportHeight = 28f,
        ).apply {
            path(
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                moveTo(14f, 4.5f)
                curveTo(10.13f, 4.5f, 7f, 7.63f, 7f, 11.5f)
                lineTo(7f, 16.2f)
                lineTo(5.2f, 18.8f)
                curveTo(4.75f, 19.45f, 5.21f, 20.3f, 6.0f, 20.3f)
                lineTo(22.0f, 20.3f)
                curveTo(22.79f, 20.3f, 23.25f, 19.45f, 22.8f, 18.8f)
                lineTo(21f, 16.2f)
                lineTo(21f, 11.5f)
                curveTo(21f, 7.63f, 17.87f, 4.5f, 14f, 4.5f)
                close()
                moveTo(11.5f, 21f)
                curveTo(11.85f, 22.45f, 12.83f, 23.5f, 14f, 23.5f)
                curveTo(15.17f, 23.5f, 16.15f, 22.45f, 16.5f, 21f)
            }
        }.build().also { _notificationsOutline28 = it }
    private var _notificationsOutline28: ImageVector? = null

    val NotificationsFill28: ImageVector
        get() = _notificationsFill28 ?: ImageVector.Builder(
            name = "VkIcons.NotificationsFill28",
            defaultWidth = 28.dp,
            defaultHeight = 28.dp,
            viewportWidth = 28f,
            viewportHeight = 28f,
        ).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(14f, 4f)
                curveTo(9.86f, 4f, 6.5f, 7.36f, 6.5f, 11.5f)
                lineTo(6.5f, 16f)
                lineTo(4.65f, 18.7f)
                curveTo(4.05f, 19.58f, 4.68f, 20.8f, 5.75f, 20.8f)
                lineTo(22.25f, 20.8f)
                curveTo(23.32f, 20.8f, 23.95f, 19.58f, 23.35f, 18.7f)
                lineTo(21.5f, 16f)
                lineTo(21.5f, 11.5f)
                curveTo(21.5f, 7.36f, 18.14f, 4f, 14f, 4f)
                close()
                moveTo(11.2f, 21.8f)
                curveTo(11.65f, 23.35f, 12.73f, 24.5f, 14f, 24.5f)
                curveTo(15.27f, 24.5f, 16.35f, 23.35f, 16.8f, 21.8f)
                close()
            }
        }.build().also { _notificationsFill28 = it }
    private var _notificationsFill28: ImageVector? = null

    // ═══════════════════════════════════════════════════════════════════════
    //  2. ФУТЕР ПОСТОВ И КОНТЕНТНЫЕ ДЕЙСТВИЯ (Сетка 20dp и 16dp — VKUI Post)
    // ═══════════════════════════════════════════════════════════════════════

    val LikeOutline20: ImageVector
        get() = _likeOutline20 ?: ImageVector.Builder(
            name = "VkIcons.LikeOutline20",
            defaultWidth = 20.dp,
            defaultHeight = 20.dp,
            viewportWidth = 20f,
            viewportHeight = 20f,
        ).apply {
            path(
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.7f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                moveTo(10f, 16.6f)
                curveTo(10f, 16.6f, 3.2f, 12.25f, 3.2f, 7.35f)
                curveTo(3.2f, 5.12f, 4.92f, 3.4f, 7.05f, 3.4f)
                curveTo(8.35f, 3.4f, 9.42f, 4.05f, 10f, 5.02f)
                curveTo(10.58f, 4.05f, 11.65f, 3.4f, 12.95f, 3.4f)
                curveTo(15.08f, 3.4f, 16.8f, 5.12f, 16.8f, 7.35f)
                curveTo(16.8f, 12.25f, 10f, 16.6f, 10f, 16.6f)
                close()
            }
        }.build().also { _likeOutline20 = it }
    private var _likeOutline20: ImageVector? = null

    val LikeFill20: ImageVector
        get() = _likeFill20 ?: ImageVector.Builder(
            name = "VkIcons.LikeFill20",
            defaultWidth = 20.dp,
            defaultHeight = 20.dp,
            viewportWidth = 20f,
            viewportHeight = 20f,
        ).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(10f, 17.25f)
                curveTo(9.68f, 17.25f, 9.36f, 17.15f, 9.08f, 16.96f)
                curveTo(7.42f, 15.85f, 2.4f, 12.18f, 2.4f, 7.35f)
                curveTo(2.4f, 4.68f, 4.48f, 2.6f, 7.05f, 2.6f)
                curveTo(8.26f, 2.6f, 9.32f, 3.12f, 10f, 3.96f)
                curveTo(10.68f, 3.12f, 11.74f, 2.6f, 12.95f, 2.6f)
                curveTo(15.52f, 2.6f, 17.6f, 4.68f, 17.6f, 7.35f)
                curveTo(17.6f, 12.18f, 12.58f, 15.85f, 10.92f, 16.96f)
                curveTo(10.64f, 17.15f, 10.32f, 17.25f, 10f, 17.25f)
                close()
            }
        }.build().also { _likeFill20 = it }
    private var _likeFill20: ImageVector? = null

    val CommentOutline20: ImageVector
        get() = _commentOutline20 ?: ImageVector.Builder(
            name = "VkIcons.CommentOutline20",
            defaultWidth = 20.dp,
            defaultHeight = 20.dp,
            viewportWidth = 20f,
            viewportHeight = 20f,
        ).apply {
            path(
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.7f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                moveTo(10f, 3.3f)
                curveTo(14.03f, 3.3f, 17.3f, 6.12f, 17.3f, 9.6f)
                curveTo(17.3f, 13.08f, 14.03f, 15.9f, 10f, 15.9f)
                curveTo(8.82f, 15.9f, 7.7f, 15.66f, 6.72f, 15.24f)
                lineTo(3.6f, 16.75f)
                curveTo(3.18f, 16.95f, 2.73f, 16.56f, 2.87f, 16.12f)
                lineTo(3.78f, 13.32f)
                curveTo(3.09f, 12.24f, 2.7f, 10.97f, 2.7f, 9.6f)
                curveTo(2.7f, 6.12f, 5.97f, 3.3f, 10f, 3.3f)
                close()
            }
        }.build().also { _commentOutline20 = it }
    private var _commentOutline20: ImageVector? = null

    val RepostOutline20: ImageVector
        get() = _repostOutline20 ?: ImageVector.Builder(
            name = "VkIcons.RepostOutline20",
            defaultWidth = 20.dp,
            defaultHeight = 20.dp,
            viewportWidth = 20f,
            viewportHeight = 20f,
        ).apply {
            path(
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.7f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                // Изогнутая стрелка «Поделиться» в стиле VK (Icon20ShareOutline)
                moveTo(11.8f, 4.2f)
                lineTo(17.2f, 9.4f)
                lineTo(11.8f, 14.6f)
                moveTo(16.8f, 9.4f)
                lineTo(9.5f, 9.4f)
                curveTo(5.91f, 9.4f, 3.2f, 11.9f, 3.2f, 15.6f)
            }
        }.build().also { _repostOutline20 = it }
    private var _repostOutline20: ImageVector? = null

    val View16: ImageVector
        get() = _view16 ?: ImageVector.Builder(
            name = "VkIcons.View16",
            defaultWidth = 16.dp,
            defaultHeight = 16.dp,
            viewportWidth = 16f,
            viewportHeight = 16f,
        ).apply {
            path(
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.4f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                moveTo(1.5f, 8f)
                curveTo(2.9f, 5.1f, 5.25f, 3.5f, 8f, 3.5f)
                curveTo(10.75f, 3.5f, 13.1f, 5.1f, 14.5f, 8f)
                curveTo(13.1f, 10.9f, 10.75f, 12.5f, 8f, 12.5f)
                curveTo(5.25f, 12.5f, 2.9f, 10.9f, 1.5f, 8f)
                close()
                moveTo(8f, 9.8f)
                curveTo(9.0f, 9.8f, 9.8f, 9.0f, 9.8f, 8f)
                curveTo(9.8f, 7.0f, 9.0f, 6.2f, 8f, 6.2f)
                curveTo(7.0f, 6.2f, 6.2f, 7.0f, 6.2f, 8f)
                curveTo(6.2f, 9.0f, 7.0f, 9.8f, 8f, 9.8f)
                close()
            }
        }.build().also { _view16 = it }
    private var _view16: ImageVector? = null

    val BookmarkOutline20: ImageVector
        get() = _bookmarkOutline20 ?: ImageVector.Builder(
            name = "VkIcons.BookmarkOutline20",
            defaultWidth = 20.dp,
            defaultHeight = 20.dp,
            viewportWidth = 20f,
            viewportHeight = 20f,
        ).apply {
            path(
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.7f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                moveTo(5.2f, 5.2f)
                curveTo(5.2f, 4.09f, 6.09f, 3.2f, 7.2f, 3.2f)
                lineTo(12.8f, 3.2f)
                curveTo(13.91f, 3.2f, 14.8f, 4.09f, 14.8f, 5.2f)
                lineTo(14.8f, 16.4f)
                curveTo(14.8f, 17.0f, 14.12f, 17.34f, 13.64f, 16.98f)
                lineTo(10f, 14.25f)
                lineTo(6.36f, 16.98f)
                curveTo(5.88f, 17.34f, 5.2f, 17.0f, 5.2f, 16.4f)
                close()
            }
        }.build().also { _bookmarkOutline20 = it }
    private var _bookmarkOutline20: ImageVector? = null

    val BookmarkFill20: ImageVector
        get() = _bookmarkFill20 ?: ImageVector.Builder(
            name = "VkIcons.BookmarkFill20",
            defaultWidth = 20.dp,
            defaultHeight = 20.dp,
            viewportWidth = 20f,
            viewportHeight = 20f,
        ).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(7.2f, 2.5f)
                lineTo(12.8f, 2.5f)
                curveTo(14.29f, 2.5f, 15.5f, 3.71f, 15.5f, 5.2f)
                lineTo(15.5f, 16.4f)
                curveTo(15.5f, 17.55f, 14.2f, 18.21f, 13.28f, 17.52f)
                lineTo(10f, 15.06f)
                lineTo(6.72f, 17.52f)
                curveTo(5.8f, 18.21f, 4.5f, 17.55f, 4.5f, 16.4f)
                lineTo(4.5f, 5.2f)
                curveTo(4.5f, 3.71f, 5.71f, 2.5f, 7.2f, 2.5f)
                close()
            }
        }.build().also { _bookmarkFill20 = it }
    private var _bookmarkFill20: ImageVector? = null

    val MoreHorizontal20: ImageVector
        get() = _moreHorizontal20 ?: ImageVector.Builder(
            name = "VkIcons.MoreHorizontal20",
            defaultWidth = 20.dp,
            defaultHeight = 20.dp,
            viewportWidth = 20f,
            viewportHeight = 20f,
        ).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(5f, 10f)
                moveTo(3.4f, 10f)
                arcTo(1.6f, 1.6f, 0f, true, false, 6.6f, 10f)
                arcTo(1.6f, 1.6f, 0f, true, false, 3.4f, 10f)
                close()
                moveTo(8.4f, 10f)
                arcTo(1.6f, 1.6f, 0f, true, false, 11.6f, 10f)
                arcTo(1.6f, 1.6f, 0f, true, false, 8.4f, 10f)
                close()
                moveTo(13.4f, 10f)
                arcTo(1.6f, 1.6f, 0f, true, false, 16.6f, 10f)
                arcTo(1.6f, 1.6f, 0f, true, false, 13.4f, 10f)
                close()
            }
        }.build().also { _moreHorizontal20 = it }
    private var _moreHorizontal20: ImageVector? = null

    // ═══════════════════════════════════════════════════════════════════════
    //  3. МЕССЕНДЖЕР, ПОИСК И ХЕДЕРЫ (Сетка 16dp / 20dp / 24dp)
    // ═══════════════════════════════════════════════════════════════════════

    val Search20: ImageVector
        get() = _search20 ?: ImageVector.Builder(
            name = "VkIcons.Search20",
            defaultWidth = 20.dp,
            defaultHeight = 20.dp,
            viewportWidth = 20f,
            viewportHeight = 20f,
        ).apply {
            path(
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.7f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                moveTo(9f, 14.8f)
                curveTo(12.2f, 14.8f, 14.8f, 12.2f, 14.8f, 9f)
                curveTo(14.8f, 5.8f, 12.2f, 3.2f, 9f, 3.2f)
                curveTo(5.8f, 3.2f, 3.2f, 5.8f, 3.2f, 9f)
                curveTo(3.2f, 12.2f, 5.8f, 14.8f, 9f, 14.8f)
                close()
                moveTo(13.2f, 13.2f)
                lineTo(16.8f, 16.8f)
            }
        }.build().also { _search20 = it }
    private var _search20: ImageVector? = null

    val Search24: ImageVector
        get() = _search24 ?: ImageVector.Builder(
            name = "VkIcons.Search24",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            path(
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                moveTo(10.8f, 17.6f)
                curveTo(14.56f, 17.6f, 17.6f, 14.56f, 17.6f, 10.8f)
                curveTo(17.6f, 7.04f, 14.56f, 4f, 10.8f, 4f)
                curveTo(7.04f, 4f, 4f, 7.04f, 4f, 10.8f)
                curveTo(4f, 14.56f, 7.04f, 17.6f, 10.8f, 17.6f)
                close()
                moveTo(15.8f, 15.8f)
                lineTo(20f, 20f)
            }
        }.build().also { _search24 = it }
    private var _search24: ImageVector? = null

    val Clear16: ImageVector
        get() = _clear16 ?: ImageVector.Builder(
            name = "VkIcons.Clear16",
            defaultWidth = 16.dp,
            defaultHeight = 16.dp,
            viewportWidth = 16f,
            viewportHeight = 16f,
        ).apply {
            path(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.EvenOdd,
            ) {
                moveTo(8f, 1.5f)
                curveTo(11.59f, 1.5f, 14.5f, 4.41f, 14.5f, 8f)
                curveTo(14.5f, 11.59f, 11.59f, 14.5f, 8f, 14.5f)
                curveTo(4.41f, 14.5f, 1.5f, 11.59f, 1.5f, 8f)
                curveTo(1.5f, 4.41f, 4.41f, 1.5f, 8f, 1.5f)
                close()
                moveTo(5.7f, 5.7f)
                curveTo(5.4f, 6.0f, 5.4f, 6.48f, 5.7f, 6.78f)
                lineTo(6.92f, 8f)
                lineTo(5.7f, 9.22f)
                curveTo(5.4f, 9.52f, 5.4f, 10.0f, 5.7f, 10.3f)
                curveTo(6.0f, 10.6f, 6.48f, 10.6f, 6.78f, 10.3f)
                lineTo(8f, 9.08f)
                lineTo(9.22f, 10.3f)
                curveTo(9.52f, 10.6f, 10.0f, 10.6f, 10.3f, 10.3f)
                curveTo(10.6f, 10.0f, 10.6f, 9.52f, 10.3f, 9.22f)
                lineTo(9.08f, 8f)
                lineTo(10.3f, 6.78f)
                curveTo(10.6f, 6.48f, 10.6f, 6.0f, 10.3f, 5.7f)
                curveTo(10.0f, 5.4f, 9.52f, 5.4f, 9.22f, 5.7f)
                lineTo(8f, 6.92f)
                lineTo(6.78f, 5.7f)
                curveTo(6.48f, 5.4f, 6.0f, 5.4f, 5.7f, 5.7f)
                close()
            }
        }.build().also { _clear16 = it }
    private var _clear16: ImageVector? = null

    val Pin16: ImageVector
        get() = _pin16 ?: ImageVector.Builder(
            name = "VkIcons.Pin16",
            defaultWidth = 16.dp,
            defaultHeight = 16.dp,
            viewportWidth = 16f,
            viewportHeight = 16f,
        ).apply {
            path(
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                moveTo(9.5f, 2.5f)
                lineTo(13.5f, 6.5f)
                lineTo(12f, 8f)
                lineTo(9.5f, 7.2f)
                lineTo(6.8f, 9.9f)
                lineTo(7.5f, 12.2f)
                lineTo(6f, 13.7f)
                lineTo(2.3f, 10f)
                lineTo(3.8f, 8.5f)
                lineTo(6.1f, 9.2f)
                lineTo(8.8f, 6.5f)
                lineTo(8f, 4f)
                close()
                moveTo(4.15f, 11.85f)
                lineTo(2f, 14f)
            }
        }.build().also { _pin16 = it }
    private var _pin16: ImageVector? = null

    val Mute16: ImageVector
        get() = _mute16 ?: ImageVector.Builder(
            name = "VkIcons.Mute16",
            defaultWidth = 16.dp,
            defaultHeight = 16.dp,
            viewportWidth = 16f,
            viewportHeight = 16f,
        ).apply {
            path(
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                moveTo(3f, 6.2f)
                lineTo(5.2f, 6.2f)
                lineTo(8.5f, 3.5f)
                lineTo(8.5f, 12.5f)
                lineTo(5.2f, 9.8f)
                lineTo(3f, 9.8f)
                close()
                moveTo(11f, 6.2f)
                lineTo(14f, 9.8f)
                moveTo(14f, 6.2f)
                lineTo(11f, 9.8f)
            }
        }.build().also { _mute16 = it }
    private var _mute16: ImageVector? = null

    val Check16: ImageVector
        get() = _check16 ?: ImageVector.Builder(
            name = "VkIcons.Check16",
            defaultWidth = 16.dp,
            defaultHeight = 16.dp,
            viewportWidth = 16f,
            viewportHeight = 16f,
        ).apply {
            path(
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.6f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                moveTo(3.5f, 8.3f)
                lineTo(6.6f, 11.4f)
                lineTo(12.8f, 4.8f)
            }
        }.build().also { _check16 = it }
    private var _check16: ImageVector? = null

    val ChecksRead16: ImageVector
        get() = _checksRead16 ?: ImageVector.Builder(
            name = "VkIcons.ChecksRead16",
            defaultWidth = 16.dp,
            defaultHeight = 16.dp,
            viewportWidth = 16f,
            viewportHeight = 16f,
        ).apply {
            path(
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                // Первая галочка
                moveTo(1.8f, 8.4f)
                lineTo(4.7f, 11.3f)
                lineTo(10.5f, 4.8f)
                // Вторая галочка (прочитано)
                moveTo(6.2f, 8.8f)
                lineTo(8.7f, 11.3f)
                lineTo(14.4f, 4.8f)
            }
        }.build().also { _checksRead16 = it }
    private var _checksRead16: ImageVector? = null

    val Verified16: ImageVector
        get() = _verified16 ?: ImageVector.Builder(
            name = "VkIcons.Verified16",
            defaultWidth = 16.dp,
            defaultHeight = 16.dp,
            viewportWidth = 16f,
            viewportHeight = 16f,
        ).apply {
            path(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.EvenOdd,
            ) {
                // Фирменная галочка верификации ВКонтакте (Icon16Verified)
                moveTo(8f, 1.2f)
                lineTo(9.85f, 2.55f)
                lineTo(12.1f, 2.35f)
                lineTo(12.8f, 4.5f)
                lineTo(14.65f, 5.85f)
                lineTo(13.95f, 8f)
                lineTo(14.65f, 10.15f)
                lineTo(12.8f, 11.5f)
                lineTo(12.1f, 13.65f)
                lineTo(9.85f, 13.45f)
                lineTo(8f, 14.8f)
                lineTo(6.15f, 13.45f)
                lineTo(3.9f, 13.65f)
                lineTo(3.2f, 11.5f)
                lineTo(1.35f, 10.15f)
                lineTo(2.05f, 8f)
                lineTo(1.35f, 5.85f)
                lineTo(3.2f, 4.5f)
                lineTo(3.9f, 2.35f)
                lineTo(6.15f, 2.55f)
                close()
                moveTo(10.85f, 6.35f)
                curveTo(11.15f, 6.05f, 11.15f, 5.55f, 10.85f, 5.25f)
                curveTo(10.55f, 4.95f, 10.05f, 4.95f, 9.75f, 5.25f)
                lineTo(7.15f, 7.85f)
                lineTo(6.05f, 6.75f)
                curveTo(5.75f, 6.45f, 5.25f, 6.45f, 4.95f, 6.75f)
                curveTo(4.65f, 7.05f, 4.65f, 7.55f, 4.95f, 7.85f)
                lineTo(6.6f, 9.5f)
                curveTo(6.9f, 9.8f, 7.4f, 9.8f, 7.7f, 9.5f)
                close()
            }
        }.build().also { _verified16 = it }
    private var _verified16: ImageVector? = null

    val Attach24: ImageVector
        get() = _attach24 ?: ImageVector.Builder(
            name = "VkIcons.Attach24",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            path(
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                moveTo(18.5f, 11.2f)
                lineTo(11.6f, 18.1f)
                curveTo(9.61f, 20.09f, 6.39f, 20.09f, 4.4f, 18.1f)
                curveTo(2.41f, 16.11f, 2.41f, 12.89f, 4.4f, 10.9f)
                lineTo(12.2f, 3.1f)
                curveTo(13.53f, 1.77f, 15.67f, 1.77f, 17f, 3.1f)
                curveTo(18.33f, 4.43f, 18.33f, 6.57f, 17f, 7.9f)
                lineTo(9.5f, 15.4f)
                curveTo(8.84f, 16.06f, 7.76f, 16.06f, 7.1f, 15.4f)
                curveTo(6.44f, 14.74f, 6.44f, 13.66f, 7.1f, 13f)
                lineTo(13.8f, 6.3f)
            }
        }.build().also { _attach24 = it }
    private var _attach24: ImageVector? = null

    val SmileOutline24: ImageVector
        get() = _smileOutline24 ?: ImageVector.Builder(
            name = "VkIcons.SmileOutline24",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            path(
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                moveTo(12f, 21f)
                curveTo(16.97f, 21f, 21f, 16.97f, 21f, 12f)
                curveTo(21f, 7.03f, 16.97f, 3f, 12f, 3f)
                curveTo(7.03f, 3f, 3f, 7.03f, 3f, 12f)
                curveTo(3f, 16.97f, 7.03f, 21f, 12f, 21f)
                close()
                moveTo(8.5f, 14.5f)
                curveTo(9.35f, 15.85f, 10.58f, 16.6f, 12f, 16.6f)
                curveTo(13.42f, 16.6f, 14.65f, 15.85f, 15.5f, 14.5f)
            }
            path(fill = SolidColor(Color.Black)) {
                moveTo(9f, 9.8f)
                moveTo(7.8f, 9.8f)
                arcTo(1.2f, 1.2f, 0f, true, false, 10.2f, 9.8f)
                arcTo(1.2f, 1.2f, 0f, true, false, 7.8f, 9.8f)
                close()
                moveTo(13.8f, 9.8f)
                arcTo(1.2f, 1.2f, 0f, true, false, 16.2f, 9.8f)
                arcTo(1.2f, 1.2f, 0f, true, false, 13.8f, 9.8f)
                close()
            }
        }.build().also { _smileOutline24 = it }
    private var _smileOutline24: ImageVector? = null

    val VoiceOutline24: ImageVector
        get() = _voiceOutline24 ?: ImageVector.Builder(
            name = "VkIcons.VoiceOutline24",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            path(
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                moveTo(12f, 3.5f)
                curveTo(13.66f, 3.5f, 15f, 4.84f, 15f, 6.5f)
                lineTo(15f, 11.5f)
                curveTo(15f, 13.16f, 13.66f, 14.5f, 12f, 14.5f)
                curveTo(10.34f, 14.5f, 9f, 13.16f, 9f, 11.5f)
                lineTo(9f, 6.5f)
                curveTo(9f, 4.84f, 10.34f, 3.5f, 12f, 3.5f)
                close()
                moveTo(6f, 11f)
                lineTo(6f, 11.5f)
                curveTo(6f, 14.81f, 8.69f, 17.5f, 12f, 17.5f)
                curveTo(15.31f, 17.5f, 18f, 14.81f, 18f, 11.5f)
                lineTo(18f, 11f)
                moveTo(12f, 17.5f)
                lineTo(12f, 20.5f)
            }
        }.build().also { _voiceOutline24 = it }
    private var _voiceOutline24: ImageVector? = null

    val Send24: ImageVector
        get() = _send24 ?: ImageVector.Builder(
            name = "VkIcons.Send24",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            path(fill = SolidColor(Color.Black)) {
                // Скруглённая стрелка-самолётик отправки VK (Icon24Send)
                moveTo(4.35f, 3.68f)
                lineTo(20.45f, 10.68f)
                curveTo(21.48f, 11.13f, 21.48f, 12.87f, 20.45f, 13.32f)
                lineTo(4.35f, 20.32f)
                curveTo(3.38f, 20.74f, 2.38f, 19.83f, 2.72f, 18.82f)
                lineTo(4.85f, 12.8f)
                lineTo(12.2f, 12f)
                lineTo(4.85f, 11.2f)
                lineTo(2.72f, 5.18f)
                curveTo(2.38f, 4.17f, 3.38f, 3.26f, 4.35f, 3.68f)
                close()
            }
        }.build().also { _send24 = it }
    private var _send24: ImageVector? = null

    val ChevronRight20: ImageVector
        get() = _chevronRight20 ?: ImageVector.Builder(
            name = "VkIcons.ChevronRight20",
            defaultWidth = 20.dp,
            defaultHeight = 20.dp,
            viewportWidth = 20f,
            viewportHeight = 20f,
        ).apply {
            path(
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.8f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                moveTo(7.5f, 4.5f)
                lineTo(13f, 10f)
                lineTo(7.5f, 15.5f)
            }
        }.build().also { _chevronRight20 = it }
    private var _chevronRight20: ImageVector? = null

    val ArrowBack24: ImageVector
        get() = _arrowBack24 ?: ImageVector.Builder(
            name = "VkIcons.ArrowBack24",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            path(
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2.1f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                moveTo(10.5f, 5.5f)
                lineTo(4f, 12f)
                lineTo(10.5f, 18.5f)
                moveTo(4.5f, 12f)
                lineTo(20f, 12f)
            }
        }.build().also { _arrowBack24 = it }
    private var _arrowBack24: ImageVector? = null

    val EditOutline20: ImageVector
        get() = _editOutline20 ?: ImageVector.Builder(
            name = "VkIcons.EditOutline20",
            defaultWidth = 20.dp,
            defaultHeight = 20.dp,
            viewportWidth = 20f,
            viewportHeight = 20f,
        ).apply {
            path(
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.7f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                moveTo(12.8f, 3.6f)
                curveTo(13.46f, 2.94f, 14.54f, 2.94f, 15.2f, 3.6f)
                lineTo(16.4f, 4.8f)
                curveTo(17.06f, 5.46f, 17.06f, 6.54f, 16.4f, 7.2f)
                lineTo(7.6f, 16.0f)
                lineTo(3.5f, 16.5f)
                lineTo(4.0f, 12.4f)
                close()
            }
        }.build().also { _editOutline20 = it }
    private var _editOutline20: ImageVector? = null

    val Add24: ImageVector
        get() = _add24 ?: ImageVector.Builder(
            name = "VkIcons.Add24",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            path(
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2.1f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                moveTo(12f, 5f)
                lineTo(12f, 19f)
                moveTo(5f, 12f)
                lineTo(19f, 12f)
            }
        }.build().also { _add24 = it }
    private var _add24: ImageVector? = null

    val PlayFill20: ImageVector
        get() = _playFill20 ?: ImageVector.Builder(
            name = "VkIcons.PlayFill20",
            defaultWidth = 20.dp,
            defaultHeight = 20.dp,
            viewportWidth = 20f,
            viewportHeight = 20f,
        ).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(6.2f, 3.8f)
                curveTo(5.4f, 3.3f, 4.4f, 3.88f, 4.4f, 4.82f)
                lineTo(4.4f, 15.18f)
                curveTo(4.4f, 16.12f, 5.4f, 16.7f, 6.2f, 16.2f)
                lineTo(15.2f, 11.02f)
                curveTo(16.0f, 10.55f, 16.0f, 9.45f, 15.2f, 8.98f)
                close()
            }
        }.build().also { _playFill20 = it }
    private var _playFill20: ImageVector? = null

    val PauseFill20: ImageVector
        get() = _pauseFill20 ?: ImageVector.Builder(
            name = "VkIcons.PauseFill20",
            defaultWidth = 20.dp,
            defaultHeight = 20.dp,
            viewportWidth = 20f,
            viewportHeight = 20f,
        ).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(6.5f, 4f)
                curveTo(5.67f, 4f, 5f, 4.67f, 5f, 5.5f)
                lineTo(5f, 14.5f)
                curveTo(5f, 15.33f, 5.67f, 16f, 6.5f, 16f)
                curveTo(7.33f, 16f, 8f, 15.33f, 8f, 14.5f)
                lineTo(8f, 5.5f)
                curveTo(8f, 4.67f, 7.33f, 4f, 6.5f, 4f)
                close()
                moveTo(13.5f, 4f)
                curveTo(12.67f, 4f, 12f, 4.67f, 12f, 5.5f)
                lineTo(12f, 14.5f)
                curveTo(12f, 15.33f, 12.67f, 16f, 13.5f, 16f)
                curveTo(14.33f, 16f, 15f, 15.33f, 15f, 14.5f)
                lineTo(15f, 5.5f)
                curveTo(15f, 4.67f, 14.33f, 4f, 13.5f, 4f)
                close()
            }
        }.build().also { _pauseFill20 = it }
    private var _pauseFill20: ImageVector? = null

    val Shuffle20: ImageVector
        get() = _shuffle20 ?: ImageVector.Builder(
            name = "VkIcons.Shuffle20",
            defaultWidth = 20.dp,
            defaultHeight = 20.dp,
            viewportWidth = 20f,
            viewportHeight = 20f,
        ).apply {
            path(
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.7f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                moveTo(3f, 6f)
                lineTo(6.2f, 6f)
                curveTo(7.5f, 6f, 8.5f, 6.8f, 9.3f, 8.2f)
                lineTo(10.7f, 11.8f)
                curveTo(11.5f, 13.2f, 12.5f, 14f, 13.8f, 14f)
                lineTo(16.8f, 14f)
                moveTo(14.5f, 11.8f)
                lineTo(17f, 14f)
                lineTo(14.5f, 16.2f)
                moveTo(3f, 14f)
                lineTo(6.2f, 14f)
                curveTo(7.5f, 14f, 8.5f, 13.2f, 9.3f, 11.8f)
                lineTo(10.7f, 8.2f)
                curveTo(11.5f, 6.8f, 12.5f, 6f, 13.8f, 6f)
                lineTo(16.8f, 6f)
                moveTo(14.5f, 3.8f)
                lineTo(17f, 6f)
                lineTo(14.5f, 8.2f)
            }
        }.build().also { _shuffle20 = it }
    private var _shuffle20: ImageVector? = null

    val SettingsOutline20: ImageVector
        get() = _settingsOutline20 ?: ImageVector.Builder(
            name = "VkIcons.SettingsOutline20",
            defaultWidth = 20.dp,
            defaultHeight = 20.dp,
            viewportWidth = 20f,
            viewportHeight = 20f,
        ).apply {
            path(
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.6f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                moveTo(10f, 12.5f)
                arcTo(2.5f, 2.5f, 0f, true, false, 10f, 7.5f)
                arcTo(2.5f, 2.5f, 0f, true, false, 10f, 12.5f)
                close()
                moveTo(15.8f, 11f)
                lineTo(17.2f, 10f)
                lineTo(15.8f, 9f)
                lineTo(15f, 7.6f)
                lineTo(14.8f, 6f)
                lineTo(13.2f, 5.6f)
                lineTo(12.2f, 6.8f)
                lineTo(10.6f, 6.6f)
                lineTo(9.4f, 5.6f)
                lineTo(7.8f, 6.2f)
                lineTo(7.8f, 7.8f)
                lineTo(6.8f, 9f)
                lineTo(5.4f, 10f)
                lineTo(6.8f, 11f)
                lineTo(7f, 12.6f)
                lineTo(7.8f, 14f)
                lineTo(9.4f, 14.6f)
                lineTo(10.6f, 13.6f)
                lineTo(12.2f, 13.8f)
                lineTo(13.2f, 14.8f)
                lineTo(14.8f, 14.2f)
                lineTo(14.8f, 12.6f)
                close()
            }
        }.build().also { _settingsOutline20 = it }
    private var _settingsOutline20: ImageVector? = null
}
