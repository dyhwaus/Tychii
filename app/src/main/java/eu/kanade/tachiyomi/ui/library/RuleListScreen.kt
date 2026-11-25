package eu.kanade.tachiyomi.ui.library

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import dev.zacsweers.metrox.viewmodel.metroViewModel
import eu.kanade.presentation.library.components.RuleListScreen
import eu.kanade.presentation.miuix.TychiiPage
import eu.kanade.presentation.util.Screen

class RuleListScreen : Screen() {

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val viewModel = metroViewModel<LibraryRulesViewModel>()

        val rules by viewModel.rules.collectAsStateWithLifecycle()

        TychiiPage {
            RuleListScreen(
                rules = rules,
                onClickRule = { navigator.push(RuleEditorScreen(it.id)) },
                onCreateRule = { navigator.push(RuleEditorScreen(null)) },
                onToggleEnabled = viewModel::setEnabled,
                navigateUp = navigator::pop,
            )
        }
    }
}
