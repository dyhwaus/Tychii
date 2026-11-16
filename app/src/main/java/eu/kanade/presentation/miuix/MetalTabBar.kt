// 金属模式的底栏：不再是一条胶囊，而是一个 tab 一个独立悬浮按钮。
// 选中的那个像开关一样弹起来——往上抬 4.dp、阴影变大变深、微微放大，
// 用弹簧弹到位，带一点过冲；点按时先微微下沉再弹起。未选中的平着。
// 没有 pill，也就没有拖动切换，点按切换。标签和角标都保留。
//
// 阴影在最外层（和底栏胶囊一样）：按钮抬起来时，阴影留在原地变大
// 变淡，像东西真的离开了地面；平移和缩放只动按钮本身。

package eu.kanade.presentation.miuix

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import eu.kanade.presentation.util.Tab
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.theme.MiuixTheme

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
    val tint = if (selected) {
        MiuixTheme.colorScheme.primary
    } else {
        MiuixTheme.colorScheme.onSurface
    }
    Box(
        modifier = modifier
            .height(56.dp)
            .dropShadow(
                shape = shape,
                shadow = Shadow(
                    radius = 10.dp + 4.dp * raise,
                    color = Color.Black,
                    alpha = (if (isDark) 0.2f else 0.1f) + 0.1f * raise,
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
                MetalEngravedIcon(
                    painter = tab.options.icon!!,
                    contentDescription = tab.options.title,
                    tint = tint,
                    isDark = isDark,
                    modifier = Modifier.size(24.dp),
                )
            }
            if (showLabels) {
                Text(
                    text = tab.options.title,
                    style = TextStyle(
                        fontSize = 11.sp,
                        lineHeight = 14.sp,
                        shadow = metalEngravedShadow(isDark),
                    ),
                    color = tint,
                    maxLines = 1,
                )
            }
        }
    }
}
