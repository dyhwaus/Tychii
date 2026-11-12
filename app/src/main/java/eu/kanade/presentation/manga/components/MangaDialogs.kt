package eu.kanade.presentation.manga.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import eu.kanade.tachiyomi.util.system.isReleaseBuildType
import kotlinx.datetime.TimeZone
import kotlinx.datetime.daysUntil
import tachiyomi.domain.manga.interactor.FetchInterval
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.material.padding
import tachiyomi.presentation.core.i18n.pluralStringResource
import tachiyomi.presentation.core.i18n.stringResource
import top.yukonga.miuix.kmp.basic.NumberPicker
import top.yukonga.miuix.kmp.basic.Text as MiuixText
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.overlay.OverlayDialog
import top.yukonga.miuix.kmp.theme.MiuixTheme
import kotlin.math.absoluteValue
import kotlin.time.Clock
import kotlin.time.Instant

@Composable
fun DeleteChaptersDialog(
    onDismissRequest: () -> Unit,
    onConfirm: () -> Unit,
) {
    OverlayDialog(
        show = true,
        title = stringResource(MR.strings.are_you_sure),
        summary = stringResource(MR.strings.confirm_delete_chapters),
        onDismissRequest = onDismissRequest,
    ) {
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
                    onDismissRequest()
                    onConfirm()
                },
                colors = ButtonDefaults.textButtonColorsPrimary(),
            )
        }
    }
}

@Composable
fun SetIntervalDialog(
    interval: Int,
    nextUpdate: Instant?,
    onDismissRequest: () -> Unit,
    onValueChanged: ((Int) -> Unit)? = null,
) {
    var selectedInterval by rememberSaveable { mutableIntStateOf(if (interval < 0) -interval else 0) }

    val nextUpdateDays = remember(nextUpdate) {
        return@remember if (nextUpdate != null) {
            val now = Clock.System.now()
            now.daysUntil(nextUpdate, TimeZone.currentSystemDefault()).coerceAtLeast(0)
        } else {
            null
        }
    }

    OverlayDialog(
        show = true,
        title = stringResource(MR.strings.pref_library_update_smart_update),
        onDismissRequest = onDismissRequest,
    ) {
        Column {
            if (nextUpdateDays != null && nextUpdateDays >= 0 && interval >= 0) {
                MiuixText(
                    text = stringResource(
                        MR.strings.manga_interval_expected_update,
                        pluralStringResource(
                            MR.plurals.day,
                            count = nextUpdateDays,
                            nextUpdateDays,
                        ),
                        pluralStringResource(
                            MR.plurals.day,
                            count = interval.absoluteValue,
                            interval.absoluteValue,
                        ),
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MiuixTheme.colorScheme.onSurface,
                )
            } else {
                MiuixText(
                    text = stringResource(MR.strings.manga_interval_expected_update_null),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MiuixTheme.colorScheme.onSurface,
                )
            }
            Spacer(Modifier.height(MaterialTheme.padding.small))

            if (onValueChanged != null && (!isReleaseBuildType)) {
                MiuixText(
                    text = stringResource(MR.strings.manga_interval_custom_amount),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MiuixTheme.colorScheme.onSurface,
                )

                // NumberPicker's label is a plain (Int) -> String, not a
                // @Composable lambda, so the string is resolved here instead.
                val defaultLabel = stringResource(MR.strings.label_default)
                BoxWithConstraints(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center,
                ) {
                    NumberPicker(
                        value = selectedInterval,
                        onValueChange = { selectedInterval = it },
                        range = 0..FetchInterval.MAX_INTERVAL,
                        label = {
                            if (it == 0) {
                                defaultLabel
                            } else {
                                it.toString()
                            }
                        },
                        visibleItemCount = 3,
                        modifier = Modifier.width(maxWidth / 2),
                    )
                }
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
                        onValueChanged?.invoke(selectedInterval)
                        onDismissRequest()
                    },
                    colors = ButtonDefaults.textButtonColorsPrimary(),
                )
            }
        }
    }
}
