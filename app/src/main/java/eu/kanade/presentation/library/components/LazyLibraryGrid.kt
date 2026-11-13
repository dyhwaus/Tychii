package eu.kanade.presentation.library.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridScope
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import tachiyomi.presentation.core.util.plus

/**
 * Waterfall (masonry) grid: a staggered grid, so cards of different
 * heights (cover + a 1- or 2-line title) flow into two uneven columns
 * instead of lining up in uniform rows. Only the title capsule, the
 * bottom bar and the floating action capsule float above it.
 */
@Composable
internal fun LazyLibraryGrid(
    modifier: Modifier = Modifier,
    columns: Int,
    contentPadding: PaddingValues,
    content: LazyStaggeredGridScope.() -> Unit,
) {
    // Plain grid, no fast-scroll thumb: the thumb sat on the right edge
    // over the covers, next to the floating action capsule.
    LazyVerticalStaggeredGrid(
        columns = if (columns == 0) StaggeredGridCells.Adaptive(128.dp) else StaggeredGridCells.Fixed(columns),
        modifier = modifier,
        contentPadding = contentPadding + PaddingValues(8.dp),
        verticalItemSpacing = CommonMangaItemDefaults.GridVerticalSpacer,
        horizontalArrangement = Arrangement.spacedBy(CommonMangaItemDefaults.GridHorizontalSpacer),
        content = content,
    )
}

internal fun LazyStaggeredGridScope.globalSearchItem(
    searchQuery: String?,
    onGlobalSearchClicked: () -> Unit,
) {
    if (!searchQuery.isNullOrEmpty()) {
        item(
            span = StaggeredGridItemSpan.FullLine,
            contentType = "library_global_search_item",
        ) {
            GlobalSearchItem(
                searchQuery = searchQuery,
                onClick = onGlobalSearchClicked,
            )
        }
    }
}
