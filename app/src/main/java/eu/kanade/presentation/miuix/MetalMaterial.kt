// 精密金属：Apple 式做法——精密、克制、细腻、轻盈。银白 Silver
// （浅色）、中性钛灰 Titanium（深色，不是 Silver 的反相），阳极
// 氧化 Anodized 只做选中态的微主题色反射。低反射、细拉丝
// （2%–5%）、柔和倒角，不做强高光带、硬倒角和斜向扫光；动态反射
// 默认关闭。文字和图标仍是刻进去的：深色刻痕，下方 1px 亮边。
// 金属件的选中态是凸起的开关（底栏）或压下去的凹槽（章节导航），
// 不是一块玻璃 pill。
//
// 和液态玻璃是两种物理逻辑：玻璃透明，透出背后的页面；金属不透明，
// 靠表面质感立起来。因为不透明，金属件不需要录背景来模糊：这个
// 模式下录制层直接不录，顶渐隐也只剩一道渐变面纱。全部用 Compose
// 的渐变和线条画，不贴位图。

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
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import top.yukonga.miuix.kmp.theme.MiuixTheme

// 基底：纵向柔和渐变，上亮下暗，像光从上面落下来。Silver 是银白、
// 浅灰，不铺纯白；Titanium 是中性钛灰，不偏蓝不偏紫，顶比原来的
// 暗底亮一档，暗色主题里边缘才读得出来。两档对比都低，没有强渐变。
val MetalLightTop = Color(0xFFF4F5F6)
val MetalLightBottom = Color(0xFFE1E3E6)
val MetalDarkTop = Color(0xFF3B3B3D)
val MetalDarkBottom = Color(0xFF262628)

// 凹槽的底色：比基底暗一档，配内阴影就是冲压下去的样子。
val MetalRecessLight = Color(0xFFC2C4C8)
val MetalRecessDark = Color(0xFF17191B)

/** 金属颜色跟 Miuix 表面走，不跟系统主题：Miuix 的深色背景才是件的底色。 */
@Composable
fun metalIsDark(): Boolean = MiuixTheme.colorScheme.background.luminance() < 0.5f

/**
 * 精密金属表面：柔和渐变基底 + 2%–5% 细拉丝 + 柔和方向性反射 +
 * 窄而柔和的倒角。只画表面，不带阴影——浮起的阴影由调用方
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
        // 倒角：窄而柔和的外缘高光，上沿略亮、下沿略暗，沿着圆角走。
        // 精密金属的倒角是加工出来的薄边，不是被光切出来的硬边。
        val chamfer = Brush.verticalGradient(
            0f to Color.White.copy(alpha = 0.30f),
            0.5f to Color.Transparent,
            1f to Color.Black.copy(alpha = 0.20f),
        )
        // 方向性反射：光从上面落下来，柔和、只有一个方向，顶亮底暗，
        // 到一半处消失。没有斜向扫光——低反射是精密金属的底线。
        val reflection = Brush.verticalGradient(
            0f to Color.White.copy(alpha = 0.05f),
            0.5f to Color.Transparent,
        )
        val spacing = 2.dp.toPx()
        val lineCount = (size.height / spacing).toInt() + 1
        onDrawBehind {
            clipPath(path) {
                drawRect(brush = base)
                // 细拉丝：1px 横线，2%–5% 不透明度，黑白交错。每条线
                // 的强度、起点、落点都由行号的哈希定——固定下来，
                // 跨帧不闪；线有轻有重、有起有落，不会每条都从左拉
                // 到右。正常阅读距离下几乎看不见，近看可辨。
                for (i in 0 until lineCount) {
                    val y = i * spacing
                    val hashA = ((i * 2654435761L) % 1000L) / 1000f
                    val hashB = ((i * 40503L) % 1000L) / 1000f
                    val hashC = ((i * 65599L) % 1000L) / 1000f
                    val alpha = 0.02f + hashA * 0.03f
                    val color = if (i % 2 == 0) Color.White else Color.Black
                    drawLine(
                        color = color.copy(alpha = alpha),
                        start = Offset(hashB * size.width * 0.1f, y),
                        end = Offset(size.width - hashC * size.width * 0.1f, y),
                        strokeWidth = 1f,
                    )
                }
                drawRect(brush = reflection)
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

/** 无色刻字两道边的暗边：比共用的毛刺更重，字才够清晰。 */
fun metalCarvedDark(isDark: Boolean): Color =
    Color.Black.copy(alpha = if (isDark) 0.65f else 0.6f)

/** 无色刻字两道边的亮边：同上。 */
fun metalCarvedLight(isDark: Boolean): Color =
    Color.White.copy(alpha = if (isDark) 0.5f else 0.85f)

/**
 * A faint body fill for carved text: the glyphs are filled
 * with a whisper of dark (light theme) / light (dark theme),
 * like an ink-filled engraving, so the letterforms read
 * against the metal instead of being the metal itself.
 */
fun metalCarvedFill(isDark: Boolean): Color =
    if (isDark) Color.White.copy(alpha = 0.2f) else Color.Black.copy(alpha = 0.2f)

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
 * 刻进去的图标（无色版）：和 MetalEngravedText 一样的刻字——
 * 两道错开 1.dp 的边，图标本身抠掉，膛里只剩金属。
 * 注意抠字只能用 DstOut，不能用 Clear：Clear 是按图层整个
 * 矩形擦的，会把两道边一起擦掉、图标直接消失；DstOut 只擦
 * 字形盖住的地方（目标 × (1 - 来源透明度)）。卡片标题用的
 * 是画调用里的 Clear——那是按字形擦的，所以没事。
 * DstOut 靠 RenderNode 的 blendMode（API 29+ 才支持）；低版本
 * 上抠不掉，主图标会画成实心黑——可读，但不是刻字。
 */
@Composable
fun MetalCarvedIcon(
    imageVector: ImageVector,
    contentDescription: String?,
    isDark: Boolean,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.graphicsLayer {
            compositingStrategy = CompositingStrategy.Offscreen
        },
    ) {
        Icon(
            imageVector = imageVector,
            contentDescription = null,
            tint = metalCarvedDark(isDark),
            modifier = Modifier.offset(y = -1.dp),
        )
        Icon(
            imageVector = imageVector,
            contentDescription = null,
            tint = metalCarvedLight(isDark),
            modifier = Modifier.offset(y = 1.dp),
        )
        Icon(
            imageVector = imageVector,
            contentDescription = contentDescription,
            tint = Color.Black,
            modifier = Modifier.graphicsLayer { blendMode = BlendMode.DstOut },
        )
    }
}

/** 同上，Painter 版（底栏的 tab 图标是 Painter）。 */
@Composable
fun MetalCarvedIcon(
    painter: Painter,
    contentDescription: String?,
    isDark: Boolean,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.graphicsLayer {
            compositingStrategy = CompositingStrategy.Offscreen
        },
    ) {
        Icon(
            painter = painter,
            contentDescription = null,
            tint = metalCarvedDark(isDark),
            modifier = Modifier.offset(y = -1.dp),
        )
        Icon(
            painter = painter,
            contentDescription = null,
            tint = metalCarvedLight(isDark),
            modifier = Modifier.offset(y = 1.dp),
        )
        Icon(
            painter = painter,
            contentDescription = contentDescription,
            tint = Color.Black,
            modifier = Modifier.graphicsLayer { blendMode = BlendMode.DstOut },
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
            val dark = metalCarvedDark(isDark)
            val light = metalCarvedLight(isDark)
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

/**
 * Carved icon pre-rendered once into a bitmap: the two edges and
 * the punch-out are drawn into an offscreen bitmap instead of a
 * live offscreen RenderNode layer, so the tab bar no longer pays
 * a layer per icon on every frame. The glyph is drawn once into
 * a scratch bitmap and reused for the two edge copies and the
 * punch-out, mirroring MetalEngravedText's saveLayer + Clear.
 * The bitmap is 1.dp taller on each side than the icon so the
 * offset edges are not clipped, and the glyph stays centred.
 * Do not use for animated painters — a bitmap freezes them at
 * one frame (the Library tab icon animates on selection).
 */
@Composable
fun rememberCarvedIconBitmap(
    painter: Painter,
    isDark: Boolean,
    iconSize: Dp = 24.dp,
): ImageBitmap {
    val density = LocalDensity.current
    val layoutDirection = LocalLayoutDirection.current
    return remember(painter, isDark, density.density, density.fontScale) {
        val edgePx = with(density) { 1.dp.toPx() }
        val iconPx = with(density) { iconSize.toPx() }.toInt()
        val width = iconPx
        val height = iconPx + (edgePx * 2).toInt()
        val glyph = ImageBitmap(width, iconPx).apply {
            val canvas = Canvas(this)
            CanvasDrawScope().draw(
                density = density,
                layoutDirection = layoutDirection,
                canvas = canvas,
                size = Size(width.toFloat(), iconPx.toFloat()),
            ) {
                // DrawScope has no draw(painter, ...) extension —
                // a Painter draws itself: with(painter) { draw() }.
                with(painter) {
                    draw(
                        size = Size(iconPx.toFloat(), iconPx.toFloat()),
                        colorFilter = ColorFilter.tint(Color.Black),
                    )
                }
            }
        }
        ImageBitmap(width, height).apply {
            val canvas = Canvas(this)
            CanvasDrawScope().draw(
                density = density,
                layoutDirection = layoutDirection,
                canvas = canvas,
                size = Size(width.toFloat(), height.toFloat()),
            ) {
                // Offscreen layer: Clear only erases the edge
                // copies inside it, never the metal beneath.
                drawContext.canvas.saveLayer(Rect(Offset.Zero, size), Paint())
                drawImage(
                    image = glyph,
                    dstOffset = IntOffset(0, 0),
                    dstSize = IntSize(width, iconPx),
                    colorFilter = ColorFilter.tint(metalCarvedDark(isDark)),
                )
                drawImage(
                    image = glyph,
                    dstOffset = IntOffset(0, (edgePx * 2).toInt()),
                    dstSize = IntSize(width, iconPx),
                    colorFilter = ColorFilter.tint(metalCarvedLight(isDark)),
                )
                drawImage(
                    image = glyph,
                    dstOffset = IntOffset(0, edgePx.toInt()),
                    dstSize = IntSize(width, iconPx),
                    blendMode = BlendMode.Clear,
                )
                drawContext.canvas.restore()
            }
        }
    }
}

/**
 * Carved label pre-rendered once into a bitmap, same idea as
 * rememberCarvedIconBitmap: the tab bar no longer pays a Canvas
 * saveLayer per label. Measured with the same style and maxLines
 * as the fill text underneath, so the carving lines up with it;
 * the bitmap is 1.dp taller on each side so the offset edges
 * are not clipped.
 */
@Composable
fun rememberCarvedTextBitmap(
    text: String,
    style: TextStyle,
    isDark: Boolean,
    maxLines: Int = 1,
): ImageBitmap {
    val density = LocalDensity.current
    val layoutDirection = LocalLayoutDirection.current
    val textMeasurer = rememberTextMeasurer()
    return remember(text, style, isDark, maxLines, density.density, density.fontScale) {
        val layout = textMeasurer.measure(
            text = AnnotatedString(text),
            style = style,
            overflow = TextOverflow.Ellipsis,
            maxLines = maxLines,
        )
        val edgePx = with(density) { 1.dp.toPx() }
        val width = layout.size.width
        val height = layout.size.height + (edgePx * 2).toInt()
        ImageBitmap(width, height).apply {
            val canvas = Canvas(this)
            CanvasDrawScope().draw(
                density = density,
                layoutDirection = layoutDirection,
                canvas = canvas,
                size = Size(width.toFloat(), height.toFloat()),
            ) {
                drawContext.canvas.saveLayer(Rect(Offset.Zero, size), Paint())
                drawText(layout, topLeft = Offset(0f, 0f), color = metalCarvedDark(isDark))
                drawText(layout, topLeft = Offset(0f, edgePx * 2), color = metalCarvedLight(isDark))
                drawText(layout, topLeft = Offset(0f, edgePx), blendMode = BlendMode.Clear)
                drawContext.canvas.restore()
            }
        }
    }
}
