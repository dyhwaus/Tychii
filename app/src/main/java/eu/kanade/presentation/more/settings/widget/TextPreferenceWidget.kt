package eu.kanade.presentation.more.settings.widget

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import top.yukonga.miuix.kmp.preference.ArrowPreference

@Composable
fun TextPreferenceWidget(
    modifier: Modifier = Modifier,
    title: String? = null,
    subtitle: String? = null,
    icon: ImageVector? = null,
    iconTint: Color = MaterialTheme.colorScheme.primary,
    widget: @Composable (() -> Unit)? = null,
    enabled: Boolean = true,
    onPreferenceClick: (() -> Unit)? = null,
) {
    ArrowPreference(
        title = title ?: "",
        modifier = modifier,
        summary = subtitle,
        startAction = icon?.let {
            {
                Icon(
                    imageVector = it,
                    tint = iconTint,
                    contentDescription = null,
                    modifier = Modifier.padding(end = 6.dp),
                )
            }
        },
        endActions = {
            widget?.invoke()
        },
        onClick = onPreferenceClick,
        enabled = enabled,
    )
}
