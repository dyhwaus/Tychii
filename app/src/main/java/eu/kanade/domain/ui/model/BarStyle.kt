package eu.kanade.domain.ui.model

import dev.icerock.moko.resources.StringResource
import tachiyomi.i18n.MR

/**
 * The visual style of the floating bars (the bottom navigation bar and the
 * centered page-title capsule): a solid color, a translucent color, or
 * liquid glass. Liquid glass only runs on API 32+; below that it behaves
 * like [TRANSLUCENT].
 */
enum class BarStyle(val titleRes: StringResource) {
    OPAQUE(MR.strings.bar_style_opaque),
    TRANSLUCENT(MR.strings.bar_style_translucent),
    LIQUID_GLASS(MR.strings.bar_style_liquid_glass),
}
