package eu.kanade.presentation.library.components

import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import eu.kanade.domain.ui.model.BarStyle
import eu.kanade.presentation.manga.components.MangaCover
import eu.kanade.presentation.miuix.MetalEngravedIcon
import eu.kanade.presentation.miuix.MetalEngravedText
import eu.kanade.presentation.miuix.metalIsDark
import eu.kanade.presentation.miuix.metalSurface
import eu.kanade.presentation.theme.isMiuixGlassSupported
import mihon.app.di.appGraph
import mihon.icons.materialsymbols.MaterialSymbols
import mihon.icons.materialsymbols.roundedfilled.PlayArrow
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.BadgeGroup
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.util.collectAsState
import tachiyomi.presentation.core.util.selectedBackground
import top.yukonga.miuix.kmp.basic.HorizontalDivider
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.theme.MiuixTheme
import tachiyomi.domain.manga.model.MangaCover as MangaCoverModel

object CommonMangaItemDefaults {
    // Neko's grid gaps: 16.dp horizontal, 4.dp vertical.
    val GridHorizontalSpacer = 16.dp
    val GridVerticalSpacer = 4.dp

    @Suppress("ConstPropertyName")
    const val BrowseFavoriteCoverAlpha = 0.34f
}

private val ContinueReadingButtonSizeSmall = 28.dp
private val ContinueReadingButtonSizeLarge = 32.dp

private val ContinueReadingButtonIconSizeSmall = 16.dp
private val ContinueReadingButtonIconSizeLarge = 20.dp

private val ContinueReadingButtonListSpacing = 8.dp

private const val GRID_SELECTED_COVER_ALPHA = 0.76f

/**
 * Layout of grid list item as a PiliPlus-style card: the cover, with
 * the title on a strip over its bottom edge — frosted glass, or
 * brushed metal in metal mode.
 * Accepts null [title] for a cover-only view.
 */
@Composable
fun MangaCompactGridItem(
    coverData: MangaCoverModel,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    isSelected: Boolean = false,
    title: String? = null,
    onClickContinueReading: (() -> Unit)? = null,
    // A tap on the cover itself, separate from the card tap — the
    // Updates page opens the manga from the cover and the chapter
    // from the card. Null means the cover is not separately clickable.
    onClickCover: (() -> Unit)? = null,
    coverAlpha: Float = 1f,
    coverBadgeStart: @Composable (RowScope.() -> Unit)? = null,
    coverBadgeEnd: @Composable (RowScope.() -> Unit)? = null,
    onCoverPositioned: ((Float) -> Unit)? = null,
) {
    GridItemSelectable(
        isSelected = isSelected,
        onClick = onClick,
        onLongClick = onLongClick,
    ) {
        // In liquid glass mode a selected item reads as almost
        // transparent — the dimmed cover over the tint — so it gets
        // a frosted veil instead (below). The bar style is read here
        // so every grid that uses this card (Library, Updates,
        // History, Explore, search) gets it without threading the
        // setting through each screen.
        val context = LocalContext.current
        val barStyle by remember { context.appGraph.uiPreferences.barStyle }.collectAsState()
        val isGlass = barStyle == BarStyle.LIQUID_GLASS && isMiuixGlassSupported
        val isMetal = barStyle == BarStyle.METAL
        // The title lives on the cover now, so the card is the cover
        // itself — no surface behind it. The strip stays crisp when
        // the cover dims on select.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(2.dp, RoundedCornerShape(8.dp))
                .clip(RoundedCornerShape(8.dp)),
        ) {
            MangaGridCover(
                cover = {
                    MangaCover.Book(
                        modifier = Modifier
                            .fillMaxWidth()
                            .alpha(if (isSelected && !isGlass) GRID_SELECTED_COVER_ALPHA else coverAlpha)
                            .reportCoverCenterY(onCoverPositioned),
                        data = coverData,
                        shape = RoundedCornerShape(8.dp),
                        onClick = onClickCover,
                    )
                    if (isSelected && isGlass) {
                        // Frosted veil over the cover: milky white, no
                        // real blur — the card sits inside the page's
                        // recorded backdrop layer, where glass would
                        // recurse (same reason as the title strip).
                        // The badges draw after this, so they stay crisp.
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .background(Color.White.copy(alpha = 0.4f)),
                        )
                    }
                },
                badgesStart = coverBadgeStart,
                badgesEnd = coverBadgeEnd,
            )
            if (title != null) {
                CoverTitleStrip(
                    title = title,
                    maxLines = 2,
                    onClickContinueReading = onClickContinueReading,
                    modifier = Modifier.align(Alignment.BottomCenter),
                    isMetal = isMetal,
                )
            }
        }
    }
}

/**
 * Frosted glass strip: a translucent veil over the cover, brighter
 * at the top, with a rim light along its top edge — the launcher
 * icon's frosted disc in strip form. No real blur: the strip sits
 * inside the page's recorded backdrop layer, where glass would
 * recurse (see the floating bar). In metal mode it is a brushed
 * metal strip with the title engraved and no rim light — the metal
 * chamfer is the top edge.
 */
@Composable
private fun CoverTitleStrip(
    title: String,
    maxLines: Int,
    onClickContinueReading: (() -> Unit)?,
    modifier: Modifier = Modifier,
    isMetal: Boolean = false,
) {
    val veil = MiuixTheme.colorScheme.surface
    val metalDark = metalIsDark()
    Box(
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (isMetal) {
                    // Square corners: the card's clip rounds the
                    // strip's bottom edge to the card's 8.dp.
                    Modifier.metalSurface(RectangleShape, metalDark)
                } else {
                    Modifier.background(
                        Brush.verticalGradient(
                            colors = listOf(
                                veil.copy(alpha = 0.45f),
                                veil.copy(alpha = 0.8f),
                            ),
                        ),
                    )
                },
            ),
    ) {
        if (!isMetal) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(Color.White.copy(alpha = 0.35f)),
            )
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (isMetal) {
                MetalEngravedText(
                    text = title,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Medium,
                    ),
                    isDark = metalDark,
                    minLines = 2,
                    maxLines = maxLines,
                )
            } else {
                Text(
                    text = title,
                    modifier = Modifier.weight(1f),
                    minLines = 2,
                    maxLines = maxLines,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Medium,
                        shadow = Shadow(
                            color = veil.copy(alpha = 0.5f),
                            blurRadius = 6f,
                            offset = Offset(0f, 1f),
                        ),
                    ),
                )
            }
            if (onClickContinueReading != null) {
                ContinueReadingButton(
                    size = ContinueReadingButtonSizeLarge,
                    iconSize = ContinueReadingButtonIconSizeLarge,
                    onClick = onClickContinueReading,
                    modifier = Modifier.padding(start = 8.dp),
                    isMetal = isMetal,
                )
            }
        }
    }
}

/**
 * Common cover layout to add contents to be drawn on top of the cover.
 */
@Composable
private fun MangaGridCover(
    modifier: Modifier = Modifier,
    cover: @Composable BoxScope.() -> Unit = {},
    badgesStart: (@Composable RowScope.() -> Unit)? = null,
    badgesEnd: (@Composable RowScope.() -> Unit)? = null,
) {
    // The cover box is sized from its measured width, not aspectRatio:
    // aspectRatio on this Box came out square in the lazy grid, so the
    // portrait covers were cropped top and bottom (ContentScale.Crop).
    BoxWithConstraints(
        modifier = modifier.fillMaxWidth(),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(maxWidth / MangaCover.Book.ratio),
        ) {
            cover()
            if (badgesStart != null) {
                BadgeGroup(
                    modifier = Modifier
                        .padding(4.dp)
                        .align(Alignment.TopStart),
                    content = badgesStart,
                )
            }

            if (badgesEnd != null) {
                BadgeGroup(
                    modifier = Modifier
                        .padding(4.dp)
                        .align(Alignment.TopEnd),
                    content = badgesEnd,
                )
            }
        }
    }
}

/**
 * Wrapper for grid items to handle selection state, click and long click.
 */
@Composable
private fun GridItemSelectable(
    isSelected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = modifier
            .clip(MaterialTheme.shapes.small)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick,
            )
            .selectedOutline(isSelected = isSelected, color = MaterialTheme.colorScheme.secondary)
            .padding(4.dp),
    ) {
        val contentColor = if (isSelected) {
            MaterialTheme.colorScheme.onSecondary
        } else {
            LocalContentColor.current
        }
        CompositionLocalProvider(LocalContentColor provides contentColor) {
            content()
        }
    }
}

/**
 * @see GridItemSelectable
 */
private fun Modifier.selectedOutline(
    isSelected: Boolean,
    color: Color,
) = drawBehind { if (isSelected) drawRect(color = color) }

// Reports the cover's vertical centre in window coordinates, so the
// selection bar can animate out of the cover that was pressed. A
// no-op when no listener is attached, so Browse grids pay nothing.
private fun Modifier.reportCoverCenterY(onCoverPositioned: ((Float) -> Unit)?) =
    if (onCoverPositioned != null) {
        onGloballyPositioned { onCoverPositioned(it.boundsInWindow().center.y) }
    } else {
        this
    }

/**
 * Layout of list item.
 */
@Composable
fun MangaListItem(
    coverData: MangaCoverModel,
    title: String,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    badge: @Composable (RowScope.() -> Unit),
    isSelected: Boolean = false,
    coverAlpha: Float = 1f,
    onClickContinueReading: (() -> Unit)? = null,
    // The Library tab hides the divider so the list is one continuous
    // background; other lists keep it.
    showDivider: Boolean = true,
    onCoverPositioned: ((Float) -> Unit)? = null,
) {
    Column {
        Row(
            modifier = Modifier
                .selectedBackground(isSelected)
                .height(56.dp)
                .combinedClickable(
                    onClick = onClick,
                    onLongClick = onLongClick,
                )
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            MangaCover.Square(
                modifier = Modifier
                    .fillMaxHeight()
                    .alpha(coverAlpha)
                    .reportCoverCenterY(onCoverPositioned),
                data = coverData,
            )
            Text(
                text = title,
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .weight(1f),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodyMedium,
            )
            BadgeGroup(content = badge)
            if (onClickContinueReading != null) {
                ContinueReadingButton(
                    size = ContinueReadingButtonSizeSmall,
                    iconSize = ContinueReadingButtonIconSizeSmall,
                    onClick = onClickContinueReading,
                    modifier = Modifier.padding(start = ContinueReadingButtonListSpacing),
                )
            }
        }
        if (showDivider) {
            HorizontalDivider(modifier = Modifier.padding(start = 16.dp))
        }
    }
}

@Composable
private fun ContinueReadingButton(
    size: Dp,
    iconSize: Dp,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isMetal: Boolean = false,
) {
    val metalDark = metalIsDark()
    // On a metal strip the button is a metal disc with an engraved
    // icon; elsewhere it keeps its primaryContainer pill. The icon
    // keeps the resume colour in both, like the detail page's start
    // button.
    val iconTint = contentColorFor(MaterialTheme.colorScheme.primaryContainer)
    Box(
        modifier = modifier.then(
            if (isMetal) Modifier.metalSurface(RoundedCornerShape(12.dp), metalDark) else Modifier,
        ),
    ) {
        IconButton(
            onClick = onClick,
            modifier = Modifier.size(size),
            minWidth = size,
            minHeight = size,
            backgroundColor = if (isMetal) {
                Color.Transparent
            } else {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.9f)
            },
            cornerRadius = 12.dp,
        ) {
            if (isMetal) {
                MetalEngravedIcon(
                    imageVector = MaterialSymbols.RoundedFilled.PlayArrow,
                    contentDescription = stringResource(MR.strings.action_resume),
                    tint = iconTint,
                    isDark = metalDark,
                    modifier = Modifier.size(iconSize),
                )
            } else {
                Icon(
                    imageVector = MaterialSymbols.RoundedFilled.PlayArrow,
                    contentDescription = stringResource(MR.strings.action_resume),
                    tint = iconTint,
                    modifier = Modifier.size(iconSize),
                )
            }
        }
    }
}
