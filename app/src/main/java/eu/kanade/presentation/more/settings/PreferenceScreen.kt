package eu.kanade.presentation.more.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEachIndexed
import eu.kanade.presentation.more.settings.screen.SearchableSettings
import eu.kanade.presentation.more.settings.widget.PreferenceGroupHeader
import kotlinx.coroutines.delay
import tachiyomi.presentation.core.components.ScrollbarLazyColumn
import top.yukonga.miuix.kmp.basic.Card
import kotlin.time.Duration.Companion.seconds

/**
 * Preference Screen composable which contains a list of [Preference] items
 * @param items [Preference] items which should be displayed on the preference screen. An item can be a single [PreferenceItem] or a group ([Preference.PreferenceGroup])
 * @param modifier [Modifier] to be applied to the preferenceScreen layout
 */
@Composable
fun PreferenceScreen(
    items: List<Preference>,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
) {
    val state = rememberLazyListState()
    val highlightKey = SearchableSettings.highlightKey
    if (highlightKey != null) {
        LaunchedEffect(Unit) {
            val i = items.findHighlightedIndex(highlightKey)
            if (i >= 0) {
                delay(0.5.seconds)
                state.animateScrollToItem(i)
            }
            SearchableSettings.highlightKey = null
        }
    }

    ScrollbarLazyColumn(
        modifier = modifier,
        state = state,
        contentPadding = contentPadding,
    ) {
        items.fastForEachIndexed { i, preference ->
            when (preference) {
                // Create Preference Group: a header above a Miuix Card
                // holding the group's rows, like KernelSU's settings.
                is Preference.PreferenceGroup -> {
                    if (!preference.visible) return@fastForEachIndexed

                    item {
                        Column {
                            PreferenceGroupHeader(
                                title = preference.title,
                                highlighted = preference.title == highlightKey,
                            )
                        }
                    }
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp),
                        ) {
                            preference.preferenceItems.forEach { item ->
                                PreferenceItem(
                                    item = item,
                                    highlightKey = highlightKey,
                                )
                            }
                        }
                    }
                    item {
                        if (i < items.lastIndex) {
                            Spacer(modifier = Modifier.height(12.dp))
                        }
                    }
                }

                // Create Preference Item
                is Preference.PreferenceItem<*, *> -> item {
                    PreferenceItem(
                        item = preference,
                        highlightKey = highlightKey,
                    )
                }
            }
        }
    }
}

// The lazy list now has, per visible group, a header item, a single Card
// item holding all of the group's rows, and a spacer item (except after
// the last group). A highlighted row scrolls to its group's Card.
private fun List<Preference>.findHighlightedIndex(highlightKey: String): Int {
    var lazyIndex = 0
    forEachIndexed { i, preference ->
        when (preference) {
            is Preference.PreferenceGroup -> {
                if (!preference.visible) return@forEachIndexed
                if (preference.title == highlightKey) return lazyIndex
                lazyIndex++
                val cardIndex = lazyIndex
                if (preference.preferenceItems.any { it.title == highlightKey }) {
                    return cardIndex
                }
                lazyIndex++
                if (i < lastIndex) lazyIndex++
            }
            is Preference.PreferenceItem<*, *> -> {
                if (preference.title == highlightKey) return lazyIndex
                lazyIndex++
            }
        }
    }
    return -1
}
