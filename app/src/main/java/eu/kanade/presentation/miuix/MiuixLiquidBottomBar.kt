// Adapted from https://github.com/SukiSU-Ultra/SukiSU-Ultra
// Copyright SukiSU-Ultra, Apache-2.0
// 移植自 SukiSU-Ultra 的 ui/component/FloatingBottomBar.kt
// （其改编自 compose-miuix-ui 官方示例 IosLiquidGlassNavigationBar，Apache-2.0）。
//
// 与原实现的差别有三处：
// 1) 包名与本项目的依赖路径；
// 2) 深浅色不再从 SukiSU 的主题工具取，而是由调用方传入 `isDark`（本项目跟随应用内设置）；
// 3) 折射/增艳复用本项目已移植的 ui/miuix/LiquidGlass.kt。
//
// 曾经有过一个可拖动的选中 pill（WeKite 式 CombinedBackdrop：页面 +
// 已染色的选中层，pill 在上面滑动、按压时透镜张开），2026-11-18
// 因点按切换延迟卡顿而整个拿掉：选中态只靠图标/文字的强调色 tint
// 表示。拿掉的同时也去掉了第二遍模糊（选中层录制）和 pill 每帧的
// 玻璃重绘——切 tab 那一帧和动画期间的开销都小了很多。

package eu.kanade.presentation.miuix

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import eu.kanade.domain.ui.model.GlassTier
import top.yukonga.miuix.kmp.blur.Backdrop
import top.yukonga.miuix.kmp.blur.blur
import top.yukonga.miuix.kmp.blur.drawBackdrop
import top.yukonga.miuix.kmp.theme.LocalContentColor
import top.yukonga.miuix.kmp.theme.MiuixTheme

/** 单个 Tab：撑满高度、均分宽度。 */
@Composable
internal fun RowScope.FloatingBottomBarItem(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .clickable(
                interactionSource = null,
                indication = null,
                role = Role.Tab,
                onClick = onClick,
            )
            .fillMaxHeight()
            .weight(1f),
        verticalArrangement = Arrangement.spacedBy(1.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
        content = content,
    )
}

/**
 * 液态玻璃悬浮底栏（SukiSU 同款底栏，不带选中 pill）。
 *
 * 一个磨砂玻璃胶囊，tab 内容直接画在上面；选中态只靠图标/文字的
 * 强调色 tint，不再有滑动的 pill——pill（拖动、CombinedBackdrop
 * 采样、第二遍模糊）因点按切换延迟卡顿而拿掉了。
 *
 * 金属模式用的是同一条胶囊：容器换成拉丝金属，tab 内容由调用方
 * 换成刻字，选中态由调用方用弹起来表示。
 *
 * @param backdrop 页面内容的模糊源
 * @param isBlurEnabled 是否启用玻璃（关闭时退化为纯色胶囊）
 * @param containerAlpha 玻璃关闭时容器色的不透明度：1f 为不透明，0.75f 为半透明
 * @param isDark 深色主题：决定薄纱是提亮还是压暗
 * @param isMetal 金属模式：容器是拉丝金属，不模糊也不用纯色
 */
@Composable
internal fun MiuixLiquidBottomBar(
    modifier: Modifier = Modifier,
    backdrop: Backdrop,
    isDark: Boolean,
    isBlurEnabled: Boolean = true,
    containerAlpha: Float = 1f,
    isMetal: Boolean = false,
    glassTier: GlassTier,
    content: @Composable RowScope.() -> Unit,
) {
    // The capsule shape MUST be a RoundedCornerShape, not CircleShape: the
    // lens() refraction in LiquidGlass.kt only works on CornerBasedShape
    // (it reads the four corner radii), and CircleShape is not one — with
    // CircleShape the refraction is silently skipped. A 32.dp radius on a
    // 64.dp tall bar is a perfect capsule, identical in appearance.
    val pillShape = remember { RoundedCornerShape(TychiiCorners.Pill) }
    val tabContentColor = MiuixTheme.colorScheme.onSurface
    val surfaceContainer = MiuixTheme.colorScheme.surfaceContainer
    val containerColor = if (isBlurEnabled) {
        surfaceContainer.copy(alpha = MiuixGlassSurfaceAlpha)
    } else {
        surfaceContainer.copy(alpha = containerAlpha)
    }

    // FULL follows the tilt; BALANCED keeps the same highlight static.
    // LITE never draws a highlight (no drawBackdrop below).
    val baseHighlight = if (glassTier == GlassTier.FULL) {
        rememberTiltBrightHighlight(extraDegrees = -45f)
    } else {
        MiuixGlassSpecular
    }

    Box(
        modifier = modifier.width(IntrinsicSize.Min),
        contentAlignment = Alignment.CenterStart,
    ) {
        Row(
            Modifier
                .dropShadow(
                    shape = pillShape,
                    shadow = Shadow(
                        radius = 10.dp,
                        color = Color.Black,
                        alpha = if (isDark) 0.2f else 0.1f,
                    ),
                )
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {},
                )
                .then(
                    if (isMetal) {
                        Modifier.metalSurface(pillShape, isDark)
                    } else if (isBlurEnabled) {
                        Modifier.drawBackdrop(
                            backdrop = backdrop,
                            shape = { pillShape },
                            effects = {
                                vibrancy()
                                blur(MiuixGlassBlurRadius.toPx(), MiuixGlassBlurRadius.toPx())
                                // WeKite 的底栏透镜：24/24，不带 depth 与色散。
                                // BALANCED 不调透镜：着色器是最贵的一环。
                                if (glassTier == GlassTier.FULL) {
                                    lens(
                                        refractionHeight = 24.dp.toPx(),
                                        refractionAmount = 24.dp.toPx(),
                                    )
                                }
                            },
                            highlight = { baseHighlight.copy(alpha = 0.75f) },
                            onDrawSurface = { drawRect(containerColor) },
                        )
                    } else {
                        Modifier.background(containerColor, pillShape)
                    },
                )
                .height(64.dp)
                .padding(4.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CompositionLocalProvider(LocalContentColor provides tabContentColor) {
                content()
            }
        }
    }
}
