package eu.kanade.presentation.category.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import eu.kanade.core.preference.asToggleableState
import eu.kanade.presentation.category.visualName
import tachiyomi.core.common.preference.CheckboxState
import tachiyomi.domain.category.model.Category
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.material.padding
import tachiyomi.presentation.core.i18n.stringResource
import top.yukonga.miuix.kmp.basic.Checkbox
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.overlay.OverlayDialog

@Composable
fun CategoryCreateDialog(
    onDismissRequest: () -> Unit,
    onCreate: (String) -> Unit,
    categories: List<String>,
) {
    var name by remember { mutableStateOf("") }

    val focusRequester = remember { FocusRequester() }
    val nameAlreadyExists = remember(name) { categories.contains(name) }

    // The dialog shell is Miuix; the text field stays Material3 because
    // Miuix TextField has no supportingText / isError, and the duplicate-name
    // message under the field is part of the UI.
    OverlayDialog(
        show = true,
        title = stringResource(MR.strings.action_add_category),
        onDismissRequest = onDismissRequest,
    ) {
        OutlinedTextField(
            modifier = Modifier
                .focusRequester(focusRequester),
            value = name,
            onValueChange = { name = it },
            label = {
                Text(text = stringResource(MR.strings.name))
            },
            supportingText = {
                val msgRes = if (name.isNotEmpty() && nameAlreadyExists) {
                    MR.strings.error_category_exists
                } else {
                    MR.strings.information_required_plain
                }
                Text(text = stringResource(msgRes))
            },
            isError = name.isNotEmpty() && nameAlreadyExists,
            singleLine = true,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
        ) {
            TextButton(
                text = stringResource(MR.strings.action_cancel),
                onClick = onDismissRequest,
            )
            TextButton(
                text = stringResource(MR.strings.action_add),
                enabled = name.isNotEmpty() && !nameAlreadyExists,
                onClick = {
                    onCreate(name)
                    onDismissRequest()
                },
            )
        }
    }

    LaunchedEffect(focusRequester) {
        focusRequester.requestFocus()
    }
}

@Composable
fun CategoryRenameDialog(
    onDismissRequest: () -> Unit,
    onRename: (String) -> Unit,
    categories: List<String>,
    category: String,
) {
    var name by remember { mutableStateOf(category) }
    var valueHasChanged by remember { mutableStateOf(false) }

    val focusRequester = remember { FocusRequester() }
    val nameAlreadyExists = remember(name) { categories.contains(name) }

    OverlayDialog(
        show = true,
        title = stringResource(MR.strings.action_rename_category),
        onDismissRequest = onDismissRequest,
    ) {
        OutlinedTextField(
            modifier = Modifier.focusRequester(focusRequester),
            value = name,
            onValueChange = {
                valueHasChanged = name != it
                name = it
            },
            label = { Text(text = stringResource(MR.strings.name)) },
            supportingText = {
                val msgRes = if (valueHasChanged && nameAlreadyExists) {
                    MR.strings.error_category_exists
                } else {
                    MR.strings.information_required_plain
                }
                Text(text = stringResource(msgRes))
            },
            isError = valueHasChanged && nameAlreadyExists,
            singleLine = true,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
        ) {
            TextButton(
                text = stringResource(MR.strings.action_cancel),
                onClick = onDismissRequest,
            )
            TextButton(
                text = stringResource(MR.strings.action_ok),
                enabled = valueHasChanged && name.isNotEmpty() && !nameAlreadyExists,
                onClick = {
                    onRename(name)
                    onDismissRequest()
                },
            )
        }
    }

    LaunchedEffect(focusRequester) {
        focusRequester.requestFocus()
    }
}

@Composable
fun CategoryDeleteDialog(
    onDismissRequest: () -> Unit,
    onDelete: () -> Unit,
    category: String,
) {
    OverlayDialog(
        show = true,
        title = stringResource(MR.strings.delete_category),
        summary = stringResource(MR.strings.delete_category_confirmation, category),
        onDismissRequest = onDismissRequest,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
        ) {
            TextButton(
                text = stringResource(MR.strings.action_cancel),
                onClick = onDismissRequest,
            )
            TextButton(
                text = stringResource(MR.strings.action_ok),
                onClick = {
                    onDelete()
                    onDismissRequest()
                },
            )
        }
    }
}

@Composable
fun ChangeCategoryDialog(
    initialSelection: List<CheckboxState<Category>>,
    onDismissRequest: () -> Unit,
    onEditCategories: () -> Unit,
    onConfirm: (List<Long>, List<Long>) -> Unit,
) {
    if (initialSelection.isEmpty()) {
        OverlayDialog(
            show = true,
            title = stringResource(MR.strings.action_move_category),
            summary = stringResource(MR.strings.information_empty_category_dialog),
            onDismissRequest = onDismissRequest,
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                TextButton(
                    text = stringResource(MR.strings.action_edit_categories),
                    onClick = {
                        onDismissRequest()
                        onEditCategories()
                    },
                )
            }
        }
        return
    }
    var selection by remember { mutableStateOf(initialSelection) }
    OverlayDialog(
        show = true,
        title = stringResource(MR.strings.action_move_category),
        onDismissRequest = onDismissRequest,
    ) {
        Column(
            modifier = Modifier.verticalScroll(rememberScrollState()),
        ) {
                selection.forEach { checkbox ->
                    val onChange: (CheckboxState<Category>) -> Unit = {
                        val index = selection.indexOf(it)
                        if (index != -1) {
                            val mutableList = selection.toMutableList()
                            mutableList[index] = it.next()
                            selection = mutableList.toList()
                        }
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onChange(checkbox) },
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        when (checkbox) {
                            is CheckboxState.TriState -> {
                                Checkbox(
                                    state = checkbox.asToggleableState(),
                                    onClick = { onChange(checkbox) },
                                )
                            }
                            is CheckboxState.State -> {
                                Checkbox(
                                    state = if (checkbox.isChecked) ToggleableState.On else ToggleableState.Off,
                                    onClick = { onChange(checkbox) },
                                )
                            }
                        }

                        Text(
                            text = checkbox.value.visualName,
                            modifier = Modifier.padding(horizontal = MaterialTheme.padding.medium),
                        )
                    }
                }
            }
        Row {
            TextButton(
                text = stringResource(MR.strings.action_edit),
                onClick = {
                    onDismissRequest()
                    onEditCategories()
                },
            )
            Spacer(modifier = Modifier.weight(1f))
            TextButton(
                text = stringResource(MR.strings.action_cancel),
                onClick = onDismissRequest,
            )
            TextButton(
                text = stringResource(MR.strings.action_ok),
                onClick = {
                    onDismissRequest()
                    onConfirm(
                        selection
                            .filter { it is CheckboxState.State.Checked || it is CheckboxState.TriState.Include }
                            .map { it.value.id },
                        selection
                            .filter { it is CheckboxState.State.None || it is CheckboxState.TriState.None }
                            .map { it.value.id },
                    )
                },
            )
        }
    }
}
