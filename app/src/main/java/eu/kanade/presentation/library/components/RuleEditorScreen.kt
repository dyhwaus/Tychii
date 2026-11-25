package eu.kanade.presentation.library.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import dev.icerock.moko.resources.StringResource
import eu.kanade.presentation.components.AppBar
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.ui.library.ReadingStateValue
import eu.kanade.tachiyomi.ui.library.RuleCondition
import eu.kanade.tachiyomi.ui.library.RuleField
import eu.kanade.tachiyomi.ui.library.parseLibrarySort
import tachiyomi.domain.library.model.LibrarySort
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.material.Scaffold
import tachiyomi.presentation.core.i18n.stringResource
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.DropdownItem
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.Switch
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.preference.OverlaySpinnerPreference

data class RuleEditorState(
    val name: String,
    val enabled: Boolean,
    val conditions: List<RuleCondition>,
    val sort: String?,
    val isNew: Boolean,
)

@Composable
fun RuleEditorScreen(
    state: RuleEditorState,
    onNameChange: (String) -> Unit,
    onEnabledChange: (Boolean) -> Unit,
    onConditionChange: (Int, RuleCondition) -> Unit,
    onAddCondition: () -> Unit,
    onDeleteCondition: (Int) -> Unit,
    onSortChange: (String?) -> Unit,
    onSave: () -> Unit,
    onDelete: () -> Unit,
    onCopy: () -> Unit,
    navigateUp: () -> Unit,
) {
    Scaffold(
        topBar = { scrollBehavior ->
            AppBar(
                title = if (state.isNew) {
                    stringResource(MR.strings.action_new_rule)
                } else {
                    state.name
                },
                navigateUp = navigateUp,
                scrollBehavior = scrollBehavior,
            )
        },
    ) { paddingValues ->
        LazyColumn(
            contentPadding = paddingValues,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item {
                TextField(
                    value = state.name,
                    onValueChange = onNameChange,
                    label = stringResource(MR.strings.label_rule_name),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp),
                )
            }
            item {
                Card(modifier = Modifier.padding(horizontal = 12.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Text(
                            text = stringResource(MR.strings.label_rule_enabled),
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.weight(1f),
                        )
                        Switch(
                            checked = state.enabled,
                            onCheckedChange = onEnabledChange,
                        )
                    }
                }
            }
            item {
                SmallTitle(text = stringResource(MR.strings.label_rule_conditions))
            }
            items(state.conditions.size) { index ->
                RuleConditionCard(
                    condition = state.conditions[index],
                    onConditionChange = { onConditionChange(index, it) },
                    onDelete = { onDeleteCondition(index) },
                )
            }
            item {
                Button(
                    onClick = onAddCondition,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp),
                ) {
                    Text(text = stringResource(MR.strings.action_add_condition))
                }
            }
            item {
                SmallTitle(text = stringResource(MR.strings.label_rule_sort))
            }
            item {
                RuleSortCard(
                    sort = state.sort,
                    onSortChange = onSortChange,
                )
            }
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Button(
                        onClick = onSave,
                        enabled = state.name.isNotBlank(),
                        colors = ButtonDefaults.buttonColorsPrimary(),
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(text = stringResource(MR.strings.action_save))
                    }
                    if (!state.isNew) {
                        TextButton(
                            text = stringResource(MR.strings.action_copy),
                            onClick = onCopy,
                        )
                        TextButton(
                            text = stringResource(MR.strings.action_delete),
                            onClick = onDelete,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RuleConditionCard(
    condition: RuleCondition,
    onConditionChange: (RuleCondition) -> Unit,
    onDelete: () -> Unit,
) {
    Card(modifier = Modifier.padding(horizontal = 12.dp)) {
        OverlaySpinnerPreference(
            items = RuleField.entries.map { DropdownItem(text = stringResource(it.titleRes)) },
            selectedIndex = condition.field.ordinal,
            title = stringResource(MR.strings.label_rule_field),
            onSelectedIndexChange = { index ->
                val field = RuleField.entries[index]
                onConditionChange(
                    condition.copy(
                        field = field,
                        operator = field.operators().first(),
                        value = field.defaultValue(),
                    ),
                )
            },
        )
        OverlaySpinnerPreference(
            items = condition.field.operators().map { DropdownItem(text = stringResource(it.titleRes)) },
            selectedIndex = condition.field.operators().indexOf(condition.operator).coerceAtLeast(0),
            title = stringResource(MR.strings.label_rule_operator),
            onSelectedIndexChange = { index ->
                onConditionChange(condition.copy(operator = condition.field.operators()[index]))
            },
        )
        when (condition.field) {
            RuleField.LAST_UPDATE_DAYS, RuleField.LAST_READ_DAYS, RuleField.UNREAD_COUNT -> {
                // The text field stays Material3, like the category
                // dialogs: Miuix TextField has no supportingText.
                OutlinedTextField(
                    value = condition.value,
                    onValueChange = { onConditionChange(condition.copy(value = it.filter(Char::isDigit))) },
                    label = { Text(text = stringResource(MR.strings.label_rule_value)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                )
            }
            else -> {
                val valueOptions = condition.field.valueOptions()
                OverlaySpinnerPreference(
                    items = valueOptions.map { DropdownItem(text = it.second) },
                    selectedIndex = valueOptions.indexOfFirst { it.first == condition.value }.coerceAtLeast(0),
                    title = stringResource(MR.strings.label_rule_value),
                    onSelectedIndexChange = { index ->
                        onConditionChange(condition.copy(value = valueOptions[index].first))
                    },
                )
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
        ) {
            TextButton(
                text = stringResource(MR.strings.action_delete),
                onClick = onDelete,
            )
        }
    }
}

@Composable
private fun RuleField.valueOptions(): List<Pair<String, String>> {
    return when (this) {
        RuleField.READING_STATE -> listOf(
            ReadingStateValue.READING.name to stringResource(MR.strings.smart_category_reading),
            ReadingStateValue.PAUSED.name to stringResource(MR.strings.smart_category_paused),
            ReadingStateValue.NOT_STARTED.name to stringResource(MR.strings.rule_reading_not_started),
            ReadingStateValue.FINISHED.name to stringResource(MR.strings.rule_reading_finished),
        )
        RuleField.STATUS -> listOf(
            SManga.ONGOING.toString() to stringResource(MR.strings.ongoing),
            SManga.COMPLETED.toString() to stringResource(MR.strings.completed),
            SManga.LICENSED.toString() to stringResource(MR.strings.licensed),
            SManga.PUBLISHING_FINISHED.toString() to stringResource(MR.strings.publishing_finished),
            SManga.CANCELLED.toString() to stringResource(MR.strings.cancelled),
            SManga.ON_HIATUS.toString() to stringResource(MR.strings.on_hiatus),
            SManga.UNKNOWN.toString() to stringResource(MR.strings.unknown),
        )
        RuleField.FAVORITE, RuleField.DOWNLOADED -> listOf(
            "true" to stringResource(MR.strings.label_yes),
            "false" to stringResource(MR.strings.label_no),
        )
        RuleField.LAST_UPDATE_DAYS, RuleField.LAST_READ_DAYS, RuleField.UNREAD_COUNT -> emptyList()
    }
}

// The sort types a rule can override with. Tracker score and random
// are left out: a rule's sort should be predictable.
private val ruleSortTypes: List<Pair<StringResource, LibrarySort.Type>> = listOf(
    MR.strings.action_sort_alpha to LibrarySort.Type.Alphabetical,
    MR.strings.action_sort_last_manga_update to LibrarySort.Type.LastUpdate,
    MR.strings.action_sort_last_read to LibrarySort.Type.LastRead,
    MR.strings.action_sort_unread_count to LibrarySort.Type.UnreadCount,
    MR.strings.action_sort_date_added to LibrarySort.Type.DateAdded,
    MR.strings.action_sort_total to LibrarySort.Type.TotalChapters,
)

@Composable
private fun RuleSortCard(
    sort: String?,
    onSortChange: (String?) -> Unit,
) {
    val currentSort = parseLibrarySort(sort)
    Card(modifier = Modifier.padding(horizontal = 12.dp)) {
        OverlaySpinnerPreference(
            items = listOf(DropdownItem(text = stringResource(MR.strings.label_rule_sort_default))) +
                ruleSortTypes.map { DropdownItem(text = stringResource(it.first)) },
            selectedIndex = if (currentSort == null) {
                0
            } else {
                ruleSortTypes.indexOfFirst { it.second == currentSort.type } + 1
            },
            title = stringResource(MR.strings.label_rule_sort),
            onSelectedIndexChange = { index ->
                if (index == 0) {
                    onSortChange(null)
                } else {
                    val type = ruleSortTypes[index - 1].second
                    val direction = if (type == LibrarySort.Type.Alphabetical) {
                        LibrarySort.Direction.Ascending
                    } else {
                        LibrarySort.Direction.Descending
                    }
                    onSortChange(LibrarySort(type, direction).serialize())
                }
            },
        )
        if (currentSort != null) {
            OverlaySpinnerPreference(
                items = listOf(
                    DropdownItem(text = stringResource(MR.strings.label_sort_ascending)),
                    DropdownItem(text = stringResource(MR.strings.label_sort_descending)),
                ),
                selectedIndex = if (currentSort.isAscending) 0 else 1,
                title = stringResource(MR.strings.label_rule_sort_direction),
                onSelectedIndexChange = { index ->
                    val direction = if (index == 0) {
                        LibrarySort.Direction.Ascending
                    } else {
                        LibrarySort.Direction.Descending
                    }
                    onSortChange(LibrarySort(currentSort.type, direction).serialize())
                },
            )
        }
    }
}
