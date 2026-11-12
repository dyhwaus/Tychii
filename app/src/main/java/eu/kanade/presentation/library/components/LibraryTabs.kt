package eu.kanade.presentation.library.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import eu.kanade.presentation.category.visualName
import tachiyomi.domain.category.model.Category
import tachiyomi.presentation.core.components.material.TabText
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
internal fun LibraryTabs(
    categories: List<Category>,
    pagerState: PagerState,
    getItemCountForCategory: (Category) -> Int?,
    onTabItemClick: (Int) -> Unit,
) {
    val currentPageIndex = pagerState.currentPage.coerceAtMost(categories.lastIndex)
    // No divider and no container color: the tab row must not draw a
    // line or a band across the page — the page is one continuous
    // background with only the floating title capsule above it.
    // No indicator either: the selected category is a filled capsule
    // instead, echoing the title capsule (same Miuix surfaceContainer
    // fill, full pill radius).
    PrimaryScrollableTabRow(
        selectedTabIndex = currentPageIndex,
        edgePadding = 0.dp,
        containerColor = Color.Transparent,
        divider = {},
        indicator = {},
    ) {
        categories.forEachIndexed { index, category ->
            val selected = currentPageIndex == index
            Tab(
                selected = selected,
                onClick = { onTabItemClick(index) },
                modifier = Modifier
                    .padding(horizontal = 4.dp, vertical = 4.dp)
                    .clip(RoundedCornerShape(50))
                    .background(
                        if (selected) {
                            MiuixTheme.colorScheme.surfaceContainer
                        } else {
                            Color.Transparent
                        },
                    ),
                text = {
                    TabText(
                        text = category.visualName,
                        badgeCount = getItemCountForCategory(category),
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                    )
                },
                selectedContentColor = MiuixTheme.colorScheme.onSurface,
                unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
