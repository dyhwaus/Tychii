package eu.kanade.presentation.library.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import eu.kanade.core.preference.PreferenceMutableState
import eu.kanade.presentation.miuix.CapsuleAction
import eu.kanade.presentation.miuix.LiquidNavBarClearance
import eu.kanade.presentation.miuix.TopCapsuleClearance
import eu.kanade.tachiyomi.ui.home.HomeScreen
import eu.kanade.tachiyomi.ui.library.LibraryItem
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import mihon.icons.materialsymbols.MaterialSymbols
import mihon.icons.materialsymbols.automirroredrounded.Sort
import mihon.icons.materialsymbols.automirroredroundedfilled.ViewList
import mihon.icons.materialsymbols.rounded.FilterList
import mihon.icons.materialsymbols.rounded.Search
import mihon.icons.materialsymbols.roundedfilled.ViewCompact
import mihon.icons.materialsymbols.roundedfilled.ViewModule
import tachiyomi.domain.category.model.Category
import tachiyomi.domain.library.model.LibraryDisplayMode
import tachiyomi.domain.library.model.LibraryManga
import tachiyomi.domain.library.model.LibrarySort
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.material.PullRefresh
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.theme.active
import kotlin.time.Duration.Companion.seconds

@Composable
fun LibraryContent(
    categories: List<Category>,
    searchQuery: String?,
    selection: Set<Long>,
    contentPadding: PaddingValues,
    currentPage: Int,
    hasActiveFilters: Boolean,
    onChangeCurrentPage: (Int) -> Unit,
    onClickManga: (Long) -> Unit,
    onContinueReadingClicked: ((LibraryManga) -> Unit)?,
    onToggleSelection: (Category, LibraryManga) -> Unit,
    onToggleRangeSelection: (Category, LibraryManga) -> Unit,
    onRefresh: suspend () -> Boolean,
    onGlobalSearchClicked: () -> Unit,
    getDisplayMode: (Int) -> PreferenceMutableState<LibraryDisplayMode>,
    getColumnsForOrientation: (Boolean) -> PreferenceMutableState<Int>,
    getItemsForCategory: (Category) -> List<LibraryItem>,
    sort: LibrarySort,
    hasTrackers: Boolean,
    onSetSort: (LibrarySort.Type, LibrarySort.Direction) -> Unit,
    displayMode: LibraryDisplayMode,
    onDisplayModeSelected: (LibraryDisplayMode) -> Unit,
    filterOptions: List<LibraryFilterOption>,
    onClickSearch: () -> Unit,
) {
    Column(
        modifier = Modifier.padding(
            top = contentPadding.calculateTopPadding(),
            start = contentPadding.calculateStartPadding(LocalLayoutDirection.current),
            end = contentPadding.calculateEndPadding(LocalLayoutDirection.current),
        ),
    ) {
        val pagerState = rememberPagerState(currentPage) { categories.size }

        val scope = rememberCoroutineScope()
        var isRefreshing by remember(pagerState.currentPage) { mutableStateOf(false) }

        // The sort/display/filter dropdowns are opened from the
        // floating capsule (HomeScreen); their buttons live there, the
        // popups render here from the root scaffold.
        var selectingSort by remember { mutableStateOf(false) }
        var selectingDisplayMode by remember { mutableStateOf(false) }
        var selectingFilter by remember { mutableStateOf(false) }
        LibraryViewOptionsDropdowns(
            sort = sort,
            hasTrackers = hasTrackers,
            onSetSort = onSetSort,
            displayMode = displayMode,
            onDisplayModeSelected = onDisplayModeSelected,
            selectingSort = selectingSort,
            onSelectingSortChange = { selectingSort = it },
            selectingDisplayMode = selectingDisplayMode,
            onSelectingDisplayModeChange = { selectingDisplayMode = it },
        )
        LibraryFilterDropdown(
            options = filterOptions,
            selectingFilter = selectingFilter,
            onSelectingFilterChange = { selectingFilter = it },
        )

        // The tab's actions for the floating capsule (HomeScreen): the
        // search/sort/display/filter icons live there now, not in the
        // tab row or the top bar. Hidden while searching or selecting
        // (the capsule hides then, like the title capsule).
        val searching = searchQuery != null
        val selecting = selection.isNotEmpty()
        // Resolved during composition (stringResource and the theme
        // colour are @Composable and can't run inside LaunchedEffect).
        val searchDescription = stringResource(MR.strings.action_search)
        val sortDescription = stringResource(MR.strings.action_sort)
        val displayDescription = stringResource(MR.strings.action_display_mode)
        val filterDescription = stringResource(MR.strings.action_filter)
        val activeColor = MaterialTheme.colorScheme.active
        LaunchedEffect(displayMode, hasActiveFilters, searching, selecting) {
            if (searching || selecting) {
                HomeScreen.setCapsuleActions(emptyList())
            } else {
                HomeScreen.setCapsuleActions(
                    listOf(
                        // The three dropdowns share their open state:
                        // opening one closes the others, and opening
                        // search closes all three. Without this, tapping
                        // a second capsule action left the first dropdown
                        // open underneath it.
                        CapsuleAction(
                            icon = MaterialSymbols.Rounded.Search,
                            contentDescription = searchDescription,
                            onClick = {
                                selectingSort = false
                                selectingDisplayMode = false
                                selectingFilter = false
                                onClickSearch()
                            },
                        ),
                        CapsuleAction(
                            icon = MaterialSymbols.AutoMirroredRounded.Sort,
                            contentDescription = sortDescription,
                            onClick = {
                                selectingSort = true
                                selectingDisplayMode = false
                                selectingFilter = false
                            },
                        ),
                        CapsuleAction(
                            icon = when (displayMode) {
                                LibraryDisplayMode.List -> MaterialSymbols.AutoMirroredRoundedFilled.ViewList
                                LibraryDisplayMode.ComfortableGrid -> MaterialSymbols.RoundedFilled.ViewModule
                                LibraryDisplayMode.CompactGrid,
                                LibraryDisplayMode.CoverOnlyGrid,
                                -> MaterialSymbols.RoundedFilled.ViewCompact
                            },
                            contentDescription = displayDescription,
                            onClick = {
                                selectingDisplayMode = true
                                selectingSort = false
                                selectingFilter = false
                            },
                        ),
                        CapsuleAction(
                            icon = MaterialSymbols.Rounded.FilterList,
                            contentDescription = filterDescription,
                            onClick = {
                                selectingFilter = true
                                selectingSort = false
                                selectingDisplayMode = false
                            },
                            tint = if (hasActiveFilters) activeColor else null,
                        ),
                    ),
                )
            }
        }

        PullRefresh(
            refreshing = isRefreshing,
            enabled = selection.isEmpty(),
            onRefresh = {
                scope.launch {
                    if (!onRefresh()) return@launch
                    // Fake refresh status but hide it after a second as it's a long running task
                    isRefreshing = true
                    delay(1.seconds)
                    isRefreshing = false
                }
            },
        ) {
            LibraryPager(
                state = pagerState,
                // Empty space below the last row so it clears the
                // floating bar (HomeScreen no longer insets the whole
                // content for it). Only the bottom: the Column above
                // already applied the Scaffold's top/start/end padding
                // (the 64.dp toolbar band), so adding it here again
                // counted the band twice and pushed the first row much
                // lower than the other pages' first item. The top gap
                // is TopCapsuleClearance: plain padding (not a toolbar
                // band) so the first row clears the floating capsule
                // and, when scrolling, the covers move up through it
                // and stay visible.
                contentPadding = PaddingValues(
                    top = TopCapsuleClearance,
                    bottom = contentPadding.calculateBottomPadding() + LiquidNavBarClearance,
                ),
                hasActiveFilters = hasActiveFilters,
                selection = selection,
                searchQuery = searchQuery,
                onGlobalSearchClicked = onGlobalSearchClicked,
                getCategoryForPage = { page -> categories[page] },
                getDisplayMode = getDisplayMode,
                getColumnsForOrientation = getColumnsForOrientation,
                getItemsForCategory = getItemsForCategory,
                onClickManga = { category, manga ->
                    if (selection.isNotEmpty()) {
                        onToggleSelection(category, manga)
                    } else {
                        onClickManga(manga.manga.id)
                    }
                },
                onLongClickManga = onToggleRangeSelection,
                onClickContinueReading = onContinueReadingClicked,
            )
        }

        LaunchedEffect(pagerState.currentPage) {
            onChangeCurrentPage(pagerState.currentPage)
        }
    }
}
