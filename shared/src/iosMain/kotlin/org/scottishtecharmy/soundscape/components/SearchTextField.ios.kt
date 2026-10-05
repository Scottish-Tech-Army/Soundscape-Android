package org.scottishtecharmy.soundscape.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import platform.UIKit.UIReturnKeyType

@Composable
actual fun SearchTextField(
    value: String,
    onValueChange: (String) -> Unit,
    onSearch: () -> Unit,
    hint: String,
    modifier: Modifier,
) {
    if (usesNativeTextFields()) {
        NativeTextField(
            value = value,
            onValueChange = onValueChange,
            label = hint,
            placeholder = hint,
            identifier = SEARCH_TEXT_FIELD_TEST_TAG,
            textStyle = MaterialTheme.typography.bodyLarge,
            returnKeyType = UIReturnKeyType.UIReturnKeySearch,
            onReturn = { text ->
                // A search for nothing is ignored, so leave the keyboard up for it
                if (text.isBlank()) {
                    false
                } else {
                    onSearch()
                    true
                }
            },
            focusOnAppear = true,
            modifier = modifier,
        )
    } else {
        ComposeSearchTextField(value, onValueChange, onSearch, hint, modifier)
    }
}
