package eu.kanade.presentation.library.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import eu.kanade.domain.ui.model.BarStyle
import eu.kanade.presentation.manga.components.MangaCover
import eu.kanade.presentation.miuix.metalIsDark
import eu.kanade.presentation.miuix.metalSurface
import eu.kanade.tachiyomi.ui.library.LibraryItem
import kotlinx.coroutines.launch
import mihon.app.di.appGraph
import mihon.icons.materialsymbols.MaterialSymbols
import mihon.icons.materialsymbols.rounded.KeyboardArrowLeft
import mihon.icons.materialsymbols.rounded.KeyboardArrowRight
import tachiyomi.domain.library.model.LibraryManga
import tachiyomi.domain.manga.model.MangaCover as MangaCoverModel
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.util.collectAsState

private val SpineWidth = 26.dp
private val SpineHeight = 150.dp
private val FocusedCoverWidth = 124.dp
private val SpineShape = RoundedCornerShape(4.dp)

/**
 * Wenku8-style 3D shelf, shown at the top of the Library grid in
 * Shelf mode: the category's books stand as narrow spines on a
 * plank, and the focused book's cover is pulled out above them.
 * The grid below stays the browsing surface, so this is a header,
 * not a replacement for it.
 */
@Composable
internal fun LibraryShelfHeader(
    items: List<LibraryItem>,
    selection: Set<Long>,
    onClick: (LibraryManga) -> Unit,
    onLongClick: (LibraryManga) -> Unit,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    var focusedIndex by remember { mutableIntStateOf(0) }

    val context = LocalContext.current
    val barStyle by remember { context.appGraph.uiPreferences.barStyle }.collectAsState()
    val isMetal = barStyle == BarStyle.METAL
    val metalDark = metalIsDark()

    val focused = items.getOrNull(focusedIndex.coerceIn(0, (items.size - 1).coerceAtLeast(0)))
    if (focused == null) return
    val focusedManga = focused.libraryManga.manga
    val selecting = selection.isNotEmpty()

    fun focus(index: Int) {
        val size = items.size
        if (size == 0) return
        val next = ((index % size) + size) % size
        focusedIndex = next
        scope.launch { listState.animateScrollToItem(next) }
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp),
            contentAlignment = Alignment.Center,
        ) {
            Box(modifier = Modifier.shadow(4.dp, RoundedCornerShape(8.dp))) {
                MangaCover.Book(
                    modifier = Modifier.width(FocusedCoverWidth),
                    data = MangaCoverModel(
                        mangaId = focusedManga.id,
                        sourceId = focusedManga.source,
                        isMangaFavorite = focusedManga.favorite,
                        url = focusedManga.thumbnailUrl,
                        lastModified = focusedManga.coverLastModified,
                    ),
                    shape = RoundedCornerShape(8.dp),
                    onClick = { onClick(focused.libraryManga) },
                )
            }
        }

        LazyRow(
            state = listState,
            modifier = Modifier
                .fillMaxWidth()
                .height(SpineHeight)
                .padding(top = 12.dp),
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            itemsIndexed(
                items = items,
                key = { _, item -> item.libraryManga.manga.id },
            ) { index, libraryItem ->
                val manga = libraryItem.libraryManga.manga
                ShelfSpine(
                    title = manga.title,
                    unreadCount = libraryItem.unreadCount,
                    isFocused = index == focusedIndex,
                    isSelected = manga.id in selection,
                    isMetal = isMetal,
                    metalDark = metalDark,
                    onClick = {
                        if (selecting) {
                            onClick(libraryItem.libraryManga)
                        } else {
                            focusedIndex = index
                        }
                    },
                    onLongClick = { onLongClick(libraryItem.libraryManga) },
                )
            }
        }

        // The plank the spines stand on.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .height(6.dp)
                .then(
                    if (isMetal) {
                        Modifier.metalSurface(RectangleShape, metalDark)
                    } else {
                        Modifier.background(MaterialTheme.colorScheme.surfaceContainerHigh)
                    },
                ),
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 4.dp, end = 4.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = { focus(focusedIndex - 1) }) {
                Icon(
                    imageVector = MaterialSymbols.Rounded.KeyboardArrowLeft,
                    contentDescription = stringResource(MR.strings.action_shelf_previous),
                )
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 4.dp),
            ) {
                Text(
                    text = focusedManga.title,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = focusedManga.author ?: focused.sourceName,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                text = "${focusedIndex + 1} / ${items.size}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            IconButton(onClick = { focus(focusedIndex + 1) }) {
                Icon(
                    imageVector = MaterialSymbols.Rounded.KeyboardArrowRight,
                    contentDescription = stringResource(MR.strings.action_shelf_next),
                )
            }
        }
    }
}

@Composable
private fun ShelfSpine(
    title: String,
    unreadCount: Long,
    isFocused: Boolean,
    isSelected: Boolean,
    isMetal: Boolean,
    metalDark: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .width(SpineWidth)
            .height(SpineHeight)
            .then(if (isFocused) Modifier.offset(y = (-8).dp) else Modifier)
            .clip(SpineShape)
            .then(
                if (isMetal) {
                    Modifier.metalSurface(SpineShape, metalDark)
                } else {
                    Modifier.background(
                        when {
                            isSelected -> MaterialTheme.colorScheme.secondaryContainer
                            isFocused -> MaterialTheme.colorScheme.surfaceContainerHigh
                            else -> MaterialTheme.colorScheme.surfaceContainer
                        },
                    )
                },
            )
            .border(
                width = if (isFocused || isSelected) 1.dp else 0.75.dp,
                color = when {
                    isSelected -> MaterialTheme.colorScheme.secondary
                    isFocused -> MaterialTheme.colorScheme.primary
                    isMetal -> Color.Transparent
                    else -> MaterialTheme.colorScheme.outline
                },
                shape = SpineShape,
            )
            .combinedClickable(onClick = onClick, onLongClick = onLongClick),
    ) {
        // Vertical title: laid out horizontally, then turned
        // clockwise so it reads top to bottom. requiredWidth keeps
        // the long layout from being clamped to the spine's width.
        Text(
            text = title,
            modifier = Modifier
                .align(Alignment.Center)
                .requiredWidth(SpineHeight - 24.dp)
                .rotate(90f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        if (unreadCount > 0) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 6.dp)
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary),
            )
        }
    }
}
