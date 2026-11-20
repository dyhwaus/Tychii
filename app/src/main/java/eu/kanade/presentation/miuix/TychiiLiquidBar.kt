package eu.kanade.presentation.miuix

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import eu.kanade.domain.ui.model.BarStyle
import eu.kanade.presentation.util.Tab
import top.yukonga.miuix.kmp.blur.Backdrop
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * Tychii's adapter for the liquid-glass bottom bar (Wenku8Reader style).
 *
 * Wires Tychii's 5 tabs into [MiuixLiquidBottomBar]: Painter-based icons
 * (including the animated library icon), the Updates/Browse badges, and the
 * "show navigation bar labels" setting. The accent is Miuix's default blue
 * ([MiuixTheme.colorScheme.primary]), not the app's Neko pink — the user
 * chose Miuix default blue for the liquid bar.
 *
 * The bar follows the same [BarStyle] as the title capsule: opaque is a
 * solid surfaceContainer, translucent is surfaceContainer at 0.75 alpha,
 * liquid glass blurs the page content behind it, brushed metal is an
 * opaque textured surface. All four use the same single capsule; in metal
 * mode the capsule surface is brushed metal and the tab icons and labels
 * are colourless carvings (the same treatment as the card titles), and
 * the selected tab pops up like a switch — the raise the old separate
 * metal buttons used — while the others stay flat. The separate metal
 * buttons ([MetalTabButtons]) are kept in the repo but no longer used.
 * [barStyle] is already the effective style (liquid glass resolved to
 * translucent on API < 32 by the caller), so glass here always means the
 * device supports it.
 *
 * Tab width: 5 tabs share the capsule by weight, with a 64.dp minimum per
 * tab (5 x 64.dp = 320.dp, fits narrow phones; Wenku8Reader used 76.dp
 * for its 3 tabs).
 */
@Composable
fun TychiiLiquidBar(
    tabs: List<Tab>,
    selectedIndex: () -> Int,
    onTabClick: (Tab) -> Unit,
    showLabels: Boolean,
    barStyle: BarStyle,
    backdrop: Backdrop,
    tabBadge: @Composable (Tab) -> (@Composable () -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val isDark = MiuixTheme.colorScheme.background.luminance() < 0.5f
    val isMetal = barStyle == BarStyle.METAL
    // The capsule floats above the gesture bar, like Wenku8Reader.
    Box(
        modifier = modifier.padding(
            start = LiquidBarOuterPadding,
            end = LiquidBarOuterPadding,
            bottom = LiquidBarVerticalPadding,
        ),
    ) {
        MiuixLiquidBottomBar(
            backdrop = backdrop,
            isDark = isDark,
            isBlurEnabled = barStyle == BarStyle.LIQUID_GLASS,
            containerAlpha = when (barStyle) {
                BarStyle.OPAQUE -> 1f
                BarStyle.TRANSLUCENT -> 0.75f
                // Ignored when blur is on (the glass container is 0.4 alpha).
                BarStyle.LIQUID_GLASS -> 1f
                // Metal is opaque; the surface is brushed metal, not a colour.
                BarStyle.METAL -> 1f
            },
            isMetal = isMetal,
        ) {
            tabs.forEachIndexed { index, tab ->
                val selected = selectedIndex() == index
                val badge = tabBadge(tab)
                // Keyed by tab, not by position: hiding the Updates
                // tab shifts the positions, and the raise animation
                // state must stay with its tab.
                key(tab) {
                    // In metal mode the selected tab pops up like a
                    // switch (the raise the old separate buttons
                    // used); in the other modes the selection is the
                    // accent tint below and the raise stays at 0.
                    val raise by animateFloatAsState(
                        targetValue = if (selected && isMetal) 1f else 0f,
                        animationSpec = spring(dampingRatio = 0.6f, stiffness = 500f),
                        label = "metalTabRaise",
                    )
                    FloatingBottomBarItem(
                        // A tap goes through onTabClick, which handles
                        // both switching and re-selecting.
                        onClick = { onTabClick(tab) },
                        modifier = Modifier
                            .widthIn(min = 64.dp)
                            .then(
                                if (isMetal) {
                                    Modifier.graphicsLayer {
                                        translationY = -4.dp.toPx() * raise
                                        val scale = 1f + 0.04f * raise
                                        scaleX = scale
                                        scaleY = scale
                                    }
                                } else {
                                    Modifier
                                },
                            ),
                    ) {
                        if (isMetal) {
                            // Colourless carving over a faint fill,
                            // like the card titles. The Library icon
                            // animates on selection, so it stays live;
                            // the other icons are pre-rendered bitmaps.
                            // The Library tab is index 0 (fixed in
                            // LibraryTab); its painter type is not
                            // public API, so the index is the marker.
                            BadgedBox(
                                badge = { badge?.invoke() },
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        painter = tab.options.icon!!,
                                        contentDescription = null,
                                        tint = metalCarvedFill(isDark),
                                        modifier = Modifier.size(24.dp),
                                    )
                                    if (tab.options.index.toInt() == 0) {
                                        MetalCarvedIcon(
                                            painter = tab.options.icon!!,
                                            contentDescription = tab.options.title,
                                            isDark = isDark,
                                            modifier = Modifier.size(24.dp),
                                        )
                                    } else {
                                        // 24.dp wide, 1.dp top and bottom
                                        // for the offset carving edges.
                                        Image(
                                            bitmap = rememberCarvedIconBitmap(
                                                painter = tab.options.icon!!,
                                                isDark = isDark,
                                            ),
                                            contentDescription = tab.options.title,
                                            modifier = Modifier.size(24.dp, 26.dp),
                                        )
                                    }
                                }
                            }
                            if (showLabels) {
                                val labelStyle = TextStyle(
                                    fontSize = 11.sp,
                                    lineHeight = 14.sp,
                                )
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = tab.options.title,
                                        style = labelStyle,
                                        color = metalCarvedFill(isDark),
                                        maxLines = 1,
                                    )
                                    val labelBitmap = rememberCarvedTextBitmap(
                                        text = tab.options.title,
                                        style = labelStyle,
                                        isDark = isDark,
                                    )
                                    val density = LocalDensity.current
                                    Image(
                                        bitmap = labelBitmap,
                                        contentDescription = null,
                                        modifier = Modifier.size(
                                            with(density) { labelBitmap.width.toDp() },
                                            with(density) { labelBitmap.height.toDp() },
                                        ),
                                    )
                                }
                            }
                        } else {
                            val tint = if (selected) {
                                MiuixTheme.colorScheme.primary
                            } else {
                                MiuixTheme.colorScheme.onSurface
                            }
                            BadgedBox(
                                badge = { badge?.invoke() },
                            ) {
                                Icon(
                                    painter = tab.options.icon!!,
                                    contentDescription = tab.options.title,
                                    modifier = Modifier.size(24.dp),
                                    tint = tint,
                                )
                            }
                            if (showLabels) {
                                Text(
                                    text = tab.options.title,
                                    fontSize = 11.sp,
                                    lineHeight = 14.sp,
                                    color = tint,
                                    maxLines = 1,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/** The capsule's outer padding from the screen edges and the gesture bar. */
private val LiquidBarOuterPadding = 12.dp
private val LiquidBarVerticalPadding = 8.dp

// Bottom clearance for the tab lists: the bar is 64.dp tall and floats
// 8.dp above the gesture bar, so a list needs this much empty space
// below its last item for the item to clear the bar. Each list adds it
// to its own bottom content padding; HomeScreen does not inset the
// whole content for it, because that left a dead band behind the bar
// that the covers scrolled under.
val LiquidNavBarClearance = 96.dp

// Top clearance for the tab lists: the title capsule floats in a
// 64.dp band below the status bar, plus 8.dp of extra top padding
// so the capsule sits clear of the status bar, so a list needs
// this much empty space above its first item for the item to clear
// the capsule. It is plain padding, not a toolbar band, so when
// the list scrolls the covers move up through it and stay visible
// instead of hiding behind a solid bar.
val TopCapsuleClearance = 72.dp
