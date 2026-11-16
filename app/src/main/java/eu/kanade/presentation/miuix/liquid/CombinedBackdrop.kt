// From https://github.com/SymonChu/WeKite (app/src/main/java/com/github/wekite/ui/content/liquid/CombinedBackdrop.kt)
// Copyright 2026, compose-miuix-ui contributors, Apache-2.0
//
// Adapted from Kyant0/AndroidLiquidGlass — https://github.com/Kyant0/AndroidLiquidGlass (Apache 2.0).
// Vendored from InstallerX-Revived's ui/library/liquid package via SymonChu/WeKite.

package eu.kanade.presentation.miuix.liquid

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.GraphicsLayerScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.unit.Density
import top.yukonga.miuix.kmp.blur.Backdrop

/**
 * A [Backdrop] that draws [first] then [second] in order, allowing a tinted/overlay
 * backdrop to be sampled on top of a base backdrop. Mirrors Kyant's `CombinedBackdrop`
 * pattern used in `LiquidBottomTabs` to layer a recorded "tinted tabs" pass over the
 * underlying app background as a single sampling source for an indicator.
 *
 * 底栏的选中 pill 用它把页面和已染色的选中层叠成一个采样源：pill 折射到的
 * 不再只是页面，还有选中态的图标。
 */
@Stable
internal class CombinedBackdrop(
    val first: Backdrop,
    val second: Backdrop,
) : Backdrop {

    override val isCoordinatesDependent: Boolean = first.isCoordinatesDependent || second.isCoordinatesDependent

    override val offsetResidualX: Float get() = first.offsetResidualX
    override val offsetResidualY: Float get() = first.offsetResidualY

    override fun DrawScope.drawBackdrop(
        density: Density,
        coordinates: LayoutCoordinates?,
        layerBlock: (GraphicsLayerScope.() -> Unit)?,
        downscaleFactor: Int,
    ) {
        with(first) { drawBackdrop(density, coordinates, layerBlock, downscaleFactor) }
        with(second) { drawBackdrop(density, coordinates, layerBlock, downscaleFactor) }
    }
}

@Composable
internal fun rememberCombinedBackdrop(first: Backdrop, second: Backdrop): Backdrop =
    remember(first, second) { CombinedBackdrop(first, second) }
