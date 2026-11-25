package eu.kanade.tachiyomi.ui.library

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.binding
import dev.zacsweers.metrox.viewmodel.ViewModelKey
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import tachiyomi.domain.library.service.LibraryPreferences
import java.util.UUID

/**
 * The rules store for the rule list and editor screens. All shared
 * state flows through the `pref_library_rules` preference, so the
 * library screen (which observes the same preference) picks up every
 * change without any direct coupling to this ViewModel.
 */
@Inject
@ViewModelKey
@ContributesIntoMap(AppScope::class, binding = binding<ViewModel>())
class LibraryRulesViewModel(
    private val libraryPreferences: LibraryPreferences,
) : ViewModel() {

    val rules: StateFlow<List<LibraryRule>> = libraryPreferences.rules.changes()
        .map { parseLibraryRules(it) }
        .stateIn(
            viewModelScope,
            SharingStarted.Eagerly,
            parseLibraryRules(libraryPreferences.rules.get()),
        )

    private fun saveAll(rules: List<LibraryRule>) {
        libraryPreferences.rules.set(serializeLibraryRules(rules))
    }

    fun setEnabled(rule: LibraryRule, enabled: Boolean) {
        saveAll(rules.value.map { if (it.id == rule.id) it.copy(enabled = enabled) else it })
    }

    fun copyRule(rule: LibraryRule, copyName: String) {
        val copy = rule.copy(id = UUID.randomUUID().toString(), name = copyName)
        saveAll(rules.value + copy)
    }

    fun deleteRule(rule: LibraryRule) {
        saveAll(rules.value.filter { it.id != rule.id })
    }

    fun deleteDraft() {
        val id = draftId ?: return
        saveAll(rules.value.filter { it.id != id })
    }

    fun copyDraft(copyName: String) {
        val id = draftId ?: return
        val rule = rules.value.firstOrNull { it.id == id } ?: return
        copyRule(rule, copyName)
    }

    // Editor draft. The draft lives here (not in the screen) so it
    // survives rotations; it is loaded once when the editor opens and
    // only written back on save.
    private var draftLoaded = false
    var draftIsNew by mutableStateOf(true)
        private set
    var draftId by mutableStateOf<String?>(null)
        private set
    var draftName by mutableStateOf("")
    var draftEnabled by mutableStateOf(true)
    val draftConditions = mutableStateListOf<RuleCondition>()
    var draftSort by mutableStateOf<String?>(null)

    fun startEdit(rule: LibraryRule?) {
        // One editor screen edits one rule; later rule changes (e.g.
        // a save from another screen) must not reset the draft.
        if (draftLoaded) return
        draftLoaded = true
        draftIsNew = rule == null
        draftId = rule?.id
        draftName = rule?.name ?: ""
        draftEnabled = rule?.enabled ?: true
        draftConditions.clear()
        draftConditions.addAll(
            rule?.conditions ?: listOf(
                RuleCondition(
                    field = RuleField.READING_STATE,
                    operator = RuleOperator.EQUALS,
                    value = ReadingStateValue.READING.name,
                ),
            ),
        )
        draftSort = rule?.sort
    }

    fun saveDraft(): Boolean {
        if (draftName.isBlank()) return false
        val rule = LibraryRule(
            id = draftId ?: UUID.randomUUID().toString(),
            name = draftName.trim(),
            enabled = draftEnabled,
            conditions = draftConditions.toList(),
            sort = draftSort,
        )
        val current = rules.value
        saveAll(
            if (draftIsNew) {
                current + rule
            } else {
                current.map { if (it.id == rule.id) rule else it }
            },
        )
        return true
    }
}
