package eu.kanade.presentation.history.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.ui.Modifier
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.overlay.OverlayDialog
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.tooling.preview.PreviewLightDark
import eu.kanade.presentation.theme.TachiyomiPreviewTheme
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.LabeledCheckbox
import tachiyomi.presentation.core.i18n.stringResource

@Composable
fun HistoryDeleteDialog(
    onDismissRequest: () -> Unit,
    onDelete: (Boolean) -> Unit,
) {
    var removeEverything by remember { mutableStateOf(false) }

    OverlayDialog(
        show = true,
        title = stringResource(MR.strings.action_remove),
        summary = stringResource(MR.strings.dialog_with_checkbox_remove_description),
        onDismissRequest = onDismissRequest,
    ) {
        LabeledCheckbox(
            label = stringResource(MR.strings.dialog_with_checkbox_reset),
            checked = removeEverything,
            onCheckedChange = { removeEverything = it },
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
        ) {
            TextButton(
                text = stringResource(MR.strings.action_cancel),
                onClick = onDismissRequest,
            )
            TextButton(
                text = stringResource(MR.strings.action_remove),
                onClick = {
                    onDelete(removeEverything)
                    onDismissRequest()
                },
            )
        }
    }
}

@Composable
fun HistoryDeleteAllDialog(
    onDismissRequest: () -> Unit,
    onDelete: () -> Unit,
) {
    OverlayDialog(
        show = true,
        title = stringResource(MR.strings.action_remove_everything),
        summary = stringResource(MR.strings.clear_history_confirmation),
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
                    onDelete()
                    onDismissRequest()
                },
            )
        }
    }
}

@PreviewLightDark
@Composable
private fun HistoryDeleteDialogPreview() {
    TachiyomiPreviewTheme {
        HistoryDeleteDialog(
            onDismissRequest = {},
            onDelete = {},
        )
    }
}
