package vk.mint.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import vk.mint.ui.anim.LocalAnimScale
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/**
 * Material 3 Expressive (`m3e-loading-indicator`) — индикатор загрузки нового поколения,
 * морфирующий между выразительными полигонами M3E (Cookie 8-lobe ↔ Clover 4-lobe ↔ Soft Star)
 * с плавным вращением внутри тонального контейнера (`contained = true`) или без фона.
 *
 * Автоматически учитывает [LocalAnimScale] (Reduced Motion: при `animScale <= 0f` рисуется
 * статичная выразительная фигура Cookie без бесконечных анимаций).
 */
@Composable
fun M3ELoadingIndicator(
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    contained: Boolean = true,
    indicatorColor: Color = if (contained) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.primary
    },
    containerColor: Color = MaterialTheme.colorScheme.primaryContainer,
) {
    val animScale = LocalAnimScale.current
    val rotationDeg: Float
    val morphPhase: Float

    if (animScale <= 0f) {
        rotationDeg = 0f
        morphPhase = 0.5f
    } else {
        val transition = rememberInfiniteTransition(label = "M3ELoadingIndicator")
        val rotDuration = (2400 * animScale).toInt().coerceAtLeast(400)
        val morphDuration = (900 * animScale).toInt().coerceAtLeast(200)
        val rot by transition.animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = rotDuration, easing = LinearEasing),
                repeatMode = RepeatMode.Restart,
            ),
            label = "M3ERotation",
        )
        val morph by transition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = morphDuration, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse,
            ),
            label = "M3EMorph",
        )
        rotationDeg = rot
        morphPhase = morph
    }

    val boxModifier = if (contained) {
        modifier
            .size(size)
            .clip(CircleShape)
            .background(containerColor)
    } else {
        modifier.size(size)
    }

    Box(
        modifier = boxModifier,
        contentAlignment = Alignment.Center,
    ) {
        val glyphSize = if (contained) size * 0.64f else size
        Canvas(modifier = Modifier.size(glyphSize)) {
            val cx = this.size.width / 2f
            val cy = this.size.height / 2f
            val rMax = min(cx, cy)
            val rotRad = rotationDeg * (PI / 180.0)
            val path = Path()
            val steps = 80
            for (i in 0 until steps) {
                val theta = (2.0 * PI * i) / steps
                // Морфинг между 4-лепестковым клевером (Clover) и 8-лепестковой печенькой (Cookie)
                val cloverWave = 0.84f + 0.16f * cos(4.0 * theta + rotRad).toFloat()
                val cookieWave = 0.88f + 0.12f * cos(8.0 * theta - rotRad).toFloat()
                val r = rMax * (cloverWave * (1f - morphPhase) + cookieWave * morphPhase)
                val x = cx + r * cos(theta + rotRad * 0.5).toFloat()
                val y = cy + r * sin(theta + rotRad * 0.5).toFloat()
                if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            path.close()
            drawPath(path = path, color = indicatorColor)
        }
    }
}
