package eu.kanade.presentation.miuix

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import eu.kanade.domain.ui.model.BarStyle
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.blur.Backdrop
import top.yukonga.miuix.kmp.blur.blur
import top.yukonga.miuix.kmp.blur.drawBackdrop
import top.yukonga.miuix.kmp.theme.MiuixTheme
import kotlinx.coroutines.launch

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
// liquid glass blurs the page content behind it and refracts it at the
// edges, like the bottom bar; brushed metal is an opaque textured
// surface. In glass mode it consumes the shared
// [backdrop], so — like the title capsule and the bottom bar — it must
// stay OUTSIDE the layer recorded by `layerBackdrop` (HomeScreen
// renders it as an overlay, never inside a tab's toolbar or content),
// or it will blur itself and crash hwui (infinite recursion in
// RenderNode::prepareTreeImpl).
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
    val isMetal = barStyle == BarStyle.METAL
    val metalDark = metalIsDark()
    val surfaceContainer = MiuixTheme.colorScheme.surfaceContainer
    val containerColor = when (barStyle) {
        BarStyle.OPAQUE -> surfaceContainer
        BarStyle.TRANSLUCENT -> surfaceContainer.copy(alpha = 0.75f)
        BarStyle.LIQUID_GLASS -> surfaceContainer.copy(alpha = MiuixGlassSurfaceAlpha)
        // Metal draws its own surface; this value is never used.
        BarStyle.METAL -> surfaceContainer
    }
    // A vertical pill: the icon buttons are ~40.dp wide, so 24.dp is
    // (close to) half the width — a capsule radius. RoundedCornerShape,
    // not CircleShape, so the backdrop shape stays a CornerBasedShape.
    val shape = remember { RoundedCornerShape(24.dp) }
    val animationScope = rememberCoroutineScope()
    val pressScale = remember { Animatable(1f) }
    // Only follow the tilt sensor in glass mode; opaque/translucent don't
    // draw a highlight at all.
    val highlight = if (isGlass) rememberTiltBrightHighlight() else null
    Column(
        modifier = modifier
            .graphicsLayer {
                scaleX = pressScale.value
                scaleY = pressScale.value
            }
            .then(
            if (isGlass) {
                Modifier.drawBackdrop(
                    backdrop = backdrop,
                    shape = { shape },
                    effects = {
                        vibrancy()
                        blur(MiuixGlassBlurRadius.toPx(), MiuixGlassBlurRadius.toPx())
                        // The lens is what makes this liquid glass rather
                        // than plain frosted blur — same refraction as
                        // the bottom bar. It only works on a
                        // CornerBasedShape, which the 24.dp rounded
                        // capsule is.
                        lens(
                            refractionHeight = 16.dp.toPx(),
                            refractionAmount = 10.dp.toPx(),
                            depthEffect = true,
                            chromaticAberration = MiuixGlassChromaticAberration,
                        )
                    },
                    highlight = { highlight?.copy(alpha = 0.66f) },
                    onDrawSurface = { drawRect(containerColor) },
                )
            } else if (isMetal) {
                Modifier.metalSurface(shape, metalDark)
            } else {
                Modifier.background(containerColor, shape)
            },
        )
            .padding(vertical = 4.dp),
    ) {
        actions.forEach { action ->
            IconButton(
                onClick = {
                    animationScope.launch {
                        pressScale.animateTo(0.975f, spring(dampingRatio = 0.62f, stiffness = 650f))
                        pressScale.animateTo(1f, spring(dampingRatio = 0.48f, stiffness = 420f))
                    }
                    action.onClick()
                },
            ) {
                if (isMetal) {
                    // 工具图标也是无色刻字；正在生效的项（原来
                    // 传了 accent tint 的）字形里淡淡填一层色，
                    // 和漫画卡片的标题一样——填色就是生效的标
                    // 记，辉光没有了。
                    val filled = action.tint != null
                    Box {
                        if (filled) {
                            Icon(
                                imageVector = action.icon,
                                contentDescription = null,
                                tint = metalCarvedFill(metalDark),
                            )
                        }
                        MetalCarvedIcon(
                            imageVector = action.icon,
                            contentDescription = action.contentDescription,
                            isDark = metalDark,
                        )
                    }
                } else {
                    Icon(
                        imageVector = action.icon,
                        contentDescription = action.contentDescription,
                        tint = action.tint ?: MiuixTheme.colorScheme.onSurface,
                    )
                }
            }
        }
    }
}
