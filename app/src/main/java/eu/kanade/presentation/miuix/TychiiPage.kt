package eu.kanade.presentation.miuix

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import top.yukonga.miuix.kmp.basic.Scaffold as MiuixScaffold

/**
 * Layout-neutral Miuix host for pushed screens.
 *
 * Miuix overlays (OverlayDialog, OverlayDropdownPopup, OverlayListPopup) only render
 * inside a Miuix Scaffold ancestor, so every screen that shows one needs this wrapper
 * around its own content, including sibling dialogs. It changes no layout: zero
 * insets and the page background color.
 */
@Composable
fun TychiiPage(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    MiuixScaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
    ) {
        content()
    }
}
