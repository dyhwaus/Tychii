package eu.kanade.tachiyomi.ui.browse.extension.details

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import dev.zacsweers.metrox.viewmodel.assistedMetroViewModel
import eu.kanade.presentation.browse.ExtensionDetailsScreen
import eu.kanade.presentation.util.Screen
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.screens.EmptyScreen
import tachiyomi.presentation.core.screens.LoadingScreen
import top.yukonga.miuix.kmp.basic.Scaffold as MiuixScaffold

data class ExtensionDetailsScreen(
    private val pkgName: String,
) : Screen() {

    @Composable
    override fun Content() {
        val viewModel =
            assistedMetroViewModel<ExtensionDetailsViewModel, ExtensionDetailsViewModel.Factory> {
                create(pkgName = pkgName)
            }
        val state by viewModel.state.collectAsStateWithLifecycle()

        val navigator = LocalNavigator.currentOrThrow

        when (val state = state) {
            ExtensionDetailsViewModel.State.Loading -> LoadingScreen()
            ExtensionDetailsViewModel.State.Uninstalled -> {
                LaunchedEffect(Unit) { navigator.pop() }
                EmptyScreen(MR.strings.empty_screen)
            }
            is ExtensionDetailsViewModel.State.Success -> {
                // The Miuix overlays on this page — the AppBar
                // overflow dropdown and the content-warning dialog
                // — only render inside a Miuix Scaffold ancestor.
                // Without it they silently never appear. The
                // wrapper is layout-neutral: zero insets, the page
                // background.
                MiuixScaffold(
                    containerColor = MaterialTheme.colorScheme.background,
                    contentWindowInsets = WindowInsets(0, 0, 0, 0),
                ) {
                    ExtensionDetailsScreen(
                        navigateUp = navigator::pop,
                        state = state,
                        onClickSourcePreferences = { navigator.push(SourcePreferencesScreen(it)) },
                        onClickEnableAll = { viewModel.toggleSources(true) },
                        onClickDisableAll = { viewModel.toggleSources(false) },
                        onClickClearCookies = viewModel::clearCookies,
                        onClickUninstall = viewModel::uninstallExtension,
                        onClickSource = viewModel::toggleSource,
                        onClickIncognito = viewModel::toggleIncognito,
                    )
                }
            }
        }
    }
}
