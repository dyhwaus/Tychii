// 拉丝金属：Tychii 的拟物材质。冷灰，不掺金不掺铜，贴合黑白。
//
// 和液态玻璃是两种物理逻辑：玻璃透明，透出背后的页面；金属不透明，
// 靠表面质感立起来——纵向微渐变打底，横向拉丝细纹，上下倒角，左上
// 一道静态柔光。文字和图标是刻进去的：深色刻痕，下方 1px 亮边。
// 选中的 tab 是冲压下去的凹槽（暗一档 + 内阴影），不是一块玻璃 pill。
//
// 因为不透明，金属件不需要录背景来模糊：这个模式下录制层直接不录，
// 顶渐隐也只剩一道渐变面纱。全部用 Compose 的渐变和线条画，不贴位图。

package eu.kanade.presentation.miuix

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import top.yukonga.miuix.kmp.theme.MiuixTheme

// 基底：纵向微渐变，上亮下暗，像光从上面落下来。
val MetalLightTop = Color(0xFFECEDEF)
val MetalLightBottom = Color(0xFFD2D4D8)
val MetalDarkTop = Color(0xFF2C2E31)
val MetalDarkBottom = Color(0xFF1D1F21)

// 凹槽的底色：比基底暗一档，配内阴影就是冲压下去的样子。
val MetalRecessLight = Color(0xFFC2C4C8)
val MetalRecessDark = Color(0xFF17191B)

/** 金属颜色跟 Miuix 表面走，不跟系统主题：Miuix 的深色背景才是件的底色。 */
@Composable
fun metalIsDark(): Boolean = MiuixTheme.colorScheme.background.luminance() < 0.5f

/**
 * 拉丝金属表面：基底渐变 + 横向拉丝细纹 + 左上柔光 + 上下倒角。
 * 只画表面，不带阴影——浮起的阴影由调用方按原来的方式加。
 */
fun Modifier.metalSurface(shape: Shape, isDark: Boolean): Modifier = this.then(
    Modifier.drawWithCache {
        val outline = shape.createOutline(size, layoutDirection, this)
        val path = Path().apply {
            when (outline) {
                is Outline.Rectangle -> addRect(outline.rect)
                is Outline.Rounded -> addRoundRect(outline.roundRect)
                is Outline.Generic -> addPath(outline.path)
            }
        }
        val base = Brush.verticalGradient(
            0f to if (isDark) MetalDarkTop else MetalLightTop,
            1f to if (isDark) MetalDarkBottom else MetalLightBottom,
        )
        // 倒角：上沿亮线，下沿暗线，沿着圆角走。
        val chamfer = Brush.verticalGradient(
            0f to Color.White.copy(alpha = 0.5f),
            0.45f to Color.Transparent,
            1f to Color.Black.copy(alpha = 0.35f),
        )
        // 左上斜柔光，静态，不跟手。
        val sheen = Brush.linearGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.06f),
                Color.Transparent,
            ),
            start = Offset.Zero,
            end = Offset(size.width * 0.7f, size.height),
        )
        val spacing = 2.dp.toPx()
        val lineCount = (size.height / spacing).toInt() + 1
        onDrawBehind {
            clipPath(path) {
                drawRect(brush = base)
                // 拉丝：1px 横线，黑白交错，每条的 alpha 由行号
                // 的哈希定——固定下来，跨帧不闪。
                for (i in 0 until lineCount) {
                    val y = i * spacing
                    val hash = ((i * 2654435761L) % 1000L) / 1000f
                    val alpha = 0.02f + hash * 0.05f
                    val color = if (i % 2 == 0) Color.White else Color.Black
                    drawLine(
                        color = color.copy(alpha = alpha),
                        start = Offset(0f, y),
                        end = Offset(size.width, y),
                        strokeWidth = 1f,
                    )
                }
                drawRect(brush = sheen)
            }
            drawOutline(
                outline = outline,
                brush = chamfer,
                style = Stroke(width = 1.dp.toPx()),
            )
        }
    },
)

/** 刻痕下方的亮边：光打在毛刺上，1px，硬边不模糊。 */
fun metalEngravedBurr(isDark: Boolean): Color =
    Color.White.copy(alpha = if (isDark) 0.28f else 0.55f)

/** 刻字的阴影：直接套在文字的 TextStyle 上。 */
fun metalEngravedShadow(isDark: Boolean): Shadow =
    Shadow(color = metalEngravedBurr(isDark), offset = Offset(0f, 1f), blurRadius = 0f)

/**
 * 刻进去的图标：先画一层亮色错开 1px，再画本体——亮边露在下方，
 * 像刻痕的毛刺反光。无障碍描述只留一份，在本体上。
 */
@Composable
fun MetalEngravedIcon(
    imageVector: ImageVector,
    contentDescription: String?,
    tint: Color,
    isDark: Boolean,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier) {
        Icon(
            imageVector = imageVector,
            contentDescription = null,
            tint = metalEngravedBurr(isDark),
            modifier = Modifier.offset(y = 1.dp),
        )
        Icon(
            imageVector = imageVector,
            contentDescription = contentDescription,
            tint = tint,
        )
    }
}

/** 同上，Painter 版（底栏的 tab 图标是 Painter）。 */
@Composable
fun MetalEngravedIcon(
    painter: Painter,
    contentDescription: String?,
    tint: Color,
    isDark: Boolean,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier) {
        Icon(
            painter = painter,
            contentDescription = null,
            tint = metalEngravedBurr(isDark),
            modifier = Modifier.offset(y = 1.dp),
        )
        Icon(
            painter = painter,
            contentDescription = contentDescription,
            tint = tint,
        )
    }
}
