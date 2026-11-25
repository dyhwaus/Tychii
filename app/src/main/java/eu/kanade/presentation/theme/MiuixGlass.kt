package eu.kanade.presentation.theme

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import eu.kanade.domain.ui.model.BarStyle
import eu.kanade.domain.ui.model.GlassTier
import mihon.app.di.appGraph
import tachiyomi.presentation.core.util.collectAsState

/**
 * Whether the liquid-glass (miuix-blur) effect can run on this device.
 *
 * miuix-blur depends on RenderEffect, and the library declares minSdk 32.
 * The app supports 26, so the manifest uses tools:overrideLibrary and all
 * blur calls are gated behind this check. Below Android 12L the floating
 * navigation bar falls back to a translucent solid color (the same look as
 * before liquid glass), with zero risk.
 */
val isMiuixGlassSupported: Boolean
    get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S_V2

/**
 * The bar style that actually renders: liquid glass needs API 32+; below
 * that it behaves like translucent. Every floating piece reads this one
 * function instead of repeating the gate.
 */
fun BarStyle.effective(): BarStyle {
    return if (this == BarStyle.LIQUID_GLASS && !isMiuixGlassSupported) {
        BarStyle.TRANSLUCENT
    } else {
        this
    }
}

// The RAM line behind AUTO: a device with at least this much memory is
// assumed to have the GPU headroom for the full recipe. A starting guess
// to validate on a real device, not a measured threshold.
private const val FULL_TIER_MIN_RAM_BYTES = 6L * 1024 * 1024 * 1024

/**
 * The tier that actually renders. AUTO picks by capability: below API 32
 * there is no glass at all (LITE); API 32 gets blur without refraction
 * (BALANCED — the lens needs RuntimeShader, API 33+); API 33+ gets the
 * full recipe only with enough RAM, BALANCED otherwise. An explicit tier
 * stays as set.
 *
 * [sdkInt] is a parameter (defaulting to the device's) so the mapping is
 * unit-testable without Robolectric.
 */
fun GlassTier.resolve(totalMemBytes: Long, sdkInt: Int = Build.VERSION.SDK_INT): GlassTier {
    return when (this) {
        GlassTier.AUTO -> when {
            sdkInt < Build.VERSION_CODES.S_V2 -> GlassTier.LITE
            sdkInt >= Build.VERSION_CODES.TIRAMISU && totalMemBytes >= FULL_TIER_MIN_RAM_BYTES -> GlassTier.FULL
            else -> GlassTier.BALANCED
        }
        else -> this
    }
}

/**
 * Refraction as its own switch: turning it off renders FULL as BALANCED
 * without touching the tier setting. Applies only to FULL.
 */
fun GlassTier.withRefraction(enabled: Boolean): GlassTier {
    return if (this == GlassTier.FULL && !enabled) GlassTier.BALANCED else this
}

private fun Context.totalMemoryBytes(): Long {
    val activityManager = getSystemService(ActivityManager::class.java) ?: return 0L
    val memoryInfo = ActivityManager.MemoryInfo()
    activityManager.getMemoryInfo(memoryInfo)
    return memoryInfo.totalMem
}

/**
 * The glass tier for this page, resolved once from the tier + refraction
 * preferences and the device's capability. Pages read this next to the
 * bar style and pass it to their glass pieces.
 */
@Composable
fun rememberGlassTier(): GlassTier {
    val context = LocalContext.current
    val setting by remember { context.appGraph.uiPreferences.glassTier }.collectAsState()
    val refractionEnabled by remember { context.appGraph.uiPreferences.refractionEnabled }.collectAsState()
    return remember(setting, refractionEnabled) {
        setting.resolve(context.totalMemoryBytes()).withRefraction(refractionEnabled)
    }
}
