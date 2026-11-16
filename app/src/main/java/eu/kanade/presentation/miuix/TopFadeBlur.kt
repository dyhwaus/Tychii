package eu.kanade.presentation.miuix

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import eu.kanade.domain.ui.model.BarStyle
import top.yukonga.miuix.kmp.blur.Backdrop
import top.yukonga.miuix.kmp.blur.ProgressiveBlur
import top.yukonga.miuix.kmp.blur.drawBackdrop
import top.yukonga.miuix.kmp.blur.progressiveBlur

/**
 * A top edge fade for a scrolling list: content scrolling up under
 * it dissolves instead of being cut off at the top edge. The veil is
 * a vertical gradient from [color] at the top to transparent at the
 * bottom; in liquid glass mode the content behind it is also
 * progressively blurred — strongest at the top, sharp at the bottom
 * — through Miuix's [ProgressiveBlur.Top].
 *
 * Like the floating capsules and the bottom bar, this consumes the
 * shared [backdrop], so it must stay OUTSIDE the layer recorded by
 * `layerBackdrop`, or it will blur itself and crash hwui. The plain
 * gradient background stays as the veil in every mode, so the fade
 * still works where the blur is off or unsupported — the backdrop
 * only adds the blur on top of it.
 *
 * @param height The full height of the fade zone, from the top edge.
 * @param color The colour the content fades into: the page
 *   background on the tab pages, the top bar's surface on Explore.
 */
@Composable
fun TopFadeBlur(
    height: Dp,
    color: Color,
    barStyle: BarStyle,
    backdrop: Backdrop,
    modifier: Modifier = Modifier,
) {
    val isGlass = barStyle == BarStyle.LIQUID_GLASS
    val veil = Brush.verticalGradient(
        0f to color,
        1f to color.copy(alpha = 0f),
    )
    val blurRadius = with(LocalDensity.current) { 12.dp.toPx() }
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .background(veil)
            .then(
                if (isGlass) {
                    Modifier.drawBackdrop(
                        backdrop = backdrop,
                        shape = { RectangleShape },
                        effects = {
                            progressiveBlur(blurRadius, blurRadius, ProgressiveBlur.Top)
                        },
                        progressiveGradient = ProgressiveBlur.Top,
                        onDrawSurface = { drawRect(brush = veil) },
                    )
                } else {
                    Modifier
                },
            ),
    )
}
