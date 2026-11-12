package eu.kanade.presentation.more.settings.widget

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import mihon.icons.materialsymbols.MaterialSymbols
import mihon.icons.materialsymbols.roundedfilled.Cancel
import mihon.icons.materialsymbols.roundedfilled.Error
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.overlay.OverlayDialog

@Composable
fun EditTextPreferenceWidget(
    title: String,
    subtitle: String?,
    icon: ImageVector?,
    value: String,
    onConfirm: suspend (String) -> Boolean,
) {
    var isDialogShown by remember { mutableStateOf(false) }

    TextPreferenceWidget(
        title = title,
        subtitle = subtitle?.format(value),
        icon = icon,
        onPreferenceClick = { isDialogShown = true },
    )

    if (isDialogShown) {
        val scope = rememberCoroutineScope()
        val onDismissRequest = { isDialogShown = false }
        var textFieldValue by rememberSaveable(stateSaver = TextFieldValue.Saver) {
            mutableStateOf(TextFieldValue(value))
        }
        OverlayDialog(
            show = true,
            title = title,
            onDismissRequest = onDismissRequest,
        ) {
            Column {
                TextField(
                    value = textFieldValue,
                    onValueChange = { textFieldValue = it },
                    trailingIcon = {
                        if (textFieldValue.text.isBlank()) {
                            Icon(imageVector = MaterialSymbols.RoundedFilled.Error, contentDescription = null)
                        } else {
                            Icon(
                                imageVector = MaterialSymbols.RoundedFilled.Cancel,
                                contentDescription = null,
                                modifier = Modifier.clickable { textFieldValue = TextFieldValue("") },
                            )
                        }
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Column(modifier = Modifier.padding(top = 12.dp)) {
                    Button(
                        enabled = textFieldValue.text != value && textFieldValue.text.isNotBlank(),
                        onClick = {
                            scope.launch {
                                if (onConfirm(textFieldValue.text)) {
                                    onDismissRequest()
                                }
                            }
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
    }
}
