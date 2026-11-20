// 金属模式的底栏：不再是一条胶囊，而是一个 tab 一个独立悬浮按钮。
// 选中的那个像开关一样弹起来——往上抬 4.dp、微微放大，用弹簧弹
// 到位，带一点过冲；点按时先微微下沉再弹起。未选中的平着。阴影
// 固定不动，抬起只靠位移和缩放表示——阴影跟着弹簧变会每帧重算。
// 没有 pill，也就没有拖动切换，点按切换。标签和角标都保留。
//
// 阴影在最外层（和底栏胶囊一样）：按钮抬起来时，阴影留在原地；
// 平移和缩放只动按钮本身。

package eu.kanade.presentation.miuix

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import eu.kanade.presentation.util.Tab
import kotlinx.coroutines.launch

/** 一排独立的金属悬浮按钮，一个 tab 一个，均分宽度。 */
@Composable
fun MetalTabButtons(
    tabs: List<Tab>,
    selectedIndex: () -> Int,
    onTabClick: (Tab) -> Unit,
    showLabels: Boolean,
    isDark: Boolean,
    tabBadge: @Composable (Tab) -> (@Composable () -> Unit)?,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        tabs.forEachIndexed { index, tab ->
            MetalTabButton(
                tab = tab,
                selected = selectedIndex() == index,
                onClick = { onTabClick(tab) },
                showLabels = showLabels,
                isDark = isDark,
                badge = tabBadge(tab),
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun MetalTabButton(
    tab: Tab,
    selected: Boolean,
    onClick: () -> Unit,
    showLabels: Boolean,
    isDark: Boolean,
    badge: (@Composable () -> Unit)?,
    modifier: Modifier = Modifier,
) {
    // 0 = 平着，1 = 弹起来。弹簧带一点过冲，像开关拨到位。
    val raise by animateFloatAsState(
        targetValue = if (selected) 1f else 0f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 500f),
        label = "tabRaise",
    )
    // 点按时微微下沉（和操作胶囊一样的弹簧），再弹回来。
    val pressScale = remember { Animatable(1f) }
    val pressScope = rememberCoroutineScope()
    val shape = remember { RoundedCornerShape(28.dp) }
    // 图标和标签是无色刻字，字形里淡淡填一层色，和漫画卡片
    // 的标题一样；选中不再发光，选中只靠弹起来表示。
    Box(
        modifier = modifier
            .height(56.dp)
            // The shadow stays fixed while the button raises:
            // animating its radius re-blurs the shadow every
            // frame of the spring, and the lift already reads
            // from the translation and the scale.
            .dropShadow(
                shape = shape,
                shadow = Shadow(
                    radius = 12.dp,
                    color = Color.Black,
                    alpha = if (isDark) 0.25f else 0.15f,
                ),
            )
            .graphicsLayer {
                translationY = -4.dp.toPx() * raise
                val scale = (1f + 0.04f * raise) * pressScale.value
                scaleX = scale
                scaleY = scale
            }
            .metalSurface(shape, isDark)
            .clip(shape)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                role = Role.Tab,
                onClick = {
                    pressScope.launch {
                        pressScale.animateTo(0.975f, spring(dampingRatio = 0.62f, stiffness = 650f))
                        pressScale.animateTo(1f, spring(dampingRatio = 0.48f, stiffness = 420f))
                    }
                    onClick()
                },
            ),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(1.dp, Alignment.CenterVertically),
        ) {
            BadgedBox(
                badge = { badge?.invoke() },
            ) {
                // 和漫画卡片的标题一样：实心淡色图标衬在刻字
                // 后面，字形抠掉的地方透出填色。静态图标的刻
                // 字预渲染成一张 bitmap，不再每帧占一个离屏层；
                // 书架图标会随选中播动画，只能现场画——它的
                // painter 类型不是公开 API，拿 tab 的序号认：
                // 书架固定是 0 号 tab。
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        painter = tab.options.icon!!,
                        contentDescription = null,
                        tint = metalCarvedFill(isDark),
                        modifier = Modifier.size(24.dp),
                    )
                    if (tab.options.index.toInt() == 0) {
                        MetalCarvedIcon(
                            painter = tab.options.icon!!,
                            contentDescription = tab.options.title,
                            isDark = isDark,
                            modifier = Modifier.size(24.dp),
                        )
                    } else {
                        // 24.dp 宽，上下各 1.dp 留给错开的刻边。
                        Image(
                            bitmap = rememberCarvedIconBitmap(
                                painter = tab.options.icon!!,
                                isDark = isDark,
                            ),
                            contentDescription = tab.options.title,
                            modifier = Modifier.size(24.dp, 26.dp),
                        )
                    }
                }
            }
            if (showLabels) {
                // 标题也一样：淡色实心字衬在刻字后面；刻字预
                // 渲染成一张 bitmap，不再每帧走 saveLayer。
                val labelStyle = TextStyle(
                    fontSize = 11.sp,
                    lineHeight = 14.sp,
                )
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = tab.options.title,
                        style = labelStyle,
                        color = metalCarvedFill(isDark),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    val labelBitmap = rememberCarvedTextBitmap(
                        text = tab.options.title,
                        style = labelStyle,
                        isDark = isDark,
                    )
                    val density = LocalDensity.current
                    Image(
                        bitmap = labelBitmap,
                        contentDescription = null,
                        modifier = Modifier.size(
                            with(density) { labelBitmap.width.toDp() },
                            with(density) { labelBitmap.height.toDp() },
                        ),
                    )
                }
            }
        }
    }
}
