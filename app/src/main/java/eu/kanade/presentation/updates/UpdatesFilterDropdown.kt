package eu.kanade.presentation.updates

import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import mihon.icons.materialsymbols.MaterialSymbols
import mihon.icons.materialsymbols.rounded.Block
import tachiyomi.core.common.preference.TriState
import top.yukonga.miuix.kmp.basic.DropdownDefaults
import top.yukonga.miuix.kmp.basic.DropdownEntry
import top.yukonga.miuix.kmp.basic.DropdownItem
import top.yukonga.miuix.kmp.popup.OverlayDropdownPopup

// One tri-state filter row in the Updates filter dropdown. Tapping
// cycles any -> only -> exclude, the same as the TriStateItem rows in
// the old settings sheet. The "excluded scanlators" switch is a plain
// boolean, represented with only any and only.
data class UpdatesFilterOption(
    val label: String,
    val state: TriState,
    val enabled: Boolean = true,
    val onToggle: () -> Unit,
)

@Composable
fun UpdatesFilterDropdown(
    options: List<UpdatesFilterOption>,
    selectingFilter: Boolean,
    onSelectingFilterChange: (Boolean) -> Unit,
) {
    OverlayDropdownPopup(
        entries = listOf(
            DropdownEntry(
                items = options.map { option ->
                    DropdownItem(
                        text = option.label,
                        enabled = option.enabled,
                        // "Only" is the selected row (checkmark);
                        // "exclude" keeps the row unselected but shows
                        // the block icon, so the three states read
                        // differently at a glance.
                        selected = option.state == TriState.ENABLED_IS,
                        icon = if (option.state == TriState.ENABLED_NOT) {
                            { modifier ->
                                Icon(
                                    imageVector = MaterialSymbols.Rounded.Block,
                                    contentDescription = null,
                                    modifier = modifier,
                                )
                            }
                        } else {
                            null
                        },
                        onClick = option.onToggle,
                    )
                },
            ),
        ),
        show = selectingFilter,
        onDismiss = { onSelectingFilterChange(false) },
        onDismissFinished = {},
        maxHeight = null,
        dropdownColors = DropdownDefaults.dropdownColors(),
        renderInRootScaffold = true,
        // Stays open so several filters can be set in one go;
        // dismissed by tapping outside, like the other dropdowns.
        collapseOnSelection = false,
    )
}
