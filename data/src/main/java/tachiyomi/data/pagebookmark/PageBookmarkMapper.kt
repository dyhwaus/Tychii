package tachiyomi.data.pagebookmark

import tachiyomi.domain.pagebookmark.model.PageBookmark
import kotlin.time.Instant

object PageBookmarkMapper {
    fun mapPageBookmark(
        id: Long,
        chapterId: Long,
        chapterName: String,
        page: Int,
        createdAt: Instant,
    ): PageBookmark = PageBookmark(
        id = id,
        chapterId = chapterId,
        chapterName = chapterName,
        page = page,
        createdAt = createdAt,
    )
}
