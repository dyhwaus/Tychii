package eu.kanade.presentation.more.settings.widget

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.unit.dp
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.Checkbox
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.overlay.OverlayDialog

@Composable
fun <T> MultiSelectListPreferenceWidget(
    values: Set<T>,
    title: String,
    subtitle: String?,
    icon: ImageVector?,
    entries: Map<out T, String>,
    onValuesChange: (Set<T>) -> Unit,
) {
    var isDialogShown by remember { mutableStateOf(false) }

    TextPreferenceWidget(
        title = title,
        subtitle = subtitle,
        icon = icon,
        onPreferenceClick = { isDialogShown = true },
    )

    if (isDialogShown) {
        MultiSelectListPreferenceDialog(
            title = title,
            values = values,
            entries = entries,
            onValuesChange = onValuesChange,
            onDismissRequest = { isDialogShown = false },
        )
    }
}

@Composable
fun <T> MultiSelectListPreferenceDialog(
    title: String?,
    values: Set<T>,
    entries: Map<out T, String>,
    onValuesChange: (Set<T>) -> Unit,
    onDismissRequest: () -> Unit,
    message: String? = null,
) {
    val selected = remember {
        entries.keys
            .filter { values.contains(it) }
            .toMutableStateList()
    }
    OverlayDialog(
        show = true,
        title = title,
        onDismissRequest = onDismissRequest,
    ) {
        Column {
            if (message != null) PreferenceDialogMessage(message)
            LazyColumn(modifier = Modifier.heightIn(max = 400.dp)) {
                entries.forEach { current ->
                    item {
                        val isSelected = selected.contains(current.key)
                        val toggle = {
                            if (isSelected) {
                                selected.remove(current.key)
                            } else {
                                selected.add(current.key)
                            }
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clickable(onClick = toggle)
                                .defaultMinSize(minHeight = 48.dp)
                                .fillMaxWidth(),
                        ) {
                            Checkbox(
                                state = if (isSelected) ToggleableState.On else ToggleableState.Off,
                                onClick = toggle,
                                modifier = Modifier.padding(end = 20.dp),
                            )
                            Text(text = current.value)
                        }
                    }
                }
            }
            Column(modifier = Modifier.padding(top = 12.dp)) {
                Button(
                    onClick = {
                        // Values without an entry stay, as they can't be unselected here
                        onValuesChange(values.filterNot { it in entries.keys }.toMutableSet().apply { addAll(selected) })
                        onDismissRequest()
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(text = stringResource(MR.strings.action_ok))
                }
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = onDismissRequest,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(text = stringResource(MR.strings.action_cancel))
                }
            }
        }
    }
}
