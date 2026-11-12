package eu.kanade.presentation.more.settings.screen.about

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.mikepenz.aboutlibraries.ui.compose.android.produceLibraries
import com.mikepenz.aboutlibraries.ui.compose.m3.LibrariesContainer
import com.mikepenz.aboutlibraries.ui.compose.variant.LibraryDetailMode
import eu.kanade.presentation.util.Screen
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import eu.kanade.tachiyomi.R
import mihon.icons.materialsymbols.MaterialSymbols
import mihon.icons.materialsymbols.automirroredrounded.ArrowBack
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SmallTopAppBar

class OpenSourceLicensesScreen : Screen() {

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        Scaffold(
            topBar = {
                SmallTopAppBar(
                    title = stringResource(MR.strings.licenses),
                    navigationIcon = {
                        IconButton(onClick = navigator::pop) {
                            Icon(
                                imageVector = MaterialSymbols.AutoMirroredRounded.ArrowBack,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurface,
                            )
                        }
                    },
                )
            },
        ) { contentPadding ->
            val libraries by produceLibraries(R.raw.aboutlibraries)
            LibrariesContainer(
                libraries = libraries,
                modifier = Modifier
                    .fillMaxSize(),
                contentPadding = contentPadding,
                detailMode = LibraryDetailMode.Sheet,
            )
        }
    }
}
