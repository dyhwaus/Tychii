// Adapted from https://github.com/nekomangaorg/Neko
// Copyright Neko contributors, Apache-2.0
package eu.kanade.tachiyomi.util.system

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.core.graphics.ColorUtils
import androidx.palette.graphics.Palette

fun Palette.getBestColor(): Int? {
    val vibPopulation = vibrantSwatch?.population ?: -1
    val mutedPopulation = mutedSwatch?.population ?: -1

    fun Palette.Swatch.isSaturatedAndMidTone(): Boolean {
        val saturation = hsl[1]
        val luminance = hsl[2]
        return saturation >= 0.25f && luminance in 0.2f..0.8f
    }

    // 1. Check Dominant
    dominantSwatch?.let { swatch -> if (swatch.isSaturatedAndMidTone()) return swatch.rgb }

    // 2. Check Vibrant
    if (vibPopulation >= mutedPopulation * 0.75f) {
        vibrantSwatch?.let {
            return it.rgb
        }
    }

    // 3. Check Muted
    val mutedSaturationLimit = if (mutedPopulation > vibPopulation * 3f) 0.1f else 0.25f
    if (
        mutedPopulation > vibPopulation * 1.5f &&
            (mutedSwatch?.hsl?.get(1) ?: 0f) > mutedSaturationLimit
    ) {
        mutedSwatch?.let {
            return it.rgb
        }
    }

    // 4. Safe Fallback: Filter nulls, sort descending by population, take top
    val fallbackSwatches = listOfNotNull(vibrantSwatch, lightVibrantSwatch, darkVibrantSwatch)
    return fallbackSwatches.maxByOrNull { it.population }?.rgb
}

/**
 * Tychii's own quieting step, on top of [getBestColor]: clamp the color's
 * HSL saturation so a loud cover can never produce a loud theme color.
 * Neko has no such step — its Vibrant-first pick can still come out
 * garish; this keeps the detail page muted. Hue and lightness are kept.
 */
fun Color.quietVibrant(maxSaturation: Float = 0.4f): Color {
    val hsl = FloatArray(3)
    ColorUtils.colorToHSL(toArgb(), hsl)
    if (hsl[1] <= maxSaturation) return this
    hsl[1] = maxSaturation
    return Color(ColorUtils.HSLToColor(hsl))
}
