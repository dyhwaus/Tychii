package eu.kanade.tachiyomi.ui.library

import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.source.model.UpdateStrategy
import io.kotest.matchers.shouldBe
import kotlinx.serialization.json.JsonObject
import org.junit.jupiter.api.Test
import tachiyomi.domain.library.model.LibraryManga
import tachiyomi.domain.library.model.LibrarySort
import tachiyomi.domain.manga.model.Manga
import kotlin.time.Duration.Companion.days
import kotlin.time.Instant

class LibraryRuleTest {

    private val now = Instant.parse("2026-11-25T07:00:00Z")

    private fun rule(vararg conditions: RuleCondition): LibraryRule {
        return LibraryRule(id = "test", name = "Test", conditions = conditions.toList())
    }

    @Test
    fun `a rule with no conditions matches everything`() {
        rule().matches(item(), now) shouldBe true
    }

    @Test
    fun `reading state conditions reuse the smart groups`() {
        val reading = RuleCondition(RuleField.READING_STATE, RuleOperator.EQUALS, "READING")
        val paused = RuleCondition(RuleField.READING_STATE, RuleOperator.EQUALS, "PAUSED")
        val notStarted = RuleCondition(RuleField.READING_STATE, RuleOperator.EQUALS, "NOT_STARTED")
        val finished = RuleCondition(RuleField.READING_STATE, RuleOperator.EQUALS, "FINISHED")

        rule(reading).matches(item(readCount = 3, lastRead = now - 1.days), now) shouldBe true
        rule(reading).matches(item(readCount = 3, lastRead = now - 40.days), now) shouldBe false
        rule(paused).matches(item(readCount = 3, lastRead = now - 40.days), now) shouldBe true
        rule(notStarted).matches(item(readCount = 0), now) shouldBe true
        rule(notStarted).matches(item(readCount = 1), now) shouldBe false
        rule(finished).matches(item(readCount = 10, totalChapters = 10), now) shouldBe true
        rule(finished).matches(item(readCount = 9, totalChapters = 10), now) shouldBe false
    }

    @Test
    fun `last update within and more than split on the day line`() {
        val within = RuleCondition(RuleField.LAST_UPDATE_DAYS, RuleOperator.WITHIN_DAYS, "7")
        val moreThan = RuleCondition(RuleField.LAST_UPDATE_DAYS, RuleOperator.MORE_THAN_DAYS, "7")

        rule(within).matches(item(lastUpdate = now - 3.days), now) shouldBe true
        rule(within).matches(item(lastUpdate = now - 10.days), now) shouldBe false
        // No recorded update counts as infinitely long ago.
        rule(within).matches(item(lastUpdate = null), now) shouldBe false
        rule(moreThan).matches(item(lastUpdate = null), now) shouldBe true
        rule(moreThan).matches(item(lastUpdate = now - 10.days), now) shouldBe true
        rule(moreThan).matches(item(lastUpdate = now - 3.days), now) shouldBe false
    }

    @Test
    fun `last read within and more than treat a missing read as infinitely long ago`() {
        val within = RuleCondition(RuleField.LAST_READ_DAYS, RuleOperator.WITHIN_DAYS, "7")
        val moreThan = RuleCondition(RuleField.LAST_READ_DAYS, RuleOperator.MORE_THAN_DAYS, "7")

        rule(within).matches(item(lastRead = now - 2.days), now) shouldBe true
        rule(within).matches(item(lastRead = null), now) shouldBe false
        rule(moreThan).matches(item(lastRead = null), now) shouldBe true
        rule(moreThan).matches(item(lastRead = now - 2.days), now) shouldBe false
    }

    @Test
    fun `status and downloaded equalities`() {
        val completed = RuleCondition(RuleField.STATUS, RuleOperator.EQUALS, SManga.COMPLETED.toString())
        rule(completed).matches(item(status = SManga.COMPLETED.toLong()), now) shouldBe true
        rule(completed).matches(item(status = SManga.ONGOING.toLong()), now) shouldBe false

        val downloaded = RuleCondition(RuleField.DOWNLOADED, RuleOperator.EQUALS, "true")
        rule(downloaded).matches(item(downloadCount = 5), now) shouldBe true
        rule(downloaded).matches(item(isLocal = true), now) shouldBe true
        rule(downloaded).matches(item(), now) shouldBe false
    }

    @Test
    fun `unread count at least and equals`() {
        val atLeast = RuleCondition(RuleField.UNREAD_COUNT, RuleOperator.AT_LEAST, "3")
        rule(atLeast).matches(item(readCount = 5, totalChapters = 10), now) shouldBe true
        rule(atLeast).matches(item(readCount = 8, totalChapters = 10), now) shouldBe false

        val equalsZero = RuleCondition(RuleField.UNREAD_COUNT, RuleOperator.EQUALS, "0")
        rule(equalsZero).matches(item(readCount = 10, totalChapters = 10), now) shouldBe true
        rule(equalsZero).matches(item(readCount = 9, totalChapters = 10), now) shouldBe false
    }

    @Test
    fun `conditions are ANDed`() {
        val both = rule(
            RuleCondition(RuleField.READING_STATE, RuleOperator.EQUALS, "READING"),
            RuleCondition(RuleField.LAST_UPDATE_DAYS, RuleOperator.WITHIN_DAYS, "7"),
        )
        both.matches(item(readCount = 3, lastRead = now - 1.days, lastUpdate = now - 3.days), now) shouldBe true
        both.matches(item(readCount = 3, lastRead = now - 1.days, lastUpdate = now - 30.days), now) shouldBe false
    }

    @Test
    fun `malformed conditions never match`() {
        // An operator that does not belong to the field.
        val wrongOperator = RuleCondition(RuleField.LAST_UPDATE_DAYS, RuleOperator.AT_LEAST, "7")
        rule(wrongOperator).matches(item(lastUpdate = now - 1.days), now) shouldBe false
        // An unparseable number.
        val badNumber = RuleCondition(RuleField.UNREAD_COUNT, RuleOperator.AT_LEAST, "abc")
        rule(badNumber).matches(item(), now) shouldBe false
        // An unknown value.
        val badValue = RuleCondition(RuleField.READING_STATE, RuleOperator.EQUALS, "SOMETHING")
        rule(badValue).matches(item(readCount = 3), now) shouldBe false
    }

    @Test
    fun `sort round trips and garbage sorts are dropped`() {
        val sort = LibrarySort(LibrarySort.Type.LastUpdate, LibrarySort.Direction.Descending)
        val withSort = LibraryRule(id = "s", name = "S", sort = sort.serialize())
        withSort.sortOrNull() shouldBe sort
        LibraryRule(id = "s", name = "S", sort = "garbage").sortOrNull() shouldBe null
        LibraryRule(id = "s", name = "S", sort = null).sortOrNull() shouldBe null
    }

    @Test
    fun `store json round trips and rejects bad input`() {
        val rules = listOf(
            LibraryRule(
                id = "a",
                name = "Recent",
                conditions = listOf(
                    RuleCondition(RuleField.LAST_UPDATE_DAYS, RuleOperator.WITHIN_DAYS, "7"),
                    RuleCondition(RuleField.FAVORITE, RuleOperator.EQUALS, "true"),
                ),
                sort = LibrarySort(LibrarySort.Type.LastUpdate, LibrarySort.Direction.Descending).serialize(),
            ),
            LibraryRule(id = "b", name = "Paused", enabled = false),
        )
        parseLibraryRules(serializeLibraryRules(rules)) shouldBe rules
        parseLibraryRules("") shouldBe emptyList()
        parseLibraryRules("not json") shouldBe emptyList()
        parseLibraryRules("{\"version\":99,\"rules\":[]}") shouldBe emptyList()
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
