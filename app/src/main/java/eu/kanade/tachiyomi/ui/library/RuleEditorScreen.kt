package eu.kanade.tachiyomi.ui.library

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import dev.zacsweers.metrox.viewmodel.metroViewModel
import eu.kanade.presentation.library.components.RuleEditorScreen
import eu.kanade.presentation.library.components.RuleEditorState
import eu.kanade.presentation.miuix.TychiiPage
import eu.kanade.presentation.util.Screen
import tachiyomi.core.common.i18n.stringResource
import tachiyomi.i18n.MR

class RuleEditorScreen(private val ruleId: String?) : Screen() {

    @Composable
    override fun Content() {
        val context = LocalContext.current
        val navigator = LocalNavigator.currentOrThrow
        val viewModel = metroViewModel<LibraryRulesViewModel>()

        val rules by viewModel.rules.collectAsStateWithLifecycle()

        LaunchedEffect(rules) {
            viewModel.startEdit(rules.firstOrNull { it.id == ruleId })
        }

        TychiiPage {
            RuleEditorScreen(
                state = RuleEditorState(
                    name = viewModel.draftName,
                    enabled = viewModel.draftEnabled,
                    conditions = viewModel.draftConditions,
                    sort = viewModel.draftSort,
                    isNew = viewModel.draftIsNew,
                ),
                onNameChange = { viewModel.draftName = it },
                onEnabledChange = { viewModel.draftEnabled = it },
                onConditionChange = { index, condition -> viewModel.draftConditions[index] = condition },
                onAddCondition = {
                    viewModel.draftConditions.add(
                        RuleCondition(
                            field = RuleField.READING_STATE,
                            operator = RuleOperator.EQUALS,
                            value = ReadingStateValue.READING.name,
                        ),
                    )
                },
                onDeleteCondition = { viewModel.draftConditions.removeAt(it) },
                onSortChange = { viewModel.draftSort = it },
                onSave = {
                    if (viewModel.saveDraft()) navigator.pop()
                },
                onDelete = {
                    viewModel.deleteDraft()
                    navigator.pop()
                },
                onCopy = {
                    // The copy is of the stored rule, so its name
                    // comes from the stored rule, not the draft.
                    val storedName = rules.firstOrNull { it.id == viewModel.draftId }?.name
                        ?: viewModel.draftName
                    viewModel.copyDraft(
                        context.stringResource(
                            MR.strings.label_rule_copy_name,
                            storedName,
                        ),
                    )
                },
                navigateUp = navigator::pop,
            )
        }
    }
}
