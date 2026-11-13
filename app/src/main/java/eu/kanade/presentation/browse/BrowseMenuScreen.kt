package eu.kanade.presentation.browse

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import mihon.icons.materialsymbols.MaterialSymbols
import mihon.icons.materialsymbols.rounded.Public
import mihon.icons.materialsymbols.rounded.Sync
import mihon.icons.materialsymbols.roundedfilled.Extension
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.ScrollbarLazyColumn
import tachiyomi.presentation.core.components.material.Scaffold
import tachiyomi.presentation.core.i18n.stringResource
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.preference.ArrowPreference

@Composable
fun BrowseMenuScreen(
    onClickSources: () -> Unit,
    onClickExtensions: () -> Unit,
    onClickMigration: () -> Unit,
) {
    Scaffold(
        // Same as the More page: HomeScreen's outer Scaffold already
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
            // One grouped Miuix card, like the More page: a first-level
            // menu; each row opens its section as its own screen.
            item {
                Card(modifier = Modifier.padding(horizontal = 12.dp)) {
                    ArrowPreference(
                        title = stringResource(MR.strings.label_sources),
                        startAction = {
                            Icon(
                                imageVector = MaterialSymbols.Rounded.Public,
                                contentDescription = null,
                            )
                        },
                        onClick = onClickSources,
                    )
                    ArrowPreference(
                        title = stringResource(MR.strings.label_extensions),
                        startAction = {
                            Icon(
                                imageVector = MaterialSymbols.RoundedFilled.Extension,
                                contentDescription = null,
                            )
                        },
                        onClick = onClickExtensions,
                    )
                    ArrowPreference(
                        title = stringResource(MR.strings.label_migration),
                        startAction = {
                            Icon(
                                imageVector = MaterialSymbols.Rounded.Sync,
                                contentDescription = null,
                            )
                        },
                        onClick = onClickMigration,
                    )
                }
            }
        }
    }
}
