package eu.kanade.presentation.more.settings.widget

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import eu.kanade.domain.ui.model.ThemeMode
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource
import top.yukonga.miuix.kmp.basic.TabRow
import top.yukonga.miuix.kmp.basic.TabRowDefaults
import top.yukonga.miuix.kmp.theme.MiuixTheme

private val options = mapOf(
    ThemeMode.SYSTEM to MR.strings.theme_system,
    ThemeMode.LIGHT to MR.strings.theme_light,
    ThemeMode.DARK to MR.strings.theme_dark,
)

@Composable
internal fun AppThemeModePreferenceWidget(
    value: ThemeMode,
    onItemClick: (ThemeMode) -> Unit,
) {
    val modes = remember { options.keys.toList() }
    TabRow(
        tabs = modes.map { stringResource(options.getValue(it)) },
        selectedTabIndex = modes.indexOf(value).coerceAtLeast(0),
        onTabSelected = { onItemClick(modes[it]) },
        // The default 9.dp spacing left black gaps between the three
        // options; flush tabs read as one segmented control.
        // The default row background is `surface` (pure black in dark
        // mode), which does not match the card (`surfaceContainer`) the
        // row sits in, so it read as a mismatched band. Match the row to
        // the card, and give the selected tab the next shade up.
        colors = TabRowDefaults.tabRowColors(
            backgroundColor = MiuixTheme.colorScheme.surfaceContainer,
            selectedBackgroundColor = MiuixTheme.colorScheme.surfaceContainerHighest,
        ),
        itemSpacing = 0.dp,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = PrefsHorizontalPadding, vertical = 8.dp),
    )
}
