package eu.kanade.presentation.miuix

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.luminance
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
 * liquid glass blurs the page content behind it. [barStyle] is already the
 * effective style (liquid glass resolved to translucent on API < 32 by the
 * caller), so glass here always means the device supports it.
 *
 * Tab width: 5 tabs share the capsule by weight, with a 64.dp minimum per
 * tab (5 x 64.dp = 320.dp, fits narrow phones; Wenku8Reader used 76.dp
 * for its 3 tabs).
 */
@Composable
fun TychiiLiquidBar(
    tabs: List<Tab>,
    selectedIndex: () -> Int,
    onTabSelected: (Int) -> Unit,
    onTabClick: (Tab) -> Unit,
    showLabels: Boolean,
    barStyle: BarStyle,
    backdrop: Backdrop,
    tabBadge: @Composable (Tab) -> (@Composable () -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val isDark = MiuixTheme.colorScheme.background.luminance() < 0.5f
    // The capsule floats above the gesture bar, like Wenku8Reader.
    Box(
        modifier = modifier.padding(
            start = LiquidBarOuterPadding,
            end = LiquidBarOuterPadding,
            bottom = LiquidBarVerticalPadding,
        ),
    ) {
        MiuixLiquidBottomBar(
            selectedIndex = selectedIndex,
            onSelected = onTabSelected,
            backdrop = backdrop,
            tabsCount = tabs.size,
            isDark = isDark,
            isBlurEnabled = barStyle == BarStyle.LIQUID_GLASS,
            containerAlpha = when (barStyle) {
                BarStyle.OPAQUE -> 1f
                BarStyle.TRANSLUCENT -> 0.75f
                // Ignored when blur is on (the glass container is 0.4 alpha).
                BarStyle.LIQUID_GLASS -> 1f
            },
        ) {
            tabs.forEachIndexed { index, tab ->
                val selected = selectedIndex() == index
                val badge = tabBadge(tab)
                FloatingBottomBarItem(
                    // A tap goes through onTabClick (which handles re-select);
                    // the bar's onSelected only fires when the animated index
                    // actually changes, so the two never double-fire.
                    onClick = { onTabClick(tab) },
                    modifier = Modifier.widthIn(min = 64.dp),
                ) {
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

/** The capsule's outer padding from the screen edges and the gesture bar. */
private val LiquidBarOuterPadding = 12.dp
private val LiquidBarVerticalPadding = 8.dp
