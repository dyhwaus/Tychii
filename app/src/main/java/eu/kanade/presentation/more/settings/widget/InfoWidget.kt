package eu.kanade.presentation.more.settings.widget

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import eu.kanade.presentation.more.settings.LocalPreferenceHighlighted
import eu.kanade.presentation.theme.TachiyomiPreviewTheme
import mihon.icons.materialsymbols.MaterialSymbols
import mihon.icons.materialsymbols.rounded.Info
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
internal fun InfoWidget(text: String) {
    Column(
        modifier = Modifier
            .highlightBackground(LocalPreferenceHighlighted.current)
            .padding(
                horizontal = PrefsHorizontalPadding,
                vertical = 16.dp,
            ),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(
            imageVector = MaterialSymbols.Rounded.Info,
            contentDescription = null,
        )
        Text(
            text = text,
            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
            fontSize = MiuixTheme.textStyles.body2.fontSize,
        )
    }
}

@PreviewLightDark
@Composable
private fun InfoWidgetPreview() {
    TachiyomiPreviewTheme {
        Surface {
            InfoWidget(text = stringResource(MR.strings.download_ahead_info))
        }
    }
}
