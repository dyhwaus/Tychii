package eu.kanade.presentation.updates

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.util.fastAll
import androidx.compose.ui.util.fastAny
import eu.kanade.presentation.components.AppBar
import eu.kanade.presentation.components.AppBarActions
import eu.kanade.presentation.manga.components.ChapterDownloadAction
import eu.kanade.presentation.manga.components.MangaBottomActionMenu
import eu.kanade.presentation.miuix.CapsuleAction
import eu.kanade.presentation.miuix.LiquidNavBarClearance
import eu.kanade.presentation.util.isTabletUi
import eu.kanade.tachiyomi.data.download.model.Download
import eu.kanade.tachiyomi.ui.home.HomeScreen
import eu.kanade.tachiyomi.ui.updates.UpdatesItem
import eu.kanade.tachiyomi.ui.updates.UpdatesViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import mihon.icons.materialsymbols.MaterialSymbols
import mihon.icons.materialsymbols.rounded.CalendarMonth
import mihon.icons.materialsymbols.rounded.FilterList
import mihon.icons.materialsymbols.rounded.FlipToBack
import mihon.icons.materialsymbols.rounded.Refresh
import mihon.icons.materialsymbols.rounded.SelectAll
import tachiyomi.i18n.MR
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.ui.unit.dp
import tachiyomi.presentation.core.components.material.PullRefresh
import tachiyomi.presentation.core.components.material.Scaffold
import tachiyomi.presentation.core.i18n.stringResource
import eu.kanade.presentation.components.QuietEmptyScreen
import tachiyomi.presentation.core.screens.LoadingScreen
import tachiyomi.presentation.core.theme.active
import tachiyomi.presentation.core.util.plus
import kotlin.time.Duration.Companion.seconds

@Composable
fun UpdateScreen(
    state: UpdatesViewModel.State,
    snackbarHostState: SnackbarHostState,
    onClickCover: (UpdatesItem) -> Unit,
    onSelectAll: (Boolean) -> Unit,
    onInvertSelection: () -> Unit,
    onCalendarClicked: () -> Unit,
    onUpdateLibrary: suspend () -> Boolean,
    onDownloadChapter: (List<UpdatesItem>, ChapterDownloadAction) -> Unit,
    onMultiBookmarkClicked: (List<UpdatesItem>, bookmark: Boolean) -> Unit,
    onMultiMarkAsReadClicked: (List<UpdatesItem>, read: Boolean) -> Unit,
    onMultiDeleteClicked: (List<UpdatesItem>) -> Unit,
    onUpdateSelected: (UpdatesItem, Boolean, Boolean) -> Unit,
    onOpenChapter: (UpdatesItem) -> Unit,
    onFilterClicked: () -> Unit,
    hasActiveFilters: Boolean,
) {
    BackHandler(enabled = state.selectionMode) {
        onSelectAll(false)
    }

    val scope = rememberCoroutineScope()

    // The tab's actions for the floating capsule (HomeScreen): the
    // filter/upcoming/refresh icons live there now, not in the top bar.
    // Hidden while selecting (the capsule hides then, like the title
    // capsule). Resolved during composition (stringResource and the
    // theme colour are @Composable and can't run inside LaunchedEffect).
    val filterDescription = stringResource(MR.strings.action_filter)
    val upcomingDescription = stringResource(MR.strings.action_view_upcoming)
    val refreshDescription = stringResource(MR.strings.action_update_library)
    val activeColor = MaterialTheme.colorScheme.active
    LaunchedEffect(hasActiveFilters, state.selectionMode) {
        if (state.selectionMode) {
            HomeScreen.setCapsuleActions(emptyList())
        } else {
            HomeScreen.setCapsuleActions(
                listOf(
                    CapsuleAction(
                        icon = MaterialSymbols.Rounded.FilterList,
                        contentDescription = filterDescription,
                        onClick = onFilterClicked,
                        tint = if (hasActiveFilters) activeColor else null,
                    ),
                    CapsuleAction(
                        icon = MaterialSymbols.Rounded.CalendarMonth,
                        contentDescription = upcomingDescription,
                        onClick = onCalendarClicked,
                    ),
                    CapsuleAction(
                        icon = MaterialSymbols.Rounded.Refresh,
                        contentDescription = refreshDescription,
                        onClick = { scope.launch { onUpdateLibrary() } },
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
            UpdatesAppBar(
                onCalendarClicked = { onCalendarClicked() },
                onUpdateLibrary = { scope.launch { onUpdateLibrary() } },
                onFilterClicked = { onFilterClicked() },
                hasFilters = hasActiveFilters,
                actionModeCounter = state.selected.size,
                onSelectAll = { onSelectAll(true) },
                onInvertSelection = { onInvertSelection() },
                onCancelActionMode = { onSelectAll(false) },
            )
        },
        bottomBar = {
            UpdatesBottomBar(
                selected = state.selected,
                onDownloadChapter = onDownloadChapter,
                onMultiBookmarkClicked = onMultiBookmarkClicked,
                onMultiMarkAsReadClicked = onMultiMarkAsReadClicked,
                onMultiDeleteClicked = onMultiDeleteClicked,
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
    ) { contentPadding ->
        when {
            state.isLoading -> LoadingScreen(Modifier.padding(contentPadding))
            else -> {
                var isRefreshing by remember { mutableStateOf(false) }

                PullRefresh(
                    refreshing = isRefreshing,
                    onRefresh = {
                        scope.launch {
                            if (!onUpdateLibrary()) return@launch
                            // Fake refresh status but hide it after a second as it's a long running task
                            isRefreshing = true
                            delay(1.seconds)
                            isRefreshing = false
                        }
                    },
                    enabled = !state.selectionMode,
                    indicatorPadding = contentPadding,
                ) {
                    if (state.items.isEmpty()) {
                        Column(
                            modifier = Modifier
                                .padding(contentPadding)
                                .fillMaxSize(),
                        ) {
                            QuietEmptyScreen(
                                stringRes = if (hasActiveFilters) {
                                    MR.strings.error_no_match
                                } else {
                                    MR.strings.information_no_recent
                                },
                                modifier = Modifier.weight(1f),
                            )
                        }
                    } else {
                        // Waterfall grid: update cards flow into two uneven
                        // columns (see LazyLibraryGrid). No fast-scroll
                        // thumb.
                        LazyVerticalStaggeredGrid(
                            columns = StaggeredGridCells.Fixed(2),
                            // Empty space below the last item so it clears
                            // the floating bar.
                            contentPadding = contentPadding + PaddingValues(bottom = LiquidNavBarClearance) + PaddingValues(8.dp),
                            verticalItemSpacing = 8.dp,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            updatesUiItems(
                                uiModels = state.getUiModel(),
                                selectionMode = state.selectionMode,
                                onUpdateSelected = onUpdateSelected,
                                onClickCover = onClickCover,
                                onClickUpdate = onOpenChapter,
                                onDownloadChapter = onDownloadChapter,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun UpdatesAppBar(
    onCalendarClicked: () -> Unit,
    onUpdateLibrary: () -> Unit,
    onFilterClicked: () -> Unit,
    hasFilters: Boolean,
    // For action mode
    actionModeCounter: Int,
    onSelectAll: () -> Unit,
    onInvertSelection: () -> Unit,
    onCancelActionMode: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AppBar(
        modifier = modifier,
        // Same as the Library page: the bar sits on the page background,
        // not Miuix surface (pure black in dark mode). Action mode keeps
        // AppBar's own tinted color, so null is passed then.
        backgroundColor = if (actionModeCounter > 0) null else MaterialTheme.colorScheme.background,
        // Phones show the page title in the centered capsule floating over
        // the toolbar (HomeScreen); tablets keep the title here (no capsule).
        title = if (isTabletUi()) stringResource(MR.strings.label_recent_updates) else null,
        actions = {
            // Phones: the centered title capsule floats in this same
            // 64.dp band, and the action icons sat under it. Hidden on
            // phones so the band holds only the capsule and the search
            // icon; tablets keep them (no capsule there). Action-mode
            // actions below are untouched — the capsule hides then.
            if (isTabletUi()) {
                AppBarActions(
                    listOf(
                        AppBar.Action(
                            title = stringResource(MR.strings.action_filter),
                            icon = MaterialSymbols.Rounded.FilterList,
                            iconTint = if (hasFilters) MaterialTheme.colorScheme.active else LocalContentColor.current,
                            onClick = onFilterClicked,
                        ),
                        AppBar.Action(
                            title = stringResource(MR.strings.action_view_upcoming),
                            icon = MaterialSymbols.Rounded.CalendarMonth,
                            onClick = onCalendarClicked,
                        ),
                        AppBar.Action(
                            title = stringResource(MR.strings.action_update_library),
                            icon = MaterialSymbols.Rounded.Refresh,
                            onClick = onUpdateLibrary,
                        ),
                    ),
                )
            }
        },
        actionModeCounter = actionModeCounter,
        onCancelActionMode = onCancelActionMode,
        actionModeActions = {
            AppBarActions(
                listOf(
                    AppBar.Action(
                        title = stringResource(MR.strings.action_select_all),
                        icon = MaterialSymbols.Rounded.SelectAll,
                        onClick = onSelectAll,
                    ),
                    AppBar.Action(
                        title = stringResource(MR.strings.action_select_inverse),
                        icon = MaterialSymbols.Rounded.FlipToBack,
                        onClick = onInvertSelection,
                    ),
                ),
            )
        },
        // No scroll behavior (see the Scaffold above): the toolbar must
        // never tint while scrolling.
    )
}

@Composable
private fun UpdatesBottomBar(
    selected: List<UpdatesItem>,
    onDownloadChapter: (List<UpdatesItem>, ChapterDownloadAction) -> Unit,
    onMultiBookmarkClicked: (List<UpdatesItem>, bookmark: Boolean) -> Unit,
    onMultiMarkAsReadClicked: (List<UpdatesItem>, read: Boolean) -> Unit,
    onMultiDeleteClicked: (List<UpdatesItem>) -> Unit,
) {
    MangaBottomActionMenu(
        visible = selected.isNotEmpty(),
        modifier = Modifier.fillMaxWidth(),
        onBookmarkClicked = {
            onMultiBookmarkClicked.invoke(selected, true)
        }.takeIf { selected.fastAny { !it.update.bookmark } },
        onRemoveBookmarkClicked = {
            onMultiBookmarkClicked.invoke(selected, false)
        }.takeIf { selected.fastAll { it.update.bookmark } },
        onMarkAsReadClicked = {
            onMultiMarkAsReadClicked(selected, true)
        }.takeIf { selected.fastAny { !it.update.read } },
        onMarkAsUnreadClicked = {
            onMultiMarkAsReadClicked(selected, false)
        }.takeIf { selected.fastAny { it.update.read || it.update.lastPageRead > 0 } },
        onDownloadClicked = {
            onDownloadChapter(selected, ChapterDownloadAction.START)
        }.takeIf {
            selected.fastAny { it.downloadStateProvider() != Download.State.DOWNLOADED }
        },
        onDeleteClicked = {
            onMultiDeleteClicked(selected)
        }.takeIf { selected.fastAny { it.downloadStateProvider() == Download.State.DOWNLOADED } },
    )
}

sealed interface UpdatesUiModel {
    data class Header(val date: LocalDate) : UpdatesUiModel
    data class Item(val item: UpdatesItem) : UpdatesUiModel
}
