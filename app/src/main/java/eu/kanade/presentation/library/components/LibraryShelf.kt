package eu.kanade.presentation.library.components

import android.graphics.RenderEffect
import android.graphics.RuntimeShader
import android.os.Build
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import eu.kanade.domain.ui.model.BarStyle
import eu.kanade.presentation.components.relativeDateText
import eu.kanade.presentation.manga.components.DotSeparatorText
import eu.kanade.presentation.manga.components.MangaCover
import eu.kanade.presentation.miuix.MetalEngravedText
import eu.kanade.presentation.miuix.TychiiCorners
import eu.kanade.presentation.miuix.metalIsDark
import eu.kanade.presentation.miuix.metalSurface
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.ui.library.LibraryItem
import eu.kanade.tachiyomi.ui.reader.ReaderActivity
import kotlinx.coroutines.launch
import mihon.app.di.appGraph
import mihon.icons.materialsymbols.MaterialSymbols
import mihon.icons.materialsymbols.rounded.KeyboardArrowLeft
import mihon.icons.materialsymbols.rounded.KeyboardArrowRight
import tachiyomi.domain.chapter.model.Chapter
import tachiyomi.domain.library.model.LibraryManga
import tachiyomi.domain.manga.model.Manga
import tachiyomi.domain.manga.model.MangaCover as MangaCoverModel
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.material.DISABLED_ALPHA
import tachiyomi.presentation.core.components.material.SECONDARY_ALPHA
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.util.collectAsState
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.HorizontalDivider
import top.yukonga.miuix.kmp.blur.isRuntimeShaderSupported
import kotlin.math.abs

private val FocusedCoverWidth = 124.dp
private val FocusedCoverHeight = 186.dp
private val CarouselPageSpacing = 12.dp
// Neighbours shrink to this and fade to this at one page away.
private const val NeighbourScale = 0.7f
private const val NeighbourAlpha = 0.35f
// How far neighbours tilt back in perspective (cover-flow):
// the page facing the centre turns its outer edge away.
private const val NeighbourTiltDegrees = 45f
// How high the centred cover lifts off the plank as it
// settles, and how far the backdrop drifts against a swipe.
private val CentreLift = 4.dp
private val BackdropDrift = 12.dp
// How many of the newest chapters the detail panel shows.
private const val LatestChapterCount = 5
// The carousel backdrop: how blurred the focused cover is, how
// far its colours bleed outward at the edges, and the plank the
// covers stand on. The blur was 28.dp — it re-renders every
// frame of the 250ms crossfade on swipe, so 16.dp keeps the look
// at a much lower cost.
private val BackdropBlurRadius = 16.dp
private val BackdropDispersion = 18.dp
private val PlankHeight = 6.dp

// Radial dispersion for the shelf backdrop: the middle stays
// clean and the colours bleed outward toward the edges, like
// light through a prism. Modelled on the liquid-glass lens
// dispersion shader (LiquidGlass.kt): the image is sampled
// three times, once per channel, with a radial offset that
// grows quadratically with the distance from the centre — so it
// is applied to the already-blurred image and the fringe
// survives the blur.
private const val RADIAL_DISPERSION_SHADER = """
uniform shader content;
uniform float2 size;
uniform float dispersion;

half4 main(float2 coord) {
    float2 center = size * 0.5;
    float2 d = coord - center;
    float dist = length(d) / length(center);
    float2 dir = d / max(length(d), 0.001);
    float2 offset = dir * dispersion * dist * dist;
    half4 redSample = content.eval(coord + offset);
    half4 greenSample = content.eval(coord);
    half4 blueSample = content.eval(coord - offset);
    return half4(redSample.r, greenSample.g, blueSample.b,
                 (redSample.a + greenSample.a + blueSample.a) / 3.0);
}
"""

/**
 * Carousel shelf, pinned above the focused manga's detail panel
 * in Shelf mode: the focused manga's cover sits in the middle,
 * its neighbours peeking in on both sides — smaller, dimmer and
 * tilted back in perspective toward the screen edges, standing
 * on the same plank. Swiping moves between books; the detail
 * panel below follows the focus.
 */
@Composable
internal fun LibraryShelfHeader(
    items: List<LibraryItem>,
    selection: Set<Long>,
    onClick: (LibraryManga) -> Unit,
    onLongClick: (LibraryManga) -> Unit,
    onFocusedChange: (LibraryItem?) -> Unit = {},
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
    LaunchedEffect(focused) { onFocusedChange(focused) }
    if (focused == null) return
    val focusedManga = focused.libraryManga.manga
    val selecting = selection.isNotEmpty()

    val pagerState = rememberPagerState(
        initialPage = focusedIndex,
        pageCount = { items.size },
    )
    // One soft tick per page change, like Wenku8's per-step
    // haptics — but not on first composition.
    val haptic = LocalHapticFeedback.current
    var previousPage by remember { mutableIntStateOf(focusedIndex) }
    // The pager is the source of truth while swiping; keep the
    // focused id in step with the settled page.
    LaunchedEffect(pagerState.currentPage) {
        val page = pagerState.currentPage
        if (page != previousPage) {
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            previousPage = page
        }
        if (page in items.indices) {
            focusedMangaId = items[page].libraryManga.manga.id
        }
    }

    // Loops around. A wrap jumps instantly instead of animating
    // across every page in between. A step to a neighbour rides
    // a spring with a touch of overshoot, so the page settles
    // with a little bounce instead of stopping dead.
    fun focus(index: Int) {
        val size = items.size
        if (size == 0) return
        val next = ((index % size) + size) % size
        scope.launch {
            if (next == pagerState.currentPage) return@launch
            if (abs(next - pagerState.currentPage) > 1) {
                pagerState.scrollToPage(next)
            } else {
                pagerState.animateScrollToPage(
                    next,
                    animationSpec = spring(dampingRatio = 0.7f, stiffness = 300f),
                )
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
                    // The cover's shadow on the plank: a soft
                    // ellipse under the cover's base, outside the
                    // cover's tilt so it stays flat on the shelf.
                    // Strongest and widest under the centred cover,
                    // fainter and narrower under the neighbours;
                    // the cover lifts off it as it settles. The
                    // blur is Android 12+ only — below that the
                    // ellipse stays unblurred.
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .width(FocusedCoverWidth + 16.dp)
                            .height(12.dp)
                            .graphicsLayer {
                                alpha = 1f - distance * 0.55f
                                scaleX = 0.85f + 0.15f * (1f - distance)
                            }
                            .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                            .then(
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                    Modifier.blur(8.dp)
                                } else {
                                    Modifier
                                },
                            ),
                    )
                    // Tap and long-press both live on this wrapper:
                    // the cover itself only takes an onClick, so a
                    // long-press for selection has to come from
                    // here.
                    Box(
                        modifier = Modifier
                            .shadow(4.dp, RoundedCornerShape(TychiiCorners.Cover))
                            .graphicsLayer {
                                // Shrink toward the bottom edge, so
                                // every cover keeps standing on the
                                // plank, and tilt back in perspective
                                // so it faces the centre — a
                                // cover-flow lean, not a flat row.
                                // pageOffset is signed: negative on
                                // the right, so the right page's
                                // left edge comes forward.
                                val scale = 1f - (1f - NeighbourScale) * distance
                                scaleX = scale
                                scaleY = scale
                                alpha = 1f - (1f - NeighbourAlpha) * distance
                                rotationY = -pageOffset.coerceIn(-1f, 1f) * NeighbourTiltDegrees
                                cameraDistance = 12f
                                transformOrigin = TransformOrigin(0.5f, 1f)
                                // The centred cover lifts off the
                                // plank as it settles; the floor
                                // shadow underneath stays put, so
                                // the lift reads as the cover
                                // floating above the shelf.
                                translationY = -CentreLift.toPx() * (1f - distance)
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
                            shape = RoundedCornerShape(TychiiCorners.Cover),
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
                .height(PlankHeight)
                .then(
                    if (isMetal) {
                        Modifier.metalSurface(RectangleShape, metalDark)
                    } else {
                        Modifier.background(MaterialTheme.colorScheme.surfaceContainerHigh)
                    },
                ),
        )
        // The shelf's front edge: a thin darker strip under the
        // plank, so the shelf reads as a board with thickness,
        // not a flat line.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .height(2.dp)
                .background(Color.Black.copy(alpha = 0.18f)),
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

/**
 * Shelf mode: the carousel on top, the focused manga's detail
 * panel below it. The panel follows the carousel's focus; the
 * grid is gone entirely.
 */
@Composable
internal fun LibraryShelf(
    items: List<LibraryItem>,
    selection: Set<Long>,
    onClick: (LibraryManga) -> Unit,
    onLongClick: (LibraryManga) -> Unit,
    onClickContinueReading: ((LibraryManga) -> Unit)?,
    onGetChapters: suspend (Manga) -> List<Chapter>,
    modifier: Modifier = Modifier,
) {
    var focusedItem by remember { mutableStateOf<LibraryItem?>(null) }
    val selecting = selection.isNotEmpty()
    // The backdrop needs Android 12 (RenderEffect blur); below
    // that the carousel keeps its plain background.
    val backdropEnabled = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    val focusedIndex = focusedItem?.let { item ->
        items.indexOfFirst { it.libraryManga.manga.id == item.libraryManga.manga.id }
            .takeIf { it >= 0 }
    } ?: 0
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val density = LocalDensity.current
        val backdropSizePx = remember(maxWidth, density) {
            with(density) {
                Size(maxWidth.toPx(), (FocusedCoverHeight + PlankHeight).toPx())
            }
        }
        // The backdrop drifts against the swipe and settles back
        // over the same 250ms as its crossfade. Wrap jumps (the
        // loop) skip the drift. The value is read inside the
        // backdrop's graphicsLayer, so the drift re-layouts only
        // that box instead of recomposing the backdrop.
        val driftPx = remember(density) { with(density) { BackdropDrift.toPx() } }
        val backdropDrift = remember { Animatable(0f) }
        var previousFocusedIndex by remember { mutableIntStateOf(-1) }
        LaunchedEffect(focusedIndex) {
            val prev = previousFocusedIndex
            previousFocusedIndex = focusedIndex
            if (prev == -1 || prev == focusedIndex) return@LaunchedEffect
            val delta = focusedIndex - prev
            if (abs(delta) != 1) return@LaunchedEffect
            backdropDrift.snapTo(-delta * driftPx)
            backdropDrift.animateTo(0f, tween(250))
        }
        Box(modifier = Modifier.fillMaxSize()) {
            // The backdrop sits behind the header, exactly over
            // the carousel + plank (186.dp + 6.dp); the info row
            // and the detail panel below it stay on the plain
            // page background so they stay legible.
            val item = focusedItem
            if (backdropEnabled && item != null) {
                ShelfCarouselBackdrop(
                    manga = item.libraryManga.manga,
                    sizePx = backdropSizePx,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(FocusedCoverHeight + PlankHeight)
                        .graphicsLayer { translationX = backdropDrift.value },
                )
            }
            Column(modifier = Modifier.fillMaxSize()) {
                LibraryShelfHeader(
                    items = items,
                    selection = selection,
                    onClick = onClick,
                    onLongClick = onLongClick,
                    onFocusedChange = { focusedItem = it },
                )
                Box(modifier = Modifier.weight(1f)) {
                    if (item != null) {
                        LibraryShelfDetail(
                            item = item,
                            selecting = selecting,
                            onGetChapters = onGetChapters,
                            onClickContinueReading = onClickContinueReading,
                            onOpenDetail = { onClick(item.libraryManga) },
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                }
            }
        }
    }
}

/**
 * The focused manga's detail panel: description, status and
 * counts, a start/resume button and the latest chapters. The
 * chapters load from the database when the focus changes; the
 * panel scrolls on its own under the pinned carousel, and its
 * scroll position resets on every focus change. While selecting,
 * the panel goes quiet: no start button, no chapter taps, no
 * tap-through to the detail page.
 */
@Composable
private fun LibraryShelfDetail(
    item: LibraryItem,
    selecting: Boolean,
    onGetChapters: suspend (Manga) -> List<Chapter>,
    onClickContinueReading: ((LibraryManga) -> Unit)?,
    onOpenDetail: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val manga = item.libraryManga.manga
    val context = LocalContext.current
    val barStyle by remember { context.appGraph.uiPreferences.barStyle }.collectAsState()
    val isMetal = barStyle == BarStyle.METAL
    val metalDark = metalIsDark()

    var chapters by remember(manga.id) { mutableStateOf<List<Chapter>?>(null) }
    LaunchedEffect(manga.id) {
        chapters = null
        chapters = onGetChapters(manga)
    }
    val scrollState = rememberScrollState()
    LaunchedEffect(manga.id) { scrollState.scrollTo(0) }

    val loaded = chapters
    val latest = loaded?.sortedByDescending { it.chapterNumber }?.take(LatestChapterCount).orEmpty()
    val anyRead = loaded?.any { it.read } == true
    val anyUnread = loaded?.any { !it.read } == true

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(start = 12.dp, end = 12.dp, top = 4.dp, bottom = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(enabled = !selecting) { onOpenDetail() },
            insideMargin = PaddingValues(vertical = 4.dp),
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                val description = manga.description
                if (!description.isNullOrBlank()) {
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    ProvideTextStyle(
                        value = MaterialTheme.typography.bodySmall.merge(
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        ),
                    ) {
                        Text(
                            text = mangaStatusText(manga.status),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        if (item.unreadCount > 0) {
                            DotSeparatorText()
                            Text(
                                text = "${item.unreadCount} ${stringResource(MR.strings.action_filter_unread)}",
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        if (loaded != null && loaded.isNotEmpty()) {
                            DotSeparatorText()
                            Text(
                                text = "${loaded.size} ${stringResource(MR.strings.chapters)}",
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
                if (loaded != null && loaded.isNotEmpty() && !selecting && onClickContinueReading != null) {
                    ShelfStartButton(
                        label = stringResource(
                            if (anyRead && anyUnread) {
                                MR.strings.action_resume
                            } else {
                                MR.strings.action_start
                            },
                        ),
                        isMetal = isMetal,
                        metalDark = metalDark,
                        onClick = { onClickContinueReading(item.libraryManga) },
                    )
                }
            }
        }
        if (loaded == null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp))
            }
        } else if (latest.isNotEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                insideMargin = PaddingValues(vertical = 4.dp),
            ) {
                Column {
                    Text(
                        text = stringResource(MR.strings.chapters),
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.padding(start = 16.dp, top = 12.dp, end = 16.dp, bottom = 4.dp),
                    )
                    latest.forEachIndexed { index, chapter ->
                        ShelfChapterRow(
                            chapter = chapter,
                            enabled = !selecting,
                            onClick = {
                                context.startActivity(
                                    ReaderActivity.newIntent(context, manga.id, chapter.id),
                                )
                            },
                        )
                        if (index < latest.lastIndex) {
                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * The start/resume button as a pill in the bar material's
 * colour: primaryContainer everywhere except metal, where it
 * is brushed metal with an engraved label. Solid, never glass:
 * the panel sits inside the recorded backdrop layer, so a
 * drawBackdrop here would recurse.
 */
@Composable
private fun ShelfStartButton(
    label: String,
    isMetal: Boolean,
    metalDark: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = remember { RoundedCornerShape(28.dp) }
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp)
            .then(
                if (isMetal) {
                    Modifier.metalSurface(shape, metalDark)
                } else {
                    Modifier.background(MaterialTheme.colorScheme.primaryContainer, shape)
                },
            )
            .clip(shape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        if (isMetal) {
            MetalEngravedText(
                text = label,
                style = MaterialTheme.typography.titleMedium,
                isDark = metalDark,
            )
        } else {
            Text(
                text = label,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
        }
    }
}

@Composable
private fun ShelfChapterRow(
    chapter: Chapter,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = chapter.name,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodyMedium,
                color = LocalContentColor.current.copy(alpha = if (chapter.read) DISABLED_ALPHA else 1f),
            )
            Text(
                text = relativeDateText(chapter.dateUpload),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodySmall,
                color = LocalContentColor.current.copy(alpha = SECONDARY_ALPHA),
            )
        }
    }
}

@Composable
private fun mangaStatusText(status: Long): String {
    return when (status) {
        SManga.ONGOING.toLong() -> stringResource(MR.strings.ongoing)
        SManga.COMPLETED.toLong() -> stringResource(MR.strings.completed)
        SManga.LICENSED.toLong() -> stringResource(MR.strings.licensed)
        SManga.PUBLISHING_FINISHED.toLong() -> stringResource(MR.strings.publishing_finished)
        SManga.CANCELLED.toLong() -> stringResource(MR.strings.cancelled)
        SManga.ON_HIATUS.toLong() -> stringResource(MR.strings.on_hiatus)
        else -> stringResource(MR.strings.unknown)
    }
}

/**
 * The carousel's background: the focused manga's cover, blurred
 * and dispersed so its colours bleed outward like light through
 * a prism, crossfading when the focus changes. The blur is an
 * inner layer and the dispersion an outer one, so the shader
 * samples the already-blurred image and the fringe survives the
 * blur. The dispersion needs Android 13 (RuntimeShader) and is
 * skipped below it — there the backdrop is just blurred. A thin
 * veil of the page background keeps the covers legible over a
 * vivid backdrop.
 */
@Composable
private fun ShelfCarouselBackdrop(
    manga: Manga,
    sizePx: Size,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    val dispersionPx = remember(density) { with(density) { BackdropDispersion.toPx() } }
    // The effect is created once per backdrop size, with the
    // uniforms set before creation: createRuntimeShaderEffect
    // snapshots them, so changing them later would do nothing.
    // graphicsLayer takes Compose's RenderEffect, so the
    // framework effect is wrapped with asComposeRenderEffect.
    val dispersionEffect = remember(sizePx, dispersionPx) {
        if (!isRuntimeShaderSupported()) {
            null
        } else {
            val shader = RuntimeShader(RADIAL_DISPERSION_SHADER)
            shader.setFloatUniform("size", sizePx.width, sizePx.height)
            shader.setFloatUniform("dispersion", dispersionPx)
            RenderEffect.createRuntimeShaderEffect(shader, "content")
                .asComposeRenderEffect()
        }
    }
    Box(modifier = modifier) {
        Crossfade(
            targetState = manga,
            animationSpec = tween(250),
            label = "shelf backdrop",
            modifier = Modifier
                .matchParentSize()
                .blur(BackdropBlurRadius)
                .graphicsLayer { renderEffect = dispersionEffect },
        ) { backdropManga ->
            AsyncImage(
                model = MangaCoverModel(
                    mangaId = backdropManga.id,
                    sourceId = backdropManga.source,
                    isMangaFavorite = backdropManga.favorite,
                    url = backdropManga.thumbnailUrl,
                    lastModified = backdropManga.coverLastModified,
                ),
                contentDescription = null,
                modifier = Modifier.matchParentSize(),
                contentScale = ContentScale.Crop,
            )
        }
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(MaterialTheme.colorScheme.background.copy(alpha = 0.25f)),
        )
    }
}
