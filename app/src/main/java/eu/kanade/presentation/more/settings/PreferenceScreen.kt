package eu.kanade.presentation.more.settings

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
                // Create Preference Group
                is Preference.PreferenceGroup -> {
                    if (!preference.visible) return@fastForEachIndexed

                    item {
                        PreferenceGroupHeader(
                            title = preference.title,
                            highlighted = preference.title == highlightKey,
                        )
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
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp),
                    ) {
                        PreferenceItem(
                            item = preference,
                            highlightKey = highlightKey,
                        )
                    }
                }
            }
        }
    }
}

// Each visible group contributes three lazy items (header, card, spacer;
// the last group has no spacer, but nothing follows it), a top-level item one.
private fun List<Preference>.findHighlightedIndex(highlightKey: String): Int {
    var index = 0
    for (preference in this) {
        when (preference) {
            is Preference.PreferenceGroup -> {
                if (!preference.visible) continue
                if (preference.title == highlightKey) return index
                if (preference.preferenceItems.any { it.title == highlightKey }) return index + 1
                index += 3
            }
            is Preference.PreferenceItem<*, *> -> {
                if (preference.title == highlightKey) return index
                index += 1
            }
        }
    }
    return -1
}
