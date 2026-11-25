package eu.kanade.presentation.library

import eu.kanade.presentation.library.components.LibraryIndexSection
import eu.kanade.presentation.library.components.libraryIndexSections
import eu.kanade.presentation.library.components.sectionKeyForTitle
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class LibraryIndexTest {

    @Test
    fun `latin initials bucket to their uppercase letter`() {
        sectionKeyForTitle("One Piece") shouldBe "O"
        sectionKeyForTitle("apple") shouldBe "A"
        sectionKeyForTitle("  Space") shouldBe "S"
    }

    @Test
    fun `kana initials bucket to their gojuuon row`() {
        sectionKeyForTitle("あの日") shouldBe "あ"
        sectionKeyForTitle("カタカナ") shouldBe "か"
        sectionKeyForTitle("がっこう") shouldBe "か"
        sectionKeyForTitle("ぢ") shouldBe "た"
        sectionKeyForTitle("ャング") shouldBe "や"
        sectionKeyForTitle("ン") shouldBe "わ"
        sectionKeyForTitle("ルルーシュ") shouldBe "ら"
    }

    @Test
    fun `kanji digits and empty titles bucket to hash`() {
        sectionKeyForTitle("進撃の巨人") shouldBe "#"
        sectionKeyForTitle("123") shouldBe "#"
        sectionKeyForTitle("") shouldBe "#"
        sectionKeyForTitle("   ") shouldBe "#"
    }

    @Test
    fun `sections follow first appearance order`() {
        libraryIndexSections(
            listOf("Apple", "Avocado", "Banana", "あい", "かき", "進撃"),
        ) shouldBe listOf(
            LibraryIndexSection("A", 0),
            LibraryIndexSection("B", 2),
            LibraryIndexSection("あ", 3),
            LibraryIndexSection("か", 4),
            LibraryIndexSection("#", 5),
        )
    }

    @Test
    fun `a repeated bucket keeps its first index`() {
        libraryIndexSections(
            listOf("Apple", "Banana", "Avocado"),
        ) shouldBe listOf(
            LibraryIndexSection("A", 0),
            LibraryIndexSection("B", 1),
        )
    }
}
