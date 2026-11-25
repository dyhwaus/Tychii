package eu.kanade.presentation.more.stats

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import eu.kanade.presentation.more.stats.components.StatsItem
import eu.kanade.presentation.more.stats.components.StatsOverviewItem
import eu.kanade.presentation.more.stats.data.StatsData
import eu.kanade.presentation.util.toDurationString
import mihon.icons.materialsymbols.MaterialSymbols
import mihon.icons.materialsymbols.rounded.CollectionsBookmark
import mihon.icons.materialsymbols.rounded.LocalLibrary
import mihon.icons.materialsymbols.rounded.Schedule
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.SectionCard
import tachiyomi.presentation.core.components.material.padding
import tachiyomi.presentation.core.i18n.stringResource
import java.util.Locale
import kotlin.time.DurationUnit
import kotlin.time.toDuration

@Composable
fun StatsScreenContent(
    state: StatsScreenState.Success,
    paddingValues: PaddingValues,
) {
    LazyColumn(
        contentPadding = paddingValues,
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.padding.small),
    ) {
        item {
            OverviewSection(state.overview)
        }
        item {
            TitlesStats(state.titles)
        }
        item {
            ChapterStats(state.chapters)
        }
        item {
            TrackerStats(state.trackers)
        }
        item {
            ReadingActivitySection(state.activity)
        }
    }
}

@Composable
private fun LazyItemScope.OverviewSection(
    data: StatsData.Overview,
) {
    val none = stringResource(MR.strings.none)
    val context = LocalContext.current
    val readDurationString = remember(data.totalReadDuration) {
        data.totalReadDuration
            .toDuration(DurationUnit.MILLISECONDS)
            .toDurationString(context, fallback = none)
    }
    SectionCard(MR.strings.label_overview_section) {
        Row(
            modifier = Modifier.height(IntrinsicSize.Min),
        ) {
            StatsOverviewItem(
                title = data.libraryMangaCount.toString(),
                subtitle = stringResource(MR.strings.in_library),
                icon = MaterialSymbols.Rounded.CollectionsBookmark,
            )
            StatsOverviewItem(
                title = readDurationString,
                subtitle = stringResource(MR.strings.label_read_duration),
                icon = MaterialSymbols.Rounded.Schedule,
            )
            StatsOverviewItem(
                title = data.completedMangaCount.toString(),
                subtitle = stringResource(MR.strings.label_completed_titles),
                icon = MaterialSymbols.Rounded.LocalLibrary,
            )
        }
    }
}

@Composable
private fun LazyItemScope.TitlesStats(
    data: StatsData.Titles,
) {
    SectionCard(MR.strings.label_titles_section) {
        Row {
            StatsItem(
                data.globalUpdateItemCount.toString(),
                stringResource(MR.strings.label_titles_in_global_update),
            )
            StatsItem(
                data.startedMangaCount.toString(),
                stringResource(MR.strings.label_started),
            )
            StatsItem(
                data.localMangaCount.toString(),
                stringResource(MR.strings.label_local),
            )
        }
    }
}

@Composable
private fun LazyItemScope.ChapterStats(
    data: StatsData.Chapters,
) {
    SectionCard(MR.strings.chapters) {
        Row {
            StatsItem(
                data.totalChapterCount.toString(),
                stringResource(MR.strings.label_total_chapters),
            )
            StatsItem(
                data.readChapterCount.toString(),
                stringResource(MR.strings.label_read_chapters),
            )
            StatsItem(
                data.downloadCount.toString(),
                stringResource(MR.strings.label_downloaded),
            )
        }
    }
}

@Composable
private fun LazyItemScope.TrackerStats(
    data: StatsData.Trackers,
) {
    val notApplicable = stringResource(MR.strings.not_applicable)
    val meanScoreStr = remember(data.trackedTitleCount, data.meanScore) {
        if (data.trackedTitleCount > 0 && !data.meanScore.isNaN()) {
            // All other numbers are localized in English
            "%.2f ★".format(Locale.ENGLISH, data.meanScore)
        } else {
            notApplicable
        }
    }
    SectionCard(MR.strings.label_tracker_section) {
        Row {
            StatsItem(
                data.trackedTitleCount.toString(),
                stringResource(MR.strings.label_tracked_titles),
            )
            StatsItem(
                meanScoreStr,
                stringResource(MR.strings.label_mean_score),
            )
            StatsItem(
                data.trackerCount.toString(),
                stringResource(MR.strings.label_used),
            )
        }
    }
}

@Composable
private fun LazyItemScope.ReadingActivitySection(
    data: StatsData.ReadingActivity,
) {
    val rateStr = remember(data.completionRate) {
        "%.0f%%".format(Locale.ENGLISH, data.completionRate * 100)
    }
    SectionCard(MR.strings.label_reading_activity_section) {
        Row {
            StatsItem(
                data.monthlyMangaCount.toString(),
                stringResource(MR.strings.label_read_this_month),
            )
            StatsItem(
                rateStr,
                stringResource(MR.strings.label_completion_rate),
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
        DailyReadChart(data.dailyReadings)
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = stringResource(MR.strings.label_time_of_day),
            style = MaterialTheme.typography.titleSmall,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            val timeLabels = listOf(
                MR.strings.label_time_night,
                MR.strings.label_time_morning,
                MR.strings.label_time_afternoon,
                MR.strings.label_time_evening,
            )
            val maxCount = data.timeOfDayCounts.maxOrNull() ?: 0
            data.timeOfDayCounts.forEachIndexed { index, count ->
                HorizontalBarRow(
                    label = stringResource(timeLabels[index]),
                    valueText = count.toString(),
                    fraction = if (maxCount > 0) count.toFloat() / maxCount else 0f,
                )
            }
        }
        if (data.sourceCounts.isNotEmpty()) {
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = stringResource(MR.strings.label_source_distribution),
                style = MaterialTheme.typography.titleSmall,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                val maxCount = data.sourceCounts.maxOf { it.count }
                data.sourceCounts.forEach { source ->
                    HorizontalBarRow(
                        label = source.name,
                        valueText = source.count.toString(),
                        fraction = source.count.toFloat() / maxCount,
                        labelWidth = 96.dp,
                    )
                }
            }
        }
    }
}

@Composable
private fun DailyReadChart(readings: List<StatsData.DailyReading>) {
    val context = LocalContext.current
    val none = stringResource(MR.strings.none)
    val totalMs = readings.sumOf { it.durationMs }
    val totalStr = remember(totalMs) {
        totalMs.toDuration(DurationUnit.MILLISECONDS).toDurationString(context, fallback = none)
    }
    val maxMs = readings.maxOfOrNull { it.durationMs } ?: 0L
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(MR.strings.label_last_7_days_read_duration),
                style = MaterialTheme.typography.titleSmall,
            )
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = totalStr,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            readings.forEach { reading ->
                val valueStr = reading.durationMs
                    .toDuration(DurationUnit.MILLISECONDS)
                    .toDurationString(context, fallback = "")
                val fraction = if (maxMs > 0L) reading.durationMs.toFloat() / maxMs else 0f
                val barHeight = if (reading.durationMs == 0L) {
                    0.dp
                } else {
                    (64.dp * fraction).coerceAtLeast(2.dp)
                }
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = valueStr,
                        style = MaterialTheme.typography.labelSmall,
                        maxLines = 1,
                    )
                    Box(
                        modifier = Modifier
                            .height(64.dp)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.BottomCenter,
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(barHeight)
                                .background(
                                    color = MaterialTheme.colorScheme.primary,
                                    shape = RoundedCornerShape(2.dp),
                                ),
                        )
                    }
                    Text(
                        text = "${reading.day.monthNumber}-${reading.day.dayOfMonth}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun HorizontalBarRow(
    label: String,
    valueText: String,
    fraction: Float,
    labelWidth: Dp = 48.dp,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.width(labelWidth),
        )
        Box(
            modifier = Modifier
                .weight(1f)
                .height(8.dp)
                .background(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(4.dp),
                ),
        ) {
            if (fraction > 0f) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(fraction)
                        .fillMaxHeight()
                        .background(
                            color = MaterialTheme.colorScheme.primary,
                            shape = RoundedCornerShape(4.dp),
                        ),
                )
            }
        }
        Text(
            text = valueText,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.End,
            modifier = Modifier.width(28.dp),
        )
    }
}
