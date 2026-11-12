package eu.kanade.presentation.manga

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.unit.dp
import eu.kanade.domain.manga.model.downloadedFilter
import eu.kanade.presentation.components.TabbedDialogPaddings
import kotlinx.coroutines.launch
import mihon.icons.materialsymbols.MaterialSymbols
import mihon.icons.materialsymbols.rounded.ArrowDownward
import mihon.icons.materialsymbols.rounded.ArrowUpward
import mihon.icons.materialsymbols.rounded.Group
import mihon.icons.materialsymbols.rounded.MoreVert
import tachiyomi.core.common.preference.TriState
import tachiyomi.domain.manga.model.Manga
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.theme.active
import top.yukonga.miuix.kmp.basic.BasicComponent
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Checkbox
import top.yukonga.miuix.kmp.basic.DropdownDefaults
import top.yukonga.miuix.kmp.basic.DropdownEntry
import top.yukonga.miuix.kmp.basic.DropdownItem
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.TabRow
import top.yukonga.miuix.kmp.basic.Text as MiuixText
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.overlay.OverlayBottomSheet
import top.yukonga.miuix.kmp.overlay.OverlayDialog
import top.yukonga.miuix.kmp.popup.OverlayDropdownPopup
import top.yukonga.miuix.kmp.preference.RadioButtonLocation
import top.yukonga.miuix.kmp.preference.RadioButtonPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun ChapterSettingsDialog(
    onDismissRequest: () -> Unit,
    manga: Manga? = null,
    downloadedOnly: Boolean,
    onDownloadFilterChanged: (TriState) -> Unit,
    onUnreadFilterChanged: (TriState) -> Unit,
    onBookmarkedFilterChanged: (TriState) -> Unit,
    scanlatorFilterActive: Boolean,
    onScanlatorFilterClicked: (() -> Unit),
    onSortModeChanged: (Long) -> Unit,
    onDisplayModeChanged: (Long) -> Unit,
    onSetAsDefault: (applyToExistingManga: Boolean) -> Unit,
    onResetToDefault: () -> Unit,
) {
    var showSetAsDefaultDialog by rememberSaveable { mutableStateOf(false) }
    if (showSetAsDefaultDialog) {
        SetAsDefaultDialog(
            onDismissRequest = { showSetAsDefaultDialog = false },
            onConfirmed = onSetAsDefault,
        )
    }

    // The shared TabbedDialog stays Material3: the library, reader and
    // updates dialogs use it too. This is a local Miuix copy of its shell
    // (tab row + pager + overflow menu) for this dialog only.
    val pagerState = rememberPagerState { 3 }
    val scope = rememberCoroutineScope()
    var overflowExpanded by remember { mutableStateOf(false) }

    OverlayBottomSheet(
        show = true,
        onDismissRequest = onDismissRequest,
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                TabRow(
                    tabs = listOf(
                        stringResource(MR.strings.action_filter),
                        stringResource(MR.strings.action_sort),
                        stringResource(MR.strings.action_display),
                    ),
                    selectedTabIndex = pagerState.currentPage,
                    onTabSelected = { scope.launch { pagerState.animateScrollToPage(it) } },
                    modifier = Modifier.weight(1f),
                )

                Box {
                    IconButton(onClick = { overflowExpanded = true }) {
                        Icon(
                            imageVector = MaterialSymbols.Rounded.MoreVert,
                            contentDescription = stringResource(MR.strings.label_more),
                            tint = MiuixTheme.colorScheme.onSurface,
                        )
                    }
                    OverlayDropdownPopup(
                        entries = listOf(
                            DropdownEntry(
                                items = listOf(
                                    DropdownItem(
                                        text = stringResource(MR.strings.set_chapter_settings_as_default),
                                        onClick = { showSetAsDefaultDialog = true },
                                    ),
                                    DropdownItem(
                                        text = stringResource(MR.strings.action_reset),
                                        onClick = onResetToDefault,
                                    ),
                                ),
                            ),
                        ),
                        show = overflowExpanded,
                        onDismiss = { overflowExpanded = false },
                        onDismissFinished = {},
                        maxHeight = null,
                        dropdownColors = DropdownDefaults.dropdownColors(),
                        renderInRootScaffold = true,
                        collapseOnSelection = true,
                    )
                }
            }

            HorizontalPager(
                modifier = Modifier.animateContentSize(),
                state = pagerState,
                verticalAlignment = Alignment.Top,
            ) { page ->
                Column(
                    modifier = Modifier
                        .padding(vertical = TabbedDialogPaddings.Vertical)
                        .verticalScroll(rememberScrollState()),
                ) {
                    when (page) {
                        0 -> {
                            FilterPage(
                                downloadFilter = manga?.downloadedFilter(downloadedOnly) ?: TriState.DISABLED,
                                onDownloadFilterChanged = onDownloadFilterChanged
                                    .takeUnless { downloadedOnly },
                                unreadFilter = manga?.unreadFilter ?: TriState.DISABLED,
                                onUnreadFilterChanged = onUnreadFilterChanged,
                                bookmarkedFilter = manga?.bookmarkedFilter ?: TriState.DISABLED,
                                onBookmarkedFilterChanged = onBookmarkedFilterChanged,
                                scanlatorFilterActive = scanlatorFilterActive,
                                onScanlatorFilterClicked = onScanlatorFilterClicked,
                            )
                        }
                        1 -> {
                            SortPage(
                                sortingMode = manga?.sorting ?: 0,
                                sortDescending = manga?.sortDescending() ?: false,
                                onItemSelected = onSortModeChanged,
                            )
                        }
                        2 -> {
                            DisplayPage(
                                displayMode = manga?.displayMode ?: 0,
                                onItemSelected = onDisplayModeChanged,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ColumnScope.FilterPage(
    downloadFilter: TriState,
    onDownloadFilterChanged: ((TriState) -> Unit)?,
    unreadFilter: TriState,
    onUnreadFilterChanged: (TriState) -> Unit,
    bookmarkedFilter: TriState,
    onBookmarkedFilterChanged: (TriState) -> Unit,
    scanlatorFilterActive: Boolean,
    onScanlatorFilterClicked: (() -> Unit),
) {
    MiuixTriStateRow(
        label = stringResource(MR.strings.label_downloaded),
        state = downloadFilter,
        onClick = onDownloadFilterChanged,
    )
    MiuixTriStateRow(
        label = stringResource(MR.strings.action_filter_unread),
        state = unreadFilter,
        onClick = onUnreadFilterChanged,
    )
    MiuixTriStateRow(
        label = stringResource(MR.strings.action_filter_bookmarked),
        state = bookmarkedFilter,
        onClick = onBookmarkedFilterChanged,
    )
    ScanlatorFilterItem(
        active = scanlatorFilterActive,
        onClick = onScanlatorFilterClicked,
    )
}

@Composable
private fun MiuixTriStateRow(
    label: String,
    state: TriState,
    onClick: ((TriState) -> Unit)?,
) {
    // The preference layer has no tri-state checkbox, so the basic
    // Checkbox (ToggleableState) is paired with a BasicComponent row.
    // Same cycle as the Material TriStateItem: off -> on -> excluded.
    val cycle = {
        onClick?.invoke(
            when (state) {
                TriState.DISABLED -> TriState.ENABLED_IS
                TriState.ENABLED_IS -> TriState.ENABLED_NOT
                TriState.ENABLED_NOT -> TriState.DISABLED
            },
        )
    }
    BasicComponent(
        title = label,
        enabled = onClick != null,
        onClick = cycle,
        endActions = {
            Checkbox(
                state = when (state) {
                    TriState.DISABLED -> ToggleableState.Off
                    TriState.ENABLED_IS -> ToggleableState.On
                    TriState.ENABLED_NOT -> ToggleableState.Indeterminate
                },
                // The row handles the click, like the Material version.
                onClick = null,
                enabled = onClick != null,
            )
        },
    )
}

@Composable
fun ScanlatorFilterItem(
    active: Boolean,
    onClick: () -> Unit,
) {
    BasicComponent(
        title = stringResource(MR.strings.scanlator),
        startAction = {
            Icon(
                imageVector = MaterialSymbols.Rounded.Group,
                contentDescription = null,
                tint = if (active) {
                    MaterialTheme.colorScheme.active
                } else {
                    MiuixTheme.colorScheme.onSurface
                },
            )
        },
        onClick = onClick,
    )
}

@Composable
private fun ColumnScope.SortPage(
    sortingMode: Long,
    sortDescending: Boolean,
    onItemSelected: (Long) -> Unit,
) {
    listOf(
        MR.strings.sort_by_source to Manga.CHAPTER_SORTING_SOURCE,
        MR.strings.sort_by_number to Manga.CHAPTER_SORTING_NUMBER,
        MR.strings.sort_by_upload_date to Manga.CHAPTER_SORTING_UPLOAD_DATE,
        MR.strings.action_sort_alpha to Manga.CHAPTER_SORTING_ALPHABET,
    ).forEach { (titleRes, mode) ->
        val arrowIcon = when {
            sortingMode != mode -> null
            sortDescending -> MaterialSymbols.Rounded.ArrowDownward
            else -> MaterialSymbols.Rounded.ArrowUpward
        }
        BasicComponent(
            title = stringResource(titleRes),
            onClick = { onItemSelected(mode) },
            endActions = {
                if (arrowIcon != null) {
                    Icon(
                        imageVector = arrowIcon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                    )
                } else {
                    Spacer(modifier = Modifier.size(24.dp))
                }
            },
        )
    }
}

@Composable
private fun ColumnScope.DisplayPage(
    displayMode: Long,
    onItemSelected: (Long) -> Unit,
) {
    listOf(
        MR.strings.show_title to Manga.CHAPTER_DISPLAY_NAME,
        MR.strings.show_chapter_number to Manga.CHAPTER_DISPLAY_NUMBER,
    ).forEach { (titleRes, mode) ->
        RadioButtonPreference(
            title = stringResource(titleRes),
            selected = displayMode == mode,
            onClick = { onItemSelected(mode) },
            radioButtonLocation = RadioButtonLocation.End,
        )
    }
}

@Composable
private fun SetAsDefaultDialog(
    onDismissRequest: () -> Unit,
    onConfirmed: (optionalChecked: Boolean) -> Unit,
) {
    var optionalChecked by rememberSaveable { mutableStateOf(false) }

    OverlayDialog(
        show = true,
        title = stringResource(MR.strings.chapter_settings),
        onDismissRequest = onDismissRequest,
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            MiuixText(
                text = stringResource(MR.strings.confirm_set_chapter_settings),
                style = MaterialTheme.typography.bodyMedium,
                color = MiuixTheme.colorScheme.onSurface,
            )

            Row(
                modifier = Modifier
                    .clickable { optionalChecked = !optionalChecked }
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Checkbox(
                    state = if (optionalChecked) ToggleableState.On else ToggleableState.Off,
                    onClick = { optionalChecked = !optionalChecked },
                    modifier = Modifier.padding(end = 20.dp),
                )
                MiuixText(
                    text = stringResource(MR.strings.also_set_chapter_settings_for_library),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MiuixTheme.colorScheme.onSurface,
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                TextButton(
                    text = stringResource(MR.strings.action_cancel),
                    onClick = onDismissRequest,
                )
                TextButton(
                    text = stringResource(MR.strings.action_ok),
                    onClick = {
                        onConfirmed(optionalChecked)
                        onDismissRequest()
                    },
                    colors = ButtonDefaults.textButtonColorsPrimary(),
                )
            }
        }
    }
}
