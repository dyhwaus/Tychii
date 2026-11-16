package eu.kanade.tachiyomi.ui.browse.source.browse

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.HorizontalDivider
import top.yukonga.miuix.kmp.basic.PopupPositionProvider
import top.yukonga.miuix.kmp.basic.Scaffold as MiuixScaffold
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.blur.Backdrop
import top.yukonga.miuix.kmp.blur.blur
import top.yukonga.miuix.kmp.blur.drawBackdrop
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.blur.rememberLayerBackdrop
import top.yukonga.miuix.kmp.overlay.OverlayListPopup
import top.yukonga.miuix.kmp.theme.MiuixTheme
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import dev.zacsweers.metrox.viewmodel.assistedMetroViewModel
import eu.kanade.domain.ui.model.BarStyle
import eu.kanade.presentation.browse.BrowseSourceContent
import eu.kanade.presentation.browse.MissingSourceScreen
import eu.kanade.presentation.category.components.ChangeCategoryDialog
import eu.kanade.presentation.components.SearchToolbar
import eu.kanade.presentation.manga.DuplicateMangaDialog
import eu.kanade.presentation.miuix.CapsuleAction
import eu.kanade.presentation.miuix.FloatMotion
import eu.kanade.presentation.miuix.MetalEngravedIcon
import eu.kanade.presentation.miuix.MiuixFloatingCapsule
import eu.kanade.presentation.miuix.MiuixGlassBlurRadius
import eu.kanade.presentation.miuix.MiuixGlassChromaticAberration
import eu.kanade.presentation.miuix.TopFadeBlur
import eu.kanade.presentation.miuix.lens
import eu.kanade.presentation.miuix.metalEngravedShadow
import eu.kanade.presentation.miuix.metalIsDark
import eu.kanade.presentation.miuix.metalSurface
import eu.kanade.presentation.miuix.rememberTiltBrightHighlight
import eu.kanade.presentation.miuix.vibrancy
import eu.kanade.presentation.theme.isMiuixGlassSupported
import eu.kanade.presentation.util.AssistContentScreen
import eu.kanade.presentation.util.Screen
import eu.kanade.tachiyomi.source.online.HttpSource
import eu.kanade.tachiyomi.ui.browse.source.browse.BrowseSourceViewModel.Listing
import eu.kanade.tachiyomi.ui.category.CategoryScreen
import eu.kanade.tachiyomi.ui.manga.MangaScreen
import eu.kanade.tachiyomi.ui.webview.WebViewScreen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import mihon.app.di.appGraph
import mihon.feature.migration.dialog.MigrateMangaDialog
import mihon.icons.materialsymbols.MaterialSymbols
import mihon.icons.materialsymbols.rounded.FilterList
import mihon.icons.materialsymbols.rounded.Search
import mihon.icons.materialsymbols.rounded.Verified
import mihon.icons.materialsymbols.roundedfilled.Favorite
import mihon.presentation.core.util.collectAsLazyPagingItems
import tachiyomi.core.common.Constants
import tachiyomi.domain.source.model.StubSource
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.material.Scaffold
import tachiyomi.presentation.core.components.material.padding
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.screens.LoadingScreen
import tachiyomi.presentation.core.theme.active
import tachiyomi.presentation.core.util.collectAsState
import tachiyomi.source.local.LocalSource

data class BrowseSourceScreen(
    val sourceId: Long,
    private val listingQuery: String?,
) : Screen(), AssistContentScreen {

    private var assistUrl: String? = null

    override fun onProvideAssistUrl() = assistUrl

    @Composable
    override fun Content() {
        val viewModel =
            assistedMetroViewModel<BrowseSourceViewModel, BrowseSourceViewModel.Factory> {
                create(sourceId = sourceId, listingQuery = listingQuery)
            }
        val state by viewModel.state.collectAsState()

        val navigator = LocalNavigator.currentOrThrow
        val navigateUp: () -> Unit = {
            when {
                !state.isUserQuery && state.toolbarQuery != null -> viewModel.setToolbarQuery(null)
                else -> navigator.pop()
            }
        }

        val source = state.source
        if (source == null) {
            LoadingScreen()
            return
        }

        if (source is StubSource) {
            MissingSourceScreen(
                source = source,
                navigateUp = navigateUp,
            )
            return
        }

        val scope = rememberCoroutineScope()
        val haptic = LocalHapticFeedback.current
        val uriHandler = LocalUriHandler.current
        val snackbarHostState = remember { SnackbarHostState() }

        val onHelpClick = { uriHandler.openUri(LocalSource.HELP_URL) }
        val onWebViewClick = f@{
            val httpSource = source as? HttpSource ?: return@f
            navigator.push(
                WebViewScreen(
                    url = httpSource.getHomeUrl(),
                    initialTitle = httpSource.name,
                    sourceId = httpSource.id,
                ),
            )
        }

        LaunchedEffect(source) {
            assistUrl = (source as? HttpSource)?.getHomeUrl()
        }

        // This page is pushed onto the main navigator, outside
        // HomeScreen, so HomeScreen's overlays (bottom bar, title
        // capsule, action capsule) do not exist here — reporting
        // actions to HomeScreen can never show them. The source
        // name in the top bar is the title, and this page owns
        // its own action capsule below.
        val context = LocalContext.current
        val barStyle by remember {
            context.appGraph.uiPreferences.barStyle
        }.collectAsState()
        // One style for the capsule: liquid glass only runs on
        // API 32+; below that it behaves like translucent.
        val effectiveBarStyle = if (barStyle == BarStyle.LIQUID_GLASS && !isMiuixGlassSupported) {
            BarStyle.TRANSLUCENT
        } else {
            barStyle
        }
        val glassEnabled = effectiveBarStyle == BarStyle.LIQUID_GLASS
        // The blur source. Created unconditionally (it is only a
        // handle); it is only *applied* to the content layer when
        // glass is enabled.
        val backdrop = rememberLayerBackdrop()

        // The capsule hides once the user has scrolled down past
        // a threshold, and comes back on any upward scroll — the
        // same behaviour as HomeScreen's capsule. The connection
        // only observes (it consumes nothing), so scrolling is
        // unaffected.
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

        // Floating action capsule, like the Library page: only the
        // search tools. Search opens the search field in the top
        // bar (it appears while toolbarQuery is set); filter opens
        // the source filters as a dropdown from the capsule. Empty
        // while searching, so the capsule hides while the search
        // field is open.
        val searchDescription = stringResource(MR.strings.action_search)
        val filterDescription = stringResource(MR.strings.action_filter)
        val capsuleActions = if (state.toolbarQuery != null) {
            emptyList()
        } else {
            listOf(
                CapsuleAction(
                    icon = MaterialSymbols.Rounded.Search,
                    contentDescription = searchDescription,
                    onClick = { viewModel.setToolbarQuery("") },
                ),
                CapsuleAction(
                    icon = MaterialSymbols.Rounded.FilterList,
                    contentDescription = filterDescription,
                    tint = if (state.listing is Listing.Search) {
                        MaterialTheme.colorScheme.active
                    } else {
                        null
                    },
                    onClick = viewModel::openFilterSheet,
                ),
            )
        }

        // This page is pushed onto the main navigator, outside
        // HomeScreen, so it has no Miuix Scaffold ancestor — and
        // Miuix overlays (dialogs, dropdowns) only render inside
        // one. The wrapper hosts them, so the filter dropdown and
        // the dialogs actually appear here; it also revives the
        // remove dialog, which silently never opened on this page.
        // It is layout-neutral: zero insets, the page background.
        MiuixScaffold(
            containerColor = MaterialTheme.colorScheme.background,
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
        ) {
            Scaffold(
                // The top bar holds the source name and, below it, the
                // filter chips. Back goes through the system back gesture.
                // While searching, the search field takes the top bar's
                // place instead.
                topBar = {
                    if (state.toolbarQuery != null) {
                        SearchToolbar(
                            searchQuery = state.toolbarQuery,
                            onChangeSearchQuery = viewModel::setToolbarQuery,
                            backgroundColor = MaterialTheme.colorScheme.background,
                            navigateUp = navigateUp,
                            onSearch = { viewModel.search(it) },
                        )
                    } else {
                        Column(
                            modifier = Modifier
                                .background(MiuixTheme.colorScheme.surface)
                                .pointerInput(Unit) {},
                        ) {
                            // Source name at the top left, with clearance from
                            // the status bar. The filter chips sit below it.
                            Text(
                                text = source.name,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(
                                    start = 16.dp,
                                    top = 75.dp,
                                    end = 16.dp,
                                    bottom = 4.dp,
                                ),
                            )
                            Row(
                                modifier = Modifier
                                    .horizontalScroll(rememberScrollState())
                                    // Breathing room above and below the chips.
                                    .padding(
                                        horizontal = MaterialTheme.padding.small,
                                        vertical = 8.dp,
                                    ),
                                horizontalArrangement = Arrangement.spacedBy(MaterialTheme.padding.small),
                            ) {
                                FilterButton(
                                    selected = state.listing == Listing.Popular,
                                    onClick = {
                                        viewModel.resetFilters()
                                        viewModel.setListing(Listing.Popular)
                                    },
                                    icon = MaterialSymbols.RoundedFilled.Favorite,
                                    label = stringResource(MR.strings.popular),
                                    barStyle = effectiveBarStyle,
                                    backdrop = backdrop,
                                )
                                if (source.supportsLatest) {
                                    FilterButton(
                                        selected = state.listing == Listing.Latest,
                                        onClick = {
                                            viewModel.resetFilters()
                                            viewModel.setListing(Listing.Latest)
                                        },
                                        icon = MaterialSymbols.Rounded.Verified,
                                        label = stringResource(MR.strings.latest),
                                        barStyle = effectiveBarStyle,
                                        backdrop = backdrop,
                                    )
                                }
                            }

                            HorizontalDivider()
                        }
                    }
                },
                snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            ) { paddingValues ->
                // The capsule floats over the list, so — like HomeScreen's
                // bar and capsules — it must stay OUTSIDE the layer
                // recorded by layerBackdrop: it blurs that layer, and
                // inside it it would blur itself and crash hwui
                // (infinite recursion in RenderNode::prepareTreeImpl).
                Box(modifier = Modifier.fillMaxSize()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .nestedScroll(capsuleScrollConnection)
                            .then(if (glassEnabled) Modifier.layerBackdrop(backdrop) else Modifier),
                    ) {
                        BrowseSourceContent(
                            source = source,
                            mangaList = viewModel.mangaPagerFlowFlow.collectAsLazyPagingItems(),
                            columns = viewModel.getColumnsPreference(LocalConfiguration.current.orientation),
                            displayMode = viewModel.displayMode,
                            snackbarHostState = snackbarHostState,
                            contentPadding = paddingValues,
                            onWebViewClick = onWebViewClick,
                            onHelpClick = { uriHandler.openUri(Constants.URL_HELP) },
                            onLocalSourceHelpClick = onHelpClick,
                            onMangaClick = { navigator.push((MangaScreen(it.id, true))) },
                            onMangaLongClick = { manga ->
                                scope.launch(Dispatchers.IO) {
                                    val duplicates = viewModel.getDuplicateLibraryManga(manga)
                                    when {
                                        manga.favorite -> viewModel.changeMangaFavorite(manga)
                                        duplicates.isNotEmpty() -> viewModel.setDialog(
                                            BrowseSourceViewModel.Dialog.AddDuplicateManga(manga, duplicates),
                                        )
                                        else -> viewModel.addFavorite(manga)
                                    }
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                }
                            },
                        )
                    }
                    // Top edge fade: content scrolling up under the
                    // top bar dissolves instead of being cut off at the
                    // bar's bottom edge. The veil matches the top bar's
                    // surface colour.
                    TopFadeBlur(
                        height = 48.dp,
                        color = MiuixTheme.colorScheme.surface,
                        barStyle = effectiveBarStyle,
                        backdrop = backdrop,
                        modifier = Modifier.align(Alignment.TopCenter),
                    )
                    // The action capsule, on the right edge over the
                    // list — the same glass as HomeScreen's capsule.
                    // Hidden while the filter panel is open (it sits
                    // over the capsule's spot).
                    AnimatedVisibility(
                        visible = capsuleScrollVisible && capsuleActions.isNotEmpty() &&
                            state.dialog !is BrowseSourceViewModel.Dialog.Filter,
                        enter = fadeIn(FloatMotion.FadeIn) + slideInHorizontally(animationSpec = FloatMotion.SlideIn, initialOffsetX = { it }),
                        exit = fadeOut(FloatMotion.FadeOut) + slideOutHorizontally(animationSpec = FloatMotion.SlideOut, targetOffsetX = { it }),
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
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

            val onDismissRequest = { viewModel.setDialog(null) }

            // Source filters as a dropdown from the capsule, like the
            // Library page's filter dropdown — not the bottom sheet
            // the filter used to be. The filter form is long, so the
            // panel is a fixed-width column with a scrolling list.
            // It renders through the Miuix Scaffold wrapper below: an
            // overlay needs the scaffold host, which this pushed page
            // otherwise lacks.
            val filterPopupMaxHeight = LocalConfiguration.current.screenHeightDp.dp * 0.7f
            OverlayListPopup(
                show = state.dialog is BrowseSourceViewModel.Dialog.Filter,
                alignment = PopupPositionProvider.Align.End,
                onDismissRequest = onDismissRequest,
                maxHeight = filterPopupMaxHeight,
                renderInRootScaffold = true,
            ) {
                Column(
                    modifier = Modifier.width(320.dp),
                ) {
                    Row(
                        modifier = Modifier.padding(8.dp),
                    ) {
                        TextButton(
                            text = stringResource(MR.strings.action_reset),
                            onClick = viewModel::resetFilters,
                        )

                        Spacer(modifier = Modifier.weight(1f))

                        Button(
                            onClick = {
                                viewModel.search(filters = state.filters)
                                onDismissRequest()
                            },
                            colors = ButtonDefaults.buttonColors(),
                        ) {
                            Text(stringResource(MR.strings.action_filter))
                        }
                    }
                    HorizontalDivider()
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                    ) {
                        items(state.filters) {
                            FilterItem(it) { viewModel.setFilters(state.filters) }
                        }
                    }
                }
            }

            when (val dialog = state.dialog) {
                // The filter panel is the dropdown above, not a dialog.
                is BrowseSourceViewModel.Dialog.Filter -> {}
                is BrowseSourceViewModel.Dialog.AddDuplicateManga -> {
                    DuplicateMangaDialog(
                        duplicates = dialog.duplicates,
                        onDismissRequest = onDismissRequest,
                        onConfirm = { viewModel.addFavorite(dialog.manga) },
                        onOpenManga = { navigator.push(MangaScreen(it.id)) },
                        onMigrate = { viewModel.setDialog(BrowseSourceViewModel.Dialog.Migrate(dialog.manga, it)) },
                    )
                }

                is BrowseSourceViewModel.Dialog.Migrate -> {
                    MigrateMangaDialog(
                        current = dialog.current,
                        target = dialog.target,
                        // Initiated from the context of [dialog.target] so we show [dialog.current].
                        onClickTitle = { navigator.push(MangaScreen(dialog.current.id)) },
                        onDismissRequest = onDismissRequest,
                    )
                }
                is BrowseSourceViewModel.Dialog.ChangeMangaCategory -> {
                    ChangeCategoryDialog(
                        initialSelection = dialog.initialSelection,
                        onDismissRequest = onDismissRequest,
                        onEditCategories = { navigator.push(CategoryScreen()) },
                        onConfirm = { include, _ ->
                            viewModel.changeMangaFavorite(dialog.manga)
                            viewModel.moveMangaToCategories(dialog.manga, include)
                        },
                    )
                }
                else -> {}
            }

            LaunchedEffect(Unit) {
                queryEvent.receiveAsFlow()
                    .collectLatest {
                        when (it) {
                            is SearchType.Genre -> viewModel.searchGenre(it.txt)
                            is SearchType.Text -> viewModel.search(it.txt)
                        }
                    }
            }
        }
    }

    suspend fun search(query: String) = queryEvent.send(SearchType.Text(query))
    suspend fun searchGenre(name: String) = queryEvent.send(SearchType.Genre(name))

    companion object {
        private val queryEvent = Channel<SearchType>()
    }

    sealed class SearchType(val txt: String) {
        class Text(txt: String) : SearchType(txt)
        class Genre(txt: String) : SearchType(txt)
    }
}

// The listing chips (Popular, Latest) in the Explore top bar. They are
// the same pill language as the floating bars, so they follow the global
// bar style: solid, translucent, frosted glass or brushed metal — the
// selected one is the accent tint, like the bottom bar's selected tab.
// In glass mode the chip gets the side capsule's blur, lens and
// highlight, but the veil stays at the translucent alpha: the chips sit
// on the header's opaque surface, so there is no page content behind
// them to blur, and the glass alpha would make the pill vanish. In
// metal mode the icon and label are engraved.
@Composable
private fun FilterButton(
    selected: Boolean,
    onClick: () -> Unit,
    icon: ImageVector,
    label: String,
    barStyle: BarStyle,
    backdrop: Backdrop,
) {
    val colorScheme = MiuixTheme.colorScheme
    val isGlass = barStyle == BarStyle.LIQUID_GLASS
    val isMetal = barStyle == BarStyle.METAL
    val metalDark = metalIsDark()
    val containerColor = when (barStyle) {
        BarStyle.OPAQUE -> colorScheme.surfaceContainer
        BarStyle.TRANSLUCENT, BarStyle.LIQUID_GLASS -> colorScheme.surfaceContainer.copy(alpha = 0.75f)
        // Metal draws its own surface; this value is never used.
        BarStyle.METAL -> colorScheme.surfaceContainer
    }
    val contentColor = if (selected) colorScheme.primary else colorScheme.onSurface
    val shape = RoundedCornerShape(12.dp)
    val highlight = if (isGlass) rememberTiltBrightHighlight() else null
    val textStyle = LocalTextStyle.current
    Box(
        modifier = Modifier
            .clip(shape)
            .then(
                if (isGlass) {
                    Modifier.drawBackdrop(
                        backdrop = backdrop,
                        shape = { shape },
                        effects = {
                            vibrancy()
                            blur(MiuixGlassBlurRadius.toPx(), MiuixGlassBlurRadius.toPx())
                            lens(
                                refractionHeight = 16.dp.toPx(),
                                refractionAmount = 10.dp.toPx(),
                                depthEffect = true,
                                chromaticAberration = MiuixGlassChromaticAberration,
                            )
                        },
                        highlight = { highlight?.copy(alpha = 0.66f) },
                        onDrawSurface = { drawRect(containerColor) },
                    )
                } else if (isMetal) {
                    Modifier.metalSurface(shape, metalDark)
                } else {
                    Modifier.background(containerColor)
                },
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (isMetal) {
                MetalEngravedIcon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = contentColor,
                    isDark = metalDark,
                    modifier = Modifier.size(18.dp),
                )
            } else {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = contentColor,
                )
            }
            Text(
                text = label,
                modifier = Modifier.padding(start = 4.dp),
                color = contentColor,
                style = if (isMetal) {
                    textStyle.copy(shadow = metalEngravedShadow(metalDark))
                } else {
                    textStyle
                },
            )
        }
    }
}
