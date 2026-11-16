// Adapted from https://github.com/SukiSU-Ultra/SukiSU-Ultra
// Copyright SukiSU-Ultra, Apache-2.0
// 移植自 SukiSU-Ultra 的 ui/component/FloatingBottomBar.kt
// （其改编自 compose-miuix-ui 官方示例 IosLiquidGlassNavigationBar，Apache-2.0）。
//
// 与原实现的差别有五处：
// 1) 包名与本项目的依赖路径；
// 2) 深浅色不再从 SukiSU 的主题工具取，而是由调用方传入 `isDark`（本项目跟随应用内设置）；
// 3) 折射/内阴影/增艳复用本项目已移植的 ui/miuix/LiquidGlass.kt 与 LiquidInnerShadow.kt。
// 4) 选中 pill 采样 WeKite 式 CombinedBackdrop（页面 + 已染色的选中层），
//    透镜与面纱按 WeKite 的参数；选中层录制时先做一遍磨砂（模糊 +
//    面纱），所以 pill 是磨砂质感而非通透的——图标是录制层的内容，
//    仍清晰。这是第二遍模糊，只覆盖 pill 大小的一条。
// 5) pill 在内容 Row 之上（WeKite 的层叠）：只有这样 pill 的拖动手势
//    才收得到触摸——内容 Row 盖在上面时，触摸根本到不了 pill。pill
//    盖住选中 tab 后，点按/长按靠 DampedDragAnimation 的 onTap/
//    onLongPress 转给 onTabReselected；选中图标靠 CombinedBackdrop
//    在 pill 里折射出来，不会被盖住。
//
// 之所以整段移植而不是"照着写"：这套底栏的观感来自几个必须同时成立的细节——
// 双层玻璃（底栏一层 + 选中项强调色的一层，后者只作为模糊源）、滑块自己的透镜折射与内阴影、
// 素材高光跟随倾角、以及按压/拖拽时的缩放曲线。少任何一处都会"差一点"。

package eu.kanade.presentation.miuix

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseOut
import androidx.compose.animation.core.spring
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastCoerceIn
import androidx.compose.ui.util.fastRoundToInt
import androidx.compose.ui.util.lerp
import eu.kanade.presentation.miuix.liquid.DampedDragAnimation
import eu.kanade.presentation.miuix.liquid.InteractiveHighlight
import eu.kanade.presentation.miuix.liquid.rememberCombinedBackdrop
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.blur.Backdrop
import top.yukonga.miuix.kmp.blur.blur
import top.yukonga.miuix.kmp.blur.drawBackdrop
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.blur.rememberLayerBackdrop
import top.yukonga.miuix.kmp.theme.LocalContentColor
import top.yukonga.miuix.kmp.theme.MiuixTheme
import kotlin.math.sign

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

/** 选中层录制时为 true：tab 图标全部按选中态（强调色）画，供 pill 折射。 */
internal val LocalBottomBarForceActiveTab = staticCompositionLocalOf { false }

/**
 * 液态玻璃悬浮底栏（SukiSU 同款底栏 + WeKite 式选中 pill）。
 *
 * @param selectedIndex 当前选中项（读作 lambda，避免每帧重组调用方）
 * @param onSelected 拖动/点击切换后的回调
 * @param onTabReselected 点按已选中的 tab 时的回调（pill 盖住它，点按
 * 到不了 tab 自己；默认走 onSelected）
 * @param onTabReselectedLongPress 长按已选中的 tab 时的回调
 * @param backdrop 页面内容的模糊源
 * @param tabsCount Tab 数量（拖动范围 0..tabsCount-1）
 * @param isBlurEnabled 是否启用玻璃（关闭时退化为纯色胶囊）
 * @param isMetal 是否启用拉丝金属：底栏是金属面，选中 pill 是冲压下去的凹槽
 * @param containerAlpha 玻璃关闭时容器色的不透明度：1f 为不透明，0.75f 为半透明
 * @param isDark 深色主题：决定薄纱是提亮还是压暗
 */
@Composable
internal fun MiuixLiquidBottomBar(
    modifier: Modifier = Modifier,
    selectedIndex: () -> Int,
    onSelected: (index: Int) -> Unit,
    onTabReselected: (index: Int) -> Unit = onSelected,
    onTabReselectedLongPress: (index: Int) -> Unit = {},
    backdrop: Backdrop,
    tabsCount: Int,
    isDark: Boolean,
    isBlurEnabled: Boolean = true,
    isMetal: Boolean = false,
    containerAlpha: Float = 1f,
    content: @Composable RowScope.() -> Unit,
) {
    // The pill shape MUST be a RoundedCornerShape, not CircleShape: the
    // lens() refraction in LiquidGlass.kt only works on CornerBasedShape
    // (it reads the four corner radii), and CircleShape is not one — with
    // CircleShape the refraction is silently skipped. A 32.dp radius on a
    // 64.dp tall bar is a perfect capsule, identical in appearance.
    val pillShape = remember { RoundedCornerShape(32.dp) }
    val accentColor = MiuixTheme.colorScheme.primary
    val tabContentColor = MiuixTheme.colorScheme.onSurface
    val surfaceContainer = MiuixTheme.colorScheme.surfaceContainer
    val containerColor = if (isBlurEnabled) {
        surfaceContainer.copy(alpha = MiuixGlassSurfaceAlpha)
    } else {
        surfaceContainer.copy(alpha = containerAlpha)
    }

    val density = LocalDensity.current
    val isLtr = LocalLayoutDirection.current == LayoutDirection.Ltr
    val animationScope = rememberCoroutineScope()

    var tabWidthPx by remember { mutableFloatStateOf(0f) }
    var totalWidthPx by remember { mutableFloatStateOf(0f) }

    val offsetAnimation = remember { Animatable(0f) }
    val rubberBandPx = with(density) { 4.dp.toPx() }
    val panelOffset by remember(rubberBandPx) {
        derivedStateOf {
            if (totalWidthPx == 0f) {
                0f
            } else {
                val fraction = (offsetAnimation.value / totalWidthPx).fastCoerceIn(-1f, 1f)
                rubberBandPx * fraction.sign * EaseOut.transform(kotlin.math.abs(fraction))
            }
        }
    }

    var currentIndex by remember(selectedIndex) { mutableIntStateOf(selectedIndex()) }

    class DampedDragAnimationHolder {
        var instance: DampedDragAnimation? = null
    }

    val holder = remember { DampedDragAnimationHolder() }

    // 回调要保鲜：动画只 remember 一次，onTabReselected 换了要读到新的。
    val onTabReselectedState = rememberUpdatedState(onTabReselected)
    val onTabReselectedLongPressState = rememberUpdatedState(onTabReselectedLongPress)

    val dampedDragAnimation = remember(animationScope, tabsCount, density, isLtr) {
        DampedDragAnimation(
            animationScope = animationScope,
            initialValue = selectedIndex().toFloat(),
            valueRange = 0f..(tabsCount - 1).toFloat(),
            visibilityThreshold = 0.001f,
            initialScale = 1f,
            pressedScale = 78f / 56f,
            canDrag = { offset ->
                val anim = holder.instance ?: return@DampedDragAnimation true
                if (tabWidthPx == 0f) return@DampedDragAnimation false

                val currentValue = anim.value
                val indicatorX = currentValue * tabWidthPx
                val padding = with(density) { 4.dp.toPx() }
                val globalTouchX = if (isLtr) {
                    padding + indicatorX + offset.x
                } else {
                    totalWidthPx - padding - tabWidthPx - indicatorX + offset.x
                }
                globalTouchX in 0f..totalWidthPx
            },
            onDragStarted = {},
            onDragStopped = {
                val targetIndex = targetValue.fastRoundToInt().fastCoerceIn(0, tabsCount - 1)
                currentIndex = targetIndex
                animateToValue(targetIndex.toFloat())
                animationScope.launch {
                    offsetAnimation.animateTo(0f, spring(1f, 300f, 0.5f))
                }
            },
            onDrag = { _, dragAmount ->
                if (tabWidthPx > 0) {
                    updateValue(
                        (targetValue + dragAmount.x / tabWidthPx * if (isLtr) 1f else -1f)
                            .fastCoerceIn(0f, (tabsCount - 1).toFloat()),
                    )
                    animationScope.launch {
                        offsetAnimation.snapTo(offsetAnimation.value + dragAmount.x)
                    }
                }
            },
            // pill 盖在选中 tab 上面，点按/长按到不了 tab 自己，这里
            // 转出去（点按已选中的 tab 就是重选，比如书架 tab 重
            // 点打开设置）。
            onTap = {
                val index = value.fastRoundToInt().fastCoerceIn(0, tabsCount - 1)
                onTabReselectedState.value(index)
            },
            onLongPress = {
                val index = value.fastRoundToInt().fastCoerceIn(0, tabsCount - 1)
                onTabReselectedLongPressState.value(index)
            },
        ).also { holder.instance = it }
    }

    LaunchedEffect(selectedIndex) {
        snapshotFlow { selectedIndex() }.collectLatest { currentIndex = it }
    }
    LaunchedEffect(dampedDragAnimation) {
        snapshotFlow { currentIndex }.drop(1).collectLatest { index ->
            dampedDragAnimation.animateToValue(index.toFloat())
            onSelected(index)
        }
    }

    val interactiveHighlight = remember(animationScope, tabWidthPx) {
        InteractiveHighlight(
            animationScope = animationScope,
            position = { size, _ ->
                Offset(
                    if (isLtr) {
                        (dampedDragAnimation.value + 0.5f) * tabWidthPx + panelOffset
                    } else {
                        size.width - (dampedDragAnimation.value + 0.5f) * tabWidthPx + panelOffset
                    },
                    size.height / 2f,
                )
            },
        )
    }

    val baseHighlight = rememberTiltBrightHighlight(extraDegrees = -45f)
    val pillHighlight = rememberTiltBrightHighlight(extraDegrees = 90f)

    // 选中层录进 tabsBackdrop，再和页面拼成 pill 的采样源。
    val tabsBackdrop = rememberLayerBackdrop()
    val combinedBackdrop = rememberCombinedBackdrop(backdrop, tabsBackdrop)

    Box(
        modifier = modifier.width(IntrinsicSize.Min),
        contentAlignment = Alignment.CenterStart,
    ) {
        Row(
            Modifier
                .onGloballyPositioned { coords ->
                    totalWidthPx = coords.size.width.toFloat()
                    val contentWidthPx = totalWidthPx - with(density) { 8.dp.toPx() }
                    tabWidthPx = (contentWidthPx / tabsCount).coerceAtLeast(0f)
                }
                .graphicsLayer { translationX = panelOffset }
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
                    if (isBlurEnabled) {
                        Modifier.drawBackdrop(
                            backdrop = backdrop,
                            shape = { pillShape },
                            effects = {
                                vibrancy()
                                blur(MiuixGlassBlurRadius.toPx(), MiuixGlassBlurRadius.toPx())
                                // WeKite 的底栏透镜：24/24，不带 depth 与色散。
                                lens(
                                    refractionHeight = 24.dp.toPx(),
                                    refractionAmount = 24.dp.toPx(),
                                )
                            },
                            highlight = { baseHighlight.copy(alpha = 0.75f) },
                            layerBlock = {
                                val width = size.width.coerceAtLeast(1f)
                                val s = lerp(
                                    1f,
                                    1f + 16.dp.toPx() / width,
                                    dampedDragAnimation.pressProgress,
                                )
                                scaleX = s
                                scaleY = s
                            },
                            onDrawSurface = { drawRect(containerColor) },
                        )
                    } else if (isMetal) {
                        Modifier.metalSurface(pillShape, isDark)
                    } else {
                        Modifier.background(containerColor, pillShape)
                    },
                )
                .then(if (isBlurEnabled) interactiveHighlight.modifier else Modifier)
                .height(64.dp)
                .padding(4.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // No tab content here: it lives in the overlay Row below,
            // on top of the indicator pill.
        }

        // 选中层：先画一层磨砂玻璃（模糊的页面 + 面纱），再叠上按
        // 选中态染色的 tab 内容，录进 tabsBackdrop，供 pill 的
        // CombinedBackdrop 采样——pill 采到的就是磨砂页 + 清晰图标。
        // 这是第二遍模糊（底栏本身已经模糊过一次页面），只覆盖 pill
        // 大小的一条；WeKite 也是这么做的。alpha 0f 且清掉语义，
        // 不参与点击；尺寸与位置和 pill 一致（56.dp 高、左右各 4.dp），
        // 这样 pill 采到的图标正好落在它下面。
        if (isBlurEnabled) {
            Row(
                Modifier
                    .clearAndSetSemantics {}
                    .alpha(0f)
                    .layerBackdrop(tabsBackdrop)
                    .graphicsLayer { translationX = panelOffset }
                    .drawBackdrop(
                        backdrop = backdrop,
                        shape = { pillShape },
                        effects = {
                            vibrancy()
                            blur(MiuixGlassBlurRadius.toPx(), MiuixGlassBlurRadius.toPx())
                            // 和底栏本身一样的透镜：24/24，不带 depth 与色散。
                            lens(
                                refractionHeight = 24.dp.toPx(),
                                refractionAmount = 24.dp.toPx(),
                            )
                        },
                        onDrawSurface = { drawRect(containerColor) },
                    )
                    .height(56.dp)
                    .padding(horizontal = 4.dp)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CompositionLocalProvider(LocalBottomBarForceActiveTab provides true) {
                    content()
                }
            }
        }

        // Tab content, under the indicator pill. The pill sits on top
        // (it has to: only then do its drag gestures receive touches),
        // and the selected icon still shows — refracted inside the pill
        // through the combined backdrop. This Row mirrors the background
        // Row's size and motion (same height, padding, rubber-band
        // offset and press scale) so the icons sit exactly where they did.
        Row(
            Modifier
                .graphicsLayer {
                    translationX = panelOffset
                    val width = size.width.coerceAtLeast(1f)
                    val s = lerp(
                        1f,
                        1f + 16.dp.toPx() / width,
                        dampedDragAnimation.pressProgress,
                    )
                    scaleX = s
                    scaleY = s
                }
                .height(64.dp)
                .padding(4.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CompositionLocalProvider(LocalContentColor provides tabContentColor) {
                content()
            }
        }

        if (tabWidthPx > 0f) {
            val tabWidthDp = with(density) { tabWidthPx.toDp() }
            if (isBlurEnabled) {
                Box(
                    Modifier
                        .padding(horizontal = 4.dp)
                        .graphicsLayer {
                            val progressOffset = dampedDragAnimation.value * tabWidthPx
                            translationX = if (isLtr) {
                                progressOffset + panelOffset
                            } else {
                                -progressOffset + panelOffset
                            }
                        }
                        .then(interactiveHighlight.gestureModifier)
                        .then(dampedDragAnimation.modifier)
                        .drawBackdrop(
                            // The pill refracts the combined backdrop:
                            // the page plus the active tab layer recorded
                            // above, so the selected icon shows through
                            // the pill in the accent colour, refracted by
                            // the pill's own lens. The bar's veil is not
                            // re-applied here — the pill stays a clear
                            // lens on the frosted bar (an earlier version
                            // did the second glass pass; too much GPU time).
                            backdrop = combinedBackdrop,
                            shape = { pillShape },
                            effects = {
                                val progress = dampedDragAnimation.pressProgress
                                // WeKite 的 pill 透镜：只在按压/拖动时张开
                                //（静止时为 0，不折射），色散 0.5 带 depth。
                                lens(
                                    refractionHeight = (10.dp * progress).toPx(),
                                    refractionAmount = (14.dp * progress).toPx(),
                                    depthEffect = true,
                                    chromaticAberration = MiuixGlassPillChromaticAberration,
                                )
                            },
                            highlight = {
                                pillHighlight.copy(alpha = dampedDragAnimation.pressProgress)
                            },
                            layerBlock = {
                                scaleX = dampedDragAnimation.scaleX
                                scaleY = dampedDragAnimation.scaleY
                                val velocity = dampedDragAnimation.velocity / 10f
                                scaleX /= 1f - (velocity * 0.75f).fastCoerceIn(-0.2f, 0.2f)
                                scaleY *= 1f - (velocity * 0.25f).fastCoerceIn(-0.2f, 0.2f)
                            },
                            onDrawSurface = {
                                val progress = dampedDragAnimation.pressProgress
                                // WeKite 的 pill 面纱：静止时一层很淡的
                                // 黑/白纱，越按越透，只留一丝黑。强调色
                                // 不再平铺——选中态的颜色现在由折射的
                                // 选中层图标本身带来。
                                drawRect(
                                    color = if (!isDark) {
                                        Color.Black.copy(alpha = 0.1f)
                                    } else {
                                        Color.White.copy(alpha = 0.1f)
                                    },
                                    alpha = 1f - progress,
                                )
                                drawRect(Color.Black.copy(alpha = 0.03f * progress))
                            },
                        )
                        .innerShadow(shape = pillShape) {
                            InnerShadow(
                                radius = 8.dp * dampedDragAnimation.pressProgress,
                                color = Color.Black.copy(alpha = 0.15f),
                                alpha = dampedDragAnimation.pressProgress,
                            )
                        }
                        .height(56.dp)
                        .width(tabWidthDp),
                )
            } else if (isMetal) {
                // 金属模式的选中态不是一块玻璃 pill，而是冲压下去的
                // 凹槽：暗一档 + 内阴影。位置和拖动和玻璃 pill 一样。
                Box(
                    Modifier
                        .padding(horizontal = 4.dp)
                        .graphicsLayer {
                            val progressOffset = dampedDragAnimation.value * tabWidthPx
                            translationX = if (isLtr) {
                                progressOffset + panelOffset
                            } else {
                                -progressOffset + panelOffset
                            }
                        }
                        .then(dampedDragAnimation.modifier)
                        .clip(pillShape)
                        .metalRecess(pillShape, isDark)
                        .height(56.dp)
                        .width(tabWidthDp),
                )
            } else {
                Box(
                    Modifier
                        .padding(horizontal = 4.dp)
                        .graphicsLayer {
                            val progressOffset = dampedDragAnimation.value * tabWidthPx
                            translationX = if (isLtr) {
                                progressOffset + panelOffset
                            } else {
                                -progressOffset + panelOffset
                            }
                        }
                        .then(dampedDragAnimation.modifier)
                        .clip(pillShape)
                        .background(accentColor.copy(alpha = 0.15f), pillShape)
                        .height(56.dp)
                        .width(tabWidthDp),
                )
            }
        }

    }
}
