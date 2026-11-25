package eu.kanade.presentation.library

import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.source.model.UpdateStrategy
import eu.kanade.tachiyomi.ui.library.LibraryItem
import eu.kanade.tachiyomi.ui.library.SmartLibraryCategory
import io.kotest.matchers.shouldBe
import kotlinx.serialization.json.JsonObject
import org.junit.jupiter.api.Test
import tachiyomi.domain.library.model.LibraryManga
import tachiyomi.domain.manga.model.Manga
import kotlin.time.Duration.Companion.days
import kotlin.time.Instant

class SmartLibraryCategoryTest {

    // A fixed "now" so the day lines are exact, not wall-clock dependent.
    private val now = Instant.parse("2026-11-25T07:00:00Z")

    @Test
    fun `reading matches started manga with unread chapters read recently`() {
        val item = item(readCount = 3, lastRead = now - 1.days)
        SmartLibraryCategory.READING.matches(item, now) shouldBe true
        SmartLibraryCategory.PAUSED.matches(item, now) shouldBe false
    }

    @Test
    fun `reading ignores never started and fully read manga`() {
        SmartLibraryCategory.READING.matches(item(readCount = 0), now) shouldBe false
        SmartLibraryCategory.READING.matches(item(readCount = 10), now) shouldBe false
    }

    @Test
    fun `paused matches started manga untouched for a month`() {
        val item = item(readCount = 3, lastRead = now - 40.days)
        SmartLibraryCategory.PAUSED.matches(item, now) shouldBe true
        SmartLibraryCategory.READING.matches(item, now) shouldBe false
    }

    @Test
    fun `paused ignores started manga with no recorded last read`() {
        val item = item(readCount = 3, lastRead = null)
        SmartLibraryCategory.PAUSED.matches(item, now) shouldBe false
        // Its staleness cannot be proven, so it still counts as reading.
        SmartLibraryCategory.READING.matches(item, now) shouldBe true
    }

    @Test
    fun `reading and paused split exactly on the thirty day line`() {
        val item = item(readCount = 3, lastRead = now - 30.days)
        SmartLibraryCategory.READING.matches(item, now) shouldBe false
        SmartLibraryCategory.PAUSED.matches(item, now) shouldBe true
    }

    @Test
    fun `long unread matches never started manga added over a week ago`() {
        val item = item(readCount = 0, favoriteAt = now - 10.days)
        SmartLibraryCategory.LONG_UNREAD.matches(item, now) shouldBe true
        // Added recently: it has not had time to go stale yet.
        val fresh = item(readCount = 0, favoriteAt = now - 1.days)
        SmartLibraryCategory.LONG_UNREAD.matches(fresh, now) shouldBe false
        // Started manga is reading or paused, never long unread.
        val started = item(readCount = 3, favoriteAt = now - 10.days)
        SmartLibraryCategory.LONG_UNREAD.matches(started, now) shouldBe false
    }

    @Test
    fun `recently updated matches manga updated within a week`() {
        SmartLibraryCategory.RECENTLY_UPDATED.matches(item(lastUpdate = now - 3.days), now) shouldBe true
        SmartLibraryCategory.RECENTLY_UPDATED.matches(item(lastUpdate = now - 10.days), now) shouldBe false
        SmartLibraryCategory.RECENTLY_UPDATED.matches(item(lastUpdate = null), now) shouldBe false
    }

    @Test
    fun `recently added matches manga added within a week`() {
        SmartLibraryCategory.RECENTLY_ADDED.matches(item(favoriteAt = now - 2.days), now) shouldBe true
        SmartLibraryCategory.RECENTLY_ADDED.matches(item(favoriteAt = now - 20.days), now) shouldBe false
        SmartLibraryCategory.RECENTLY_ADDED.matches(item(favoriteAt = null), now) shouldBe false
    }

    @Test
    fun `completed matches completed status only`() {
        val completed = item(status = SManga.COMPLETED.toLong())
        SmartLibraryCategory.COMPLETED.matches(completed, now) shouldBe true
        SmartLibraryCategory.COMPLETED.matches(item(status = SManga.ONGOING.toLong()), now) shouldBe false
    }

    @Test
    fun `downloaded matches local or downloaded manga`() {
        SmartLibraryCategory.DOWNLOADED.matches(item(isLocal = true), now) shouldBe true
        SmartLibraryCategory.DOWNLOADED.matches(item(downloadCount = 5), now) shouldBe true
        SmartLibraryCategory.DOWNLOADED.matches(item(), now) shouldBe false
    }

    private fun item(
        status: Long = SManga.ONGOING.toLong(),
        lastUpdate: Instant? = null,
        favoriteAt: Instant? = null,
        totalChapters: Long = 10,
        readCount: Long = 0,
        lastRead: Instant? = null,
        downloadCount: Int = 0,
        isLocal: Boolean = false,
    ): LibraryItem {
        val manga = Manga(
            id = 1L,
            source = 1L,
            lastUpdate = lastUpdate,
            nextUpdate = null,
            fetchInterval = 0,
            favoriteAt = favoriteAt,
            viewerFlags = 0L,
            chapterFlags = 0L,
            coverLastModified = null,
            url = "/manga",
            title = "Test",
            artist = null,
            author = null,
            description = null,
            genre = null,
            status = status,
            thumbnailUrl = null,
            updateStrategy = UpdateStrategy.ALWAYS_UPDATE,
            initialized = true,
            notes = "",
            memo = JsonObject(emptyMap()),
        )
        return LibraryItem(
            libraryManga = LibraryManga(
                manga = manga,
                categories = emptyList(),
                totalChapters = totalChapters,
                readCount = readCount,
                bookmarkCount = 0L,
                latestUpload = null,
                chapterFetchedAt = null,
                lastRead = lastRead,
            ),
            downloadCount = downloadCount,
            unreadCount = totalChapters - readCount,
            isLocal = isLocal,
            sourceName = "Test",
            sourceLanguage = "en",
            badges = LibraryItem.Badges(
                downloadCount = downloadCount,
                unreadCount = totalChapters - readCount,
                isLocal = isLocal,
                sourceLanguage = "en",
            ),
        )
    }
}
