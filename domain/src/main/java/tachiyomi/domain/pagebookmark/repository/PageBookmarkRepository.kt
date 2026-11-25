package tachiyomi.domain.pagebookmark.repository

import kotlinx.coroutines.flow.Flow
import tachiyomi.domain.pagebookmark.model.PageBookmark
import kotlin.time.Instant

interface PageBookmarkRepository {

    fun subscribeByMangaId(mangaId: Long): Flow<List<PageBookmark>>

    suspend fun add(chapterId: Long, page: Int, createdAt: Instant)

    suspend fun delete(bookmarkId: Long)
}
