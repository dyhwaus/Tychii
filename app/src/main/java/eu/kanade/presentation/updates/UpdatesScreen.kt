package eu.kanade.presentation.updates

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.util.fastAll
import androidx.compose.ui.util.fastAny
import eu.kanade.presentation.manga.components.ChapterDownloadAction
import eu.kanade.presentation.manga.components.MangaBottomActionMenu
import eu.kanade.presentation.miuix.CapsuleAction
import eu.kanade.presentation.miuix.FloatMotion
import eu.kanade.presentation.miuix.LiquidNavBarClearance
import eu.kanade.presentation.miuix.TopCapsuleClearance
import eu.kanade.presentation.miuix.TychiiCorners
import eu.kanade.tachiyomi.data.download.model.Download
import eu.kanade.tachiyomi.ui.home.HomeScreen
import eu.kanade.tachiyomi.ui.updates.UpdatesItem
import eu.kanade.tachiyomi.ui.updates.UpdatesViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import mihon.icons.materialsymbols.MaterialSymbols
import mihon.icons.materialsymbols.rounded.CalendarMonth
import mihon.icons.materialsymbols.rounded.Close
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
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.Text as MiuixText
import top.yukonga.miuix.kmp.theme.MiuixTheme
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
    filterOptions: List<UpdatesFilterOption>,
    hasActiveFilters: Boolean,
) {
    BackHandler(enabled = state.selectionMode) {
        onSelectAll(false)
    }

    val scope = rememberCoroutineScope()

    // The filter dropdown expands from the floating capsule, like the
    // Library page's dropdowns, instead of sliding up from below.
    var selectingFilter by remember { mutableStateOf(false) }
    UpdatesFilterDropdown(
        options = filterOptions,
        selectingFilter = selectingFilter,
        onSelectingFilterChange = { selectingFilter = it },
    )

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
                        // Toggles: tapping the open filter closes it
                        // again. The capsule sits above the dropdown
                        // layer, so the re-tap reaches the button.
                        onClick = { selectingFilter = !selectingFilter },
                        tint = if (hasActiveFilters) activeColor else null,
                    ),
                    CapsuleAction(
                        icon = MaterialSymbols.Rounded.CalendarMonth,
                        contentDescription = upcomingDescription,
                        onClick = {
                            selectingFilter = false
                            onCalendarClicked()
                        },
                    ),
                    CapsuleAction(
                        icon = MaterialSymbols.Rounded.Refresh,
                        contentDescription = refreshDescription,
                        onClick = {
                            selectingFilter = false
                            scope.launch { onUpdateLibrary() }
                        },
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
        // No top bar at rest: the toolbar is an original Mihon
        // container whose band sits above the covers and cuts them
        // off. It only appears in selection mode, when its action
        // mode is actually needed. The title lives in the floating
        // capsule and the actions in the floating action capsule.
        topBar = { _ ->
            if (state.selected.isNotEmpty()) {
                UpdatesAppBar(
                    actionModeCounter = state.selected.size,
                    onSelectAll = { onSelectAll(true) },
                    onInvertSelection = { onInvertSelection() },
                    onCancelActionMode = { onSelectAll(false) },
                )
            }
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
                            // the floating bar, and above the first item so
                            // it clears the floating capsule. The top gap
                            // is plain padding, not a toolbar band, so when
                            // scrolling the covers move up through it and
                            // stay visible.
                            contentPadding = contentPadding + PaddingValues(top = TopCapsuleClearance, bottom = LiquidNavBarClearance) + PaddingValues(8.dp),
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
    // For action mode
    actionModeCounter: Int,
    onSelectAll: () -> Unit,
    onInvertSelection: () -> Unit,
    onCancelActionMode: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // A floating pill like the Library selection bar, not the
    // full-width Material band. The bar only ever renders in
    // selection mode — the top bar is empty at rest — so there is no
    // regular branch here; filter/upcoming/refresh live in the
    // floating action capsule instead.
    val backgroundColor = MiuixTheme.colorScheme.surfaceContainer
        .copy(alpha = if (isSystemInDarkTheme()) 0.9f else 0.95f)
    AnimatedVisibility(
        visible = true,
        // The pill mounts when selection starts and unmounts when it
        // ends, so only the enter plays; the exit never gets the chance.
        enter = slideInVertically(FloatMotion.SlideIn) { -it } + fadeIn(FloatMotion.FadeIn),
        exit = fadeOut(FloatMotion.FadeOut),
    ) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(top = 8.dp, start = 12.dp, end = 12.dp)
            .clip(RoundedCornerShape(TychiiCorners.Pill))
            .background(backgroundColor)
            .height(64.dp)
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onCancelActionMode) {
            Icon(
                imageVector = MaterialSymbols.Rounded.Close,
                contentDescription = stringResource(MR.strings.action_cancel),
            )
        }
        Box(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 8.dp),
        ) {
            MiuixText(
                text = "$actionModeCounter",
                style = MiuixTheme.textStyles.title2,
            )
        }
        IconButton(onClick = onSelectAll) {
            Icon(
                imageVector = MaterialSymbols.Rounded.SelectAll,
                contentDescription = stringResource(MR.strings.action_select_all),
            )
        }
        IconButton(onClick = onInvertSelection) {
            Icon(
                imageVector = MaterialSymbols.Rounded.FlipToBack,
                contentDescription = stringResource(MR.strings.action_select_inverse),
            )
        }
    }
    }
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
