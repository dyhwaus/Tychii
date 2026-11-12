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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.unit.dp
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.Checkbox
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.overlay.OverlayDialog

private enum class State {
    CHECKED,
    INVERSED,
    UNCHECKED,
}

@Composable
fun <T> TriStateListDialog(
    title: String,
    message: String? = null,
    items: List<T>,
    initialChecked: List<T>,
    initialInversed: List<T>,
    itemLabel: @Composable (T) -> String,
    onDismissRequest: () -> Unit,
    onValueChanged: (newIncluded: List<T>, newExcluded: List<T>) -> Unit,
) {
    val selected = remember {
        items
            .map {
                when (it) {
                    in initialChecked -> State.CHECKED
                    in initialInversed -> State.INVERSED
                    else -> State.UNCHECKED
                }
            }
            .toMutableStateList()
    }
    OverlayDialog(
        show = true,
        title = title,
        onDismissRequest = onDismissRequest,
    ) {
        Column {
            if (message != null) {
                Text(
                    text = message,
                    modifier = Modifier.padding(bottom = 8.dp),
                )
            }

            LazyColumn(modifier = Modifier.heightIn(max = 400.dp)) {
                itemsIndexed(items = items) { index, item ->
                    val state = selected[index]
                    val cycle = {
                        selected[index] = when (state) {
                            State.UNCHECKED -> State.CHECKED
                            State.CHECKED -> State.INVERSED
                            State.INVERSED -> State.UNCHECKED
                        }
                    }
                    Row(
                        modifier = Modifier
                            .clickable(onClick = cycle)
                            .defaultMinSize(minHeight = 48.dp)
                            .fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Checkbox(
                            state = when (state) {
                                State.UNCHECKED -> ToggleableState.Off
                                State.CHECKED -> ToggleableState.On
                                State.INVERSED -> ToggleableState.Indeterminate
                            },
                            onClick = cycle,
                            modifier = Modifier.padding(end = 20.dp),
                        )
                        Text(text = itemLabel(item))
                    }
                }
            }

            Column(modifier = Modifier.padding(top = 12.dp)) {
                Button(
                    onClick = {
                        val included = items.mapIndexedNotNull { index, category ->
                            if (selected[index] == State.CHECKED) category else null
                        }
                        val excluded = items.mapIndexedNotNull { index, category ->
                            if (selected[index] == State.INVERSED) category else null
                        }
                        onValueChanged(included, excluded)
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
