package eu.kanade.presentation.miuix

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import eu.kanade.domain.ui.model.BarStyle
import eu.kanade.domain.ui.model.GlassTier
import top.yukonga.miuix.kmp.blur.Backdrop
import top.yukonga.miuix.kmp.blur.blur
import top.yukonga.miuix.kmp.blur.drawBackdrop
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * A compact chip (icon + label) that follows the global bar style:
 * liquid glass samples the backdrop, metal engraves, anything else is
 * a flat surface. Originally the Explore page's filter chip; shared now
 * that the Library's smart groups need the same shape.
 *
 * [backdrop] is null for chips that sit inside the backdrop-recorded
 * layer (the Library row): a glass chip there would recurse, so it
 * renders as translucent instead — the same veil the glass branch
 * draws over its blur. [glassTier] defaults to FULL; it only matters
 * when a backdrop is passed.
 */
@Composable
fun FilterChip(
    selected: Boolean,
    onClick: () -> Unit,
    icon: ImageVector? = null,
    label: String,
    barStyle: BarStyle,
    glassTier: GlassTier = GlassTier.FULL,
    backdrop: Backdrop? = null,
) {
    val colorScheme = MiuixTheme.colorScheme
    // Glass only when a backdrop is actually handed in, and never in
    // LITE (which renders translucent instead).
    val glassBackdrop = if (barStyle == BarStyle.LIQUID_GLASS && glassTier != GlassTier.LITE) backdrop else null
    val isMetal = barStyle == BarStyle.METAL
    val metalDark = metalIsDark()
    val containerColor = when (barStyle) {
        BarStyle.OPAQUE -> colorScheme.surfaceContainer
        BarStyle.TRANSLUCENT, BarStyle.LIQUID_GLASS -> colorScheme.surfaceContainer.copy(alpha = 0.75f)
        // Metal draws its own surface; this value is never used.
        BarStyle.METAL -> colorScheme.surfaceContainer
    }
    val contentColor = if (selected) colorScheme.primary else colorScheme.onSurface
    val shape = RoundedCornerShape(12.dp)
    // BALANCED keeps the highlight static.
    val highlight = when {
        glassBackdrop == null -> null
        glassTier == GlassTier.FULL -> rememberTiltBrightHighlight()
        else -> MiuixGlassSpecular
    }
    val textStyle = LocalTextStyle.current
    Box(
        modifier = Modifier
            .clip(shape)
            .then(
                if (glassBackdrop != null) {
                    Modifier.drawBackdrop(
                        backdrop = glassBackdrop,
                        shape = { shape },
                        effects = {
                            vibrancy()
                            blur(MiuixGlassBlurRadius.toPx(), MiuixGlassBlurRadius.toPx())
                            // BALANCED skips the lens: the shader is the
                            // expensive part.
                            if (glassTier == GlassTier.FULL) {
                                lens(
                                    refractionHeight = 16.dp.toPx(),
                                    refractionAmount = 10.dp.toPx(),
                                    depthEffect = true,
                                    chromaticAberration = MiuixGlassChromaticAberration,
                                )
                            }
                        },
                        highlight = { highlight?.copy(alpha = 0.66f) },
                        onDrawSurface = { drawRect(containerColor) },
                    )
                } else if (isMetal) {
                    Modifier.metalSurface(shape, metalDark)
                } else {
                    Modifier.background(containerColor)
                },
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (icon != null) {
                if (isMetal) {
                    MetalEngravedIcon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = contentColor,
                        isDark = metalDark,
                        modifier = Modifier.size(18.dp),
                    )
                } else {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = contentColor,
                    )
                }
            }
            Text(
                text = label,
                modifier = Modifier.padding(start = if (icon != null) 4.dp else 0.dp),
                color = contentColor,
                style = if (isMetal) {
                    textStyle.copy(shadow = metalEngravedShadow(metalDark))
                } else {
                    textStyle
                },
            )
        }
    }
}
