package eu.kanade.presentation.updates

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.overlay.OverlayDialog
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource

@Composable
fun UpdatesDeleteConfirmationDialog(
    onDismissRequest: () -> Unit,
    onConfirm: () -> Unit,
) {
    OverlayDialog(
        show = true,
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
                    onConfirm()
                    onDismissRequest()
                },
            )
        }
    }
}
