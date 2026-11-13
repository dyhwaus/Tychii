package eu.kanade.tachiyomi.ui.browse

import androidx.compose.animation.graphics.res.animatedVectorResource
import androidx.compose.animation.graphics.vector.AnimatedImageVector
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.navigator.currentOrThrow
import cafe.adriel.voyager.navigator.tab.LocalTabNavigator
import cafe.adriel.voyager.navigator.tab.TabOptions
import eu.kanade.presentation.browse.BrowseMenuScreen
import eu.kanade.presentation.util.Tab
import eu.kanade.presentation.util.rememberAnimatedVectorPainter
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.ui.browse.source.globalsearch.GlobalSearchScreen
import eu.kanade.tachiyomi.ui.home.HomeScreen
import eu.kanade.tachiyomi.ui.main.MainActivity
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.receiveAsFlow
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource

data object BrowseTab : Tab {

    override val options: TabOptions
        @Composable
        get() {
            val isSelected = LocalTabNavigator.current.current.key == key
            val enter = AnimatedImageVector.animatedVectorResource(R.drawable.animated_browse_enter)
            val exit = AnimatedImageVector.animatedVectorResource(R.drawable.animated_browse_exit)
            return TabOptions(
                index = 3u,
                title = stringResource(MR.strings.browse),
                icon = rememberAnimatedVectorPainter(enter, exit, isSelected),
            )
        }

    override suspend fun onReselect(navigator: Navigator) {
        navigator.push(GlobalSearchScreen())
    }

    // A section to open (from a launcher shortcut). Buffered so the
    // request survives switching to this tab; the menu collects it and
    // pushes the section screen.
    private val showSectionChannel = Channel<BrowseSection>(1, BufferOverflow.DROP_OLDEST)

    fun showSection(section: BrowseSection) {
        showSectionChannel.trySend(section)
    }

    @Composable
    override fun Content() {
        val context = LocalContext.current
        val navigator = LocalNavigator.currentOrThrow
        // The menu has no search or selection mode: the capsule is always
        // shown. (Without this, it would keep the previous tab's hidden state.)
        LaunchedEffect(Unit) {
            HomeScreen.showTitleCapsule(true)
        }
        BrowseMenuScreen(
            onClickSources = { navigator.push(BrowseSectionScreen(BrowseSection.Sources)) },
            onClickExtensions = { navigator.push(BrowseSectionScreen(BrowseSection.Extensions)) },
            onClickMigration = { navigator.push(BrowseSectionScreen(BrowseSection.Migration)) },
        )
        LaunchedEffect(Unit) {
            showSectionChannel.receiveAsFlow()
                .collectLatest { navigator.push(BrowseSectionScreen(it)) }
        }
        LaunchedEffect(Unit) {
            (context as? MainActivity)?.ready = true
        }
    }
}
