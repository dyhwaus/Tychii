package mihon.feature.migration.list.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.overlay.OverlayDialog

@Composable
fun MigrationExitDialog(
    onDismissRequest: () -> Unit,
    exitMigration: () -> Unit,
) {
    OverlayDialog(
        show = true,
        title = stringResource(MR.strings.migrationListScreen_exitDialogTitle),
        onDismissRequest = onDismissRequest,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
        ) {
            TextButton(
                text = stringResource(MR.strings.migrationListScreen_exitDialog_cancelLabel),
                onClick = onDismissRequest,
            )
            TextButton(
                text = stringResource(MR.strings.migrationListScreen_exitDialog_stopLabel),
                onClick = exitMigration,
            )
        }
    }
}
