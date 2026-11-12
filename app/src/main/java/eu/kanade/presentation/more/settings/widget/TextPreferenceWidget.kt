package eu.kanade.presentation.more.settings.widget

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.PreviewLightDark
import eu.kanade.presentation.more.settings.LocalPreferenceHighlighted
import eu.kanade.presentation.theme.TachiyomiPreviewTheme
import mihon.icons.materialsymbols.MaterialSymbols
import mihon.icons.materialsymbols.rounded.Preview
import top.yukonga.miuix.kmp.preference.ArrowPreference

@Composable
fun TextPreferenceWidget(
    modifier: Modifier = Modifier,
    title: String? = null,
    subtitle: String? = null,
    icon: ImageVector? = null,
    iconTint: Color = MaterialTheme.colorScheme.primary,
    widget: @Composable (() -> Unit)? = null,
    enabled: Boolean = true,
    onPreferenceClick: (() -> Unit)? = null,
) {
    ArrowPreference(
        title = title.orEmpty(),
        summary = subtitle,
        modifier = modifier.highlightBackground(LocalPreferenceHighlighted.current),
        startAction = icon?.let { { PreferenceIcon(it, iconTint) } },
        endActions = widget?.let { { it() } } ?: {},
        onClick = onPreferenceClick,
        enabled = enabled,
    )
}

@PreviewLightDark
@Composable
private fun TextPreferenceWidgetPreview() {
    TachiyomiPreviewTheme {
        Surface {
            Column {
                TextPreferenceWidget(
                    title = "Text preference with icon",
                    subtitle = "Text preference summary",
                    icon = MaterialSymbols.Rounded.Preview,
                    onPreferenceClick = {},
                )
                TextPreferenceWidget(
                    title = "Text preference",
                    subtitle = "Text preference summary",
                    onPreferenceClick = {},
                )
            }
        }
    }
}
