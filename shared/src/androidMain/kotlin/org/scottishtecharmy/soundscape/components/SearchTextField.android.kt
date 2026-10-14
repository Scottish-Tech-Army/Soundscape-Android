package org.scottishtecharmy.soundscape.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
actual fun SearchTextField(
    value: String,
    onValueChange: (String) -> Unit,
    onSearch: () -> Unit,
    hint: String,
    modifier: Modifier,
) = ComposeSearchTextField(value, onValueChange, onSearch, hint, modifier)
