package eu.kanade.presentation.more.settings.widget

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.StartOffset
import androidx.compose.animation.core.StartOffsetType
import androidx.compose.animation.core.repeatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import top.yukonga.miuix.kmp.basic.BasicComponent
import kotlin.time.Duration.Companion.seconds

@Composable
internal fun BasePreferenceWidget(
    modifier: Modifier = Modifier,
    title: String? = null,
    subcomponent: @Composable (ColumnScope.() -> Unit)? = null,
    icon: @Composable (() -> Unit)? = null,
    onClick: (() -> Unit)? = null,
    widget: @Composable (() -> Unit)? = null,
    enabled: Boolean = true,
) {
    BasicComponent(
        modifier = modifier,
        title = title,
        startAction = icon,
        endActions = widget?.let { { it() } },
        bottomAction = subcomponent?.let {
            {
                Column { it() }
            }
        },
        onClick = onClick,
        enabled = enabled,
    )
}

/**
 * A preference row icon in the Miuix startAction slot: Material icon in the
 * theme primary color. BasicComponent already leaves an 8.dp gap after the
 * startAction, the extra end padding matches the earlier Miuix attempt.
 */
@Composable
internal fun PreferenceIcon(
    icon: ImageVector,
    tint: Color = MaterialTheme.colorScheme.primary,
) {
    Icon(
        imageVector = icon,
        contentDescription = null,
        tint = tint,
        modifier = Modifier.padding(end = 6.dp),
    )
}

@Composable
internal fun Modifier.highlightBackground(highlighted: Boolean): Modifier {
    var highlightFlag by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        if (highlighted) {
            highlightFlag = true
            delay(3.seconds)
            highlightFlag = false
        }
    }
    val highlight by animateColorAsState(
        targetValue = if (highlightFlag) {
            MaterialTheme.colorScheme.surfaceTint.copy(alpha = .12f)
        } else {
            Color.Transparent
        },
        animationSpec = if (highlightFlag) {
            repeatable(
                iterations = 5,
                animation = tween(durationMillis = 200),
                repeatMode = RepeatMode.Reverse,
                initialStartOffset = StartOffset(
                    offsetMillis = 600,
                    offsetType = StartOffsetType.Delay,
                ),
            )
        } else {
            tween(200)
        },
        label = "highlight",
    )
    return this.background(color = highlight)
}

internal val TrailingWidgetBuffer = 16.dp
internal val PrefsHorizontalPadding = 16.dp
