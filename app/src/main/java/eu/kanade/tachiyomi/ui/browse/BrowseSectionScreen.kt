package eu.kanade.tachiyomi.ui.browse

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import dev.zacsweers.metrox.viewmodel.metroViewModel
import eu.kanade.presentation.components.AppBar
import eu.kanade.presentation.components.AppBarActions
import eu.kanade.presentation.components.AppBarTitle
import eu.kanade.presentation.components.SearchToolbar
import eu.kanade.presentation.components.TabContent
import eu.kanade.presentation.util.Screen
import eu.kanade.tachiyomi.ui.browse.extension.ExtensionsViewModel
import eu.kanade.tachiyomi.ui.browse.extension.extensionsTab
import eu.kanade.tachiyomi.ui.browse.migration.sources.migrateSourceTab
import eu.kanade.tachiyomi.ui.browse.source.sourcesTab
import eu.kanade.tachiyomi.ui.home.HomeScreen
import tachiyomi.presentation.core.components.material.Scaffold
import tachiyomi.presentation.core.i18n.stringResource

enum class BrowseSection {
    Sources,
    Extensions,
    Migration,
}

// One of the Browse menu's sections as its own screen. Each section is
// still the same TabContent the tabbed page used, rendered here with its
// own top bar (back button, title, actions) instead of the pager.
data class BrowseSectionScreen(val section: BrowseSection) : Screen() {

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        // navigator.pop() returns Boolean; the explicit Unit type keeps
        // the lambda from inferring () -> Boolean, which the call sites
        // below would reject.
        val navigateUp: () -> Unit = { navigator.pop() }
        when (section) {
            BrowseSection.Sources -> {
                BrowseSectionContent(
                    tab = sourcesTab(),
                    navigateUp = navigateUp,
                )
            }
            BrowseSection.Extensions -> {
                val viewModel = metroViewModel<ExtensionsViewModel>()
                val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
                // Hide the title capsule while the search field is open
                // (it would sit under it), like the tabbed page did.
                LaunchedEffect(searchQuery) {
                    HomeScreen.showTitleCapsule(searchQuery == null)
                }
                BrowseSectionContent(
                    tab = extensionsTab(viewModel),
                    navigateUp = navigateUp,
                    searchQuery = searchQuery,
                    onChangeSearchQuery = viewModel::search,
                )
            }
            BrowseSection.Migration -> {
                BrowseSectionContent(
                    tab = migrateSourceTab(),
                    navigateUp = navigateUp,
                )
            }
        }
    }
}

@Composable
private fun BrowseSectionContent(
    tab: TabContent,
    navigateUp: () -> Unit,
    searchQuery: String? = null,
    onChangeSearchQuery: (String?) -> Unit = {},
) {
    val snackbarHostState = remember { SnackbarHostState() }
    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            if (tab.searchEnabled) {
                SearchToolbar(
                    searchQuery = searchQuery,
                    onChangeSearchQuery = onChangeSearchQuery,
                    titleContent = { AppBarTitle(stringResource(tab.titleRes)) },
                    navigateUp = navigateUp,
                    actions = { AppBarActions(tab.actions) },
                )
            } else {
                AppBar(
                    title = stringResource(tab.titleRes),
                    navigateUp = navigateUp,
                    actions = { AppBarActions(tab.actions) },
                )
            }
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
    ) { contentPadding ->
        tab.content(contentPadding, snackbarHostState)
    }
}
