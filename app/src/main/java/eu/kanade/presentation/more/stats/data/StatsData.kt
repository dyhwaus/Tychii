package eu.kanade.presentation.more.stats.data

import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.toLocalDateTime
import tachiyomi.domain.history.model.History
import kotlin.time.Instant

sealed interface StatsData {

    data class Overview(
        val libraryMangaCount: Int,
        val completedMangaCount: Int,
        val totalReadDuration: Long,
    ) : StatsData

    data class Titles(
        val globalUpdateItemCount: Int,
        val startedMangaCount: Int,
        val localMangaCount: Int,
    ) : StatsData

    data class Chapters(
        val totalChapterCount: Int,
        val readChapterCount: Int,
        val downloadCount: Int,
    ) : StatsData

    data class Trackers(
        val trackedTitleCount: Int,
        val meanScore: Double,
        val trackerCount: Int,
    ) : StatsData

    /**
     * One day of reading time, for the last-7-days chart.
     */
    data class DailyReading(
        val day: LocalDate,
        val durationMs: Long,
    )

    /**
     * One source's share of the library, for the source breakdown.
     */
    data class SourceCount(
        val name: String,
        val count: Int,
    )

    /**
     * Reading activity, computed from the history table only: per-day
     * read durations, this month's distinct manga, the share of fully
     * read completed titles, chapters read per time-of-day bucket, and
     * the library's split by source. Pages read is deliberately absent
     * — the database stores no per-page events, and that gap must not
     * be filled by guessing.
     */
    data class ReadingActivity(
        val dailyReadings: List<DailyReading>,
        val monthlyMangaCount: Int,
        val completionRate: Float,
        val timeOfDayCounts: List<Int>,
        val sourceCounts: List<SourceCount>,
    ) : StatsData
}

/**
 * Aggregates reading activity from history rows and the library's
 * source ids. History rows whose read time was reset (readAt == null)
 * carry no day, so they are left out of the daily, monthly and
 * time-of-day figures — their duration still counts in the total
 * read duration shown in the overview.
 *
 * Time-of-day buckets are [night 0-5, morning 6-11, afternoon 12-17,
 * evening 18-23], counted in chapters read. Source counts are the
 * top 5 by library size. Source names arrive as a pre-resolved map
 * because looking them up is a suspend call.
 */
fun computeReadingActivity(
    historyByManga: Map<Long, List<History>>,
    librarySources: List<Long>,
    completedCount: Int,
    sourceNames: Map<Long, String>,
    now: Instant,
    zone: TimeZone = TimeZone.currentSystemDefault(),
): StatsData.ReadingActivity {
    val today = now.toLocalDateTime(zone).date
    val rows = historyByManga.values.flatten()

    val durationsByDate = rows
        .mapNotNull { history -> history.readAt?.toLocalDateTime(zone)?.date?.let { it to history.readDuration } }
        .groupBy({ it.first }, { it.second })
        .mapValues { (_, durations) -> durations.sum() }
    val dailyReadings = (6 downTo 0).map { offset ->
        val day = today.minus(DatePeriod(days = offset))
        StatsData.DailyReading(day, durationsByDate[day] ?: 0L)
    }

    val monthlyMangaCount = historyByManga.count { (_, mangaRows) ->
        mangaRows.any { history ->
            history.readAt?.toLocalDateTime(zone)?.date?.let { it.year == today.year && it.month == today.month } == true
        }
    }

    val timeOfDayCounts = IntArray(4)
    rows.forEach { history ->
        val hour = history.readAt?.toLocalDateTime(zone)?.hour ?: return@forEach
        timeOfDayCounts[when (hour) { in 0..5 -> 0; in 6..11 -> 1; in 12..17 -> 2; else -> 3 }]++
    }

    val completionRate = if (librarySources.isEmpty()) {
        0f
    } else {
        completedCount.toFloat() / librarySources.size
    }

    val sourceCounts = librarySources
        .groupingBy { it }
        .eachCount()
        .entries
        .sortedByDescending { it.value }
        .take(5)
        .map { (sourceId, count) -> StatsData.SourceCount(sourceNames[sourceId].orEmpty(), count) }

    return StatsData.ReadingActivity(
        dailyReadings = dailyReadings,
        monthlyMangaCount = monthlyMangaCount,
        completionRate = completionRate,
        timeOfDayCounts = timeOfDayCounts.toList(),
        sourceCounts = sourceCounts,
    )
}
