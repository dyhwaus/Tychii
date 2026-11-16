package eu.kanade.presentation.miuix

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import eu.kanade.domain.ui.model.BarStyle
import top.yukonga.miuix.kmp.blur.Backdrop
import top.yukonga.miuix.kmp.blur.blur
import top.yukonga.miuix.kmp.blur.drawBackdrop
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * A centered Miuix capsule showing the current page title, floating over
 * the top of the main tab pages (a HomeScreen overlay, like the bottom
 * bar — it replaces the top-left title of each tab's own toolbar).
 *
 * Follows the same [BarStyle] as the bottom bar: opaque is a solid
 * surfaceContainer, translucent is surfaceContainer at 0.75 alpha, liquid
 * glass blurs the page content behind it. In glass mode it consumes the
 * shared [backdrop], so it must stay OUTSIDE the layer recorded by
 * `layerBackdrop` (like the bottom bar): never place it inside a tab's
 * toolbar or content, or it will blur itself and crash hwui (infinite
 * recursion in RenderNode::prepareTreeImpl).
 *
 * The glass here is blur + vibrancy + specular highlight, plus a
 * very shallow lens (8.dp / 4.dp, chromatic aberration at a quarter
 * of the shared value) — just enough for a glass edge, not enough to
 * distort the small pill's text.
 */
@Composable
fun MiuixCapsuleTitle(
    title: String,
    barStyle: BarStyle,
    backdrop: Backdrop,
    modifier: Modifier = Modifier,
) {
    val isGlass = barStyle == BarStyle.LIQUID_GLASS
    val surfaceContainer = MiuixTheme.colorScheme.surfaceContainer
    val containerColor = when (barStyle) {
        BarStyle.OPAQUE -> surfaceContainer
        BarStyle.TRANSLUCENT -> surfaceContainer.copy(alpha = 0.75f)
        BarStyle.LIQUID_GLASS -> surfaceContainer.copy(alpha = MiuixGlassSurfaceAlpha)
    }
    // ~40.dp tall pill (titleMedium + 8.dp vertical padding): 20.dp is a
    // perfect capsule radius. RoundedCornerShape, not CircleShape, so the
    // backdrop shape stays a CornerBasedShape.
    val shape = remember { RoundedCornerShape(20.dp) }
    // Only follow the tilt sensor in glass mode; opaque/translucent don't
    // draw a highlight at all.
    val highlight = if (isGlass) rememberTiltBrightHighlight() else null
    Box(
        modifier = modifier.then(
            if (isGlass) {
                Modifier.drawBackdrop(
                    backdrop = backdrop,
                    shape = { shape },
                    effects = {
                        vibrancy()
                        blur(MiuixGlassBlurRadius.toPx(), MiuixGlassBlurRadius.toPx())
                        // Very shallow refraction keeps title glyphs crisp while
                        // giving the capsule a real glass edge.
                        lens(
                            refractionHeight = 8.dp.toPx(),
                            refractionAmount = 4.dp.toPx(),
                            depthEffect = true,
                            chromaticAberration = MiuixGlassChromaticAberration * 0.25f,
                        )
                    },
                    highlight = { highlight?.copy(alpha = 0.64f) },
                    onDrawSurface = { drawRect(containerColor) },
                )
            } else {
                Modifier.background(containerColor, shape)
            },
        ),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MiuixTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .widthIn(max = 180.dp)
                .padding(horizontal = 16.dp, vertical = 8.dp),
        )
    }
}
