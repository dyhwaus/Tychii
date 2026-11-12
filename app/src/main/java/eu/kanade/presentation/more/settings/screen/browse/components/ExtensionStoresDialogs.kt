package eu.kanade.presentation.more.settings.screen.browse.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.overlay.OverlayDialog
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun ExtensionStoreCreateDialog(
    onDismissRequest: () -> Unit,
    onCreate: (String) -> Unit,
    storeIndexUrls: Set<String>,
    processing: Boolean,
    errorMessage: String?,
) {
    val state = rememberTextFieldState()
    val storeAlreadyExists by remember(storeIndexUrls) {
        derivedStateOf {
            val indexUrl = state.text.toString()
            storeIndexUrls.contains(indexUrl)
        }
    }

    val focusRequester = remember { FocusRequester() }
    val isError = errorMessage != null || storeAlreadyExists

    OverlayDialog(
        show = true,
        title = stringResource(MR.strings.extensionStoresScreen_addStore_title),
        onDismissRequest = onDismissRequest,
    ) {
        Column {
            TextField(
                state = state,
                label = stringResource(MR.strings.extensionStoresScreen_addStoreInput_inputLabel),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                lineLimits = TextFieldLineLimits.SingleLine,
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester),
            )
            val msgRes = if (storeAlreadyExists) {
                MR.strings.extensionStoresScreen_addStore_alreadyExists
            } else {
                MR.strings.information_required_plain
            }
            Text(
                text = errorMessage ?: stringResource(msgRes),
                color = if (isError) MiuixTheme.colorScheme.error else MiuixTheme.colorScheme.onSurfaceVariantSummary,
                fontSize = MiuixTheme.textStyles.body2.fontSize,
                modifier = Modifier.padding(top = 8.dp),
            )
            Column(modifier = Modifier.padding(top = 12.dp)) {
                Button(
                    onClick = { onCreate(state.text.toString()) },
                    enabled = !processing && state.text.isNotEmpty() && !storeAlreadyExists,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = stringResource(
                            resource = if (processing) {
                                MR.strings.extensionStoresScreen_addStore_processing
                            } else {
                                MR.strings.action_add
                            },
                        ),
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = onDismissRequest,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(text = stringResource(MR.strings.action_cancel))
                }
            }
        }
    }

    LaunchedEffect(focusRequester) {
        focusRequester.requestFocus()
    }
}

@Composable
fun ExtensionStoreDeleteDialog(
    onDismissRequest: () -> Unit,
    onDelete: () -> Unit,
    storeName: String,
    storeIndexUrl: String,
) {
    OverlayDialog(
        show = true,
        title = stringResource(MR.strings.extensionStoresScreen_deleteStore_title),
        summary = stringResource(MR.strings.extensionStoresScreen_deleteStore_body, storeName, storeIndexUrl),
        onDismissRequest = onDismissRequest,
    ) {
        Column(modifier = Modifier.padding(top = 12.dp)) {
            Button(
                onClick = {
                    onDelete()
                    onDismissRequest()
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(text = stringResource(MR.strings.action_ok))
            }
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = onDismissRequest,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(text = stringResource(MR.strings.action_cancel))
            }
        }
    }
}

@Composable
fun ExtensionStoreConfirmDialog(
    onDismissRequest: () -> Unit,
    onCreate: () -> Unit,
    storeIndexUrl: String,
    storeAlreadyExists: Boolean,
    processing: Boolean,
    errorMessage: String?,
) {
    val state = rememberTextFieldState(initialText = storeIndexUrl)
    OverlayDialog(
        show = true,
        title = stringResource(MR.strings.extensionStoresScreen_addStore_title),
        onDismissRequest = onDismissRequest,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(text = stringResource(MR.strings.extensionStoresScreen_addStoreDeeplink_bodyText))
            TextField(
                state = state,
                readOnly = true,
                modifier = Modifier.fillMaxWidth(),
            )
            when {
                storeAlreadyExists -> {
                    Text(
                        text = stringResource(MR.strings.extensionStoresScreen_addStore_alreadyExists),
                        color = MiuixTheme.colorScheme.error,
                        fontSize = MiuixTheme.textStyles.body2.fontSize,
                    )
                }
                errorMessage != null -> {
                    Text(
                        text = errorMessage,
                        color = MiuixTheme.colorScheme.error,
                        fontSize = MiuixTheme.textStyles.body2.fontSize,
                    )
                }
            }
            Column(modifier = Modifier.padding(top = 12.dp)) {
                Button(
                    onClick = onCreate,
                    enabled = !storeAlreadyExists && !processing,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = stringResource(
                            resource = if (processing) {
                                MR.strings.extensionStoresScreen_addStore_processing
                            } else {
                                MR.strings.action_add
                            },
                        ),
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = onDismissRequest,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(text = stringResource(MR.strings.action_cancel))
                }
            }
        }
    }
}
