package eu.kanade.presentation.library.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import eu.kanade.core.preference.PreferenceMutableState
import eu.kanade.presentation.miuix.LiquidNavBarClearance
import eu.kanade.tachiyomi.ui.library.LibraryItem
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import tachiyomi.domain.category.model.Category
import tachiyomi.domain.library.model.LibraryDisplayMode
import tachiyomi.domain.library.model.LibraryManga
import tachiyomi.domain.library.model.LibrarySort
import tachiyomi.presentation.core.components.material.PullRefresh
import kotlin.time.Duration.Companion.seconds

@Composable
fun LibraryContent(
    categories: List<Category>,
    searchQuery: String?,
    selection: Set<Long>,
    contentPadding: PaddingValues,
    currentPage: Int,
    hasActiveFilters: Boolean,
    showPageTabs: Boolean,
    onChangeCurrentPage: (Int) -> Unit,
    onClickManga: (Long) -> Unit,
    onContinueReadingClicked: ((LibraryManga) -> Unit)?,
    onToggleSelection: (Category, LibraryManga) -> Unit,
    onToggleRangeSelection: (Category, LibraryManga) -> Unit,
    onRefresh: suspend () -> Boolean,
    onGlobalSearchClicked: () -> Unit,
    getItemCountForCategory: (Category) -> Int?,
    getDisplayMode: (Int) -> PreferenceMutableState<LibraryDisplayMode>,
    getColumnsForOrientation: (Boolean) -> PreferenceMutableState<Int>,
    getItemsForCategory: (Category) -> List<LibraryItem>,
    sort: LibrarySort,
    hasTrackers: Boolean,
    onSetSort: (LibrarySort.Type, LibrarySort.Direction) -> Unit,
    displayMode: LibraryDisplayMode,
    onDisplayModeSelected: (LibraryDisplayMode) -> Unit,
    onClickFilter: () -> Unit,
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

        // The view-option icons (sort, display mode, filter) sit at the
        // end of the category tab row, so they are always reachable
        // without opening the settings sheet — and clear of the centered
        // title capsule, which is why they left the top bar. When there
        // are no tabs the icons stand alone on the same row.
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (showPageTabs && categories.isNotEmpty() && (categories.size > 1 || !categories.first().isSystemCategory)) {
                LaunchedEffect(categories) {
                    if (categories.size <= pagerState.currentPage) {
                        pagerState.scrollToPage(categories.size - 1)
                    }
                }
                Box(modifier = Modifier.weight(1f)) {
                    LibraryTabs(
                        categories = categories,
                        pagerState = pagerState,
                        getItemCountForCategory = getItemCountForCategory,
                        onTabItemClick = {
                            scope.launch {
                                pagerState.animateScrollToPage(it)
                            }
                        },
                    )
                }
            } else {
                Spacer(modifier = Modifier.weight(1f))
            }
            LibraryViewOptions(
                sort = sort,
                hasTrackers = hasTrackers,
                onSetSort = onSetSort,
                displayMode = displayMode,
                onDisplayModeSelected = onDisplayModeSelected,
                hasActiveFilters = hasActiveFilters,
                onClickFilter = onClickFilter,
            )
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
                // The grid needs this much empty space below its last row
                // so the row clears the floating bar (HomeScreen no longer
                // insets the whole content for it).
                contentPadding = PaddingValues(
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
