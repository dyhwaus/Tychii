package eu.kanade.presentation.library.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SmallExtendedFloatingActionButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import eu.kanade.presentation.components.AppBar
import eu.kanade.presentation.components.QuietEmptyScreen
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.ui.library.LibraryRule
import eu.kanade.tachiyomi.ui.library.ReadingStateValue
import eu.kanade.tachiyomi.ui.library.RuleCondition
import eu.kanade.tachiyomi.ui.library.RuleField
import eu.kanade.tachiyomi.ui.library.RuleOperator
import mihon.icons.materialsymbols.MaterialSymbols
import mihon.icons.materialsymbols.rounded.Add
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.material.SECONDARY_ALPHA
import tachiyomi.presentation.core.components.material.Scaffold
import tachiyomi.presentation.core.components.material.padding
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.util.shouldExpandFAB
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Switch

@Composable
fun RuleListScreen(
    rules: List<LibraryRule>,
    onClickRule: (LibraryRule) -> Unit,
    onCreateRule: () -> Unit,
    onToggleEnabled: (LibraryRule, Boolean) -> Unit,
    navigateUp: () -> Unit,
) {
    val lazyListState = rememberLazyListState()
    Scaffold(
        topBar = { scrollBehavior ->
            AppBar(
                title = stringResource(MR.strings.label_library_rules),
                navigateUp = navigateUp,
                scrollBehavior = scrollBehavior,
            )
        },
        floatingActionButton = {
            SmallExtendedFloatingActionButton(
                text = { Text(text = stringResource(MR.strings.action_new_rule)) },
                icon = { Icon(imageVector = MaterialSymbols.Rounded.Add, contentDescription = null) },
                onClick = onCreateRule,
                expanded = lazyListState.shouldExpandFAB(),
            )
        },
    ) { paddingValues ->
        if (rules.isEmpty()) {
            QuietEmptyScreen(
                stringRes = MR.strings.label_rule_no_rules,
                modifier = Modifier.padding(paddingValues),
            )
            return@Scaffold
        }

        LazyColumn(
            state = lazyListState,
            contentPadding = paddingValues,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(rules, key = { it.id }) { rule ->
                Card(
                    modifier = Modifier.padding(horizontal = 12.dp),
                    onClick = { onClickRule(rule) },
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = rule.name,
                                style = MaterialTheme.typography.titleMedium,
                            )
                            val summaries = mutableListOf<String>()
                            for (condition in rule.conditions) {
                                summaries.add(condition.summaryText())
                            }
                            Text(
                                text = if (summaries.isEmpty()) {
                                    stringResource(MR.strings.label_rule_condition_count, 0)
                                } else {
                                    summaries.joinToString(" · ")
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = SECONDARY_ALPHA),
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        Switch(
                            checked = rule.enabled,
                            onCheckedChange = { onToggleEnabled(rule, it) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RuleCondition.summaryText(): String {
    val fieldLabel = stringResource(field.titleRes)
    return when (operator) {
        RuleOperator.EQUALS -> stringResource(
            MR.strings.rule_summary_equals,
            fieldLabel,
            valueLabel(),
        )
        RuleOperator.WITHIN_DAYS -> stringResource(
            MR.strings.rule_summary_within_days,
            fieldLabel,
            stringResource(MR.strings.day_short, value.toIntOrNull() ?: 0),
        )
        RuleOperator.MORE_THAN_DAYS -> stringResource(
            MR.strings.rule_summary_more_than_days,
            fieldLabel,
            stringResource(MR.strings.day_short, value.toIntOrNull() ?: 0),
        )
        RuleOperator.AT_LEAST -> stringResource(
            MR.strings.rule_summary_at_least,
            fieldLabel,
            value,
        )
    }
}

@Composable
private fun RuleCondition.valueLabel(): String {
    return when (field) {
        RuleField.READING_STATE -> when (value) {
            ReadingStateValue.READING.name -> stringResource(MR.strings.smart_category_reading)
            ReadingStateValue.PAUSED.name -> stringResource(MR.strings.smart_category_paused)
            ReadingStateValue.NOT_STARTED.name -> stringResource(MR.strings.rule_reading_not_started)
            ReadingStateValue.FINISHED.name -> stringResource(MR.strings.rule_reading_finished)
            else -> value
        }
        RuleField.STATUS -> when (value.toIntOrNull()) {
            SManga.ONGOING -> stringResource(MR.strings.ongoing)
            SManga.COMPLETED -> stringResource(MR.strings.completed)
            SManga.LICENSED -> stringResource(MR.strings.licensed)
            SManga.PUBLISHING_FINISHED -> stringResource(MR.strings.publishing_finished)
            SManga.CANCELLED -> stringResource(MR.strings.cancelled)
            SManga.ON_HIATUS -> stringResource(MR.strings.on_hiatus)
            else -> stringResource(MR.strings.unknown)
        }
        RuleField.FAVORITE, RuleField.DOWNLOADED -> if (value == "true") {
            stringResource(MR.strings.label_yes)
        } else {
            stringResource(MR.strings.label_no)
        }
        RuleField.LAST_UPDATE_DAYS, RuleField.LAST_READ_DAYS, RuleField.UNREAD_COUNT -> value
    }
}
