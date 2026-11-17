package tachiyomi.domain.library.model

sealed interface LibraryDisplayMode {

    data object CompactGrid : LibraryDisplayMode
    data object List : LibraryDisplayMode
    data object CoverOnlyGrid : LibraryDisplayMode
    // Wenku8-style 3D shelf: a spine strip with the focused book's
    // cover pulled out above it, over the compact grid.
    data object Shelf : LibraryDisplayMode

    object Serializer {
        fun deserialize(serialized: String): LibraryDisplayMode {
            return LibraryDisplayMode.deserialize(serialized)
        }

        fun serialize(value: LibraryDisplayMode): String {
            return value.serialize()
        }
    }

    companion object {
        val values by lazy { setOf(CompactGrid, List, CoverOnlyGrid, Shelf) }
        val default = CompactGrid

        fun deserialize(serialized: String): LibraryDisplayMode {
            return when (serialized) {
                // The comfortable grid was merged into the compact
                // grid (same columns, same card); old installs may
                // still have it saved.
                "COMFORTABLE_GRID" -> CompactGrid
                "COMPACT_GRID" -> CompactGrid
                "COVER_ONLY_GRID" -> CoverOnlyGrid
                "LIST" -> List
                "SHELF" -> Shelf
                else -> default
            }
        }
    }

    fun serialize(): String {
        return when (this) {
            CompactGrid -> "COMPACT_GRID"
            CoverOnlyGrid -> "COVER_ONLY_GRID"
            List -> "LIST"
            Shelf -> "SHELF"
        }
    }
}
