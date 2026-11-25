package eu.kanade.presentation.more.stats

import eu.kanade.presentation.more.stats.data.StatsData
import eu.kanade.presentation.more.stats.data.computeReadingActivity
import io.kotest.matchers.shouldBe
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import org.junit.jupiter.api.Test
import tachiyomi.domain.history.model.History
import kotlin.time.Instant

class ReadingActivityTest {

    private val zone = TimeZone.of("UTC")
    private val now = Instant.parse("2026-11-25T07:00:00Z")

    private fun history(chapterId: Long, readAt: String?, durationMs: Long): History {
        return History(
            id = chapterId,
            chapterId = chapterId,
            readAt = readAt?.let { Instant.parse(it) },
            readDuration = durationMs,
        )
    }

    @Test
    fun `daily monthly buckets and sources aggregate correctly`() {
        val historyByManga = mapOf(
            1L to listOf(
                history(11, "2026-11-25T01:00:00Z", 30 * 60_000),
                history(12, "2026-11-25T08:30:00Z", 45 * 60_000),
                history(13, "2026-11-24T14:00:00Z", 60 * 60_000),
                history(14, "2026-11-20T21:00:00Z", 20 * 60_000),
                history(15, "2026-10-15T10:00:00Z", 15 * 60_000),
                history(16, null, 10 * 60_000),
            ),
            2L to listOf(
                history(21, "2026-11-23T05:59:00Z", 5 * 60_000),
                history(22, "2026-11-23T06:00:00Z", 5 * 60_000),
                history(23, "2026-11-19T12:00:00Z", 10 * 60_000),
            ),
            3L to emptyList(),
        )

        val activity = computeReadingActivity(
            historyByManga = historyByManga,
            librarySources = listOf(1L, 1L, 2L, 3L),
            completedCount = 1,
            sourceNames = mapOf(1L to "Source 1", 2L to "Source 2", 3L to "Source 3", 4L to "Source 4", 5L to "Source 5", 6L to "Source 6", 7L to "Source 7"),
            now = now,
            zone = zone,
        )

        activity.dailyReadings shouldBe listOf(
            StatsData.DailyReading(LocalDate(2026, 11, 19), 10 * 60_000L),
            StatsData.DailyReading(LocalDate(2026, 11, 20), 20 * 60_000L),
            StatsData.DailyReading(LocalDate(2026, 11, 21), 0L),
            StatsData.DailyReading(LocalDate(2026, 11, 22), 0L),
            StatsData.DailyReading(LocalDate(2026, 11, 23), 10 * 60_000L),
            StatsData.DailyReading(LocalDate(2026, 11, 24), 60 * 60_000L),
            StatsData.DailyReading(LocalDate(2026, 11, 25), 75 * 60_000L),
        )
        activity.monthlyMangaCount shouldBe 2
        activity.completionRate shouldBe 0.25f
        // Morning counts three: 08:30 and 06:00 in November, plus the
        // 10:00 row from October — the buckets cover all history.
        activity.timeOfDayCounts shouldBe listOf(2, 3, 2, 1)
        activity.sourceCounts shouldBe listOf(
            StatsData.SourceCount("Source 1", 2),
            StatsData.SourceCount("Source 2", 1),
            StatsData.SourceCount("Source 3", 1),
        )
    }

    @Test
    fun `empty library and history give zeroes`() {
        val activity = computeReadingActivity(
            historyByManga = emptyMap(),
            librarySources = emptyList(),
            completedCount = 0,
            sourceNames = mapOf(1L to "Source 1", 2L to "Source 2", 3L to "Source 3", 4L to "Source 4", 5L to "Source 5", 6L to "Source 6", 7L to "Source 7"),
            now = now,
            zone = zone,
        )

        activity.dailyReadings.map { it.durationMs } shouldBe List(7) { 0L }
        activity.dailyReadings.first().day shouldBe LocalDate(2026, 11, 19)
        activity.dailyReadings.last().day shouldBe LocalDate(2026, 11, 25)
        activity.monthlyMangaCount shouldBe 0
        activity.completionRate shouldBe 0f
        activity.timeOfDayCounts shouldBe listOf(0, 0, 0, 0)
        activity.sourceCounts shouldBe emptyList()
    }

    @Test
    fun `source counts keep only the top five`() {
        val activity = computeReadingActivity(
            historyByManga = emptyMap(),
            librarySources = listOf(1L, 1L, 1L, 1L, 1L, 2L, 2L, 2L, 2L, 3L, 3L, 3L, 4L, 4L, 5L, 6L, 7L),
            completedCount = 0,
            sourceNames = mapOf(1L to "Source 1", 2L to "Source 2", 3L to "Source 3", 4L to "Source 4", 5L to "Source 5", 6L to "Source 6", 7L to "Source 7"),
            now = now,
            zone = zone,
        )

        activity.sourceCounts shouldBe listOf(
            StatsData.SourceCount("Source 1", 5),
            StatsData.SourceCount("Source 2", 4),
            StatsData.SourceCount("Source 3", 3),
            StatsData.SourceCount("Source 4", 2),
            StatsData.SourceCount("Source 5", 1),
        )
    }
}
