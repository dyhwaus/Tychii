package eu.kanade.presentation.reader.appbars

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import eu.kanade.domain.ui.model.BarStyle
import eu.kanade.presentation.miuix.FloatMotion
import eu.kanade.presentation.miuix.MiuixGlassBlurRadius
import eu.kanade.presentation.miuix.MiuixGlassChromaticAberration
import eu.kanade.presentation.miuix.MiuixGlassSurfaceAlpha
import eu.kanade.presentation.miuix.TychiiCorners
import eu.kanade.presentation.miuix.lens
import eu.kanade.presentation.miuix.metalIsDark
import eu.kanade.presentation.miuix.metalSurface
import eu.kanade.presentation.miuix.rememberTiltBrightHighlight
import eu.kanade.presentation.miuix.vibrancy
import eu.kanade.presentation.reader.components.ChapterNavigator
import eu.kanade.presentation.reader.components.ChapterNavigatorType
import eu.kanade.presentation.util.isTabletUi
import eu.kanade.tachiyomi.ui.reader.setting.ReaderOrientation
import eu.kanade.tachiyomi.ui.reader.setting.ReadingMode
import tachiyomi.presentation.core.components.material.padding
import top.yukonga.miuix.kmp.blur.Backdrop
import top.yukonga.miuix.kmp.blur.blur
import top.yukonga.miuix.kmp.blur.drawBackdrop
import top.yukonga.miuix.kmp.theme.MiuixTheme


@Composable
fun ReaderAppBars(
    visible: Boolean,

    mangaTitle: String?,
    chapterTitle: String?,
    navigateUp: () -> Unit,
    onClickTopAppBar: () -> Unit,
    bookmarked: Boolean,
    onToggleBookmarked: () -> Unit,
    onOpenInWebView: (() -> Unit)?,
    onOpenInBrowser: (() -> Unit)?,
    onShare: (() -> Unit)?,

    chapterNavigatorType: ChapterNavigatorType,
    verticalNavigatorHeight: Float,
    onNextChapter: () -> Unit,
    enabledNext: Boolean,
    onPreviousChapter: () -> Unit,
    enabledPrevious: Boolean,
    currentPage: Int,
    totalPages: Int,
    onPageIndexChange: (Int) -> Unit,
    onPageIndexChangeFinished: () -> Unit,

    readingMode: ReadingMode,
    onClickReadingMode: () -> Unit,
    orientation: ReaderOrientation,
    onClickOrientation: () -> Unit,
    cropEnabled: Boolean,
    onClickCropBorder: () -> Unit,
    onClickSettings: () -> Unit,
    barStyle: BarStyle = BarStyle.TRANSLUCENT,
    backdrop: Backdrop? = null,
) {
    // The bars float over the manga page: in liquid glass mode they use
    // the same frosted material as the bottom bar and capsules (blur +
    // vibrancy + lens + tilt highlight over a surface veil), otherwise
    // the solid/translucent surface — or the brushed-metal surface — of
    // the global bar style. Both bars are floating pills now — the same
    // action capsules elsewhere — instead of full-width sharp bands,
    // with the chapter navigator pill lined up on the same side
    // margins.
    val glassBackdrop = if (barStyle == BarStyle.LIQUID_GLASS) backdrop else null
    val isGlass = glassBackdrop != null
    val surfaceContainer = MiuixTheme.colorScheme.surfaceContainer
    val containerColor = when {
        isGlass -> surfaceContainer.copy(alpha = MiuixGlassSurfaceAlpha)
        barStyle == BarStyle.OPAQUE -> surfaceContainer
        else -> surfaceContainer.copy(alpha = 0.75f)
    }
    val pillShape = remember { RoundedCornerShape(TychiiCorners.Bar) }
    val highlight = if (isGlass) rememberTiltBrightHighlight() else null
    val pillBackground = if (isGlass) {
        Modifier.drawBackdrop(
            backdrop = glassBackdrop,
            shape = { pillShape },
            effects = {
                vibrancy()
                blur(MiuixGlassBlurRadius.toPx(), MiuixGlassBlurRadius.toPx())
                lens(
                    refractionHeight = 24.dp.toPx(),
                    refractionAmount = 24.dp.toPx(),
                    depthEffect = true,
                    chromaticAberration = MiuixGlassChromaticAberration,
                )
            },
            highlight = { highlight?.copy(alpha = 0.75f) },
            onDrawSurface = { drawRect(containerColor) },
        )
    } else if (barStyle == BarStyle.METAL) {
        Modifier.metalSurface(pillShape, metalIsDark())
    } else {
        Modifier.background(containerColor, pillShape)
    }
    val sidePadding = if (isTabletUi()) 24.dp else 12.dp

    Column(modifier = Modifier.fillMaxHeight()) {
        AnimatedVisibility(
            visible = visible,
            enter = slideInVertically(FloatMotion.SlideIn) { -it } + fadeIn(FloatMotion.FadeIn),
            exit = slideOutVertically(FloatMotion.SlideOut) { -it } + fadeOut(FloatMotion.FadeOut),
        ) {
            ReaderTopBar(
                modifier = Modifier
                    .statusBarsPadding()
                    .padding(top = 8.dp, start = sidePadding, end = sidePadding)
                    .clip(pillShape)
                    .then(pillBackground)
                    .clickable(onClick = onClickTopAppBar),
                mangaTitle = mangaTitle,
                chapterTitle = chapterTitle,
                navigateUp = navigateUp,
                bookmarked = bookmarked,
                onToggleBookmarked = onToggleBookmarked,
                onOpenInWebView = onOpenInWebView,
                onOpenInBrowser = onOpenInBrowser,
                onShare = onShare,
            )
        }

        if (!chapterNavigatorType.isHorizontal()) {
            val sliderOnLeft = chapterNavigatorType == ChapterNavigatorType.VERTICAL_LEFT
            CompositionLocalProvider(
                LocalLayoutDirection provides if (sliderOnLeft) LayoutDirection.Ltr else LayoutDirection.Rtl,
            ) {
                Row(modifier = Modifier.weight(1f)) {
                    AnimatedVisibility(
                        visible = visible,
                        enter = slideInHorizontally(FloatMotion.SlideIn) { if (sliderOnLeft) -it else it } +
                            fadeIn(FloatMotion.FadeIn),
                        exit = slideOutHorizontally(FloatMotion.SlideOut) { if (sliderOnLeft) -it else it } +
                            fadeOut(FloatMotion.FadeOut),
                    ) {
                        Row {
                            Spacer(modifier = Modifier.width(MaterialTheme.padding.small))
                            Box(
                                modifier = Modifier.fillMaxHeight(),
                                contentAlignment = Alignment.BottomCenter,
                            ) {
                                ChapterNavigator(
                                    modifier = Modifier.fillMaxHeight(verticalNavigatorHeight),
                                    type = chapterNavigatorType,
                                    onNextChapter = onNextChapter,
                                    enabledNext = enabledNext,
                                    onPreviousChapter = onPreviousChapter,
                                    enabledPrevious = enabledPrevious,
                                    currentPage = currentPage,
                                    totalPages = totalPages,
                                    onPageIndexChange = onPageIndexChange,
                                    onPageIndexChangeFinished = onPageIndexChangeFinished,
                                    barStyle = barStyle,
                                    backdrop = backdrop,
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        } else {
            Spacer(Modifier.weight(1f))
        }

        AnimatedVisibility(
            visible = visible,
            enter = slideInVertically(FloatMotion.SlideIn) { it } + fadeIn(FloatMotion.FadeIn),
            exit = slideOutVertically(FloatMotion.SlideOut) { it } + fadeOut(FloatMotion.FadeOut),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(MaterialTheme.padding.small)) {
                if (chapterNavigatorType.isHorizontal()) {
                    ChapterNavigator(
                        type = chapterNavigatorType,
                        onNextChapter = onNextChapter,
                        enabledNext = enabledNext,
                        onPreviousChapter = onPreviousChapter,
                        enabledPrevious = enabledPrevious,
                        currentPage = currentPage,
                        totalPages = totalPages,
                        onPageIndexChange = onPageIndexChange,
                        onPageIndexChangeFinished = onPageIndexChangeFinished,
                        barStyle = barStyle,
                        backdrop = backdrop,
                    )
                }
                ReaderBottomBar(
                    modifier = Modifier
                        .fillMaxWidth()
                        .windowInsetsPadding(WindowInsets.navigationBars)
                        .padding(start = sidePadding, end = sidePadding, bottom = 8.dp)
                        .clip(pillShape)
                        .then(pillBackground),
                    readingMode = readingMode,
                    onClickReadingMode = onClickReadingMode,
                    orientation = orientation,
                    onClickOrientation = onClickOrientation,
                    cropEnabled = cropEnabled,
                    onClickCropBorder = onClickCropBorder,
                    onClickSettings = onClickSettings,
                )
            }
        }
    }
}
