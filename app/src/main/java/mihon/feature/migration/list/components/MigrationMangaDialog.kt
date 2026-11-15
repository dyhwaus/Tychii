package mihon.feature.migration.list.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.pluralStringResource
import tachiyomi.presentation.core.i18n.stringResource
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.overlay.OverlayDialog

@Composable
fun MigrationMangaDialog(
    onDismissRequest: () -> Unit,
    copy: Boolean,
    totalCount: Int,
    skippedCount: Int,
    onMigrate: () -> Unit,
) {
    OverlayDialog(
        show = true,
        title = pluralStringResource(
            resource = if (copy) {
                MR.plurals.migrationListScreen_migrateDialog_copyTitle
            } else {
                MR.plurals.migrationListScreen_migrateDialog_migrateTitle
            },
            count = totalCount,
            totalCount,
        ),
        summary = if (skippedCount > 0) {
            pluralStringResource(
                resource = MR.plurals.migrationListScreen_migrateDialog_skipText,
                count = skippedCount,
                skippedCount,
            )
        } else {
            null
        },
        onDismissRequest = onDismissRequest,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
        ) {
            TextButton(
                text = stringResource(MR.strings.migrationListScreen_migrateDialog_cancelLabel),
                onClick = onDismissRequest,
            )
            TextButton(
                text = stringResource(
                    resource = if (copy) {
                        MR.strings.migrationListScreen_migrateDialog_copyLabel
                    } else {
                        MR.strings.migrationListScreen_migrateDialog_migrateLabel
                    },
                ),
                onClick = onMigrate,
            )
        }
    }
}
