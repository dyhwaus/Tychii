package eu.kanade.tachiyomi.ui.browse.source.browse

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.DropdownDefaults
import top.yukonga.miuix.kmp.basic.DropdownEntry
import top.yukonga.miuix.kmp.basic.DropdownItem
import top.yukonga.miuix.kmp.basic.HorizontalDivider
import top.yukonga.miuix.kmp.popup.OverlayDropdownPopup
import top.yukonga.miuix.kmp.theme.MiuixTheme
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import dev.zacsweers.metrox.viewmodel.assistedMetroViewModel
import eu.kanade.presentation.browse.BrowseSourceContent
import eu.kanade.presentation.browse.MissingSourceScreen
import eu.kanade.presentation.browse.components.RemoveMangaDialog
import eu.kanade.presentation.category.components.ChangeCategoryDialog
import eu.kanade.presentation.components.SearchToolbar
import eu.kanade.presentation.manga.DuplicateMangaDialog
import eu.kanade.presentation.miuix.CapsuleAction
import eu.kanade.presentation.util.AssistContentScreen
import eu.kanade.presentation.util.Screen
import eu.kanade.tachiyomi.source.online.HttpSource
import eu.kanade.tachiyomi.ui.browse.source.browse.BrowseSourceViewModel.Listing
import eu.kanade.tachiyomi.ui.category.CategoryScreen
import eu.kanade.tachiyomi.ui.home.HomeScreen
import eu.kanade.tachiyomi.ui.manga.MangaScreen
import eu.kanade.tachiyomi.ui.webview.WebViewScreen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import mihon.feature.migration.dialog.MigrateMangaDialog
import mihon.icons.materialsymbols.MaterialSymbols
import mihon.icons.materialsymbols.automirroredroundedfilled.ViewList
import mihon.icons.materialsymbols.rounded.FilterList
import mihon.icons.materialsymbols.rounded.Search
import mihon.icons.materialsymbols.rounded.Verified
import mihon.icons.materialsymbols.roundedfilled.Favorite
import mihon.icons.materialsymbols.roundedfilled.ViewCompact
import mihon.icons.materialsymbols.roundedfilled.ViewModule
import mihon.presentation.core.util.collectAsLazyPagingItems
import tachiyomi.core.common.Constants
import tachiyomi.domain.library.model.LibraryDisplayMode
import tachiyomi.domain.source.model.StubSource
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.material.Scaffold
import tachiyomi.presentation.core.components.material.padding
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.screens.LoadingScreen
import tachiyomi.source.local.LocalSource

data class BrowseSourceScreen(
    val sourceId: Long,
    private val listingQuery: String?,
) : Screen(), AssistContentScreen {

    private var assistUrl: String? = null

    override fun onProvideAssistUrl() = assistUrl

    @Composable
    override fun Content() {
        val viewModel =
            assistedMetroViewModel<BrowseSourceViewModel, BrowseSourceViewModel.Factory> {
                create(sourceId = sourceId, listingQuery = listingQuery)
            }
        val state by viewModel.state.collectAsState()

        val navigator = LocalNavigator.currentOrThrow
        val navigateUp: () -> Unit = {
            when {
                !state.isUserQuery && state.toolbarQuery != null -> viewModel.setToolbarQuery(null)
                else -> navigator.pop()
            }
        }

        val source = state.source
        if (source == null) {
            LoadingScreen()
            return
        }

        if (source is StubSource) {
            MissingSourceScreen(
                source = source,
                navigateUp = navigateUp,
            )
            return
        }

        val scope = rememberCoroutineScope()
        val haptic = LocalHapticFeedback.current
        val uriHandler = LocalUriHandler.current
        val snackbarHostState = remember { SnackbarHostState() }

        val onHelpClick = { uriHandler.openUri(LocalSource.HELP_URL) }
        val onWebViewClick = f@{
            val httpSource = source as? HttpSource ?: return@f
            navigator.push(
                WebViewScreen(
                    url = httpSource.getHomeUrl(),
                    initialTitle = httpSource.name,
                    sourceId = httpSource.id,
                ),
            )
        }

        LaunchedEffect(source) {
            assistUrl = (source as? HttpSource)?.getHomeUrl()
        }

        // The source name is the page title now (in the top bar below),
        // so the floating title capsule is hidden here — it would sit
        // over the source name. Returning to the Browse menu restores
        // it (the menu reports showTitleCapsule(true)). The floating
        // action capsule still shows: it no longer follows the title
        // capsule (see HomeScreen), it follows the actions reported
        // here, which are empty while searching.
        LaunchedEffect(Unit) {
            HomeScreen.showTitleCapsule(false)
        }

        // Floating action capsule, like the Library page: search and
        // cover layout. Search opens the search field in the top bar
        // (it appears while toolbarQuery is set); display opens a
        // dropdown with the four layouts. Empty while searching, so
        // the capsule hides while the search field is open.
        var selectingDisplayMode by remember { mutableStateOf(false) }
        // The descriptions are read here, in the composable body —
        // stringResource is a @Composable call and cannot run inside
        // the LaunchedEffect below (a suspend block, not a composable
        // context). The Library page hoists them the same way.
        val searchDescription = stringResource(MR.strings.action_search)
        val displayDescription = stringResource(MR.strings.action_display_mode)
        LaunchedEffect(state.toolbarQuery, viewModel.displayMode) {
            if (state.toolbarQuery != null) {
                HomeScreen.setCapsuleActions(emptyList())
            } else {
                HomeScreen.setCapsuleActions(
                    listOf(
                        CapsuleAction(
                            icon = MaterialSymbols.Rounded.Search,
                            contentDescription = searchDescription,
                            onClick = { viewModel.setToolbarQuery("") },
                        ),
                        CapsuleAction(
                            icon = when (viewModel.displayMode) {
                                LibraryDisplayMode.List -> MaterialSymbols.AutoMirroredRoundedFilled.ViewList
                                LibraryDisplayMode.ComfortableGrid -> MaterialSymbols.RoundedFilled.ViewModule
                                LibraryDisplayMode.CompactGrid,
                                LibraryDisplayMode.CoverOnlyGrid,
                                -> MaterialSymbols.RoundedFilled.ViewCompact
                            },
                            contentDescription = displayDescription,
                            onClick = { selectingDisplayMode = true },
                        ),
                    ),
                )
            }
        }

        Scaffold(
            // The top bar holds the source name and, below it, the
            // filter chips. Back goes through the system back gesture.
            // While searching, the search field takes the top bar's
            // place instead.
            topBar = {
                if (state.toolbarQuery != null) {
                    SearchToolbar(
                        searchQuery = state.toolbarQuery,
                        onChangeSearchQuery = viewModel::setToolbarQuery,
                        backgroundColor = MaterialTheme.colorScheme.background,
                        navigateUp = navigateUp,
                        onSearch = { viewModel.search(it) },
                    )
                } else {
                    Column(
                        modifier = Modifier
                            .background(MiuixTheme.colorScheme.surface)
                            .pointerInput(Unit) {},
                    ) {
                        // Source name at the top left, with clearance from
                        // the status bar. The filter chips sit below it.
                        Text(
                            text = source.name,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(
                                start = 16.dp,
                                top = 40.dp,
                                end = 16.dp,
                                bottom = 4.dp,
                            ),
                        )
                        Row(
                            modifier = Modifier
                                .horizontalScroll(rememberScrollState())
                                // Breathing room above and below the chips.
                                .padding(
                                    horizontal = MaterialTheme.padding.small,
                                    vertical = 8.dp,
                                ),
                            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.padding.small),
                        ) {
                            FilterButton(
                                selected = state.listing == Listing.Popular,
                                onClick = {
                                    viewModel.resetFilters()
                                    viewModel.setListing(Listing.Popular)
                                },
                                icon = MaterialSymbols.RoundedFilled.Favorite,
                                label = stringResource(MR.strings.popular),
                            )
                            if (source.supportsLatest) {
                                FilterButton(
                                    selected = state.listing == Listing.Latest,
                                    onClick = {
                                        viewModel.resetFilters()
                                        viewModel.setListing(Listing.Latest)
                                    },
                                    icon = MaterialSymbols.Rounded.Verified,
                                    label = stringResource(MR.strings.latest),
                                )
                            }
                            if (state.filters.isNotEmpty()) {
                                FilterButton(
                                    selected = state.listing is Listing.Search,
                                    onClick = viewModel::openFilterSheet,
                                    icon = MaterialSymbols.Rounded.FilterList,
                                    label = stringResource(MR.strings.action_filter),
                                )
                            }
                        }

                        HorizontalDivider()
                    }
                }
            },
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        ) { paddingValues ->
            BrowseSourceContent(
                source = source,
                mangaList = viewModel.mangaPagerFlowFlow.collectAsLazyPagingItems(),
                columns = viewModel.getColumnsPreference(LocalConfiguration.current.orientation),
                displayMode = viewModel.displayMode,
                snackbarHostState = snackbarHostState,
                contentPadding = paddingValues,
                onWebViewClick = onWebViewClick,
                onHelpClick = { uriHandler.openUri(Constants.URL_HELP) },
                onLocalSourceHelpClick = onHelpClick,
                onMangaClick = { navigator.push((MangaScreen(it.id, true))) },
                onMangaLongClick = { manga ->
                    scope.launch(Dispatchers.IO) {
                        val duplicates = viewModel.getDuplicateLibraryManga(manga)
                        when {
                            manga.favorite -> viewModel.setDialog(BrowseSourceViewModel.Dialog.RemoveManga(manga))
                            duplicates.isNotEmpty() -> viewModel.setDialog(
                                BrowseSourceViewModel.Dialog.AddDuplicateManga(manga, duplicates),
                            )
                            else -> viewModel.addFavorite(manga)
                        }
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    }
                },
            )
        }

        // Cover layout dropdown for the floating action capsule's
        // display button, mirroring the Library page's.
        OverlayDropdownPopup(
            entries = listOf(
                DropdownEntry(
                    items = listOf(
                        MR.strings.action_display_grid to LibraryDisplayMode.CompactGrid,
                        MR.strings.action_display_comfortable_grid to LibraryDisplayMode.ComfortableGrid,
                        MR.strings.action_display_cover_only_grid to LibraryDisplayMode.CoverOnlyGrid,
                        MR.strings.action_display_list to LibraryDisplayMode.List,
                    ).map { (titleRes, mode) ->
                        DropdownItem(
                            text = stringResource(titleRes),
                            selected = viewModel.displayMode == mode,
                            onClick = {
                                selectingDisplayMode = false
                                viewModel.displayMode = mode
                            },
                        )
                    },
                ),
            ),
            show = selectingDisplayMode,
            onDismiss = { selectingDisplayMode = false },
            onDismissFinished = {},
            maxHeight = null,
            dropdownColors = DropdownDefaults.dropdownColors(),
            renderInRootScaffold = true,
            collapseOnSelection = true,
        )

        val onDismissRequest = { viewModel.setDialog(null) }
        when (val dialog = state.dialog) {
            is BrowseSourceViewModel.Dialog.Filter -> {
                SourceFilterDialog(
                    onDismissRequest = onDismissRequest,
                    filters = state.filters,
                    onReset = viewModel::resetFilters,
                    onFilter = { viewModel.search(filters = state.filters) },
                    onUpdate = viewModel::setFilters,
                )
            }
            is BrowseSourceViewModel.Dialog.AddDuplicateManga -> {
                DuplicateMangaDialog(
                    duplicates = dialog.duplicates,
                    onDismissRequest = onDismissRequest,
                    onConfirm = { viewModel.addFavorite(dialog.manga) },
                    onOpenManga = { navigator.push(MangaScreen(it.id)) },
                    onMigrate = { viewModel.setDialog(BrowseSourceViewModel.Dialog.Migrate(dialog.manga, it)) },
                )
            }

            is BrowseSourceViewModel.Dialog.Migrate -> {
                MigrateMangaDialog(
                    current = dialog.current,
                    target = dialog.target,
                    // Initiated from the context of [dialog.target] so we show [dialog.current].
                    onClickTitle = { navigator.push(MangaScreen(dialog.current.id)) },
                    onDismissRequest = onDismissRequest,
                )
            }
            is BrowseSourceViewModel.Dialog.RemoveManga -> {
                RemoveMangaDialog(
                    onDismissRequest = onDismissRequest,
                    onConfirm = {
                        viewModel.changeMangaFavorite(dialog.manga)
                    },
                    mangaToRemove = dialog.manga,
                )
            }
            is BrowseSourceViewModel.Dialog.ChangeMangaCategory -> {
                ChangeCategoryDialog(
                    initialSelection = dialog.initialSelection,
                    onDismissRequest = onDismissRequest,
                    onEditCategories = { navigator.push(CategoryScreen()) },
                    onConfirm = { include, _ ->
                        viewModel.changeMangaFavorite(dialog.manga)
                        viewModel.moveMangaToCategories(dialog.manga, include)
                    },
                )
            }
            else -> {}
        }

        LaunchedEffect(Unit) {
            queryEvent.receiveAsFlow()
                .collectLatest {
                    when (it) {
                        is SearchType.Genre -> viewModel.searchGenre(it.txt)
                        is SearchType.Text -> viewModel.search(it.txt)
                    }
                }
        }
    }

    suspend fun search(query: String) = queryEvent.send(SearchType.Text(query))
    suspend fun searchGenre(name: String) = queryEvent.send(SearchType.Genre(name))

    companion object {
        private val queryEvent = Channel<SearchType>()
    }

    sealed class SearchType(val txt: String) {
        class Text(txt: String) : SearchType(txt)
        class Genre(txt: String) : SearchType(txt)
    }
}

// Same small Miuix button chip as the global search toolbar (Miuix has
// no chip; selected uses the primary button colors).
@Composable
private fun FilterButton(
    selected: Boolean,
    onClick: () -> Unit,
    icon: ImageVector,
    label: String,
) {
    Button(
        onClick = onClick,
        colors = if (selected) ButtonDefaults.buttonColorsPrimary() else ButtonDefaults.buttonColors(),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
        )
        Text(
            text = label,
            modifier = Modifier.padding(start = 4.dp),
        )
    }
}
