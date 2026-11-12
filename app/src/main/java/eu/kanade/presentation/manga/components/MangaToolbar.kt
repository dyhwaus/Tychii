package eu.kanade.presentation.manga.components

import androidx.compose.foundation.layout.Box
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import eu.kanade.presentation.manga.DownloadAction
import mihon.icons.materialsymbols.MaterialSymbols
import mihon.icons.materialsymbols.automirroredrounded.ArrowBack
import mihon.icons.materialsymbols.rounded.Close
import mihon.icons.materialsymbols.rounded.Download
import mihon.icons.materialsymbols.rounded.FilterList
import mihon.icons.materialsymbols.rounded.FlipToBack
import mihon.icons.materialsymbols.rounded.MoreVert
import mihon.icons.materialsymbols.rounded.SelectAll
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.pluralStringResource
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.theme.active
import top.yukonga.miuix.kmp.basic.DropdownDefaults
import top.yukonga.miuix.kmp.basic.DropdownEntry
import top.yukonga.miuix.kmp.basic.DropdownItem
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.SmallTopAppBar
import top.yukonga.miuix.kmp.popup.OverlayDropdownPopup
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun MangaToolbar(
    title: String,
    hasFilters: Boolean,
    navigateUp: () -> Unit,
    onClickFilter: () -> Unit,
    onClickShare: (() -> Unit)?,
    onClickDownload: ((DownloadAction) -> Unit)?,
    onClickEditCategory: (() -> Unit)?,
    onClickRefresh: () -> Unit,
    onClickMigrate: (() -> Unit)?,
    onClickEditNotes: () -> Unit,

    // For action mode
    actionModeCounter: Int,
    onCancelActionMode: () -> Unit,
    onSelectAll: () -> Unit,
    onInvertSelection: () -> Unit,

    titleAlphaProvider: () -> Float,
    backgroundAlphaProvider: () -> Float,
    modifier: Modifier = Modifier,
) {
    val isActionMode = actionModeCounter > 0
    // The title fades in with the scroll (it is also shown large in the info
    // box below); the bar background fades in the same way. Miuix's
    // SmallTopAppBar takes plain colors, so the fade is done through their
    // alpha instead of a modifier.
    SmallTopAppBar(
        title = if (isActionMode) actionModeCounter.toString() else title,
        modifier = modifier,
        color = MiuixTheme.colorScheme.surface.copy(
            alpha = if (isActionMode) 1f else backgroundAlphaProvider(),
        ),
        titleColor = MiuixTheme.colorScheme.onSurface.copy(
            alpha = if (isActionMode) 1f else titleAlphaProvider(),
        ),
        navigationIcon = {
            IconButton(
                onClick = if (isActionMode) onCancelActionMode else navigateUp,
            ) {
                Icon(
                    imageVector = if (isActionMode) {
                        MaterialSymbols.Rounded.Close
                    } else {
                        MaterialSymbols.AutoMirroredRounded.ArrowBack
                    },
                    contentDescription = null,
                    tint = MiuixTheme.colorScheme.onSurface,
                )
            }
        },
        actions = {
            if (isActionMode) {
                IconButton(onClick = onSelectAll) {
                    Icon(
                        imageVector = MaterialSymbols.Rounded.SelectAll,
                        contentDescription = stringResource(MR.strings.action_select_all),
                        tint = MiuixTheme.colorScheme.onSurface,
                    )
                }
                IconButton(onClick = onInvertSelection) {
                    Icon(
                        imageVector = MaterialSymbols.Rounded.FlipToBack,
                        contentDescription = stringResource(MR.strings.action_select_inverse),
                        tint = MiuixTheme.colorScheme.onSurface,
                    )
                }
            } else {
                if (onClickDownload != null) {
                    var downloadExpanded by remember { mutableStateOf(false) }
                    Box {
                        IconButton(onClick = { downloadExpanded = !downloadExpanded }) {
                            Icon(
                                imageVector = MaterialSymbols.Rounded.Download,
                                contentDescription = stringResource(MR.strings.manga_download),
                                tint = MiuixTheme.colorScheme.onSurface,
                            )
                        }
                        OverlayDropdownPopup(
                            entries = listOf(
                                DropdownEntry(
                                    items = downloadMenuItems(onClickDownload),
                                ),
                            ),
                            show = downloadExpanded,
                            onDismiss = { downloadExpanded = false },
                            onDismissFinished = {},
                            maxHeight = null,
                            dropdownColors = DropdownDefaults.dropdownColors(),
                            renderInRootScaffold = true,
                            collapseOnSelection = true,
                        )
                    }
                }

                IconButton(onClick = onClickFilter) {
                    Icon(
                        imageVector = MaterialSymbols.Rounded.FilterList,
                        contentDescription = stringResource(MR.strings.action_filter),
                        tint = if (hasFilters) {
                            MaterialTheme.colorScheme.active
                        } else {
                            MiuixTheme.colorScheme.onSurface
                        },
                    )
                }

                var overflowExpanded by remember { mutableStateOf(false) }
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
                                items = buildList {
                                    add(
                                        DropdownItem(
                                            text = stringResource(MR.strings.action_webview_refresh),
                                            onClick = onClickRefresh,
                                        ),
                                    )
                                    if (onClickEditCategory != null) {
                                        add(
                                            DropdownItem(
                                                text = stringResource(MR.strings.action_edit_categories),
                                                onClick = onClickEditCategory,
                                            ),
                                        )
                                    }
                                    if (onClickMigrate != null) {
                                        add(
                                            DropdownItem(
                                                text = stringResource(MR.strings.action_migrate),
                                                onClick = onClickMigrate,
                                            ),
                                        )
                                    }
                                    if (onClickShare != null) {
                                        add(
                                            DropdownItem(
                                                text = stringResource(MR.strings.action_share),
                                                onClick = onClickShare,
                                            ),
                                        )
                                    }
                                    add(
                                        DropdownItem(
                                            text = stringResource(MR.strings.action_notes),
                                            onClick = onClickEditNotes,
                                        ),
                                    )
                                },
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
        },
    )
}

@Composable
private fun downloadMenuItems(
    onDownloadClicked: (DownloadAction) -> Unit,
): List<DropdownItem> {
    return listOf(
        DownloadAction.NEXT_1_CHAPTER to pluralStringResource(MR.plurals.download_amount, 1, 1),
        DownloadAction.NEXT_5_CHAPTERS to pluralStringResource(MR.plurals.download_amount, 5, 5),
        DownloadAction.NEXT_10_CHAPTERS to pluralStringResource(MR.plurals.download_amount, 10, 10),
        DownloadAction.NEXT_25_CHAPTERS to pluralStringResource(MR.plurals.download_amount, 25, 25),
        DownloadAction.UNREAD_CHAPTERS to stringResource(MR.strings.download_unread),
        DownloadAction.BOOKMARKED_CHAPTERS to stringResource(MR.strings.download_bookmarked),
    ).map { (downloadAction, text) ->
        DropdownItem(
            text = text,
            onClick = { onDownloadClicked(downloadAction) },
        )
    }
}
