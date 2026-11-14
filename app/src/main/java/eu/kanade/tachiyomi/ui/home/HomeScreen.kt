package eu.kanade.tachiyomi.ui.home

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Badge
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteDefaults
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteItem
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteType
import androidx.compose.material3.adaptive.navigationsuite.rememberNavigationSuiteScaffoldState
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import tachiyomi.presentation.core.util.collectAsState
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEach
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import cafe.adriel.voyager.navigator.tab.LocalTabNavigator
import cafe.adriel.voyager.navigator.tab.TabNavigator
import eu.kanade.domain.ui.model.BarStyle
import eu.kanade.presentation.miuix.CapsuleAction
import eu.kanade.presentation.miuix.MiuixCapsuleTitle
import eu.kanade.presentation.miuix.MiuixFloatingCapsule
import eu.kanade.presentation.miuix.TychiiLiquidBar
import eu.kanade.presentation.theme.isMiuixGlassSupported
import eu.kanade.presentation.util.Screen
import eu.kanade.presentation.util.isTabletUi
import eu.kanade.tachiyomi.ui.browse.BrowseSection
import eu.kanade.tachiyomi.ui.browse.BrowseTab
import eu.kanade.tachiyomi.ui.download.DownloadQueueScreen
import eu.kanade.tachiyomi.ui.history.HistoryTab
import eu.kanade.tachiyomi.ui.library.LibraryTab
import eu.kanade.tachiyomi.ui.manga.MangaScreen
import eu.kanade.tachiyomi.ui.more.MoreTab
import eu.kanade.tachiyomi.ui.updates.UpdatesTab
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import mihon.app.di.appGraph
import soup.compose.material.motion.animation.materialFadeThroughIn
import soup.compose.material.motion.animation.materialFadeThroughOut
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.pluralStringResource
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.blur.rememberLayerBackdrop
import top.yukonga.miuix.kmp.basic.Scaffold as MiuixScaffold

object HomeScreen : Screen() {

    private val librarySearchEvent = Channel<String>()
    private val openTabEvent = Channel<Tab>()
    private val showBottomNavEvent = Channel<Boolean>()
    private val showTitleCapsuleEvent = Channel<Boolean>()
    private val capsuleTitleEvent = Channel<String>()
    // The current tab's floating-capsule actions (search, sort, ...).
    // Conflated: only the latest report matters, and a tab re-reports
    // whenever the state its actions depend on changes.
    private val capsuleActionsEvent = Channel<List<CapsuleAction>>(Channel.CONFLATED)

    @Suppress("ConstPropertyName")
    private const val TabFadeDuration = 200

    @Suppress("ConstPropertyName")
    private const val TabNavigatorKey = "HomeTabs"

    private val TABS = listOf(
        LibraryTab,
        UpdatesTab,
        HistoryTab,
        BrowseTab,
        MoreTab,
    )

    @Composable
    override fun Content() {
        val context = LocalContext.current
        val navigator = LocalNavigator.currentOrThrow
        val showNavBarLabels by remember {
            context.appGraph.uiPreferences.showNavBarLabels
        }.collectAsState()
        val barStyle by remember {
            context.appGraph.uiPreferences.barStyle
        }.collectAsState()
        val hideUpdatesTab by remember {
            context.appGraph.uiPreferences.hideUpdatesTab
        }.collectAsState()
        // One style for both floating bars (bottom navigation + title
        // capsule). Liquid glass only runs on API 32+; below that it
        // behaves like translucent (see MiuixGlass.kt).
        val effectiveBarStyle = if (barStyle == BarStyle.LIQUID_GLASS && !isMiuixGlassSupported) {
            BarStyle.TRANSLUCENT
        } else {
            barStyle
        }
        val glassEnabled = effectiveBarStyle == BarStyle.LIQUID_GLASS
        // The Updates tab can be hidden from Settings; the bar and the
        // rail both render this list, so a hidden tab is never selectable.
        val tabs = if (hideUpdatesTab) TABS - UpdatesTab else TABS
        // The blur source. Created unconditionally (it is only a handle);
        // it is only *applied* to the content layer when glass is enabled.
        val backdrop = rememberLayerBackdrop()
        TabNavigator(
            tab = LibraryTab,
            key = TabNavigatorKey,
        ) { tabNavigator ->
            // If the Updates tab is hidden while it is the current tab,
            // fall back to the Library tab (otherwise the bar would have
            // no selected tab).
            LaunchedEffect(hideUpdatesTab) {
                if (hideUpdatesTab && tabNavigator.current == UpdatesTab) {
                    tabNavigator.current = LibraryTab
                }
            }
            // Provide usable navigator to content screen
            CompositionLocalProvider(LocalNavigator provides navigator) {
                val tabletUi = isTabletUi()
                var showNavBar by remember { mutableStateOf(true) }
                LaunchedEffect(Unit) {
                    showBottomNavEvent.receiveAsFlow().collectLatest { showNavBar = it }
                }
                // The title capsule hides while a tab's toolbar is in
                // search or selection mode (its text field / counter would
                // sit under the capsule). Each tab reports its own state.
                var showTitleCapsule by remember { mutableStateOf(true) }
                LaunchedEffect(Unit) {
                    showTitleCapsuleEvent.receiveAsFlow().collectLatest { showTitleCapsule = it }
                }
                // A tab may report its own capsule text (the Library tab
                // reports "书架 · 分类名"). Only the Library tab uses
                // this; other tabs keep their TabOptions title, and the
                // override is ignored while another tab is current, so
                // no tab needs to reset it.
                var libraryCapsuleTitle by remember { mutableStateOf<String?>(null) }
                LaunchedEffect(Unit) {
                    capsuleTitleEvent.receiveAsFlow().collectLatest { libraryCapsuleTitle = it }
                }
                // The floating capsule's actions, reported by the current
                // tab (each tab re-reports when the state its actions
                // depend on changes; a tab with no actions reports an
                // empty list, hiding the capsule).
                var capsuleActions by remember { mutableStateOf<List<CapsuleAction>>(emptyList()) }
                LaunchedEffect(Unit) {
                    capsuleActionsEvent.receiveAsFlow().collectLatest { capsuleActions = it }
                }
                // The capsule hides once the user has scrolled down past
                // a threshold, and comes back on any upward scroll. The
                // connection only observes (it consumes nothing), so
                // scrolling is unaffected.
                val capsuleHideThresholdPx = with(LocalDensity.current) { 120.dp.toPx() }
                var capsuleScrollVisible by remember { mutableStateOf(true) }
                var capsuleScrolledDown by remember { mutableFloatStateOf(0f) }
                val capsuleScrollConnection = remember(capsuleHideThresholdPx) {
                    object : NestedScrollConnection {
                        override fun onPostScroll(
                            consumed: Offset,
                            available: Offset,
                            source: NestedScrollSource,
                        ): Offset {
                            if (consumed.y < 0f) {
                                capsuleScrolledDown += -consumed.y
                                if (capsuleScrolledDown > capsuleHideThresholdPx) {
                                    capsuleScrollVisible = false
                                }
                            } else if (consumed.y > 0f) {
                                capsuleScrolledDown = 0f
                                capsuleScrollVisible = true
                            }
                            return Offset.Zero
                        }
                    }
                }
                // Switching tabs shows the capsule again (the new tab
                // starts at the top); the tab reports its own actions
                // then too.
                LaunchedEffect(tabNavigator.current) {
                    capsuleScrollVisible = true
                    capsuleScrolledDown = 0f
                }
                if (tabletUi) {
                    // Tablets keep the Material3 navigation rail; the floating
                    // bar is a phone layout.
                    val navigationSuiteState = rememberNavigationSuiteScaffoldState()
                    LaunchedEffect(navigationSuiteState) {
                        navigationSuiteState.show()
                    }
                    NavigationSuiteScaffold(
                        navigationSuiteType = NavigationSuiteType.NavigationRail,
                        state = navigationSuiteState,
                        navigationSuiteColors = NavigationSuiteDefaults.colors(
                            navigationRailContainerColor = MaterialTheme.colorScheme
                                .surfaceColorAtElevation(3.dp),
                        ),
                        navigationItemVerticalArrangement = Arrangement.Center,
                        navigationItems = {
                            tabs.fastForEach {
                                NavigationSuiteItem(it, NavigationSuiteType.NavigationRail)
                            }
                        },
                    ) {
                        TabContent(tabNavigator)
                    }
                } else {
                    // Phones use the liquid-glass floating bar (Wenku8Reader
                    // style). The bar, the title capsule and the floating
                    // action capsule must stay OUTSIDE the layer recorded
                    // by the backdrop: the backdrop is applied only to the
                    // tab content below, and the three are siblings in an
                    // outer Box. Putting any of them inside the recorded
                    // layer would make it blur itself and crash hwui
                    // (infinite recursion in
                    // RenderNode::prepareTreeImpl — see Wenku8Reader's
                    // MainScaffold for the two verified pitfalls).
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .nestedScroll(capsuleScrollConnection),
                    ) {
                        MiuixScaffold(
                            // The Scaffold background must match the app theme
                            // background. Miuix's own dark surface is pure black
                            // while the app background is #1A1C1E, and that
                            // mismatch showed as a visible seam behind the
                            // floating bar in dark mode.
                            containerColor = MaterialTheme.colorScheme.background,
                        ) { paddingValues ->
                            // No bottom clearance and no background of its
                            // own here: the content used to end 96.dp above
                            // the bottom, leaving a dead band behind the bar
                            // that the covers scrolled under (only half a
                            // cover showed at the bottom). Each tab list now
                            // pads its own bottom by LiquidNavBarClearance,
                            // so its last item clears the bar, and the page
                            // is one continuous background (the Scaffold's).
                            Box(
                                modifier = Modifier
                                    .padding(paddingValues)
                                    .then(
                                        if (glassEnabled) {
                                            Modifier.layerBackdrop(backdrop)
                                        } else {
                                            Modifier
                                        },
                                    ),
                            ) {
                                TabContent(tabNavigator)
                            }
                        }
                        // Centered page-title capsule, replacing the
                        // top-left title of each tab's own toolbar. The Box
                        // is the 64.dp toolbar band below the status bar,
                        // and the capsule is centered in it. The extra
                        // 8.dp top padding keeps the capsule clear of the
                        // status bar: centered in the bare 64.dp band it
                        // sat only ~12.dp below the status bar and read
                        // as touching it, especially on pages whose own
                        // toolbar is gone (e.g. Extensions). Hidden while
                        // a tab is searching or selecting (see
                        // showTitleCapsule above).
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .statusBarsPadding()
                                .padding(top = 8.dp)
                                .height(64.dp)
                                .fillMaxWidth(),
                            contentAlignment = Alignment.Center,
                        ) {
                            AnimatedVisibility(
                                visible = showTitleCapsule,
                                enter = fadeIn(),
                                exit = fadeOut(),
                            ) {
                                MiuixCapsuleTitle(
                                    // The Library tab reports its own
                                    // title ("书架 · 分类名"); other tabs
                                    // use their TabOptions title.
                                    title = if (tabNavigator.current == LibraryTab) {
                                        libraryCapsuleTitle ?: tabNavigator.current.options.title
                                    } else {
                                        tabNavigator.current.options.title
                                    },
                                    barStyle = effectiveBarStyle,
                                    backdrop = backdrop,
                                )
                            }
                        }
                        AnimatedVisibility(
                            visible = showNavBar,
                            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .navigationBarsPadding(),
                        ) {
                            val scope = rememberCoroutineScope()
                            TychiiLiquidBar(
                                tabs = tabs,
                                selectedIndex = {
                                    tabs.indexOfFirst {
                                        it::class == tabNavigator.current::class
                                    }
                                },
                                onTabSelected = { index ->
                                    tabNavigator.current = tabs[index]
                                },
                                onTabClick = { tab ->
                                    if (tabNavigator.current::class == tab::class) {
                                        scope.launch { tab.onReselect(navigator) }
                                    } else {
                                        tabNavigator.current = tab
                                    }
                                },
                                showLabels = showNavBarLabels,
                                barStyle = effectiveBarStyle,
                                backdrop = backdrop,
                                tabBadge = { tab -> tabBadge(tab) },
                            )
                        }
                        // Floating action capsule: the current tab's
                        // search/sort/... icons, on the right edge over
                        // the list. Same glass as the title capsule and
                        // the bottom bar, so it stays outside the recorded
                        // layer (a sibling of the Scaffold, like those
                        // two). No layer behind it — it floats over the
                        // covers with only a 12.dp gap from the screen
                        // edge, covering part of the content while shown.
                        // Hidden while scrolling down, and when the tab
                        // reports no actions. It does NOT follow the
                        // title capsule: a tab hides the title capsule
                        // while searching or selecting by reporting no
                        // actions then, and a page may hide the title
                        // capsule for its own reason (the Explore page
                        // shows the source name as its title) while still
                        // offering actions.
                        AnimatedVisibility(
                            visible = capsuleScrollVisible && capsuleActions.isNotEmpty(),
                            enter = fadeIn() + slideInHorizontally(initialOffsetX = { it }),
                            exit = fadeOut() + slideOutHorizontally(targetOffsetX = { it }),
                            modifier = Modifier
                                .align(Alignment.CenterEnd)
                                // Clear of the right edge: 12.dp left the
                                // capsule hugging the screen edge.
                                .padding(end = 20.dp),
                        ) {
                            MiuixFloatingCapsule(
                                actions = capsuleActions,
                                barStyle = effectiveBarStyle,
                                backdrop = backdrop,
                            )
                        }
                    }
                }
            }

            val goToLibraryTab = { tabNavigator.current = LibraryTab }

            BackHandler(enabled = tabNavigator.current != LibraryTab, onBack = goToLibraryTab)

            LaunchedEffect(Unit) {
                launch {
                    librarySearchEvent.receiveAsFlow().collectLatest {
                        goToLibraryTab()
                        LibraryTab.search(it)
                    }
                }
                launch {
                    openTabEvent.receiveAsFlow().collectLatest {
                        tabNavigator.current = when (it) {
                            is Tab.Library -> LibraryTab
                            // The Updates shortcut still arrives when the
                            // tab is hidden; fall back to the Library tab
                            // instead of opening a tab that is not in the
                            // bar. Read the preference here (not the
                            // composed state) so the value is never stale.
                            Tab.Updates -> if (context.appGraph.uiPreferences.hideUpdatesTab.get()) {
                                LibraryTab
                            } else {
                                UpdatesTab
                            }
                            Tab.History -> HistoryTab
                            is Tab.Browse -> {
                                // A launcher shortcut opens its section
                                // directly instead of the menu; the menu
                                // collects the request and pushes it.
                                BrowseTab.showSection(
                                    if (it.toExtensions) {
                                        BrowseSection.Extensions
                                    } else {
                                        BrowseSection.Sources
                                    },
                                )
                                BrowseTab
                            }
                            is Tab.More -> MoreTab
                        }

                        if (it is Tab.Library && it.mangaIdToOpen != null) {
                            navigator.push(MangaScreen(it.mangaIdToOpen))
                        }
                        if (it is Tab.More && it.toDownloads) {
                            navigator.push(DownloadQueueScreen)
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun TabContent(tabNavigator: TabNavigator) {
        AnimatedContent(
            targetState = tabNavigator.current,
            transitionSpec = {
                materialFadeThroughIn(
                    initialScale = 1f,
                    durationMillis = TabFadeDuration,
                ) togetherWith materialFadeThroughOut(durationMillis = TabFadeDuration)
            },
            label = "tabContent",
        ) {
            tabNavigator.saveableState(key = "currentTab", it) {
                it.Content()
            }
        }
    }

    @Composable
    private fun NavigationSuiteItem(
        tab: eu.kanade.presentation.util.Tab,
        navigationSuiteType: NavigationSuiteType,
    ) {
        val tabNavigator = LocalTabNavigator.current
        val navigator = LocalNavigator.currentOrThrow
        val scope = rememberCoroutineScope()
        val selected = tabNavigator.current::class == tab::class
        NavigationSuiteItem(
            navigationSuiteType = navigationSuiteType,
            selected = selected,
            onClick = {
                if (!selected) {
                    tabNavigator.current = tab
                } else {
                    scope.launch { tab.onReselect(navigator) }
                }
            },
            icon = {
                Icon(
                    painter = tab.options.icon!!,
                    contentDescription = tab.options.title,
                )
            },
            label = {
                Text(
                    text = tab.options.title,
                    style = MaterialTheme.typography.labelLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            },
            badge = tabBadge(tab),
        )
    }

    @Composable
    private fun tabBadge(tab: eu.kanade.presentation.util.Tab): (@Composable () -> Unit)? {
        val context = LocalContext.current
        val count by produceState(initialValue = 0, tab) {
            val graph = context.appGraph
            when (tab) {
                is UpdatesTab -> {
                    combine(
                        graph.libraryPreferences.newShowUpdatesCount.changes(),
                        graph.libraryPreferences.newUpdatesCount.changes(),
                    ) { show, count ->
                        if (show) count else 0
                    }
                        .collectLatest { value = it }
                }

                is BrowseTab -> {
                    graph.sourcePreferences.extensionUpdatesCount.changes()
                        .collectLatest { value = it }
                }

                else -> value = 0
            }
        }
        if (count <= 0) return null
        return {
            Badge {
                val desc = when (tab) {
                    is UpdatesTab -> pluralStringResource(
                        MR.plurals.notification_chapters_generic,
                        count = count,
                        count,
                    )

                    is BrowseTab -> pluralStringResource(
                        MR.plurals.update_check_notification_ext_updates,
                        count = count,
                        count,
                    )

                    else -> null
                }
                Text(
                    text = count.toString(),
                    modifier = Modifier.semantics {
                        if (desc != null) contentDescription = desc
                    },
                )
            }
        }
    }

    suspend fun search(query: String) {
        librarySearchEvent.send(query)
    }

    suspend fun openTab(tab: Tab) {
        openTabEvent.send(tab)
    }

    suspend fun showBottomNav(show: Boolean) {
        showBottomNavEvent.send(show)
    }

    suspend fun showTitleCapsule(show: Boolean) {
        showTitleCapsuleEvent.send(show)
    }

    suspend fun setCapsuleTitle(title: String) {
        capsuleTitleEvent.send(title)
    }

    suspend fun setCapsuleActions(actions: List<CapsuleAction>) {
        capsuleActionsEvent.send(actions)
    }

    sealed interface Tab {
        data class Library(val mangaIdToOpen: Long? = null) : Tab
        data object Updates : Tab
        data object History : Tab
        data class Browse(val toExtensions: Boolean = false) : Tab
        data class More(val toDownloads: Boolean) : Tab
    }
}
