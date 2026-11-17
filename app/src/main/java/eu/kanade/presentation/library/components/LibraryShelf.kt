package eu.kanade.presentation.library.components

import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
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
import kotlin.math.abs

private val FocusedCoverWidth = 124.dp
private val FocusedCoverHeight = 186.dp
private val CarouselPageSpacing = 12.dp
// Neighbours shrink to this and fade to this at one page away.
private const val NeighbourScale = 0.78f
private const val NeighbourAlpha = 0.35f

/**
 * Carousel shelf, pinned above the Library grid in Shelf mode:
 * the focused manga's cover sits in the middle, its neighbours
 * peeking in on both sides — smaller and dimmer toward the
 * screen edges, standing on the same plank. Swiping moves
 * between books; the grid below stays the browsing surface.
 */
@Composable
internal fun LibraryShelfHeader(
    items: List<LibraryItem>,
    selection: Set<Long>,
    onClick: (LibraryManga) -> Unit,
    onLongClick: (LibraryManga) -> Unit,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    // Focus is a manga id, not an index: a sort or filter change
    // keeps the same book focused, and a book that left the list
    // falls back to the first one instead of pointing at whatever
    // slid into the old index. -1 means "first item".
    var focusedMangaId by remember { mutableLongStateOf(-1L) }
    val focusedIndex = if (focusedMangaId == -1L) {
        0
    } else {
        items.indexOfFirst { it.libraryManga.manga.id == focusedMangaId }
            .takeIf { it >= 0 } ?: 0
    }

    val context = LocalContext.current
    val barStyle by remember { context.appGraph.uiPreferences.barStyle }.collectAsState()
    val isMetal = barStyle == BarStyle.METAL
    val metalDark = metalIsDark()

    val focused = items.getOrNull(focusedIndex)
    if (focused == null) return
    val focusedManga = focused.libraryManga.manga
    val selecting = selection.isNotEmpty()

    val pagerState = rememberPagerState(
        initialPage = focusedIndex,
        pageCount = { items.size },
    )
    // The pager is the source of truth while swiping; keep the
    // focused id in step with the settled page.
    LaunchedEffect(pagerState.currentPage) {
        val page = pagerState.currentPage
        if (page in items.indices) {
            focusedMangaId = items[page].libraryManga.manga.id
        }
    }

    // Loops around. A wrap jumps instantly instead of animating
    // across every page in between.
    fun focus(index: Int) {
        val size = items.size
        if (size == 0) return
        val next = ((index % size) + size) % size
        scope.launch {
            if (next == pagerState.currentPage) return@launch
            if (abs(next - pagerState.currentPage) > 1) {
                pagerState.scrollToPage(next)
            } else {
                pagerState.animateScrollToPage(next)
            }
        }
    }

    Column(modifier = modifier.fillMaxWidth()) {
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            // Half the leftover width on each side, so the focused
            // page sits dead centre with one neighbour peeking in
            // on each side.
            val sidePadding = ((maxWidth - FocusedCoverWidth) / 2).coerceAtLeast(0.dp)
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(FocusedCoverHeight),
                contentPadding = PaddingValues(horizontal = sidePadding),
                pageSpacing = CarouselPageSpacing,
            ) { page ->
                val libraryItem = items.getOrNull(page) ?: return@HorizontalPager
                val manga = libraryItem.libraryManga.manga
                // 0 on the centred page, ±1 on the neighbours,
                // fractional while dragging.
                val pageOffset = (pagerState.currentPage - page) +
                    pagerState.currentPageOffsetFraction
                val distance = abs(pageOffset).coerceIn(0f, 1f)
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.BottomCenter,
                ) {
                    // Tap and long-press both live on this wrapper:
                    // the cover itself only takes an onClick, so a
                    // long-press for selection has to come from
                    // here.
                    Box(
                        modifier = Modifier
                            .shadow(4.dp, RoundedCornerShape(8.dp))
                            .graphicsLayer {
                                // Shrink toward the bottom edge, so
                                // every cover keeps standing on the
                                // plank.
                                val scale = 1f - (1f - NeighbourScale) * distance
                                scaleX = scale
                                scaleY = scale
                                alpha = 1f - (1f - NeighbourAlpha) * distance
                                transformOrigin = TransformOrigin(0.5f, 1f)
                            }
                            .combinedClickable(
                                onClick = {
                                    when {
                                        selecting -> onClick(libraryItem.libraryManga)
                                        page == pagerState.currentPage -> onClick(libraryItem.libraryManga)
                                        else -> focus(page)
                                    }
                                },
                                onLongClick = { onLongClick(libraryItem.libraryManga) },
                            ),
                    ) {
                        MangaCover.Book(
                            modifier = Modifier.width(FocusedCoverWidth),
                            data = MangaCoverModel(
                                mangaId = manga.id,
                                sourceId = manga.source,
                                isMangaFavorite = manga.favorite,
                                url = manga.thumbnailUrl,
                                lastModified = manga.coverLastModified,
                            ),
                            shape = RoundedCornerShape(8.dp),
                        )
                    }
                }
            }
        }

        // The plank the covers stand on.
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
