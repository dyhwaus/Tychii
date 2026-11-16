// Adapted from https://github.com/YuKongA/compose-miuix-ui
// Copyright YuKongA, Apache-2.0
// 移植自 SukiSU-Ultra 的 ui/component/miuix/animation/DampedDragAnimation.kt
// （其改编自 compose-miuix-ui 示例，Apache-2.0）。
// 点按/长按的识别是本项目自己写的：累计位移没超过 touchSlop 认点按，
// 按住不动超过长按时限认长按（行为对齐 WeKite 的 pill，但实现没有
// 照搬它那份 GPL 代码）。

package eu.kanade.presentation.miuix.liquid

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.MutatorMutex
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.unit.IntSize
import kotlin.math.abs
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.android.awaitFrame
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * 带阻尼的拖动动画：底栏滑块用它实现"按下跟手、松手吸附、按住时膨胀"。
 *
 * 与 `Animatable` 的区别在于它同时维护速度、按压进度与 X/Y 两个方向的缩放，
 * 并由 [modifier] 统一挂上拖动手势。
 */
internal class DampedDragAnimation(
    private val animationScope: CoroutineScope,
    val initialValue: Float,
    val valueRange: ClosedRange<Float>,
    val visibilityThreshold: Float,
    val initialScale: Float,
    val pressedScale: Float,
    val canDrag: (Offset) -> Boolean = { true },
    val onDragStarted: DampedDragAnimation.(position: Offset) -> Unit,
    val onDragStopped: DampedDragAnimation.() -> Unit,
    val onDrag: DampedDragAnimation.(size: IntSize, dragAmount: Offset) -> Unit,
    // 手指没拖动就松开时回调（点按 pill）。pill 盖在选中 tab 上面，
    // tab 自己的 onClick 收不到这次点按，靠这里转出去。
    val onTap: DampedDragAnimation.() -> Unit = {},
    // 按住 pill 不动超过长按时限时回调；同理是给 tab 的长按转用的。
    val onLongPress: DampedDragAnimation.() -> Unit = {},
) {

    private val valueAnimationSpec = spring(1f, 1000f, visibilityThreshold)
    private val velocityAnimationSpec = spring(0.5f, 300f, visibilityThreshold * 10f)
    private val pressProgressAnimationSpec = spring(1f, 1000f, 0.001f)
    private val scaleXAnimationSpec = spring(0.6f, 250f, 0.001f)
    private val scaleYAnimationSpec = spring(0.7f, 250f, 0.001f)

    private val valueAnimation = Animatable(initialValue, visibilityThreshold)
    private val velocityAnimation = Animatable(0f, 5f)
    private val pressProgressAnimation = Animatable(0f, 0.001f)
    private val scaleXAnimation = Animatable(initialScale, 0.001f)
    private val scaleYAnimation = Animatable(initialScale, 0.001f)

    private val mutatorMutex = MutatorMutex()
    private val velocityTracker = VelocityTracker()

    val value: Float get() = valueAnimation.value
    val targetValue: Float get() = valueAnimation.targetValue
    val pressProgress: Float get() = pressProgressAnimation.value
    val scaleX: Float get() = scaleXAnimation.value
    val scaleY: Float get() = scaleYAnimation.value
    val velocity: Float get() = velocityAnimation.value

    val modifier: Modifier = Modifier.pointerInput(Unit) {
        // 点按与长按的认法和系统手势一致：松手时累计位移没超过
        // touchSlop 就是点按；按住不动超过长按时限就是长按，拖
        // 动超过 touchSlop 就停掉长按计时。
        val tapSlop = viewConfiguration.touchSlop
        val longPressTimeoutMs = viewConfiguration.longPressTimeoutMillis
        var draggedPx = 0f
        var longPressJob: Job? = null
        var longPressFired = false
        inspectDragGestures(
            onDragStart = { down ->
                draggedPx = 0f
                longPressFired = false
                onDragStarted(down.position)
                press()
                longPressJob = animationScope.launch {
                    delay(longPressTimeoutMs)
                    longPressFired = true
                    onLongPress()
                }
            },
            onDragEnd = {
                longPressJob?.cancel()
                longPressJob = null
                onDragStopped()
                release()
                if (!longPressFired && draggedPx <= tapSlop) {
                    onTap()
                }
            },
            onDragCancel = {
                longPressJob?.cancel()
                longPressJob = null
                longPressFired = false
                onDragStopped()
                release()
            },
        ) { change, dragAmount ->
            val position = change.position
            val previousPosition = change.previousPosition

            val isInside = canDrag(position)
            val wasInside = canDrag(previousPosition)

            if (isInside && wasInside) {
                draggedPx += abs(dragAmount.x) + abs(dragAmount.y)
                onDrag(size, dragAmount)
                if (draggedPx > tapSlop) {
                    longPressJob?.cancel()
                    longPressJob = null
                }
            }
        }
    }

    fun press() {
        velocityTracker.resetTracking()
        animationScope.launch {
            launch { pressProgressAnimation.animateTo(1f, pressProgressAnimationSpec) }
            launch { scaleXAnimation.animateTo(pressedScale, scaleXAnimationSpec) }
            launch { scaleYAnimation.animateTo(pressedScale, scaleYAnimationSpec) }
        }
    }

    fun release() {
        animationScope.launch {
            awaitFrame()
            if (value != targetValue) {
                val threshold = (valueRange.endInclusive - valueRange.start) * 0.025f
                snapshotFlow { valueAnimation.value }
                    .first { abs(it - valueAnimation.targetValue) < threshold }
            }
            launch { pressProgressAnimation.animateTo(0f, pressProgressAnimationSpec) }
            launch { scaleXAnimation.animateTo(initialScale, scaleXAnimationSpec) }
            launch { scaleYAnimation.animateTo(initialScale, scaleYAnimationSpec) }
        }
    }

    fun updateValue(value: Float) {
        val targetValue = value.coerceIn(valueRange)
        animationScope.launch {
            launch { valueAnimation.animateTo(targetValue, valueAnimationSpec) { updateVelocity() } }
        }
    }

    fun animateToValue(value: Float) {
        animationScope.launch {
            mutatorMutex.mutate {
                press()
                val targetValue = value.coerceIn(valueRange)
                launch { valueAnimation.animateTo(targetValue, valueAnimationSpec) }
                if (velocity != 0f) {
                    launch { velocityAnimation.animateTo(0f, velocityAnimationSpec) }
                }
                release()
            }
        }
    }

    private fun updateVelocity() {
        velocityTracker.addPosition(
            System.currentTimeMillis(),
            Offset(value, 0f),
        )
        val targetVelocity =
            velocityTracker.calculateVelocity().x / (valueRange.endInclusive - valueRange.start)
        animationScope.launch { velocityAnimation.animateTo(targetVelocity, velocityAnimationSpec) }
    }
}
