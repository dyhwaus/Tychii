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
        var i = 0
        while (i < items.size) {
            when (val preference = items[i]) {
                // Create Preference Group
                is Preference.PreferenceGroup -> {
                    if (preference.visible) {
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
                        if (i < items.lastIndex) {
                            item {
                                Spacer(modifier = Modifier.height(12.dp))
                            }
                        }
                    }
                    i++
                }

                // Consecutive top-level items share one card, like a group
                // without a header. Each item used to get its own card, so
                // a run of them showed a hairline gap between every two
                // boxes while grouped items sat in one continuous card.
                is Preference.PreferenceItem<*, *> -> {
                    val run = mutableListOf<Preference.PreferenceItem<*, *>>()
                    var j = i
                    while (j < items.size && items[j] is Preference.PreferenceItem<*, *>) {
                        run.add(items[j] as Preference.PreferenceItem<*, *>)
                        j++
                    }
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp),
                        ) {
                            run.forEach { item ->
                                PreferenceItem(
                                    item = item,
                                    highlightKey = highlightKey,
                                )
                            }
                        }
                    }
                    if (j < items.size) {
                        item {
                            Spacer(modifier = Modifier.height(12.dp))
                        }
                    }
                    i = j
                }
            }
        }
    }
}

// A visible group contributes three lazy items (header, card, spacer;
// two when last), a run of top-level items two (card, spacer; one when
// last). Mirrors the emission above so search highlight scrolls to the
// right card.
private fun List<Preference>.findHighlightedIndex(highlightKey: String): Int {
    var index = 0
    var i = 0
    while (i < size) {
        when (val preference = this[i]) {
            is Preference.PreferenceGroup -> {
                if (!preference.visible) {
                    i++
                    continue
                }
                if (preference.title == highlightKey) return index
                if (preference.preferenceItems.any { it.title == highlightKey }) return index + 1
                index += if (i < lastIndex) 3 else 2
                i++
            }
            is Preference.PreferenceItem<*, *> -> {
                var j = i
                while (j < size && this[j] is Preference.PreferenceItem<*, *>) {
                    if ((this[j] as Preference.PreferenceItem<*, *>).title == highlightKey) return index
                    j++
                }
                index += if (j < size) 2 else 1
                i = j
            }
        }
    }
    return -1
}
