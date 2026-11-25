package eu.kanade.presentation.library.components

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * One index section: every title bucketing to [key], starting at [firstItemIndex].
 */
data class LibraryIndexSection(
    val key: String,
    val firstItemIndex: Int,
)

// Gojuuon rows, each with its hiragana and katakana members: base kana,
// small kana, voiced (dakuten) and half-voiced (handakuten) forms.
// Titles starting with kanji, digits or anything else fall into "#":
// there is no reading to bucket them by.
private val KANA_ROWS = listOf(
    "あ" to "あいうえおぁぃぅぇぉアイウエオァィゥェォ",
    "か" to "かきくけこがぎぐげごカキクケコガギグゲゴ",
    "さ" to "さしすせそざじずぜぞサシスセソザジズゼゾ",
    "た" to "たちつてとだぢづでどっタチツテトダヂヅデドッ",
    "な" to "なにぬねのナニヌネノ",
    "は" to "はひふへほばびぶべぼぱぴぷぺぽハヒフヘホバビブベボパピプペポ",
    "ま" to "まみむめもマミムメモ",
    "や" to "やゆよゃゅょヤユヨャュョ",
    "ら" to "らりるれろラリルレロ",
    "わ" to "わをんゎゐゑワヲンヮヰヱ",
)

private val KANA_ROW_BY_CHAR: Map<Char, String> = buildMap {
    for ((label, chars) in KANA_ROWS) {
        for (ch in chars) put(ch, label)
    }
}

/**
 * The index bucket for a title: A-Z for Latin initials, the gojuuon row
 * for kana initials, "#" for everything else (kanji, digits, symbols).
 */
internal fun sectionKeyForTitle(title: String): String {
    val first = title.firstOrNull { !it.isWhitespace() } ?: return "#"
    if (first in 'A'..'Z' || first in 'a'..'z') return first.uppercaseChar().toString()
    KANA_ROW_BY_CHAR[first]?.let { return it }
    return "#"
}

/**
 * Sections for an already-sorted title list, in first-appearance order:
 * each key appears once, at its first item, so the rail follows the list
 * even if the sort interleaves buckets.
 */
internal fun libraryIndexSections(titles: List<String>): List<LibraryIndexSection> {
    val firstIndexByKey = linkedMapOf<String, Int>()
    titles.forEachIndexed { index, title ->
        firstIndexByKey.putIfAbsent(sectionKeyForTitle(title), index)
    }
    return firstIndexByKey.map { (key, index) -> LibraryIndexSection(key, index) }
}

/**
 * A slim index rail on the right edge of the library: one letter per
 * section, tap or drag to jump. The 20.dp width matches the floating
 * action capsule's end padding, so the two never overlap. The letters
 * sit in a slim pill so they stay legible over any cover, and the pill
 * has no vertical padding: the touch-to-section mapping assumes the
 * touch area is exactly as tall as the letters.
 */
@Composable
internal fun LibraryIndexRail(
    sections: List<LibraryIndexSection>,
    onJumpToSection: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    var activeKey by remember { mutableStateOf<String?>(null) }
    var railHeightPx by remember { mutableIntStateOf(0) }

    fun selectAt(y: Float) {
        if (railHeightPx <= 0 || sections.isEmpty()) return
        val index = ((y / railHeightPx) * sections.size).toInt().coerceIn(0, sections.lastIndex)
        val section = sections[index]
        activeKey = section.key
        onJumpToSection(section.firstItemIndex)
    }

    Box(modifier = modifier.fillMaxSize()) {
        // The position bubble: the current section key, big, in the
        // middle of the screen while the rail is being dragged.
        activeKey?.let { key ->
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .background(
                        MiuixTheme.colorScheme.surfaceContainer.copy(alpha = 0.92f),
                        RoundedCornerShape(16.dp),
                    )
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = key,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = MiuixTheme.colorScheme.primary,
                )
            }
        }

        Column(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .width(20.dp)
                .onSizeChanged { railHeightPx = it.height }
                .pointerInput(sections) {
                    awaitPointerEventScope {
                        while (true) {
                            val down = awaitFirstDown(requireUnconsumed = false)
                            down.consume()
                            selectAt(down.position.y)
                            // Track the drag until the finger lifts.
                            while (true) {
                                val event = awaitPointerEvent()
                                val change = event.changes.firstOrNull() ?: break
                                if (!change.pressed) break
                                change.consume()
                                selectAt(change.position.y)
                            }
                            activeKey = null
                        }
                    }
                },
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier
                    .width(16.dp)
                    .background(
                        MiuixTheme.colorScheme.surfaceContainer.copy(alpha = 0.85f),
                        RoundedCornerShape(8.dp),
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    for (section in sections) {
                        val isActive = section.key == activeKey
                        Text(
                            text = section.key,
                            fontSize = 10.sp,
                            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                            color = if (isActive) {
                                MiuixTheme.colorScheme.primary
                            } else {
                                MiuixTheme.colorScheme.onSurface.copy(alpha = 0.65f)
                            },
                        )
                    }
                }
            }
        }
    }
}
