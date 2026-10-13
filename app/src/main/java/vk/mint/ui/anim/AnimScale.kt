package vk.mint.ui.anim

import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf

/**
 * Fix #224: глобальный масштаб длительности анимаций интерфейса.
 *
 * Берётся из [vk.mint.data.local.VkMintPrefs.Snapshot.interfaceAnimSpeed] (0..100 → /100f):
 *  - 1f  — нормальная скорость (100%)
 *  - 0.5f — вдвое быстрее (50%)
 *  - 0f  — анимации полностью выключены (мгновенные переходы через [snap])
 *
 * Предоставляется через CompositionLocalProvider в VkMintNavHost (обёрнуто вокруг
 * Scaffold+NavHost, чтобы покрывать все экраны). Глубокие composable-области
 * (message bubbles, swipe gestures) читают [LocalAnimScale].current через
 * @Composable-хелперы [scaledTween]/[scaledSpring].
 *
 * Для transition-ламбд NavHost (где нет @Composable scope) используется
 * не-композабл [tweenScaled] с явным scale-параметром.
 */
val LocalAnimScale = staticCompositionLocalOf { 1f }

/**
 * #ANIM-FASTER-10 (2026-09-12): базовый множитель скорости анимаций интерфейса.
 * Жалоба пользователя: «само приложение медленное, что-то тупит» →
 * «ускорить анимацию на 10%». 0.9f = все длительности ×0.9 (на 10% быстрее),
 * spring-stiffness ×1/0.9 (быстрее settle). Применяется в ЕДИНСТВЕННОЙ точке —
 * провайдере LocalAnimScale в VkMintNavHost — чтобы накрыть и хелперы
 * (scaledTween/scaledSpring/tweenScaled), и прямые чтения LocalAnimScale.current.
 * Пользовательский преф «Скорость анимации» умножается поверх базы.
 */
const val ANIM_BASE_SPEED = 0.9f

/**
 * Fix #228: масштаб стикер-фото в чате (0..40, % увеличения от оригинала).
 * 0 — исходный размер, 40 — +40% к оригиналу. Берётся из
 * [vk.mint.data.local.VkMintPrefs.Snapshot.stickerPhotoScale].
 * Предоставляется через CompositionLocalProvider в ChatDetailScreen,
 * читается в MessageBubble для рендера стикер-фото (isStickerLike).
 */
val LocalStickerPhotoScale = staticCompositionLocalOf { 0 }

/**
 * tween с учётом [LocalAnimScale]. При scale ≤ 0f возвращает [snap] (мгновенно).
 * @Composable — читает [LocalAnimScale].current.
 */
@Composable
fun <T> scaledTween(
    durationMillis: Int = 300,
    easing: Easing = FastOutSlowInEasing,
): FiniteAnimationSpec<T> {
    val scale = LocalAnimScale.current
    return if (scale <= 0f) snap()
    else tween(durationMillis = (durationMillis * scale).toInt().coerceAtLeast(1), easing = easing)
}

/**
 * spring с учётом [LocalAnimScale]. При scale ≤ 0f возвращает [snap] (мгновенно).
 * Иначе — [spring] с пропорционально увеличенной stiffness (быстрее settle при
 * меньшем scale). Spring не длительность-ориентирован, поэтому «скорость»
 * аппроксимируется через stiffness.
 */
@Composable
fun <T> scaledSpring(
    dampingRatio: Float = Spring.DampingRatioNoBouncy,
    stiffness: Float = Spring.StiffnessMedium,
): FiniteAnimationSpec<T> {
    val scale = LocalAnimScale.current
    return if (scale <= 0f) snap()
    else spring(dampingRatio = dampingRatio, stiffness = (stiffness / scale.coerceAtLeast(0.15f)))
}

/**
 * Не-@Composable вариант [scaledTween] для transition-ламбд NavHost
 * (AnimatedContentTransitionScope.() -> EnterTransition не имеет Composable scope).
 * scale читается один раз в родительском @Composable и передаётся явно.
 */
fun <T> tweenScaled(
    scale: Float,
    durationMillis: Int = 300,
    easing: Easing = FastOutSlowInEasing,
): FiniteAnimationSpec<T> = if (scale <= 0f) snap()
else tween(durationMillis = (durationMillis * scale).toInt().coerceAtLeast(1), easing = easing)

/**
 * Не-@Composable вариант [scaledSpring] для gesture-колбэков
 * (detectHorizontalDragGestures onDragEnd/onDragCancel — не @Composable scope).
 * scale читается в родительском @Composable (через LocalAnimScale.current) и
 * передаётся явно. При scale ≤ 0f → snap (мгновенно), иначе spring с stiffness/scale.
 */
fun <T> springScaled(
    scale: Float,
    dampingRatio: Float = Spring.DampingRatioNoBouncy,
    stiffness: Float = Spring.StiffnessMedium,
): FiniteAnimationSpec<T> = if (scale <= 0f) snap()
else spring(dampingRatio = dampingRatio, stiffness = (stiffness / scale.coerceAtLeast(0.15f)))

/**
 * #M3E-MOTION: Спецификация пружинной физики Material 3 Expressive (https://matraic.github.io/m3e/styles/motion.html).
 *
 * M3 Expressive использует 6 канонических токенов пружинной физики (Fast / Default / Slow × Spatial / Effects):
 *  - Spatial (пространство, геометрия, морфинг, скейл): damping 0.8f (выразительный лёгкий упругий отклик)
 *      • Fast Spatial: stiffness 1400f (переключатели, нажатия кнопок, микро-интеракции)
 *      • Default Spatial: stiffness 700f (карточки, шторки, навигационные панели)
 *      • Slow Spatial: stiffness 300f (полноэкранные переходы, крупные контейнеры)
 *  - Effects (прозрачность, цвет, альфа): damping 1.0f (DampingRatioNoBouncy, без перехлёста альфы)
 *      • Fast Effects: stiffness 3800f
 *      • Default Effects: stiffness 1600f
 *      • Slow Effects: stiffness 800f
 */
object M3ExpressiveMotion {
    const val DampingExpressive: Float = 0.8f
    const val DampingStandard: Float = Spring.DampingRatioNoBouncy // 1.0f

    // Legacy aliases preserved for existing call sites
    const val StiffnessSpatial: Float = 700f
    const val StiffnessEffects: Float = 1600f

    // Official M3E Spatial spring stiffness tokens (damping = 0.8f)
    const val StiffnessFastSpatial: Float = 1400f
    const val StiffnessDefaultSpatial: Float = 700f
    const val StiffnessSlowSpatial: Float = 300f

    // Official M3E Effects spring stiffness tokens (damping = 1.0f)
    const val StiffnessFastEffects: Float = 3800f
    const val StiffnessDefaultEffects: Float = 1600f
    const val StiffnessSlowEffects: Float = 800f

    fun <T> fastSpatial(scale: Float): FiniteAnimationSpec<T> =
        expressiveSpringScaled(scale, DampingExpressive, StiffnessFastSpatial)

    fun <T> defaultSpatial(scale: Float): FiniteAnimationSpec<T> =
        expressiveSpringScaled(scale, DampingExpressive, StiffnessDefaultSpatial)

    fun <T> slowSpatial(scale: Float): FiniteAnimationSpec<T> =
        expressiveSpringScaled(scale, DampingExpressive, StiffnessSlowSpatial)

    fun <T> fastEffects(scale: Float): FiniteAnimationSpec<T> =
        expressiveSpringScaled(scale, DampingStandard, StiffnessFastEffects)

    fun <T> defaultEffects(scale: Float): FiniteAnimationSpec<T> =
        expressiveSpringScaled(scale, DampingStandard, StiffnessDefaultEffects)

    fun <T> slowEffects(scale: Float): FiniteAnimationSpec<T> =
        expressiveSpringScaled(scale, DampingStandard, StiffnessSlowEffects)

    @Composable
    fun <T> fastSpatial(): FiniteAnimationSpec<T> =
        expressiveSpring(DampingExpressive, StiffnessFastSpatial)

    @Composable
    fun <T> defaultSpatial(): FiniteAnimationSpec<T> =
        expressiveSpring(DampingExpressive, StiffnessDefaultSpatial)

    @Composable
    fun <T> slowSpatial(): FiniteAnimationSpec<T> =
        expressiveSpring(DampingExpressive, StiffnessSlowSpatial)

    @Composable
    fun <T> fastEffects(): FiniteAnimationSpec<T> =
        expressiveSpring(DampingStandard, StiffnessFastEffects)

    @Composable
    fun <T> defaultEffects(): FiniteAnimationSpec<T> =
        expressiveSpring(DampingStandard, StiffnessDefaultEffects)

    @Composable
    fun <T> slowEffects(): FiniteAnimationSpec<T> =
        expressiveSpring(DampingStandard, StiffnessSlowEffects)
}

/**
 * Не-@Composable пружина Material 3 Expressive для переходов NavHost и AnimatedVisibility.
 * При scale <= 0f возвращает [snap] (Reduced Motion / выключенные анимации).
 * При scale > 0f возвращает [spring] с адаптацией stiffness под скорость интерфейса.
 */
fun <T> expressiveSpringScaled(
    scale: Float,
    dampingRatio: Float = M3ExpressiveMotion.DampingStandard,
    stiffness: Float = M3ExpressiveMotion.StiffnessEffects,
): FiniteAnimationSpec<T> = if (scale <= 0f) snap()
else spring(
    dampingRatio = dampingRatio,
    stiffness = (stiffness / scale.coerceAtLeast(0.15f)),
)

/**
 * @Composable пружина Material 3 Expressive для компонентов внутри composable-иерархии.
 * Читает [LocalAnimScale].current.
 */
@Composable
fun <T> expressiveSpring(
    dampingRatio: Float = M3ExpressiveMotion.DampingStandard,
    stiffness: Float = M3ExpressiveMotion.StiffnessEffects,
): FiniteAnimationSpec<T> {
    val scale = LocalAnimScale.current
    return if (scale <= 0f) snap()
    else spring(
        dampingRatio = dampingRatio,
        stiffness = (stiffness / scale.coerceAtLeast(0.15f)),
    )
}


