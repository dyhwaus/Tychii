package eu.kanade.presentation.theme

import android.os.Build

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
