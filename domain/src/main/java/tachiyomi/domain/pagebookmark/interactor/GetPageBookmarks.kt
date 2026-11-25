package tachiyomi.domain.pagebookmark.interactor

import dev.zacsweers.metro.Inject
import kotlinx.coroutines.flow.Flow
import tachiyomi.domain.pagebookmark.model.PageBookmark
import tachiyomi.domain.pagebookmark.repository.PageBookmarkRepository

@Inject
class GetPageBookmarks(
    private val repository: PageBookmarkRepository,
) {

    fun subscribe(mangaId: Long): Flow<List<PageBookmark>> {
        return repository.subscribeByMangaId(mangaId)
    }
}
