package eu.kanade.presentation.more.settings.widget

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import top.yukonga.miuix.kmp.basic.SmallTitle

@Composable
fun PreferenceGroupHeader(title: String, highlighted: Boolean = false) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .highlightBackground(highlighted),
    ) {
        SmallTitle(text = title)
    }
}
