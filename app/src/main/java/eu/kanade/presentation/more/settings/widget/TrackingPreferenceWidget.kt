package eu.kanade.presentation.more.settings.widget

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import eu.kanade.presentation.more.settings.LocalPreferenceHighlighted
import eu.kanade.presentation.track.components.TrackLogoIcon
import eu.kanade.tachiyomi.data.track.EnhancedTracker
import eu.kanade.tachiyomi.data.track.Tracker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import mihon.icons.materialsymbols.MaterialSymbols
import mihon.icons.materialsymbols.rounded.Check
import mihon.icons.materialsymbols.rounded.Refresh
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource
import top.yukonga.miuix.kmp.basic.BasicComponent
import top.yukonga.miuix.kmp.basic.IconButton

@Composable
fun TrackingPreferenceWidget(
    modifier: Modifier = Modifier,
    tracker: Tracker,
    isLoggedIn: Boolean,
    isRefreshing: Boolean,
    onClick: (() -> Unit)? = null,
) {
    val scope = rememberCoroutineScope { Dispatchers.IO }
    val displayName = tracker.getDisplayUsername()
    BasicComponent(
        modifier = modifier.highlightBackground(LocalPreferenceHighlighted.current),
        title = tracker.name,
        summary = if (isLoggedIn && displayName.isNotBlank()) displayName else null,
        startAction = { TrackLogoIcon(tracker) },
        endActions = {
            if (isLoggedIn) {
                if (tracker !is EnhancedTracker) {
                    IconButton(
                        enabled = !isRefreshing,
                        onClick = {
                            scope.launch { tracker.refreshUser() }
                        },
                    ) {
                        if (!isRefreshing) {
                            Icon(
                                imageVector = MaterialSymbols.Rounded.Refresh,
                                modifier = Modifier
                                    .padding(4.dp)
                                    .size(32.dp),
                                contentDescription = stringResource(MR.strings.refresh_tracker_profile),
                            )
                        } else {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 2.dp,
                            )
                        }
                    }
                }
                Icon(
                    imageVector = MaterialSymbols.Rounded.Check,
                    modifier = Modifier
                        .padding(4.dp)
                        .size(32.dp),
                    tint = Color(0xFF4CAF50),
                    contentDescription = stringResource(MR.strings.login_success),
                )
            }
        },
        onClick = onClick,
    )
}
