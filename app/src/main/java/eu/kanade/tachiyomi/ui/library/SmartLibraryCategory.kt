package eu.kanade.tachiyomi.ui.library

import dev.icerock.moko.resources.StringResource
import eu.kanade.tachiyomi.source.model.SManga
import kotlin.time.Clock
import tachiyomi.i18n.MR
import kotlin.time.Duration.Companion.days
import kotlin.time.Instant

// The day lines behind "recent" and "long": a manga updated or added
// within a week counts as recent; a read older than a month counts as
// stale. Starting guesses, not measured behavior — tune them if they
// feel wrong on a real library.
private val RECENT_THRESHOLD = 7.days
private val STALE_THRESHOLD = 30.days

/**
 * The built-in smart groups: one-tap views over the library, computed
 * from data the library already holds (read state, update time, status,
 * downloads, added time). They are filters, not partitions — a manga
 * can belong to several at once — and they compose with the existing
 * tri-state filters and the search query.
 *
 * Reading, Long unread and Paused split the unread shelf three ways and
 * never overlap: never started, started and read recently, started and
 * untouched for a month. A started manga with no recorded last-read
 * time counts as reading (its staleness cannot be proven).
 */
enum class SmartLibraryCategory(val titleRes: StringResource) {
    READING(MR.strings.smart_category_reading),
    RECENTLY_UPDATED(MR.strings.smart_category_recently_updated),
    LONG_UNREAD(MR.strings.smart_category_long_unread),
    COMPLETED(MR.strings.smart_category_completed),
    DOWNLOADED(MR.strings.smart_category_downloaded),
    RECENTLY_ADDED(MR.strings.smart_category_recently_added),
    PAUSED(MR.strings.smart_category_paused),
    ;

    fun matches(item: LibraryItem, now: Instant = Clock.System.now()): Boolean {
        val manga = item.libraryManga
        // Cross-module properties can't be smart-cast after a null
        // check, so the dates are read into locals first.
        val lastRead = manga.lastRead
        val lastUpdate = manga.manga.lastUpdate
        val favoriteAt = manga.manga.favoriteAt
        return when (this) {
            READING -> manga.hasStarted &&
                manga.unreadCount > 0 &&
                (lastRead == null || lastRead > now - STALE_THRESHOLD)
            RECENTLY_UPDATED -> lastUpdate != null &&
                lastUpdate >= now - RECENT_THRESHOLD
            LONG_UNREAD -> manga.unreadCount > 0 &&
                !manga.hasStarted &&
                (favoriteAt == null || favoriteAt <= now - RECENT_THRESHOLD)
            COMPLETED -> manga.manga.status.toInt() == SManga.COMPLETED
            DOWNLOADED -> item.isLocal || item.downloadCount > 0
            RECENTLY_ADDED -> favoriteAt != null &&
                favoriteAt >= now - RECENT_THRESHOLD
            PAUSED -> manga.hasStarted &&
                manga.unreadCount > 0 &&
                lastRead != null &&
                lastRead <= now - STALE_THRESHOLD
        }
    }
}
