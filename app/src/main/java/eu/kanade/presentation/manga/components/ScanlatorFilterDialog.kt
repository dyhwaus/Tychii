package eu.kanade.presentation.manga.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.unit.dp
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Checkbox
import top.yukonga.miuix.kmp.basic.HorizontalDivider
import top.yukonga.miuix.kmp.basic.Text as MiuixText
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.overlay.OverlayDialog
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun ScanlatorFilterDialog(
    availableScanlators: Set<String>,
    excludedScanlators: Set<String>,
    onDismissRequest: () -> Unit,
    onConfirm: (Set<String>) -> Unit,
) {
    val sortedAvailableScanlators = remember(availableScanlators) {
        availableScanlators.sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it })
    }
    val mutableExcludedScanlators = remember(excludedScanlators) { excludedScanlators.toMutableStateList() }
    OverlayDialog(
        show = true,
        title = stringResource(MR.strings.exclude_scanlators),
        onDismissRequest = onDismissRequest,
    ) {
        Column {
            if (sortedAvailableScanlators.isEmpty()) {
                MiuixText(
                    text = stringResource(MR.strings.no_scanlators_found),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MiuixTheme.colorScheme.onSurface,
                )
            } else {
                Box {
                    val state = rememberLazyListState()
                    LazyColumn(
                        state = state,
                        modifier = Modifier.heightIn(max = 400.dp),
                    ) {
                        sortedAvailableScanlators.forEach { scanlator ->
                            item {
                                val isExcluded = mutableExcludedScanlators.contains(scanlator)
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .clickable {
                                            if (isExcluded) {
                                                mutableExcludedScanlators.remove(scanlator)
                                            } else {
                                                mutableExcludedScanlators.add(scanlator)
                                            }
                                        }
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                ) {
                                    Checkbox(
                                        state = if (isExcluded) ToggleableState.On else ToggleableState.Off,
                                        // The row handles the click.
                                        onClick = null,
                                        modifier = Modifier.padding(end = 20.dp),
                                    )
                                    MiuixText(
                                        text = scanlator,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MiuixTheme.colorScheme.onSurface,
                                    )
                                }
                            }
                        }
                    }
                    if (state.canScrollBackward) HorizontalDivider(modifier = Modifier.align(Alignment.TopCenter))
                    if (state.canScrollForward) HorizontalDivider(modifier = Modifier.align(Alignment.BottomCenter))
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (sortedAvailableScanlators.isEmpty()) {
                    Spacer(modifier = Modifier.weight(1f))
                    TextButton(
                        text = stringResource(MR.strings.action_cancel),
                        onClick = onDismissRequest,
                    )
                } else {
                    if (mutableExcludedScanlators.isEmpty()) {
                        TextButton(
                            text = stringResource(MR.strings.action_select_all),
                            onClick = { mutableExcludedScanlators.addAll(availableScanlators) },
                        )
                    } else {
                        TextButton(
                            text = stringResource(MR.strings.action_reset),
                            onClick = mutableExcludedScanlators::clear,
                        )
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    TextButton(
                        text = stringResource(MR.strings.action_cancel),
                        onClick = onDismissRequest,
                    )
                    TextButton(
                        text = stringResource(MR.strings.action_ok),
                        onClick = {
                            onConfirm(mutableExcludedScanlators.toSet())
                            onDismissRequest()
                        },
                        colors = ButtonDefaults.textButtonColorsPrimary(),
                    )
                }
            }
        }
    }
}
