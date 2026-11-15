package mihon.feature.migration.list.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource
import top.yukonga.miuix.kmp.basic.LinearProgressIndicator
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.overlay.OverlayDialog

@Composable
fun MigrationProgressDialog(
    progress: Float,
    exitMigration: () -> Unit,
) {
    // Not dismissable by back press or tapping outside, like before
    // (the old dialog set both DialogProperties flags to false).
    OverlayDialog(
        show = true,
        onDismissRequest = {},
    ) {
        if (!progress.isNaN()) {
            val progressAnimated by animateFloatAsState(
                targetValue = progress,
                label = "migration_progress",
            )
            LinearProgressIndicator(
                modifier = Modifier.fillMaxWidth(),
                progress = progressAnimated,
            )
        }
        Spacer(Modifier.height(16.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
        ) {
            TextButton(
                text = stringResource(MR.strings.migrationListScreen_progressDialog_cancelLabel),
                onClick = exitMigration,
            )
        }
    }
}
