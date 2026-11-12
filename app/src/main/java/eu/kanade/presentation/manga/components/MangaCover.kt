package eu.kanade.presentation.manga.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import eu.kanade.presentation.util.rememberResourceBitmapPainter
import eu.kanade.tachiyomi.R

enum class MangaCover(val ratio: Float) {
    Square(1f / 1f),
    Book(2f / 3f),
    ;

    @Composable
    operator fun invoke(
        data: Any?,
        modifier: Modifier = Modifier,
        contentDescription: String = "",
        shape: Shape = RoundedCornerShape(12.dp),
        onClick: (() -> Unit)? = null,
        // Static glass gloss (skeuomorphic): a diagonal light sheen over
        // the cover, like laminated glass. Only used for the bookshelf
        // grid — covers are opaque, so there is no backdrop blur, just
        // this surface treatment. Static on purpose: a tilt-following
        // highlight would recompose the whole grid on every sensor update.
        glassEffect: Boolean = false,
    ) {
        Box(
            modifier = modifier
                .aspectRatio(ratio)
                .clip(shape)
                // Neko-style thin cover outline.
                .border(0.75.dp, Color(0xFF9D9D9D), shape)
                .then(
                    if (onClick != null) {
                        Modifier.clickable(
                            role = Role.Button,
                            onClick = onClick,
                        )
                    } else {
                        Modifier
                    },
                ),
        ) {
            AsyncImage(
                model = data,
                placeholder = ColorPainter(CoverPlaceholderColor),
                error = rememberResourceBitmapPainter(id = R.drawable.cover_error),
                contentDescription = contentDescription,
                modifier = Modifier.matchParentSize(),
                contentScale = ContentScale.Crop,
            )
            if (glassEffect) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(brush = CoverGlassGloss, shape = shape),
                )
            }
        }
    }
}

private val CoverPlaceholderColor = Color(0x1F888888)

// Diagonal light sheen: bright at the top-left, fading to transparent,
// with a faint dark edge at the bottom-right for physical depth.
private val CoverGlassGloss = Brush.linearGradient(
    0f to Color.White.copy(alpha = 0.18f),
    0.35f to Color.White.copy(alpha = 0.05f),
    0.7f to Color.Transparent,
    1f to Color.Black.copy(alpha = 0.08f),
)
