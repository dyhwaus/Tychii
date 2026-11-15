package eu.kanade.presentation.miuix

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.tween
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize

/**
 * One motion language for everything that floats: the title capsule,
 * the action capsule, the bottom bar, the selection pills, the reader
 * bars and the bottom action menus. Every floating element enters in
 * 300 ms and leaves in 250 ms with the same easing, so they all move
 * like one object instead of each using AnimatedVisibility's defaults
 * (spring slides, 150 ms fades) or its own tween.
 *
 * The start button is left out on purpose: its M3
 * animateFloatingActionButton keeps the layout measured while hidden,
 * which the list's bottom clearance depends on.
 */
object FloatMotion {
    const val EnterDurationMillis = 300
    const val ExitDurationMillis = 250

    val Easing = FastOutSlowInEasing

    val FadeIn: FiniteAnimationSpec<Float> = tween(EnterDurationMillis, easing = Easing)
    val FadeOut: FiniteAnimationSpec<Float> = tween(ExitDurationMillis, easing = Easing)
    val SlideIn: FiniteAnimationSpec<IntOffset> = tween(EnterDurationMillis, easing = Easing)
    val SlideOut: FiniteAnimationSpec<IntOffset> = tween(ExitDurationMillis, easing = Easing)
    val SizeIn: FiniteAnimationSpec<IntSize> = tween(EnterDurationMillis, easing = Easing)
    val SizeOut: FiniteAnimationSpec<IntSize> = tween(ExitDurationMillis, easing = Easing)
}
