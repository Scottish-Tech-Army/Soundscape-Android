package org.scottishtecharmy.soundscape.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.ImeAction
import org.scottishtecharmy.soundscape.screens.talkbackPasteAction

internal const val SEARCH_TEXT_FIELD_TEST_TAG = "mainSearchBarTextField"

/**
 * The text field of the expanded [MainSearchBar]. It takes the keyboard focus as soon as it
 * appears, and calls [onSearch] when the keyboard's Search key is pressed.
 *
 * This is per platform because of VoiceOver. It only offers its text editing rotors - Edit, with
 * Select/Copy/Cut/Paste, and moving the cursor by character or word - for a view implementing
 * UITextInput. Compose Multiplatform describes its text fields to VoiceOver as plain accessibility
 * elements, so while VoiceOver is running iOS swaps in a real UITextField. Everything else uses
 * [ComposeSearchTextField].
 */
@Composable
expect fun SearchTextField(
    value: String,
    onValueChange: (String) -> Unit,
    onSearch: () -> Unit,
    hint: String,
    modifier: Modifier = Modifier,
)

@Composable
internal fun ComposeSearchTextField(
    value: String,
    onValueChange: (String) -> Unit,
    onSearch: () -> Unit,
    hint: String,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val textStyle = MaterialTheme.typography.bodyLarge.copy(color = colors.onSurface)
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = true,
        cursorBrush = SolidColor(colors.primary),
        textStyle = textStyle,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { onSearch() }),
        modifier = modifier
            .focusRequester(focusRequester)
            .testTag(SEARCH_TEXT_FIELD_TEST_TAG)
            // A screen reader user has no Search key to press after
            // pasting, so search for what they pasted straight away.
            .talkbackPasteAction { pasted ->
                onValueChange(value + pasted)
                onSearch()
            },
        decorationBox = { inner ->
            Box(Modifier.fillMaxWidth()) {
                if (value.isEmpty()) {
                    Text(
                        text = hint,
                        style = textStyle.copy(color = colors.onSurfaceVariant)
                    )
                }
                inner()
            }
        }
    )
}
