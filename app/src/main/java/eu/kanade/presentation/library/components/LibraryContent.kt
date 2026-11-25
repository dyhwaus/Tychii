package eu.kanade.presentation.library.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.dp
import eu.kanade.core.preference.PreferenceMutableState
import eu.kanade.presentation.miuix.CapsuleAction
import eu.kanade.presentation.miuix.FilterChip
import eu.kanade.presentation.miuix.LiquidNavBarClearance
import eu.kanade.presentation.miuix.TopCapsuleClearance
import eu.kanade.presentation.theme.effective
import eu.kanade.tachiyomi.ui.home.HomeScreen
import eu.kanade.tachiyomi.ui.library.LibraryItem
import eu.kanade.tachiyomi.ui.library.LibraryRule
import eu.kanade.tachiyomi.ui.library.SmartLibraryCategory
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import mihon.app.di.appGraph
import mihon.icons.materialsymbols.MaterialSymbols
import mihon.icons.materialsymbols.automirroredrounded.Sort
import mihon.icons.materialsymbols.automirroredroundedfilled.ViewList
import mihon.icons.materialsymbols.rounded.Book
import mihon.icons.materialsymbols.rounded.FilterList
import mihon.icons.materialsymbols.rounded.Search
import mihon.icons.materialsymbols.roundedfilled.ViewCompact
import tachiyomi.domain.category.model.Category
import tachiyomi.domain.chapter.model.Chapter
import tachiyomi.domain.library.model.LibraryDisplayMode
import tachiyomi.domain.library.model.LibraryManga
import tachiyomi.domain.library.model.LibrarySort
import tachiyomi.domain.manga.model.Manga
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.material.PullRefresh
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.theme.active
import tachiyomi.presentation.core.util.collectAsState
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
    onGetChapters: suspend (Manga) -> List<Chapter>,
    onToggleSelection: (Category, LibraryManga) -> Unit,
    onToggleRangeSelection: (Category, LibraryManga) -> Unit,
    onSelectionAnchorChange: (Float) -> Unit,
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
    smartCategory: SmartLibraryCategory?,
    onSmartCategorySelected: (SmartLibraryCategory?) -> Unit,
    rules: List<LibraryRule>,
    smartRule: LibraryRule?,
    onRuleSelected: (LibraryRule?) -> Unit,
    onManageRules: () -> Unit,
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
        // Latest known cover positions, by manga id. A plain map, not
        // state: positions are only read when a cover is pressed.
        val coverPositions = remember { mutableMapOf<Long, Float>() }
        val context = LocalContext.current
        val barStyle by remember {
            context.appGraph.uiPreferences.barStyle
        }.collectAsState()
        val effectiveBarStyle = barStyle.effective()
        val chipScrollState = rememberScrollState()
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
                                LibraryDisplayMode.Shelf -> MaterialSymbols.Rounded.Book
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

        // The smart groups and the user's rules: one-tap views over
        // the library. Hidden while searching or selecting, like the
        // floating capsule. The row sits inside the
        // backdrop-recorded layer, so the chips get no backdrop: a
        // glass chip there would recurse. The trailing add chip opens
        // the rule manager; a rule chip filters, tapping it again
        // clears it.
        if (!searching && !selecting) {
            Row(
                modifier = Modifier
                    .horizontalScroll(chipScrollState)
                    // Breathing room above and below the chips.
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                SmartLibraryCategory.entries.forEach { category ->
                    FilterChip(
                        selected = category == smartCategory,
                        onClick = { onSmartCategorySelected(category) },
                        label = stringResource(category.titleRes),
                        barStyle = effectiveBarStyle,
                        backdrop = null,
                    )
                }
                rules.filter { it.enabled }.forEach { rule ->
                    FilterChip(
                        selected = rule == smartRule,
                        onClick = { onRuleSelected(rule) },
                        label = rule.name,
                        barStyle = effectiveBarStyle,
                        backdrop = null,
                    )
                }
                FilterChip(
                    selected = false,
                    onClick = onManageRules,
                    label = stringResource(MR.strings.label_library_rules),
                    barStyle = effectiveBarStyle,
                    backdrop = null,
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
                onCoverPositioned = { id, y -> coverPositions[id] = y },
                onClickManga = { category, manga ->
                    if (selection.isNotEmpty()) {
                        // The bottom bar animates out of the cover that
                        // was pressed, so remember its position before
                        // toggling (the covers already report it).
                        coverPositions[manga.manga.id]?.let(onSelectionAnchorChange)
                        onToggleSelection(category, manga)
                    } else {
                        onClickManga(manga.manga.id)
                    }
                },
                onLongClickManga = { category, manga ->
                    coverPositions[manga.manga.id]?.let(onSelectionAnchorChange)
                    onToggleRangeSelection(category, manga)
                },
                onClickContinueReading = onContinueReadingClicked,
                onGetChapters = onGetChapters,
            )
        }

        LaunchedEffect(pagerState.currentPage) {
            onChangeCurrentPage(pagerState.currentPage)
        }
    }
}
