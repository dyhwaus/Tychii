package tachiyomi.domain.pagebookmark.interactor

import dev.zacsweers.metro.Inject
import kotlin.time.Clock
import tachiyomi.domain.pagebookmark.repository.PageBookmarkRepository

@Inject
class AddPageBookmark(
    private val repository: PageBookmarkRepository,
) {

    suspend fun await(chapterId: Long, page: Int) {
        repository.add(chapterId, page, Clock.System.now())
    }
}
