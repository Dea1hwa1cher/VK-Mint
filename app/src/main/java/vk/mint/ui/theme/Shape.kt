package vk.mint.ui.theme

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import vk.mint.data.model.AvatarShape
import vk.mint.ui.anim.M3ExpressiveMotion
import vk.mint.ui.anim.expressiveSpring
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/**
 * VK Mint 2.0 / Material Design 3 Expressive (M3E) Shape scale.
 *
 * Canonical M3E corner rounding tokens (`@m3e/web` `ShapeToken.ts`):
 * - extraSmall (4.dp): Snackbars, text input fields, small badges, inline tags.
 * - small (8.dp): Chips (FilterChip, AssistChip), segmented buttons, compact tooltips.
 * - medium (12.dp): Standard cards, dropdown menus, list item containers.
 * - large (16.dp): Prominent content cards (Feed, Profile, Community, Settings), media cards.
 * - extraLarge (28.dp): Modal bottom sheets, dialogs, date/time pickers, large floating panels.
 */
val VkMintShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

/**
 * Расширенная шкала форм Material 3 Expressive (`ShapeToken.ts` @m3e/web v2.10.0),
 * включая промежуточные и увеличенные радиусы для крупных поверхностей и морфинга.
 */
object M3ExpressiveShapes {
    val none: RoundedCornerShape = RoundedCornerShape(0.dp)
    val extraSmall: RoundedCornerShape = RoundedCornerShape(4.dp)
    val small: RoundedCornerShape = RoundedCornerShape(8.dp)
    val medium: RoundedCornerShape = RoundedCornerShape(12.dp)
    val large: RoundedCornerShape = RoundedCornerShape(16.dp)
    val largeIncreased: RoundedCornerShape = RoundedCornerShape(20.dp)
    val extraLarge: RoundedCornerShape = RoundedCornerShape(28.dp)
    val extraLargeIncreased: RoundedCornerShape = RoundedCornerShape(32.dp)
    val extraExtraLarge: RoundedCornerShape = RoundedCornerShape(48.dp)
    val full: Shape = CircleShape
}

/**
 * Пружинный морфинг радиуса скругления при нажатии/фокусе по спецификации M3 Expressive
 * (например, переход 16.dp -> 8.dp или 28.dp -> 12.dp на интерактивных карточках и кнопках).
 */
@Composable
fun rememberM3EMorphingShape(
    pressed: Boolean,
    restCorner: Dp = 16.dp,
    pressedCorner: Dp = 8.dp,
): Shape {
    val corner by animateDpAsState(
        targetValue = if (pressed) pressedCorner else restCorner,
        animationSpec = expressiveSpring(
            dampingRatio = M3ExpressiveMotion.DampingExpressive,
            stiffness = M3ExpressiveMotion.StiffnessSpatial,
        ),
        label = "M3EMorphingCorner",
    )
    return RoundedCornerShape(corner)
}

/**
 * Варианты формы аватарок и декоративных бейджей M3 Expressive:
 * классические (круг, скруглённый квадрат, квадрат) + выразительные полигоны M3E
 * (печенька / 8-лепестковый scallop, 4-листный клевер, скруглённый гексагон, арка).
 */
object AvatarShapes {
    val Circle: Shape = CircleShape
    val RoundedSquare: Shape = RoundedCornerShape(percent = 28)
    val Square: Shape = RoundedCornerShape(percent = 10)

    /** 8-лепестковая форма M3E Cookie (Sunny Scallop): r(θ) = R * (0.90 + 0.10 * cos(8θ)). */
    val Cookie: Shape = GenericShape { size, _ ->
        val cx = size.width / 2f
        val cy = size.height / 2f
        val rMax = min(cx, cy)
        val steps = 96
        for (i in 0 until steps) {
            val theta = (2.0 * PI * i) / steps
            val r = rMax * (0.90f + 0.10f * cos(8.0 * theta).toFloat())
            val x = cx + r * cos(theta).toFloat()
            val y = cy + r * sin(theta).toFloat()
            if (i == 0) moveTo(x, y) else lineTo(x, y)
        }
        close()
    }

    /** 4-листный клевер M3E Clover: диагональные лепестки r(θ) = R * (0.87 + 0.13 * cos(4(θ - π/4))). */
    val Clover: Shape = GenericShape { size, _ ->
        val cx = size.width / 2f
        val cy = size.height / 2f
        val rMax = min(cx, cy)
        val steps = 96
        for (i in 0 until steps) {
            val theta = (2.0 * PI * i) / steps
            val r = rMax * (0.87f + 0.13f * cos(4.0 * (theta - PI / 4.0)).toFloat())
            val x = cx + r * cos(theta).toFloat()
            val y = cy + r * sin(theta).toFloat()
            if (i == 0) moveTo(x, y) else lineTo(x, y)
        }
        close()
    }

    /** Скруглённый 6-угольник M3E Hexagon с C1-гладкими скруглениями вершин. */
    val Hexagon: Shape = GenericShape { size, _ ->
        val cx = size.width / 2f
        val cy = size.height / 2f
        val r = min(cx, cy)
        val vx = FloatArray(6)
        val vy = FloatArray(6)
        for (k in 0 until 6) {
            val angle = -PI / 2.0 + k * (PI / 3.0)
            vx[k] = cx + r * cos(angle).toFloat()
            vy[k] = cy + r * sin(angle).toFloat()
        }
        val cornerFraction = 0.22f
        for (k in 0 until 6) {
            val prev = (k + 5) % 6
            val next = (k + 1) % 6
            val ax = vx[k] + cornerFraction * (vx[prev] - vx[k])
            val ay = vy[k] + cornerFraction * (vy[prev] - vy[k])
            val bx = vx[k] + cornerFraction * (vx[next] - vx[k])
            val by = vy[k] + cornerFraction * (vy[next] - vy[k])
            if (k == 0) moveTo(ax, ay) else lineTo(ax, ay)
            quadraticTo(vx[k], vy[k], bx, by)
        }
        close()
    }

    /** M3E Arch — классическая арка (полукруглый купол сверху + мягко скруглённое основание). */
    val Arch: Shape = GenericShape { size, _ ->
        val w = size.width
        val h = size.height
        val minDim = min(w, h)
        val rBottom = minDim * 0.20f
        moveTo(0f, w / 2f)
        arcTo(
            rect = Rect(0f, 0f, w, w),
            startAngleDegrees = 180f,
            sweepAngleDegrees = 180f,
            forceMoveTo = false,
        )
        lineTo(w, h - rBottom)
        arcTo(
            rect = Rect(w - 2f * rBottom, h - 2f * rBottom, w, h),
            startAngleDegrees = 0f,
            sweepAngleDegrees = 90f,
            forceMoveTo = false,
        )
        lineTo(rBottom, h)
        arcTo(
            rect = Rect(0f, h - 2f * rBottom, 2f * rBottom, h),
            startAngleDegrees = 90f,
            sweepAngleDegrees = 90f,
            forceMoveTo = false,
        )
        lineTo(0f, w / 2f)
        close()
    }

    fun forShape(shape: AvatarShape): Shape = when (shape) {
        AvatarShape.CIRCLE -> Circle
        AvatarShape.ROUNDED_SQUARE -> RoundedSquare
        AvatarShape.SQUARE -> Square
        AvatarShape.COOKIE -> Cookie
        AvatarShape.CLOVER -> Clover
        AvatarShape.HEXAGON -> Hexagon
        AvatarShape.ARCH -> Arch
    }
}

val LocalAvatarShape = compositionLocalOf<Shape> { CircleShape }

/**
 * Хелпер для получения текущей формы аватарок в Composable-дереве.
 */
val avatarShape: Shape
    @Composable
    @ReadOnlyComposable
    get() = LocalAvatarShape.current

