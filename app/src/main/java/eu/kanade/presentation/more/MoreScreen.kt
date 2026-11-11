package eu.kanade.presentation.more

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import eu.kanade.presentation.more.settings.widget.SwitchPreferenceWidget
import eu.kanade.presentation.more.settings.widget.TextPreferenceWidget
import eu.kanade.tachiyomi.ui.more.DownloadQueueState
import mihon.icons.materialsymbols.MaterialSymbols
import mihon.icons.materialsymbols.automirroredrounded.Help
import mihon.icons.materialsymbols.automirroredrounded.Label
import mihon.icons.materialsymbols.rounded.CloudOff
import mihon.icons.materialsymbols.rounded.Download
import mihon.icons.materialsymbols.rounded.Eyeglasses2
import mihon.icons.materialsymbols.rounded.Info
import mihon.icons.materialsymbols.rounded.QueryStats
import mihon.icons.materialsymbols.rounded.Settings
import mihon.icons.materialsymbols.rounded.Storage
import mihon.icons.materialsymbols.roundedfilled.VolunteerActivism
import tachiyomi.core.common.Constants
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.ScrollbarLazyColumn
import tachiyomi.presentation.core.components.material.Scaffold
import tachiyomi.presentation.core.i18n.pluralStringResource
import tachiyomi.presentation.core.i18n.stringResource
import top.yukonga.miuix.kmp.basic.Card

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
    onClickSupport: () -> Unit,
    onClickAbout: () -> Unit,
) {
    val uriHandler = LocalUriHandler.current

    Scaffold { contentPadding ->
        ScrollbarLazyColumn(contentPadding = contentPadding) {
            item {
                LogoHeader(
                    iconPadding = PaddingValues(vertical = 32.dp),
                )
            }
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp),
                ) {
                    SwitchPreferenceWidget(
                        title = stringResource(MR.strings.label_downloaded_only),
                        subtitle = stringResource(MR.strings.downloaded_only_summary),
                        icon = MaterialSymbols.Rounded.CloudOff,
                        checked = downloadedOnly,
                        onCheckedChanged = onDownloadedOnlyChange,
                    )
                    SwitchPreferenceWidget(
                        title = stringResource(MR.strings.pref_incognito_mode),
                        subtitle = stringResource(MR.strings.pref_incognito_mode_summary),
                        icon = MaterialSymbols.Rounded.Eyeglasses2,
                        checked = incognitoMode,
                        onCheckedChanged = onIncognitoModeChange,
                    )
                }
            }

            item { Spacer(modifier = Modifier.height(12.dp)) }

            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp),
                ) {
                    val downloadQueueState = downloadQueueStateProvider()
                    TextPreferenceWidget(
                        title = stringResource(MR.strings.label_download_queue),
                        subtitle = when (downloadQueueState) {
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
                                pluralStringResource(MR.plurals.download_queue_summary, count = pending, pending)
                            }
                        },
                        icon = MaterialSymbols.Rounded.Download,
                        onPreferenceClick = onClickDownloadQueue,
                    )
                    TextPreferenceWidget(
                        title = stringResource(MR.strings.categories),
                        icon = MaterialSymbols.AutoMirroredRounded.Label,
                        onPreferenceClick = onClickCategories,
                    )
                    TextPreferenceWidget(
                        title = stringResource(MR.strings.label_stats),
                        icon = MaterialSymbols.Rounded.QueryStats,
                        onPreferenceClick = onClickStats,
                    )
                    TextPreferenceWidget(
                        title = stringResource(MR.strings.label_data_storage),
                        icon = MaterialSymbols.Rounded.Storage,
                        onPreferenceClick = onClickDataAndStorage,
                    )
                }
            }

            item { Spacer(modifier = Modifier.height(12.dp)) }

            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp),
                ) {
                    TextPreferenceWidget(
                        title = stringResource(MR.strings.label_settings),
                        icon = MaterialSymbols.Rounded.Settings,
                        onPreferenceClick = onClickSettings,
                    )
                    TextPreferenceWidget(
                        title = stringResource(MR.strings.label_support_us),
                        icon = MaterialSymbols.RoundedFilled.VolunteerActivism,
                        onPreferenceClick = onClickSupport,
                    )
                    TextPreferenceWidget(
                        title = stringResource(MR.strings.pref_category_about),
                        icon = MaterialSymbols.Rounded.Info,
                        onPreferenceClick = onClickAbout,
                    )
                    TextPreferenceWidget(
                        title = stringResource(MR.strings.label_help),
                        icon = MaterialSymbols.AutoMirroredRounded.Help,
                        onPreferenceClick = { uriHandler.openUri(Constants.URL_HELP) },
                    )
                }
            }
        }
    }
}
