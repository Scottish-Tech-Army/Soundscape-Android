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
import androidx.compose.ui.text.TextStyle
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
import platform.UIKit.accessibilityHint
import platform.UIKit.accessibilityLabel
import platform.UIKit.setAccessibilityHint
import platform.UIKit.setAccessibilityLabel
import platform.darwin.NSObject

/**
 * Master switch for the native text fields. Like the accessibility proxy in
 * StartsSpeechControl.ios.kt this is hand-rolled Compose/UIKit interop, so keep a one-line
 * route back to stock Compose behaviour for when CMP is next upgraded.
 */
private const val USE_NATIVE_TEXT_FIELDS = true

/** Frames to wait for CMP to put the field in a window, before which it can't take focus. */
private const val WINDOW_ATTACH_FRAME_BUDGET = 30

/**
 * Whether text fields should be a [NativeTextField] rather than Compose's own, which is while
 * VoiceOver is running.
 *
 * VoiceOver only offers its text editing rotors - Edit, with Select/Copy/Cut/Paste, and moving
 * the cursor by character or word - for a view implementing UITextInput. Compose Multiplatform
 * describes its text fields to VoiceOver as plain accessibility elements, which get none of them.
 *
 * VoiceOver can be turned on or off while a field is on screen. Callers hold the text, so
 * swapping one kind of field for the other only costs the cursor position.
 */
@Composable
internal fun usesNativeTextFields(): Boolean =
    USE_NATIVE_TEXT_FIELDS && rememberVoiceOverRunning()

@OptIn(ExperimentalComposeUiApi::class)
private fun nativeFieldProperties(interactionMode: UIKitInteropInteractionMode) =
    UIKitInteropProperties(
        interactionMode = interactionMode,
        // The point of the whole exercise: VoiceOver traverses the UITextField itself, and so
        // finds a UITextInput to offer its text editing rotors for.
        isNativeAccessibilityEnabled = true,
        // Composites the field above the Metal canvas, so that it can have a clear background
        // and show the Compose surface behind it.
        placedAsOverlay = true,
    )

/** For a field with nothing to scroll underneath it, which takes its touches straight away. */
private val fixedFieldProperties =
    nativeFieldProperties(UIKitInteropInteractionMode.NonCooperative)

/** For a field in a scrolling container, where a drag starting on the field scrolls it. */
private val scrollableFieldProperties =
    nativeFieldProperties(UIKitInteropInteractionMode.Cooperative())

/**
 * A single line UITextField, with no decoration of its own, for VoiceOver users - see
 * [usesNativeTextFields].
 *
 * Native accessibility resolution means that we own what VoiceOver says rather than Compose:
 * [label] and [hint] are spoken, with VoiceOver adding its own "Double tap to edit" after
 * [hint], and [identifier] stands in for a testTag. [placeholder] is what's drawn in the field while it's empty.
 *
 * [onReturn] is called for the keyboard's Return key, drawn as [returnKeyType], and returns
 * whether to put the keyboard away - Compose's keyboard controller knows nothing about this
 * field.
 */
@Composable
internal fun NativeTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    textStyle: TextStyle,
    modifier: Modifier = Modifier,
    hint: String? = null,
    placeholder: String? = null,
    identifier: String? = null,
    returnKeyType: UIReturnKeyType = UIReturnKeyType.UIReturnKeyDone,
    onReturn: (String) -> Boolean = { true },
    focusOnAppear: Boolean = false,
    inScrollingContainer: Boolean = false,
) {
    val colors = MaterialTheme.colorScheme
    val textColor = colors.onSurface.toUIColor()
    val tintColor = colors.primary.toUIColor()
    // A Compose text size is in sp, which is points scaled by the font scale
    val fontSize = textStyle.fontSize.value * LocalDensity.current.fontScale

    // The field and its delegate are created once while the callbacks are fresh closures on
    // every recomposition, so they're read through these stable State instances.
    val currentOnValueChange = rememberUpdatedState(onValueChange)
    val currentOnReturn = rememberUpdatedState(onReturn)

    // UITextField only holds its delegate weakly, so it has to be remembered here
    val delegate = remember { ReturnKeyDelegate(currentOnReturn) }
    val field = remember { createTextField(delegate) }

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

    if (focusOnAppear) {
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
    }

    UIKitView(
        factory = { field },
        // An interop view has no intrinsic size for its container to wrap
        modifier = modifier.height(spacing.targetSize),
        update = { view ->
            // Guarded writes: setting the text moves the cursor to the end, and re-setting
            // what's spoken on the element VoiceOver is focused on makes it stutter.
            if (view.text.orEmpty() != value) view.text = value
            if (view.placeholder != placeholder) view.placeholder = placeholder
            if (view.accessibilityLabel() != label) view.setAccessibilityLabel(label)
            if (view.accessibilityHint() != hint) view.setAccessibilityHint(hint)
            // UITextField adopts UIAccessibilityIdentification, but the Kotlin/Native bindings
            // only expose accessibilityIdentifier on the protocol - see
            // StartsSpeechControl.ios.kt
            (view as? UIAccessibilityIdentificationProtocol)?.let {
                if (it.accessibilityIdentifier() != identifier) {
                    it.setAccessibilityIdentifier(identifier)
                }
            }
            view.returnKeyType = returnKeyType
            view.textColor = textColor
            view.tintColor = tintColor
            view.font = UIFont.systemFontOfSize(fontSize.toDouble())
        },
        onRelease = { view -> view.resignFirstResponder() },
        properties = if (inScrollingContainer) scrollableFieldProperties else fixedFieldProperties,
    )
}

@OptIn(ExperimentalForeignApi::class)
private fun createTextField(delegate: UITextFieldDelegateProtocol): UITextField =
    UITextField(frame = CGRectZero.readValue()).apply {
        backgroundColor = UIColor.clearColor
        borderStyle = UITextBorderStyle.UITextBorderStyleNone
        this.delegate = delegate
    }

private class ReturnKeyDelegate(
    private val onReturn: State<(String) -> Boolean>,
) : NSObject(), UITextFieldDelegateProtocol {
    override fun textFieldShouldReturn(textField: UITextField): Boolean {
        if (onReturn.value(textField.text.orEmpty())) textField.resignFirstResponder()
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
