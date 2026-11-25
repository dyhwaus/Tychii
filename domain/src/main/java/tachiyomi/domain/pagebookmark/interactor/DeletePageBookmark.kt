package tachiyomi.domain.pagebookmark.interactor

import dev.zacsweers.metro.Inject
import tachiyomi.domain.pagebookmark.repository.PageBookmarkRepository

@Inject
class DeletePageBookmark(
    private val repository: PageBookmarkRepository,
) {

    suspend fun await(bookmarkId: Long) {
        repository.delete(bookmarkId)
    }
}
