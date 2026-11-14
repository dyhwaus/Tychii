package eu.kanade.presentation.reader.settings

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults

// Small selectable chip for the reader settings pages, mirroring the
// Explore page's FilterButton: a Miuix button, primary when selected.
@Composable
internal fun ReaderFilterChip(
    selected: Boolean,
    onClick: () -> Unit,
    label: String,
) {
    Button(
        onClick = onClick,
        colors = if (selected) ButtonDefaults.buttonColorsPrimary() else ButtonDefaults.buttonColors(),
    ) {
        Text(text = label)
    }
}
