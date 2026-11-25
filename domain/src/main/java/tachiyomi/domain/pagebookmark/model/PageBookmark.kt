package tachiyomi.domain.pagebookmark.model

import kotlin.time.Instant

/**
 * A bookmark on one page inside a chapter. [page] is the 0-based page
 * index, the same index the reader uses for `lastPageRead` and
 * `requestedPage`. [chapterName] comes from the joined chapter row.
 */
data class PageBookmark(
    val id: Long,
    val chapterId: Long,
    val chapterName: String,
    val page: Int,
    val createdAt: Instant,
)
