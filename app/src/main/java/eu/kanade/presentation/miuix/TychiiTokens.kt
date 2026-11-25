package eu.kanade.presentation.miuix

import androidx.compose.ui.unit.dp

/**
 * The shared corner radii: one value per element family, so the floating
 * pieces and the covers all round the same way instead of each carrying
 * its own literal. Motion already lives in FloatMotion, and the list
 * clearances in TychiiLiquidBar (TopCapsuleClearance,
 * LiquidNavBarClearance).
 */
object TychiiCorners {
    /** Floating pills: the bottom bar, selection bars, bottom action menus. */
    val Pill = 32.dp

    /** Floating bars: the reader bars, the chapter navigator, the action capsule. */
    val Bar = 24.dp

    /** Manga covers in the grid and on the shelf. */
    val Cover = 8.dp
}
