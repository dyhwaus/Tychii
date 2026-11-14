package eu.kanade.presentation.reader.settings

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import top.yukonga.miuix.kmp.basic.Card

// One grouped card for a section of the reader settings, mirroring the
// main settings (PreferenceScreen): the section's items sit in a card,
// and sections are separated by a 12.dp gap. Item content keeps its own
// padding (SettingsItemsPaddings), so the card only adds its outer
// 12.dp horizontal margin.
@Composable
internal fun ColumnScope.ReaderSettingsCard(
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp),
    ) {
        content()
    }
}

@Composable
internal fun ReaderSettingsSectionGap() {
    Spacer(modifier = Modifier.height(12.dp))
}
