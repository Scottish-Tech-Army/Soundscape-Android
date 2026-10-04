package org.scottishtecharmy.soundscape.components

import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.viewinterop.UIKitInteropInteractionMode
import androidx.compose.ui.viewinterop.UIKitInteropProperties
import androidx.compose.ui.viewinterop.UIKitView
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.readValue
import org.scottishtecharmy.soundscape.screens.rememberVoiceOverRunning
import org.scottishtecharmy.soundscape.ui.theme.spacing
import platform.CoreGraphics.CGRectZero
import platform.Foundation.NSNotificationCenter
import platform.Foundation.NSOperationQueue
import platform.UIKit.UIAccessibilityIdentificationProtocol
import platform.UIKit.UIAccessibilityLayoutChangedNotification
import platform.UIKit.UIAccessibilityPostNotification
import platform.UIKit.UIColor
import platform.UIKit.UIFont
import platform.UIKit.UIReturnKeyType
import platform.UIKit.UITextBorderStyle
import platform.UIKit.UITextField
import platform.UIKit.UITextFieldDelegateProtocol
import platform.UIKit.UITextFieldTextDidChangeNotification
import platform.UIKit.accessibilityLabel
import platform.UIKit.setAccessibilityLabel
import platform.darwin.NSObject

/**
 * Master switch for the native search field. Like the accessibility proxy in
 * StartsSpeechControl.ios.kt this is hand-rolled Compose/UIKit interop, so keep a one-line
 * route back to stock Compose behaviour for when CMP is next upgraded.
 */
private const val USE_NATIVE_SEARCH_FIELD = true

/** Frames to wait for CMP to put the field in a window, before which it can't take focus. */
private const val WINDOW_ATTACH_FRAME_BUDGET = 30

@OptIn(ExperimentalComposeUiApi::class)
private val nativeFieldProperties = UIKitInteropProperties(
    // The field takes its touches straight away, there being nothing to scroll underneath it.
    interactionMode = UIKitInteropInteractionMode.NonCooperative,
    // The point of the whole exercise: VoiceOver traverses the UITextField itself, and so
    // finds a UITextInput to offer its text editing rotors for.
    isNativeAccessibilityEnabled = true,
    // Composites the field above the Metal canvas, so that it can have a clear background
    // and show the Compose surface behind it.
    placedAsOverlay = true,
)

@Composable
actual fun SearchTextField(
    value: String,
    onValueChange: (String) -> Unit,
    onSearch: () -> Unit,
    hint: String,
    modifier: Modifier,
) {
    // VoiceOver can be turned on or off while the search is open. The text lives in the
    // caller, so swapping one field for the other only costs the cursor position.
    if (USE_NATIVE_SEARCH_FIELD && rememberVoiceOverRunning()) {
        NativeSearchTextField(value, onValueChange, onSearch, hint, modifier)
    } else {
        ComposeSearchTextField(value, onValueChange, onSearch, hint, modifier)
    }
}

@Composable
private fun NativeSearchTextField(
    value: String,
    onValueChange: (String) -> Unit,
    onSearch: () -> Unit,
    hint: String,
    modifier: Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val textColor = colors.onSurface.toUIColor()
    val tintColor = colors.primary.toUIColor()
    // The Compose field's size is in sp, which is points scaled by the font scale
    val fontSize = MaterialTheme.typography.bodyLarge.fontSize.value * LocalDensity.current.fontScale

    // The field and its delegate are created once while the callbacks are fresh closures on
    // every recomposition, so they're read through these stable State instances.
    val currentOnValueChange = rememberUpdatedState(onValueChange)
    val currentOnSearch = rememberUpdatedState(onSearch)

    // UITextField only holds its delegate weakly, so it has to be remembered here
    val delegate = remember { SearchFieldDelegate(currentOnSearch) }
    val field = remember { createSearchField(delegate) }

    DisposableEffect(field) {
        val center = NSNotificationCenter.defaultCenter
        val observer = center.addObserverForName(
            name = UITextFieldTextDidChangeNotification,
            `object` = field,
            queue = NSOperationQueue.mainQueue,
        ) {
            currentOnValueChange.value(field.text.orEmpty())
        }
        onDispose { center.removeObserver(observer) }
    }

    // Take the keyboard, and move VoiceOver to the field so that the user lands where they
    // are typing. CMP puts the field in a window at a frame boundary, after composition.
    LaunchedEffect(field) {
        var frames = 0
        while (field.window == null && frames < WINDOW_ATTACH_FRAME_BUDGET) {
            withFrameNanos { }
            frames++
        }
        if (field.becomeFirstResponder()) {
            UIAccessibilityPostNotification(UIAccessibilityLayoutChangedNotification, field)
        }
    }

    UIKitView(
        factory = { field },
        // An interop view has no intrinsic size for the header row to wrap
        modifier = modifier.height(spacing.targetSize),
        update = { view ->
            // Guarded writes: setting the text moves the cursor to the end, and re-setting
            // the label on the element VoiceOver is focused on makes it stutter.
            if (view.text.orEmpty() != value) view.text = value
            if (view.placeholder != hint) view.placeholder = hint
            if (view.accessibilityLabel() != hint) view.setAccessibilityLabel(hint)
            view.textColor = textColor
            view.tintColor = tintColor
            view.font = UIFont.systemFontOfSize(fontSize.toDouble())
        },
        onRelease = { view -> view.resignFirstResponder() },
        properties = nativeFieldProperties,
    )
}

@OptIn(ExperimentalForeignApi::class)
private fun createSearchField(delegate: UITextFieldDelegateProtocol): UITextField =
    UITextField(frame = CGRectZero.readValue()).apply {
        backgroundColor = UIColor.clearColor
        borderStyle = UITextBorderStyle.UITextBorderStyleNone
        returnKeyType = UIReturnKeyType.UIReturnKeySearch
        this.delegate = delegate
        // UITextField adopts UIAccessibilityIdentification, but the Kotlin/Native bindings
        // only expose accessibilityIdentifier on the protocol - see StartsSpeechControl.ios.kt
        (this as? UIAccessibilityIdentificationProtocol)
            ?.setAccessibilityIdentifier(SEARCH_TEXT_FIELD_TEST_TAG)
    }

private class SearchFieldDelegate(
    private val onSearch: State<() -> Unit>,
) : NSObject(), UITextFieldDelegateProtocol {
    override fun textFieldShouldReturn(textField: UITextField): Boolean {
        // A search for nothing is ignored, so leave the keyboard up for it. Otherwise put it
        // away ourselves: Compose's keyboard controller knows nothing about this field.
        if (!textField.text.isNullOrBlank()) {
            textField.resignFirstResponder()
            onSearch.value()
        }
        return false
    }
}

private fun Color.toUIColor(): UIColor =
    UIColor(
        red = red.toDouble(),
        green = green.toDouble(),
        blue = blue.toDouble(),
        alpha = alpha.toDouble(),
    )
