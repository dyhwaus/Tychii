package tachiyomi.data.pagebookmark

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.flow.Flow
import tachiyomi.data.Database
import tachiyomi.data.subscribeToList
import tachiyomi.domain.pagebookmark.model.PageBookmark
import tachiyomi.domain.pagebookmark.repository.PageBookmarkRepository
import kotlin.time.Instant

@Inject
@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
class PageBookmarkRepositoryImpl(
    private val database: Database,
) : PageBookmarkRepository {

    override fun subscribeByMangaId(mangaId: Long): Flow<List<PageBookmark>> {
        return database.pageBookmarkQueries
            .getByMangaId(mangaId, PageBookmarkMapper::mapPageBookmark)
            .subscribeToList()
    }

    override suspend fun add(chapterId: Long, page: Int, createdAt: Instant) {
        database.pageBookmarkQueries.insert(chapterId, page, createdAt)
    }

    override suspend fun delete(bookmarkId: Long) {
        database.pageBookmarkQueries.deleteById(bookmarkId)
    }
}
