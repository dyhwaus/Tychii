package eu.kanade.presentation.library.components

import android.content.res.Configuration
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import eu.kanade.core.preference.PreferenceMutableState
import eu.kanade.tachiyomi.ui.library.LibraryItem
import kotlinx.coroutines.launch
import tachiyomi.domain.category.model.Category
import tachiyomi.domain.chapter.model.Chapter
import tachiyomi.domain.library.model.LibraryDisplayMode
import tachiyomi.domain.library.model.LibraryManga
import tachiyomi.domain.library.model.LibrarySort
import tachiyomi.domain.library.model.sort
import tachiyomi.domain.manga.model.Manga
import tachiyomi.i18n.MR
import eu.kanade.presentation.components.QuietEmptyScreen
import tachiyomi.presentation.core.util.plus

// The index rail only earns its edge space on a list long enough to
// need jumping: below this, scrolling is faster than aiming at a letter.
private const val MIN_ITEMS_FOR_INDEX_RAIL = 30

@Composable
fun LibraryPager(
    state: PagerState,
    contentPadding: PaddingValues,
    hasActiveFilters: Boolean,
    selection: Set<Long>,
    searchQuery: String?,
    onGlobalSearchClicked: () -> Unit,
    getCategoryForPage: (Int) -> Category,
    getDisplayMode: (Int) -> PreferenceMutableState<LibraryDisplayMode>,
    getColumnsForOrientation: (Boolean) -> PreferenceMutableState<Int>,
    getItemsForCategory: (Category) -> List<LibraryItem>,
    onClickManga: (Category, LibraryManga) -> Unit,
    onLongClickManga: (Category, LibraryManga) -> Unit,
    onClickContinueReading: ((LibraryManga) -> Unit)?,
    onGetChapters: suspend (Manga) -> List<Chapter>,
    onCoverPositioned: (Long, Float) -> Unit,
) {
    HorizontalPager(
        modifier = Modifier.fillMaxSize(),
        state = state,
        verticalAlignment = Alignment.Top,
    ) { page ->
        if (page !in ((state.currentPage - 1)..(state.currentPage + 1))) {
            // To make sure only one offscreen page is being composed
            return@HorizontalPager
        }
        val category = getCategoryForPage(page)
        val items = getItemsForCategory(category)

        // One scroll state per list, hoisted so the index rail can jump it.
        // Each page composes separately, so these are per-category.
        val gridState = rememberLazyStaggeredGridState()
        val listState = rememberLazyListState()
        val scope = rememberCoroutineScope()

        if (items.isEmpty()) {
            LibraryPagerEmptyScreen(
                searchQuery = searchQuery,
                hasActiveFilters = hasActiveFilters,
                contentPadding = contentPadding,
                onGlobalSearchClicked = onGlobalSearchClicked,
            )
            return@HorizontalPager
        }

        // The fast index rail: only when this page's own sort is
        // alphabetical (the buckets must match the list order), the list
        // is not a search result, nothing is selected, and the category
        // is big enough to need jumping. Shelf mode has no scrollable list.
        val sections = remember(items) {
            libraryIndexSections(items.map { it.libraryManga.manga.title })
        }
        val showIndexRail = searchQuery == null &&
            selection.isEmpty() &&
            category.sort.type == LibrarySort.Type.Alphabetical &&
            items.size >= MIN_ITEMS_FOR_INDEX_RAIL &&
            sections.size >= 2

        val displayMode by getDisplayMode(page)
        val columns by if (displayMode != LibraryDisplayMode.List) {
            val configuration = LocalConfiguration.current
            val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

            remember(isLandscape) { getColumnsForOrientation(isLandscape) }
        } else {
            remember { mutableIntStateOf(0) }
        }

        val onClickManga: (LibraryManga) -> Unit = { onClickManga(category, it) }
        val onLongClickManga: (LibraryManga) -> Unit = { onLongClickManga(category, it) }

        when (displayMode) {
            LibraryDisplayMode.List -> {
                Box(modifier = Modifier.fillMaxSize()) {
                    LibraryList(
                        items = items,
                        contentPadding = contentPadding,
                        listState = listState,
                        selection = selection,
                        onClick = onClickManga,
                        onLongClick = onLongClickManga,
                        onClickContinueReading = onClickContinueReading,
                        searchQuery = searchQuery,
                        onGlobalSearchClicked = onGlobalSearchClicked,
                        onCoverPositioned = onCoverPositioned,
                    )
                    if (showIndexRail) {
                        LibraryIndexRail(
                            sections = sections,
                            onJumpToSection = { index ->
                                // The list always emits an (empty when not
                                // searching) global-search item first, so
                                // item i sits at list index i + 1.
                                scope.launch { listState.scrollToItem(index + 1) }
                            },
                        )
                    }
                }
            }
            LibraryDisplayMode.CompactGrid, LibraryDisplayMode.CoverOnlyGrid -> {
                Box(modifier = Modifier.fillMaxSize()) {
                    LibraryCompactGrid(
                        items = items,
                        showTitle = displayMode is LibraryDisplayMode.CompactGrid,
                        columns = columns,
                        contentPadding = contentPadding,
                        gridState = gridState,
                        selection = selection,
                        onClick = onClickManga,
                        onLongClick = onLongClickManga,
                        onClickContinueReading = onClickContinueReading,
                        searchQuery = searchQuery,
                        onGlobalSearchClicked = onGlobalSearchClicked,
                        onCoverPositioned = onCoverPositioned,
                    )
                    if (showIndexRail) {
                        LibraryIndexRail(
                            sections = sections,
                            onJumpToSection = { index ->
                                // The grid emits the global-search item only
                                // while searching, and the rail hides then, so
                                // item i sits at grid index i.
                                scope.launch { gridState.scrollToItem(index) }
                            },
                        )
                    }
                }
            }
            LibraryDisplayMode.Shelf -> {
                // Back to the carousel on top, but the grid below
                // is gone: the focused manga's detail panel lives
                // under it now.
                LibraryShelf(
                    items = items,
                    selection = selection,
                    onClick = onClickManga,
                    onLongClick = onLongClickManga,
                    onClickContinueReading = onClickContinueReading,
                    onGetChapters = onGetChapters,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(
                            top = contentPadding.calculateTopPadding(),
                            bottom = contentPadding.calculateBottomPadding(),
                        ),
                )
            }
        }
    }
}

@Composable
private fun LibraryPagerEmptyScreen(
    searchQuery: String?,
    hasActiveFilters: Boolean,
    contentPadding: PaddingValues,
    onGlobalSearchClicked: () -> Unit,
) {
    val msg = when {
        !searchQuery.isNullOrEmpty() -> MR.strings.no_results_found
        hasActiveFilters -> MR.strings.error_no_match
        else -> MR.strings.information_no_manga_category
    }

    Column(
        modifier = Modifier
            .padding(contentPadding + PaddingValues(8.dp))
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        if (!searchQuery.isNullOrEmpty()) {
            GlobalSearchItem(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.CenterHorizontally),
                searchQuery = searchQuery,
                onClick = onGlobalSearchClicked,
            )
        }

        QuietEmptyScreen(
            stringRes = msg,
            modifier = Modifier.weight(1f),
        )
    }
}
