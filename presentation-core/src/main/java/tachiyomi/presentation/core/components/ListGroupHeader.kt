package tachiyomi.presentation.core.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import top.yukonga.miuix.kmp.basic.SmallTitle

@Composable
fun ListGroupHeader(
    text: String,
    modifier: Modifier = Modifier,
) {
    SmallTitle(
        text = text,
        modifier = modifier,
        insideMargin = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
    )
}
