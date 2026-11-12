package eu.kanade.presentation.more.settings.widget

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import eu.kanade.presentation.more.settings.LocalPreferenceHighlighted
import top.yukonga.miuix.kmp.preference.CheckboxLocation
import top.yukonga.miuix.kmp.preference.CheckboxPreference

@Composable
fun CheckboxPreferenceWidget(
    modifier: Modifier = Modifier,
    title: String,
    subtitle: String? = null,
    icon: ImageVector? = null,
    checked: Boolean = false,
    enabled: Boolean = true,
    onCheckedChanged: (Boolean) -> Unit,
) {
    CheckboxPreference(
        title = title,
        checked = checked,
        onCheckedChange = onCheckedChanged,
        summary = subtitle,
        modifier = modifier.highlightBackground(LocalPreferenceHighlighted.current),
        startAction = icon?.let { { PreferenceIcon(it) } },
        checkboxLocation = CheckboxLocation.End,
        enabled = enabled,
    )
}
