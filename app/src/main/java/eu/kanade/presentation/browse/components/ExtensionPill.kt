package eu.kanade.presentation.browse.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

/**
 * Status capsule for an extension (install state, obsolete, content
 * warning, private installer). A true pill: fully rounded ends, a
 * tinted background of the status color, and a medium-weight label.
 */
@Composable
internal fun ExtensionPill(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.onSurfaceVariant,
) {
    Text(
        text = text,
        modifier = modifier
            .background(color = color.copy(alpha = PILL_BACKGROUND_ALPHA), shape = CircleShape)
            .padding(horizontal = 8.dp, vertical = 3.dp),
        color = color,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        style = MaterialTheme.typography.labelSmall.copy(
            fontWeight = FontWeight.Medium,
        ),
    )
}

private const val PILL_BACKGROUND_ALPHA = 0.15f
