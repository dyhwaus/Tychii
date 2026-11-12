package eu.kanade.presentation.more.settings.screen.browse.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import eu.kanade.presentation.category.components.CategoryFloatingActionButton
import eu.kanade.presentation.more.settings.screen.browse.ExtensionStoreScreenState
import mihon.domain.extension.model.ExtensionStore
import mihon.icons.materialsymbols.MaterialSymbols
import mihon.icons.materialsymbols.automirroredrounded.ArrowBack
import mihon.icons.materialsymbols.rounded.Refresh
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.material.padding
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SmallTopAppBar
import tachiyomi.presentation.core.components.material.topSmallPaddingValues
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.screens.EmptyScreen
import tachiyomi.presentation.core.util.plus

@Composable
fun ExtensionStoresScreen(
    state: ExtensionStoreScreenState.Success,
    onClickCreate: () -> Unit,
    onCopy: (ExtensionStore) -> Unit,
    onOpenWebsite: (ExtensionStore) -> Unit,
    onOpenDiscord: (ExtensionStore) -> Unit,
    onClickDelete: (ExtensionStore) -> Unit,
    onClickRefresh: () -> Unit,
    navigateUp: () -> Unit,
) {
    val lazyListState = rememberLazyListState()
    Scaffold(
        topBar = {
            SmallTopAppBar(
                title = stringResource(MR.strings.extensionStores),
                navigationIcon = {
                    IconButton(onClick = navigateUp) {
                        Icon(
                            imageVector = MaterialSymbols.AutoMirroredRounded.ArrowBack,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onClickRefresh) {
                        Icon(
                            imageVector = MaterialSymbols.Rounded.Refresh,
                            contentDescription = stringResource(resource = MR.strings.action_webview_refresh),
                        )
                    }
                },
            )
        },
        floatingActionButton = {
            CategoryFloatingActionButton(
                lazyListState = lazyListState,
                onCreate = onClickCreate,
            )
        },
    ) { paddingValues ->
        if (state.isEmpty) {
            EmptyScreen(
                MR.strings.extensionStoresScreen_emptyLabel,
                modifier = Modifier.padding(paddingValues),
            )
            return@Scaffold
        }

        ExtensionStoresContent(
            repos = state.stores,
            lazyListState = lazyListState,
            paddingValues = paddingValues + topSmallPaddingValues +
                PaddingValues(horizontal = MaterialTheme.padding.medium),
            onCopy = onCopy,
            onOpenWebsite = onOpenWebsite,
            onOpenDiscord = onOpenDiscord,
            onClickDelete = onClickDelete,
        )
    }
}
