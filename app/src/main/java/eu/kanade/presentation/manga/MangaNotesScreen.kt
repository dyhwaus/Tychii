package eu.kanade.presentation.manga

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import eu.kanade.presentation.manga.components.MangaNotesTextArea
import eu.kanade.tachiyomi.ui.manga.notes.MangaNotesScreen
import mihon.icons.materialsymbols.MaterialSymbols
import mihon.icons.materialsymbols.automirroredrounded.ArrowBack
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SmallTopAppBar
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun MangaNotesScreen(
    state: MangaNotesScreen.State,
    navigateUp: () -> Unit,
    onUpdate: (String) -> Unit,
) {
    Scaffold(
        topBar = {
            SmallTopAppBar(
                title = stringResource(MR.strings.action_edit_notes),
                subtitle = state.manga.title,
                navigationIcon = {
                    IconButton(onClick = navigateUp) {
                        Icon(
                            imageVector = MaterialSymbols.AutoMirroredRounded.ArrowBack,
                            contentDescription = null,
                            tint = MiuixTheme.colorScheme.onSurface,
                        )
                    }
                },
            )
        },
        // Same insets as the Material Scaffold wrapper this replaces.
        // The page stays on the app theme background, not the Miuix
        // surface: the backdrop gradient and the cover-color wash
        // are built on it.
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets.systemBars,
    ) { contentPadding ->
        MangaNotesTextArea(
            state = state,
            onUpdate = onUpdate,
            modifier = Modifier
                .padding(contentPadding)
                .consumeWindowInsets(contentPadding)
                .imePadding(),
        )
    }
}
