package eu.kanade.presentation.more.settings.widget

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.ScrollbarLazyColumn
import tachiyomi.presentation.core.i18n.stringResource
import top.yukonga.miuix.kmp.basic.DropdownItem
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.overlay.OverlayDialog
import top.yukonga.miuix.kmp.preference.OverlaySpinnerPreference
import top.yukonga.miuix.kmp.preference.RadioButtonPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun <T> ListPreferenceWidget(
    value: T,
    title: String,
    subtitle: String?,
    icon: ImageVector?,
    entries: Map<out T, String>,
    onValueChange: (T) -> Unit,
) {
    val keys = remember(entries) { entries.keys.toList() }
    OverlaySpinnerPreference(
        items = keys.map { key -> DropdownItem(text = entries[key].orEmpty()) },
        selectedIndex = keys.indexOf(value).coerceAtLeast(0),
        title = title,
        summary = subtitle,
        startAction = icon?.let { { PreferenceIcon(it) } },
        // The current value is already shown as the subtitle.
        showValue = false,
        onSelectedIndexChange = { index -> onValueChange(keys[index]) },
    )
}

@Composable
fun <T> ListPreferenceDialog(
    title: String?,
    value: T,
    entries: Map<out T, String>,
    onValueChange: (T) -> Unit,
    onDismissRequest: () -> Unit,
    message: String? = null,
) {
    OverlayDialog(
        show = true,
        title = title,
        onDismissRequest = onDismissRequest,
    ) {
        Column {
            if (message != null) PreferenceDialogMessage(message)
            Box(modifier = Modifier.heightIn(max = 400.dp)) {
                val state = rememberLazyListState()
                ScrollbarLazyColumn(state = state) {
                    entries.forEach { current ->
                        val isSelected = value == current.key
                        item {
                            RadioButtonPreference(
                                title = current.value,
                                selected = isSelected,
                                onClick = {
                                    onValueChange(current.key)
                                    onDismissRequest()
                                },
                            )
                        }
                    }
                }
                if (state.canScrollBackward) HorizontalDivider(modifier = Modifier.align(Alignment.TopCenter))
                if (state.canScrollForward) HorizontalDivider(modifier = Modifier.align(Alignment.BottomCenter))
            }
            TextButton(
                text = stringResource(MR.strings.action_cancel),
                onClick = onDismissRequest,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
            )
        }
    }
}

@Composable
internal fun PreferenceDialogMessage(message: String) {
    Text(
        text = message,
        modifier = Modifier.padding(bottom = 8.dp),
        color = MiuixTheme.colorScheme.onSurface,
        fontSize = MaterialTheme.typography.bodyMedium.fontSize,
    )
}
