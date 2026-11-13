package eu.kanade.presentation.library.components

import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import dev.icerock.moko.resources.StringResource
import mihon.icons.materialsymbols.MaterialSymbols
import mihon.icons.materialsymbols.rounded.ArrowDownward
import mihon.icons.materialsymbols.rounded.ArrowUpward
import tachiyomi.domain.library.model.LibraryDisplayMode
import tachiyomi.domain.library.model.LibrarySort
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource
import top.yukonga.miuix.kmp.basic.DropdownDefaults
import top.yukonga.miuix.kmp.basic.DropdownEntry
import top.yukonga.miuix.kmp.basic.DropdownItem
import top.yukonga.miuix.kmp.popup.OverlayDropdownPopup

// The Library page's sort and display-mode dropdowns. Their buttons
// live in the floating capsule (HomeScreen); this only renders the
// dropdowns, which pop up from the root scaffold. (The filter button
// opens the settings sheet, so it needs no dropdown.)
@Composable
fun LibraryViewOptionsDropdowns(
    sort: LibrarySort,
    hasTrackers: Boolean,
    onSetSort: (LibrarySort.Type, LibrarySort.Direction) -> Unit,
    displayMode: LibraryDisplayMode,
    onDisplayModeSelected: (LibraryDisplayMode) -> Unit,
    selectingSort: Boolean,
    onSelectingSortChange: (Boolean) -> Unit,
    selectingDisplayMode: Boolean,
    onSelectingDisplayModeChange: (Boolean) -> Unit,
) {
    OverlayDropdownPopup(
        entries = listOf(
            DropdownEntry(
                items = sortOptions(hasTrackers).map { (titleRes, mode) ->
                    DropdownItem(
                        text = stringResource(titleRes),
                        selected = sort.type == mode,
                        // The direction rides on the selected row,
                        // like the arrow on the settings sheet's sort item.
                        icon = if (sort.type == mode && mode != LibrarySort.Type.Random) {
                            { modifier ->
                                Icon(
                                    imageVector = if (sort.isAscending) {
                                        MaterialSymbols.Rounded.ArrowUpward
                                    } else {
                                        MaterialSymbols.Rounded.ArrowDownward
                                    },
                                    contentDescription = null,
                                    modifier = modifier,
                                )
                            }
                        } else {
                            null
                        },
                        onClick = {
                            onSelectingSortChange(false)
                            if (mode == LibrarySort.Type.Random) {
                                onSetSort(mode, LibrarySort.Direction.Ascending)
                            } else {
                                // Same rule as the settings sheet:
                                // tapping the active type flips the
                                // direction, any other type keeps it.
                                val sortDescending = !sort.isAscending
                                val direction = if (sort.type == mode) {
                                    if (sortDescending) {
                                        LibrarySort.Direction.Ascending
                                    } else {
                                        LibrarySort.Direction.Descending
                                    }
                                } else {
                                    if (sortDescending) {
                                        LibrarySort.Direction.Descending
                                    } else {
                                        LibrarySort.Direction.Ascending
                                    }
                                }
                                onSetSort(mode, direction)
                            }
                        },
                    )
                },
            ),
        ),
        show = selectingSort,
        onDismiss = { onSelectingSortChange(false) },
        onDismissFinished = {},
        maxHeight = null,
        dropdownColors = DropdownDefaults.dropdownColors(),
        renderInRootScaffold = true,
        collapseOnSelection = true,
    )
    OverlayDropdownPopup(
        entries = listOf(
            DropdownEntry(
                items = displayModes.map { (titleRes, mode) ->
                    DropdownItem(
                        text = stringResource(titleRes),
                        selected = displayMode == mode,
                        onClick = {
                            onSelectingDisplayModeChange(false)
                            onDisplayModeSelected(mode)
                        },
                    )
                },
            ),
        ),
        show = selectingDisplayMode,
        onDismiss = { onSelectingDisplayModeChange(false) },
        onDismissFinished = {},
        maxHeight = null,
        dropdownColors = DropdownDefaults.dropdownColors(),
        renderInRootScaffold = true,
        collapseOnSelection = true,
    )
}

// Same sort list as the settings sheet's sort page (tracker score only
// shows when a tracker is logged in).
private fun sortOptions(hasTrackers: Boolean): List<Pair<StringResource, LibrarySort.Type>> {
    val trackerMeanPair = if (hasTrackers) {
        MR.strings.action_sort_tracker_score to LibrarySort.Type.TrackerMean
    } else {
        null
    }
    return listOfNotNull(
        MR.strings.action_sort_alpha to LibrarySort.Type.Alphabetical,
        MR.strings.action_sort_total to LibrarySort.Type.TotalChapters,
        MR.strings.action_sort_last_read to LibrarySort.Type.LastRead,
        MR.strings.action_sort_last_manga_update to LibrarySort.Type.LastUpdate,
        MR.strings.action_sort_unread_count to LibrarySort.Type.UnreadCount,
        MR.strings.action_sort_latest_chapter to LibrarySort.Type.LatestChapter,
        MR.strings.action_sort_chapter_fetch_date to LibrarySort.Type.ChapterFetchDate,
        MR.strings.action_sort_date_added to LibrarySort.Type.DateAdded,
        trackerMeanPair,
        MR.strings.action_sort_random to LibrarySort.Type.Random,
    )
}

private val displayModes = listOf(
    MR.strings.action_display_grid to LibraryDisplayMode.CompactGrid,
    MR.strings.action_display_comfortable_grid to LibraryDisplayMode.ComfortableGrid,
    MR.strings.action_display_cover_only_grid to LibraryDisplayMode.CoverOnlyGrid,
    MR.strings.action_display_list to LibraryDisplayMode.List,
)
