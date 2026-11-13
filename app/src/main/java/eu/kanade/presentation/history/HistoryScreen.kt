package eu.kanade.presentation.history

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.tooling.preview.PreviewParameter
import eu.kanade.domain.ui.UiPreferences
import eu.kanade.presentation.components.AppBar
import eu.kanade.presentation.components.AppBarActions
import eu.kanade.presentation.components.AppBarTitle
import eu.kanade.presentation.components.SearchToolbar
import eu.kanade.presentation.components.relativeDateText
import eu.kanade.presentation.history.components.HistoryItem
import eu.kanade.presentation.miuix.CapsuleAction
import eu.kanade.presentation.miuix.LiquidNavBarClearance
import eu.kanade.presentation.theme.TachiyomiPreviewTheme
import eu.kanade.presentation.util.isTabletUi
import eu.kanade.presentation.util.rememberIs24HourSystem
import eu.kanade.tachiyomi.ui.history.HistoryViewModel
import eu.kanade.tachiyomi.ui.home.HomeScreen
import kotlinx.datetime.LocalDate
import mihon.app.di.appGraph
import mihon.icons.materialsymbols.MaterialSymbols
import mihon.icons.materialsymbols.rounded.DeleteSweep
import mihon.icons.materialsymbols.rounded.Search
import tachiyomi.domain.history.model.HistoryWithRelations
import tachiyomi.i18n.MR
import androidx.compose.foundation.lazy.LazyColumn
import tachiyomi.presentation.core.components.ListGroupHeader
import tachiyomi.presentation.core.components.material.Scaffold
import tachiyomi.presentation.core.i18n.stringResource
import eu.kanade.presentation.components.QuietEmptyScreen
import tachiyomi.presentation.core.screens.LoadingScreen
import tachiyomi.presentation.core.util.plus
import tachiyomi.presentation.core.util.collectAsState

@Composable
fun HistoryScreen(
    state: HistoryViewModel.State,
    snackbarHostState: SnackbarHostState,
    onSearchQueryChange: (String?) -> Unit,
    onClickCover: (mangaId: Long) -> Unit,
    onClickResume: (mangaId: Long, chapterId: Long) -> Unit,
    onClickFavorite: (mangaId: Long) -> Unit,
    onDialogChange: (HistoryViewModel.Dialog?) -> Unit,
) {
    // The tab's actions for the floating capsule (HomeScreen): the
    // search and clear-history icons live there now, not in the top
    // bar. Hidden while searching (the capsule hides then, like the
    // title capsule).
    val searchDescription = stringResource(MR.strings.action_search)
    val clearDescription = stringResource(MR.strings.pref_clear_history)
    LaunchedEffect(state.searchQuery) {
        if (state.searchQuery != null) {
            HomeScreen.setCapsuleActions(emptyList())
        } else {
            HomeScreen.setCapsuleActions(
                listOf(
                    CapsuleAction(
                        icon = MaterialSymbols.Rounded.Search,
                        contentDescription = searchDescription,
                        onClick = { onSearchQueryChange("") },
                    ),
                    CapsuleAction(
                        icon = MaterialSymbols.Rounded.DeleteSweep,
                        contentDescription = clearDescription,
                        onClick = { onDialogChange(HistoryViewModel.Dialog.DeleteAll) },
                    ),
                ),
            )
        }
    }

    Scaffold(
        // Same as the Library page: HomeScreen's outer Scaffold already
        // applies the system-bar insets, so this inner Scaffold must not
        // re-apply them (dead strips top and bottom). And no scroll
        // behavior on the toolbar: a pinned scroll behavior would tint
        // the toolbar (surfaceTint overlay) as soon as the list scrolls.
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = { _ ->
            SearchToolbar(
                // The bar sits on the page background: AppBar's Miuix-surface
                // default is pure black in dark mode and showed as a band.
                backgroundColor = MaterialTheme.colorScheme.background,
                titleContent = { if (isTabletUi()) AppBarTitle(stringResource(MR.strings.history)) },
                searchQuery = state.searchQuery,
                onChangeSearchQuery = onSearchQueryChange,
                // Phones: the floating capsule provides the search icon,
                // so the toolbar's magnifier is hidden there; tablets
                // keep it (no capsule there). The reset button while
                // searching still shows.
                showSearchIcon = isTabletUi(),
                actions = {
                    // Phones: the centered title capsule floats in this
                    // same 64.dp band, and the action icon sat under it.
                    // Hidden on phones so the band holds only the capsule;
                    // tablets keep it (no capsule there).
                    if (isTabletUi()) {
                        AppBarActions(
                            listOf(
                                AppBar.Action(
                                    title = stringResource(MR.strings.pref_clear_history),
                                    icon = MaterialSymbols.Rounded.DeleteSweep,
                                    onClick = {
                                        onDialogChange(HistoryViewModel.Dialog.DeleteAll)
                                    },
                                ),
                            ),
                        )
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
    ) { contentPadding ->
        state.list.let {
            if (it == null) {
                LoadingScreen(Modifier.padding(contentPadding))
            } else if (it.isEmpty()) {
                val msg = if (!state.searchQuery.isNullOrEmpty()) {
                    MR.strings.no_results_found
                } else {
                    MR.strings.information_no_recent_manga
                }
                QuietEmptyScreen(
                    stringRes = msg,
                    modifier = Modifier.padding(contentPadding),
                )
            } else {
                HistoryScreenContent(
                    history = it,
                    contentPadding = contentPadding,
                    onClickCover = { history -> onClickCover(history.mangaId) },
                    onClickResume = { history -> onClickResume(history.mangaId, history.chapterId) },
                    onClickDelete = { item -> onDialogChange(HistoryViewModel.Dialog.Delete(item)) },
                    onClickFavorite = { history -> onClickFavorite(history.mangaId) },
                )
            }
        }
    }
}

@Composable
private fun HistoryScreenContent(
    history: List<HistoryUiModel>,
    contentPadding: PaddingValues,
    onClickCover: (HistoryWithRelations) -> Unit,
    onClickResume: (HistoryWithRelations) -> Unit,
    onClickDelete: (HistoryWithRelations) -> Unit,
    onClickFavorite: (HistoryWithRelations) -> Unit,
) {
    val context = LocalContext.current
    val locale = LocalLocale.current
    val is24HourSystem = rememberIs24HourSystem()
    val timeFormat by remember { context.appGraph.uiPreferences.timeFormat }.collectAsState()
    val timeFormatter = remember(timeFormat, locale, is24HourSystem) {
        UiPreferences.timeFormat(timeFormat, locale.platformLocale, is24HourSystem)
    }

    // Plain list, no fast-scroll thumb (see LazyLibraryGrid).
    LazyColumn(
        // Empty space below the last item so it clears the floating bar.
        contentPadding = contentPadding + PaddingValues(bottom = LiquidNavBarClearance),
    ) {
        items(
            items = history,
            key = { "history-${it.hashCode()}" },
            contentType = {
                when (it) {
                    is HistoryUiModel.Header -> "header"
                    is HistoryUiModel.Item -> "item"
                }
            },
        ) { item ->
            when (item) {
                is HistoryUiModel.Header -> {
                    ListGroupHeader(
                        modifier = Modifier.animateItem(),
                        text = relativeDateText(item.date),
                    )
                }
                is HistoryUiModel.Item -> {
                    val value = item.item
                    HistoryItem(
                        modifier = Modifier.animateItem(),
                        history = value,
                        timeFormatter = timeFormatter,
                        onClickCover = { onClickCover(value) },
                        onClickResume = { onClickResume(value) },
                        onClickDelete = { onClickDelete(value) },
                        onClickFavorite = { onClickFavorite(value) },
                    )
                }
            }
        }
    }
}

sealed interface HistoryUiModel {
    data class Header(val date: LocalDate) : HistoryUiModel
    data class Item(val item: HistoryWithRelations) : HistoryUiModel
}

@PreviewLightDark
@Composable
internal fun HistoryScreenPreviews(
    @PreviewParameter(HistoryviewModelStateProvider::class)
    historyState: HistoryViewModel.State,
) {
    TachiyomiPreviewTheme {
        HistoryScreen(
            state = historyState,
            snackbarHostState = SnackbarHostState(),
            onSearchQueryChange = {},
            onClickCover = {},
            onClickResume = { _, _ -> run {} },
            onDialogChange = {},
            onClickFavorite = {},
        )
    }
}
