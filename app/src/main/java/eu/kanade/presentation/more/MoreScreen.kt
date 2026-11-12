package eu.kanade.presentation.more

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import eu.kanade.tachiyomi.ui.more.DownloadQueueState
import mihon.icons.materialsymbols.MaterialSymbols
import mihon.icons.materialsymbols.automirroredrounded.Label
import mihon.icons.materialsymbols.rounded.CloudOff
import mihon.icons.materialsymbols.rounded.Download
import mihon.icons.materialsymbols.rounded.Eyeglasses2
import mihon.icons.materialsymbols.rounded.QueryStats
import mihon.icons.materialsymbols.rounded.Settings
import mihon.icons.materialsymbols.rounded.Storage
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.ScrollbarLazyColumn
import tachiyomi.presentation.core.components.material.Scaffold
import tachiyomi.presentation.core.i18n.pluralStringResource
import tachiyomi.presentation.core.i18n.stringResource
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.preference.ArrowPreference
import top.yukonga.miuix.kmp.preference.SwitchPreference

@Composable
fun MoreScreen(
    downloadQueueStateProvider: () -> DownloadQueueState,
    downloadedOnly: Boolean,
    onDownloadedOnlyChange: (Boolean) -> Unit,
    incognitoMode: Boolean,
    onIncognitoModeChange: (Boolean) -> Unit,
    onClickDownloadQueue: () -> Unit,
    onClickCategories: () -> Unit,
    onClickStats: () -> Unit,
    onClickDataAndStorage: () -> Unit,
    onClickSettings: () -> Unit,
) {
    Scaffold(
        // Same as the Library page: HomeScreen's outer Scaffold already
        // applies the system-bar insets, so this inner Scaffold must not
        // re-apply them (dead strips top and bottom).
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
    ) { contentPadding ->
        ScrollbarLazyColumn(contentPadding = contentPadding) {
            // Top blank band: the other main tabs have a 64.dp toolbar
            // occupying the top band under the title capsule, so their
            // content starts below it. This page has no top bar — without
            // this spacer the first row would slide under the capsule
            // and its text would overlap it.
            item {
                Spacer(modifier = Modifier.height(64.dp))
            }
            // Grouped Miuix cards, one card per group (same grouping the
            // old dividers marked). No dividers between rows — that is
            // the Miuix convention. Entries and click behaviour are
            // unchanged.
            item {
                Card(modifier = Modifier.padding(horizontal = 12.dp)) {
                    SwitchPreference(
                        checked = downloadedOnly,
                        onCheckedChange = onDownloadedOnlyChange,
                        title = stringResource(MR.strings.label_downloaded_only),
                        summary = stringResource(MR.strings.downloaded_only_summary),
                        startAction = {
                            Icon(
                                imageVector = MaterialSymbols.Rounded.CloudOff,
                                contentDescription = null,
                            )
                        },
                    )
                    SwitchPreference(
                        checked = incognitoMode,
                        onCheckedChange = onIncognitoModeChange,
                        title = stringResource(MR.strings.pref_incognito_mode),
                        summary = stringResource(MR.strings.pref_incognito_mode_summary),
                        startAction = {
                            Icon(
                                imageVector = MaterialSymbols.Rounded.Eyeglasses2,
                                contentDescription = null,
                            )
                        },
                    )
                }
            }
            item {
                Spacer(modifier = Modifier.height(12.dp))
            }
            item {
                Card(modifier = Modifier.padding(horizontal = 12.dp)) {
                    val downloadQueueState = downloadQueueStateProvider()
                    ArrowPreference(
                        title = stringResource(MR.strings.label_download_queue),
                        summary = when (downloadQueueState) {
                            DownloadQueueState.Stopped -> null
                            is DownloadQueueState.Paused -> {
                                val pending = downloadQueueState.pending
                                if (pending == 0) {
                                    stringResource(MR.strings.paused)
                                } else {
                                    "${stringResource(MR.strings.paused)} • ${
                                        pluralStringResource(
                                            MR.plurals.download_queue_summary,
                                            count = pending,
                                            pending,
                                        )
                                    }"
                                }
                            }
                            is DownloadQueueState.Downloading -> {
                                val pending = downloadQueueState.pending
                                pluralStringResource(
                                    MR.plurals.download_queue_summary,
                                    count = pending,
                                    pending,
                                )
                            }
                        },
                        startAction = {
                            Icon(
                                imageVector = MaterialSymbols.Rounded.Download,
                                contentDescription = null,
                            )
                        },
                        onClick = onClickDownloadQueue,
                    )
                    ArrowPreference(
                        title = stringResource(MR.strings.categories),
                        startAction = {
                            Icon(
                                imageVector = MaterialSymbols.AutoMirroredRounded.Label,
                                contentDescription = null,
                            )
                        },
                        onClick = onClickCategories,
                    )
                    ArrowPreference(
                        title = stringResource(MR.strings.label_stats),
                        startAction = {
                            Icon(
                                imageVector = MaterialSymbols.Rounded.QueryStats,
                                contentDescription = null,
                            )
                        },
                        onClick = onClickStats,
                    )
                    ArrowPreference(
                        title = stringResource(MR.strings.label_data_storage),
                        startAction = {
                            Icon(
                                imageVector = MaterialSymbols.Rounded.Storage,
                                contentDescription = null,
                            )
                        },
                        onClick = onClickDataAndStorage,
                    )
                }
            }
            item {
                Spacer(modifier = Modifier.height(12.dp))
            }
            item {
                Card(modifier = Modifier.padding(horizontal = 12.dp)) {
                    ArrowPreference(
                        title = stringResource(MR.strings.label_settings),
                        startAction = {
                            Icon(
                                imageVector = MaterialSymbols.Rounded.Settings,
                                contentDescription = null,
                            )
                        },
                        onClick = onClickSettings,
                    )
                }
            }
        }
    }
}
