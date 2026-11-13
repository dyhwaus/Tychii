package eu.kanade.presentation.miuix

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import eu.kanade.domain.ui.model.BarStyle
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.blur.Backdrop
import top.yukonga.miuix.kmp.blur.blur
import top.yukonga.miuix.kmp.blur.drawBackdrop
import top.yukonga.miuix.kmp.theme.MiuixTheme

// One action in the floating capsule: an icon button. tint is null for
// the default onSurface colour; a tab passes its accent tint when the
// action is active (e.g. the filter icon while filters are on).
data class CapsuleAction(
    val icon: ImageVector,
    val contentDescription: String,
    val onClick: () -> Unit,
    val tint: Color? = null,
)

// A vertical floating capsule holding a tab's action icons (search,
// sort, display mode, filter, ...), on the right edge over the list.
// Same material as the title capsule and the bottom bar: opaque is a
// solid surfaceContainer, translucent is surfaceContainer at 0.75 alpha,
// liquid glass blurs the page content behind it. In glass mode it
// consumes the shared [backdrop], so — like the title capsule and the
// bottom bar — it must stay OUTSIDE the layer recorded by
// `layerBackdrop` (HomeScreen renders it as an overlay, never inside a
// tab's toolbar or content), or it will blur itself and crash hwui
// (infinite recursion in RenderNode::prepareTreeImpl).
//
// There is no layer behind it: it floats over the covers with only a
// gap (the caller pads it from the screen edge), covering part of the
// content while it is shown. It hides while the user scrolls (see
// HomeScreen's scroll observer).
@Composable
fun MiuixFloatingCapsule(
    actions: List<CapsuleAction>,
    barStyle: BarStyle,
    backdrop: Backdrop,
    modifier: Modifier = Modifier,
) {
    val isGlass = barStyle == BarStyle.LIQUID_GLASS
    val surfaceContainer = MiuixTheme.colorScheme.surfaceContainer
    val containerColor = when (barStyle) {
        BarStyle.OPAQUE -> surfaceContainer
        BarStyle.TRANSLUCENT -> surfaceContainer.copy(alpha = 0.75f)
        BarStyle.LIQUID_GLASS -> surfaceContainer.copy(alpha = 0.4f)
    }
    // A vertical pill: the icon buttons are ~40.dp wide, so 24.dp is
    // (close to) half the width — a capsule radius. RoundedCornerShape,
    // not CircleShape, so the backdrop shape stays a CornerBasedShape.
    val shape = remember { RoundedCornerShape(24.dp) }
    // Only follow the tilt sensor in glass mode; opaque/translucent don't
    // draw a highlight at all.
    val highlight = if (isGlass) rememberTiltBrightHighlight() else null
    Column(
        modifier = modifier.then(
            if (isGlass) {
                Modifier.drawBackdrop(
                    backdrop = backdrop,
                    shape = { shape },
                    effects = {
                        vibrancy()
                        blur(4.dp.toPx(), 4.dp.toPx())
                    },
                    highlight = { highlight?.copy(alpha = 0.75f) },
                    onDrawSurface = { drawRect(containerColor) },
                )
            } else {
                Modifier.background(containerColor, shape)
            },
        )
            .padding(vertical = 4.dp),
    ) {
        actions.forEach { action ->
            IconButton(onClick = action.onClick) {
                Icon(
                    imageVector = action.icon,
                    contentDescription = action.contentDescription,
                    tint = action.tint ?: MiuixTheme.colorScheme.onSurface,
                )
            }
        }
    }
}
