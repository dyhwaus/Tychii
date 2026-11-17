// 铝合金五金件：Tychii 的拟物材质，Smartisan 的做法——底栏不是一块
// “金属材质”，而是一件真实的物件，像 Smartisan 图标里的金属零件。
// 冷灰，不掺金不掺铜，贴合黑白。
//
// 和液态玻璃是两种物理逻辑：玻璃透明，透出背后的页面；金属不透明，
// 靠表面质感立起来——纵向柔和渐变打底，细腻的缎面纹理，顶部一道高光
// 带，上下倒角，左上一道静态柔光。铝比铬柔：没有镜面的亮带与暗反射
// 带，颜色均匀，纹理细而淡。文字和图标是刻进去的：深色刻痕，下方
// 1px 亮边。金属件的选中态是凸起的开关（底栏）或压下去的凹槽（章节
// 导航），不是一块玻璃 pill。
//
// 因为不透明，金属件不需要录背景来模糊：这个模式下录制层直接不录，
// 顶渐隐也只剩一道渐变面纱。全部用 Compose 的渐变和线条画，不贴位图。

package eu.kanade.presentation.miuix

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import top.yukonga.miuix.kmp.theme.MiuixTheme

// 基底：纵向柔和渐变，上亮下暗，像光从上面落下来。铝的颜色均匀，
// 没有铬的亮带与暗反射带；比原来的拉丝钢亮一档，对比更低。
val MetalLightTop = Color(0xFFF3F4F6)
val MetalLightBottom = Color(0xFFDCDEE1)
val MetalDarkTop = Color(0xFF34373B)
val MetalDarkBottom = Color(0xFF222427)

// 凹槽的底色：比基底暗一档，配内阴影就是冲压下去的样子。
val MetalRecessLight = Color(0xFFC2C4C8)
val MetalRecessDark = Color(0xFF17191B)

/** 金属颜色跟 Miuix 表面走，不跟系统主题：Miuix 的深色背景才是件的底色。 */
@Composable
fun metalIsDark(): Boolean = MiuixTheme.colorScheme.background.luminance() < 0.5f

/**
 * 铝合金五金件表面：柔和渐变基底 + 细腻缎面纹理 + 顶部高光带 +
 * 左上柔光 + 上下倒角。只画表面，不带阴影——浮起的阴影由调用方
 * 按原来的方式加。
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
        // 倒角：上沿亮线，下沿暗线，沿着圆角走。五金件的倒角硬，
        // 像被光切出来的边。
        val chamfer = Brush.verticalGradient(
            0f to Color.White.copy(alpha = 0.6f),
            0.45f to Color.Transparent,
            1f to Color.Black.copy(alpha = 0.4f),
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
        // 顶部高光带：光打在真实金属条的顶面上，柔和的白光从顶部
        // 漫下来，到三成处消失。没有它，铝面太平，不像一件东西。
        val topLight = Brush.verticalGradient(
            0f to Color.White.copy(alpha = 0.18f),
            0.3f to Color.Transparent,
        )
        val spacing = 2.dp.toPx()
        val lineCount = (size.height / spacing).toInt() + 1
        onDrawBehind {
            clipPath(path) {
                drawRect(brush = base)
                // 缎面纹理：1px 横线，黑白交错，比拉丝钢细而淡——
                // 铝是细腻的哑光面，不是粗拉丝。每条的 alpha 由行号
                // 的哈希定——固定下来，跨帧不闪。
                for (i in 0 until lineCount) {
                    val y = i * spacing
                    val hash = ((i * 2654435761L) % 1000L) / 1000f
                    val alpha = 0.01f + hash * 0.02f
                    val color = if (i % 2 == 0) Color.White else Color.Black
                    drawLine(
                        color = color.copy(alpha = alpha),
                        start = Offset(0f, y),
                        end = Offset(size.width, y),
                        strokeWidth = 1f,
                    )
                }
                drawRect(brush = sheen)
                drawRect(brush = topLight)
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

/**
 * 刻进去的文字：字本身无色——金属从字形里透出来，只留刻痕的
 * 两道边：上沿暗边，下沿亮边。主字不画填充色：先在离屏层里画
 * 两道错开 1.dp 的边，再把字形本身抠掉，字膛里就只剩金属。
 * 一层看不见的字负责定尺寸与换行，画出来的和单层字一样。
 */
@Composable
fun MetalEngravedText(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = LocalTextStyle.current,
    isDark: Boolean,
    minLines: Int = 1,
    maxLines: Int = Int.MAX_VALUE,
    overflow: TextOverflow = TextOverflow.Ellipsis,
) {
    var layout by remember { mutableStateOf<TextLayoutResult?>(null) }
    Box(modifier = modifier) {
        Text(
            text = text,
            color = Color.Transparent,
            style = style.copy(shadow = null),
            minLines = minLines,
            maxLines = maxLines,
            overflow = overflow,
            onTextLayout = { layout = it },
        )
        Canvas(modifier = Modifier.matchParentSize()) {
            val result = layout ?: return@Canvas
            val edge = 1.dp.toPx()
            val dark = Color.Black.copy(alpha = if (isDark) 0.4f else 0.35f)
            val light = metalEngravedBurr(isDark)
            // Offscreen layer: Clear only erases the edge copies
            // inside it, never the metal beneath.
            drawContext.canvas.saveLayer(Rect(Offset.Zero, size), Paint())
            drawText(result, topLeft = Offset(0f, -edge), color = dark)
            drawText(result, topLeft = Offset(0f, edge), color = light)
            drawText(result, topLeft = Offset.Zero, blendMode = BlendMode.Clear)
            drawContext.canvas.restore()
        }
    }
}
