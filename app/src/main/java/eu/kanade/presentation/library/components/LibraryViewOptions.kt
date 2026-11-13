package eu.kanade.presentation.library.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import dev.icerock.moko.resources.StringResource
import mihon.icons.materialsymbols.MaterialSymbols
import mihon.icons.materialsymbols.automirroredrounded.Sort
import mihon.icons.materialsymbols.automirroredroundedfilled.ViewList
import mihon.icons.materialsymbols.rounded.ArrowDownward
import mihon.icons.materialsymbols.rounded.ArrowUpward
import mihon.icons.materialsymbols.rounded.FilterList
import mihon.icons.materialsymbols.roundedfilled.ViewCompact
import mihon.icons.materialsymbols.roundedfilled.ViewModule
import tachiyomi.domain.library.model.LibraryDisplayMode
import tachiyomi.domain.library.model.LibrarySort
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.theme.active
import top.yukonga.miuix.kmp.basic.DropdownDefaults
import top.yukonga.miuix.kmp.basic.DropdownEntry
import top.yukonga.miuix.kmp.basic.DropdownItem
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.popup.OverlayDropdownPopup
import top.yukonga.miuix.kmp.theme.MiuixTheme

// The Library page's view options as small icons: sort, display mode and
// filter. They used to live in the top bar, but the centered title capsule
// sits over that band on phones, so they moved here — pinned to the end
// of the category tab row, clear of the capsule.
@Composable
fun LibraryViewOptions(
    sort: LibrarySort,
    hasTrackers: Boolean,
    onSetSort: (LibrarySort.Type, LibrarySort.Direction) -> Unit,
    displayMode: LibraryDisplayMode,
    onDisplayModeSelected: (LibraryDisplayMode) -> Unit,
    hasActiveFilters: Boolean,
    onClickFilter: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var selectingSort by remember { mutableStateOf(false) }
    var selectingDisplayMode by remember { mutableStateOf(false) }
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box {
            IconButton(onClick = { selectingSort = true }) {
                Icon(
                    imageVector = MaterialSymbols.AutoMirroredRounded.Sort,
                    contentDescription = stringResource(MR.strings.action_sort),
                    tint = MiuixTheme.colorScheme.onSurface,
                )
            }
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
                                    selectingSort = false
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
                onDismiss = { selectingSort = false },
                onDismissFinished = {},
                maxHeight = null,
                dropdownColors = DropdownDefaults.dropdownColors(),
                renderInRootScaffold = true,
                collapseOnSelection = true,
            )
        }
        Box {
            IconButton(onClick = { selectingDisplayMode = true }) {
                Icon(
                    imageVector = when (displayMode) {
                        LibraryDisplayMode.List -> MaterialSymbols.AutoMirroredRoundedFilled.ViewList
                        LibraryDisplayMode.ComfortableGrid -> MaterialSymbols.RoundedFilled.ViewModule
                        LibraryDisplayMode.CompactGrid,
                        LibraryDisplayMode.CoverOnlyGrid,
                        -> MaterialSymbols.RoundedFilled.ViewCompact
                    },
                    contentDescription = stringResource(MR.strings.action_display_mode),
                    tint = MiuixTheme.colorScheme.onSurface,
                )
            }
            OverlayDropdownPopup(
                entries = listOf(
                    DropdownEntry(
                        items = displayModes.map { (titleRes, mode) ->
                            DropdownItem(
                                text = stringResource(titleRes),
                                selected = displayMode == mode,
                                onClick = {
                                    selectingDisplayMode = false
                                    onDisplayModeSelected(mode)
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
        }
        IconButton(onClick = onClickFilter) {
            Icon(
                imageVector = MaterialSymbols.Rounded.FilterList,
                contentDescription = stringResource(MR.strings.action_filter),
                tint = if (hasActiveFilters) {
                    MaterialTheme.colorScheme.active
                } else {
                    MiuixTheme.colorScheme.onSurface
                },
            )
        }
    }
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
