package eu.kanade.presentation.theme

import eu.kanade.domain.ui.model.GlassTier
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class GlassTierTest {

    private val eightGb = 8L * 1024 * 1024 * 1024
    private val fourGb = 4L * 1024 * 1024 * 1024

    @Test
    fun `auto resolves full on api 33 with enough ram`() {
        GlassTier.AUTO.resolve(eightGb, sdkInt = 33) shouldBe GlassTier.FULL
    }

    @Test
    fun `auto resolves balanced on api 33 with little ram`() {
        GlassTier.AUTO.resolve(fourGb, sdkInt = 33) shouldBe GlassTier.BALANCED
    }

    @Test
    fun `auto resolves balanced on api 32 regardless of ram`() {
        GlassTier.AUTO.resolve(eightGb, sdkInt = 32) shouldBe GlassTier.BALANCED
    }

    @Test
    fun `auto resolves lite below api 32`() {
        GlassTier.AUTO.resolve(eightGb, sdkInt = 31) shouldBe GlassTier.LITE
    }

    @Test
    fun `explicit tiers stay as set`() {
        GlassTier.FULL.resolve(fourGb, sdkInt = 31) shouldBe GlassTier.FULL
        GlassTier.BALANCED.resolve(eightGb, sdkInt = 33) shouldBe GlassTier.BALANCED
        GlassTier.LITE.resolve(eightGb, sdkInt = 33) shouldBe GlassTier.LITE
    }

    @Test
    fun `refraction off renders full as balanced`() {
        GlassTier.FULL.withRefraction(false) shouldBe GlassTier.BALANCED
        GlassTier.FULL.withRefraction(true) shouldBe GlassTier.FULL
        GlassTier.BALANCED.withRefraction(false) shouldBe GlassTier.BALANCED
        GlassTier.LITE.withRefraction(false) shouldBe GlassTier.LITE
    }
}
