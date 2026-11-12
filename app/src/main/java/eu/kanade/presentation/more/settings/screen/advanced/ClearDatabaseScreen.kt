package eu.kanade.presentation.more.settings.screen.advanced

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastMap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.binding
import dev.zacsweers.metrox.viewmodel.ViewModelKey
import dev.zacsweers.metrox.viewmodel.metroViewModel
import eu.kanade.presentation.browse.components.SourceIcon
import eu.kanade.presentation.util.Screen
import eu.kanade.tachiyomi.util.system.toast
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mihon.core.metro.AppCoroutineScope
import androidx.compose.ui.state.ToggleableState
import mihon.icons.materialsymbols.MaterialSymbols
import mihon.icons.materialsymbols.automirroredrounded.ArrowBack
import mihon.icons.materialsymbols.rounded.FlipToBack
import mihon.icons.materialsymbols.rounded.SelectAll
import tachiyomi.domain.history.repository.HistoryRepository
import tachiyomi.domain.manga.repository.MangaRepository
import tachiyomi.domain.source.interactor.GetSourcesWithNonLibraryManga
import tachiyomi.domain.source.model.Source
import tachiyomi.domain.source.model.SourceWithCount
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.LazyColumnWithAction
import tachiyomi.presentation.core.components.material.padding
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.screens.EmptyScreen
import tachiyomi.presentation.core.screens.LoadingScreen
import tachiyomi.presentation.core.util.selectedBackground
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.Checkbox
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SmallTopAppBar
import top.yukonga.miuix.kmp.basic.Switch
import top.yukonga.miuix.kmp.basic.Text as MiuixText
import top.yukonga.miuix.kmp.overlay.OverlayDialog

class ClearDatabaseScreen : Screen() {

    @Composable
    override fun Content() {
        val context = LocalContext.current
        val navigator = LocalNavigator.currentOrThrow
        val viewModel = metroViewModel<ClearDatabaseViewModel>()
        val state by viewModel.state.collectAsState()
        val scope = rememberCoroutineScope()

        when (val s = state) {
            is ClearDatabaseViewModel.State.Loading -> LoadingScreen()
            is ClearDatabaseViewModel.State.Ready -> {
                if (s.showConfirmation) {
                    var keepReadManga by remember { mutableStateOf(true) }
                    OverlayDialog(
                        show = true,
                        title = stringResource(MR.strings.are_you_sure),
                        onDismissRequest = viewModel::hideConfirmation,
                    ) {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(MaterialTheme.padding.small),
                        ) {
                            Text(text = stringResource(MR.strings.clear_database_text))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(MaterialTheme.padding.extraSmall),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    text = stringResource(MR.strings.clear_db_exclude_read),
                                    modifier = Modifier.weight(1f),
                                )
                                Switch(
                                    checked = keepReadManga,
                                    onCheckedChange = { keepReadManga = it },
                                )
                            }
                            if (!keepReadManga) {
                                Text(
                                    text = stringResource(MR.strings.clear_database_history_warning),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.error,
                                )
                            }
                            Column(modifier = Modifier.padding(top = 12.dp)) {
                                Button(
                                    onClick = {
                                        scope.launch(Dispatchers.Main) {
                                            viewModel.removeMangaBySourceId(keepReadManga)
                                            viewModel.clearSelection()
                                            viewModel.hideConfirmation()
                                            context.toast(MR.strings.clear_database_completed)
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                ) {
                                    MiuixText(text = stringResource(MR.strings.action_ok))
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Button(
                                    onClick = viewModel::hideConfirmation,
                                    modifier = Modifier.fillMaxWidth(),
                                ) {
                                    MiuixText(text = stringResource(MR.strings.action_cancel))
                                }
                            }
                        }
                    }
                }

                Scaffold(
                    topBar = {
                        SmallTopAppBar(
                            title = stringResource(MR.strings.pref_clear_database),
                            navigationIcon = {
                                IconButton(onClick = navigator::pop) {
                                    Icon(
                                        imageVector = MaterialSymbols.AutoMirroredRounded.ArrowBack,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurface,
                                    )
                                }
                            },
                            actions = {
                                if (s.items.isNotEmpty()) {
                                    IconButton(onClick = viewModel::selectAll) {
                                        Icon(
                                            imageVector = MaterialSymbols.Rounded.SelectAll,
                                            contentDescription = stringResource(MR.strings.action_select_all),
                                        )
                                    }
                                    IconButton(onClick = viewModel::invertSelection) {
                                        Icon(
                                            imageVector = MaterialSymbols.Rounded.FlipToBack,
                                            contentDescription = stringResource(MR.strings.action_select_inverse),
                                        )
                                    }
                                }
                            },
                        )
                    },
                ) { contentPadding ->
                    if (s.items.isEmpty()) {
                        EmptyScreen(
                            message = stringResource(MR.strings.database_clean),
                            modifier = Modifier.padding(contentPadding),
                        )
                    } else {
                        LazyColumnWithAction(
                            contentPadding = contentPadding,
                            actionLabel = stringResource(MR.strings.action_delete),
                            actionEnabled = s.selection.isNotEmpty(),
                            onClickAction = viewModel::showConfirmation,
                        ) {
                            items(s.items) { sourceWithCount ->
                                ClearDatabaseItem(
                                    source = sourceWithCount.source,
                                    count = sourceWithCount.count,
                                    isSelected = s.selection.contains(sourceWithCount.id),
                                    onClickSelect = { viewModel.toggleSelection(sourceWithCount.source) },
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun ClearDatabaseItem(
        source: Source,
        count: Long,
        isSelected: Boolean,
        onClickSelect: () -> Unit,
    ) {
        Row(
            modifier = Modifier
                .selectedBackground(isSelected)
                .clickable(onClick = onClickSelect)
                .padding(horizontal = 8.dp)
                .height(56.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SourceIcon(source = source)
            Column(
                modifier = Modifier
                    .padding(start = 8.dp)
                    .weight(1f),
            ) {
                Text(
                    text = source.visualName,
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(text = stringResource(MR.strings.clear_database_source_item_count, count))
            }
            Checkbox(
                state = if (isSelected) ToggleableState.On else ToggleableState.Off,
                onClick = { onClickSelect() },
            )
        }
    }
}

@Inject
@ViewModelKey
@ContributesIntoMap(AppScope::class, binding = binding<ViewModel>())
class ClearDatabaseViewModel(
    @AppCoroutineScope private val appScope: CoroutineScope,
    private val mangaRepository: MangaRepository,
    private val historyRepository: HistoryRepository,
    private val getSourcesWithNonLibraryManga: GetSourcesWithNonLibraryManga,
) : ViewModel() {

    val state: StateFlow<ClearDatabaseViewModel.State>
        field = MutableStateFlow<ClearDatabaseViewModel.State>(State.Loading)

    init {
        viewModelScope.launch(Dispatchers.IO) {
            getSourcesWithNonLibraryManga.subscribe()
                .collectLatest { list ->
                    state.update { old ->
                        val items = list.sortedBy { it.name }
                        when (old) {
                            State.Loading -> State.Ready(items)
                            is State.Ready -> old.copy(items = items)
                        }
                    }
                }
        }
    }

    suspend fun removeMangaBySourceId(keepReadManga: Boolean) {
        val state = state.value as? State.Ready ?: return
        // Finishes even if the screen is left while it runs
        appScope.launch {
            mangaRepository.deleteNonLibraryManga(state.selection, keepReadManga)
            historyRepository.deleteResetHistory()
        }.join()
    }

    fun toggleSelection(source: Source) = state.update { state ->
        if (state !is State.Ready) return@update state
        val mutableList = state.selection.toMutableList()
        if (mutableList.contains(source.id)) {
            mutableList.remove(source.id)
        } else {
            mutableList.add(source.id)
        }
        state.copy(selection = mutableList)
    }

    fun clearSelection() = state.update { state ->
        if (state !is State.Ready) return@update state
        state.copy(selection = emptyList())
    }

    fun selectAll() = state.update { state ->
        if (state !is State.Ready) return@update state
        state.copy(selection = state.items.fastMap { it.id })
    }

    fun invertSelection() = state.update { state ->
        if (state !is State.Ready) return@update state
        state.copy(
            selection = state.items
                .fastMap { it.id }
                .filterNot { it in state.selection },
        )
    }

    fun showConfirmation() = state.update { state ->
        if (state !is State.Ready) return@update state
        state.copy(showConfirmation = true)
    }

    fun hideConfirmation() = state.update { state ->
        if (state !is State.Ready) return@update state
        state.copy(showConfirmation = false)
    }

    sealed interface State {
        @Immutable
        data object Loading : State

        @Immutable
        data class Ready(
            val items: List<SourceWithCount>,
            val selection: List<Long> = emptyList(),
            val showConfirmation: Boolean = false,
        ) : State
    }
}
