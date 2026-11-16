package eu.kanade.presentation.manga.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RectangleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import eu.kanade.presentation.theme.TachiyomiPreviewTheme
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.material.padding
import tachiyomi.presentation.core.i18n.pluralStringResource
import tachiyomi.presentation.core.util.secondaryItemAlpha
import top.yukonga.miuix.kmp.basic.HorizontalDivider
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun MissingChapterCountListItem(
    count: Int,
    modifier: Modifier = Modifier,
    // The card corner shape for this row: rounded on the section's outer
    // corners only (first/last row), supplied by the list.
    cardShape: Shape = RectangleShape,
) {
    // Same card dressing as the chapter rows. The alpha dims only the
    // inner content, not the card surface.
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
            .background(MiuixTheme.colorScheme.surfaceContainer, cardShape)
            .clip(cardShape),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = MaterialTheme.padding.medium,
                    vertical = MaterialTheme.padding.small,
                )
                .secondaryItemAlpha(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.padding.medium),
        ) {
            HorizontalDivider(modifier = Modifier.weight(1f))
            Text(
                text = pluralStringResource(MR.plurals.missing_chapters, count = count, count),
                style = MaterialTheme.typography.labelMedium,
            )
            HorizontalDivider(modifier = Modifier.weight(1f))
        }
    }
}

@PreviewLightDark
@Composable
private fun Preview() {
    TachiyomiPreviewTheme {
        Surface {
            MissingChapterCountListItem(count = 42)
        }
    }
}
