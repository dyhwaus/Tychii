package eu.kanade.domain.ui.model

import dev.icerock.moko.resources.StringResource
import tachiyomi.i18n.MR

/**
 * How much of the liquid-glass recipe a page renders, when its bar
 * style is liquid glass. Other styles ignore it entirely.
 *
 * AUTO resolves by device capability (API level + total RAM); an
 * explicit tier stays as set. FULL is today's recipe, BALANCED drops
 * the lens shader, LITE drops the backdrop entirely and renders
 * translucent. Turning refraction off renders FULL as BALANCED.
 */
enum class GlassTier(val titleRes: StringResource) {
    AUTO(MR.strings.glass_tier_auto),
    FULL(MR.strings.glass_tier_full),
    BALANCED(MR.strings.glass_tier_balanced),
    LITE(MR.strings.glass_tier_lite),
}
