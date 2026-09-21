package org.scottishtecharmy.soundscape.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.UIKitInteropInteractionMode
import androidx.compose.ui.viewinterop.UIKitInteropProperties
import androidx.compose.ui.viewinterop.UIKitView
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.readValue
import platform.CoreGraphics.CGRectZero
import platform.Foundation.NSNotificationCenter
import platform.Foundation.NSOperationQueue
import platform.UIKit.UIAccessibilityIdentificationProtocol
import platform.UIKit.UIAccessibilityIsVoiceOverRunning
import platform.UIKit.UIAccessibilityTraitButton
import platform.UIKit.UIAccessibilityTraitPlaysSound
import platform.UIKit.UIAccessibilityTraitStartsMediaSession
import platform.UIKit.UIAccessibilityVoiceOverStatusDidChangeNotification
import platform.UIKit.UIColor
import platform.UIKit.UIView
import platform.UIKit.accessibilityHint
import platform.UIKit.accessibilityLabel
import platform.UIKit.accessibilityTraits
import platform.UIKit.setAccessibilityActivateBlock
import platform.UIKit.setAccessibilityHint
import platform.UIKit.setAccessibilityLabel
import platform.UIKit.setAccessibilityTraits
import platform.UIKit.setIsAccessibilityElement

/**
 * Master switch for the native accessibility proxy. This is the app's only hand-rolled
 * Compose/UIKit interop and it leans on Compose Multiplatform 1.11.1 internals (see the
 * comments on [proxyProperties] and [attachedToNonInteractiveWrapper]), so keep a one-line
 * route back to stock Compose behaviour for when CMP is next upgraded.
 */
private const val USE_NATIVE_A11Y_PROXY = true

/** Frames to wait for CMP to splice the proxy into its interop wrapping view. */
private const val WRAPPER_ATTACH_FRAME_BUDGET = 30

/**
 * `UIAccessibilityTraitStartsMediaSession` is the point of the whole exercise: it tells
 * VoiceOver to go quiet for the media session this control's activation starts, instead of
 * speaking the label over the top of the callout.
 *
 * `UIAccessibilityTraitPlaysSound` says the element makes its own sound when activated, which
 * is the trait Apple documents for silencing assistive-technology audio during an action that
 * produces sound - aimed at VoiceOver's own activation click rather than its speech.
 *
 * `UIAccessibilityTraitButton` restores what Compose would otherwise have derived from the
 * control's onClick semantics.
 */
private val startsSpeechTraits: ULong =
    UIAccessibilityTraitButton or
        UIAccessibilityTraitStartsMediaSession or
        UIAccessibilityTraitPlaysSound

@OptIn(ExperimentalComposeUiApi::class)
private val proxyProperties = UIKitInteropProperties(
    // MUST be non-null. InteropWrappingView.updateAccessibilityElements() publishes the
    // interop subviews to accessibility only while interactionMode != null - with null it
    // publishes an empty list, the proxy is invisible to VoiceOver, and this all silently
    // does nothing. Touches are kept off the proxy by clearing userInteractionEnabled on the
    // wrapping view instead: see attachedToNonInteractiveWrapper().
    interactionMode = UIKitInteropInteractionMode.Cooperative(),
    // Makes VoiceOver traverse the proxy view rather than parsing Compose semantics for it.
    isNativeAccessibilityEnabled = true,
    // Composites the proxy above the Metal canvas. With false, CMP punches a transparent
    // cut-out through the canvas and the Compose control underneath disappears.
    placedAsOverlay = true,
)

@Composable
actual fun StartsSpeechControl(
    label: String,
    hint: String,
    identifier: String?,
    onActivate: () -> Unit,
    modifier: Modifier,
    content: @Composable (Modifier) -> Unit,
) {
    val proxied = USE_NATIVE_A11Y_PROXY && rememberVoiceOverRunning()

    // Centred: the wrapper takes the caller's sizing, so a control shorter than its
    // touch target would otherwise be top-aligned inside it.
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        // The Compose element is hidden only while the proxy is genuinely there, so no
        // failure mode can leave the control unreachable for VoiceOver.
        content(
            if (proxied) {
                Modifier.fillMaxSize().talkbackHidden()
            } else {
                Modifier.fillMaxSize()
            },
        )

        if (proxied) {
            // The activate block must not capture onActivate: that is a fresh closure on
            // every recomposition while the block is installed once, so it would pin the
            // first frame's callbacks. The State instance is stable, so read through it.
            val currentOnActivate = rememberUpdatedState(onActivate)
            val proxy = remember { createProxyView(currentOnActivate) }

            // VoiceOver reads a hint verbatim, so the "Double tap to ..." phrasing TalkBack
            // composes for itself has to be supplied here - exactly as talkbackHint does for
            // hints that go through Compose semantics.
            val spokenHint = activationHint(hint)

            attachedToNonInteractiveWrapper(proxy)

            UIKitView(
                factory = { proxy },
                modifier = Modifier.matchParentSize(),
                update = { view -> view.configure(label, spokenHint, identifier) },
                properties = proxyProperties,
            )
        }
    }
}

@OptIn(ExperimentalForeignApi::class)
private fun createProxyView(onActivate: State<() -> Unit>): UIView =
    UIView(frame = CGRectZero.readValue()).apply {
        backgroundColor = UIColor.clearColor
        setOpaque(false)
        setIsAccessibilityElement(true)
        setAccessibilityTraits(startsSpeechTraits)
        // accessibilityActivate() is a final extension on NSObject in the Kotlin/Native
        // bindings and cannot be overridden, but the block-based hook can be installed.
        // Returning true tells VoiceOver the activation was handled, so it doesn't also
        // synthesise a tap.
        setAccessibilityActivateBlock {
            onActivate.value.invoke()
            true
        }
    }

/**
 * Writes the screen-reader facing properties, which native accessibility resolution means we
 * now own rather than Compose: the label it would have merged from the control's text, the
 * hint it would have taken from the onClick action's label, and the identifier it would have
 * taken from the testTag.
 *
 * Guarded writes - re-setting these on the element VoiceOver is focused on makes it stutter.
 */
private fun UIView.configure(label: String, hint: String, identifier: String?) {
    if (accessibilityLabel() != label) setAccessibilityLabel(label)
    if (accessibilityHint() != hint) setAccessibilityHint(hint)
    // UIView adopts UIAccessibilityIdentification, but the Kotlin/Native bindings only
    // expose accessibilityIdentifier on the protocol, not on UIView. Safe-cast rather than
    // hard-cast: if a future binding drops the conformance we lose the automation identifier
    // - which nothing uses on iOS yet - rather than crashing the app.
    (this as? UIAccessibilityIdentificationProtocol)?.let {
        if (it.accessibilityIdentifier() != identifier) it.setAccessibilityIdentifier(identifier)
    }
    if (accessibilityTraits() != startsSpeechTraits) setAccessibilityTraits(startsSpeechTraits)
}

/**
 * Clears userInteractionEnabled on the interop wrapping view CMP puts the proxy inside.
 *
 * Accessibility exposure and touch handling read different flags: the former reads
 * InteropWrappingView.interactionMode (which must stay non-null), the latter reads
 * userInteractionEnabled. Leave the wrapper interactive and UIKit hit-testing finds it, so
 * TouchesGestureRecognizer records a hit-test result and cancels the touch sequence when the
 * tap ends - meaning the Compose control underneath never fires at all. Cleared, hit-testing
 * skips the interop branch entirely and presses, ripple, long-press and drag-off-to-cancel
 * behave exactly as they did before the proxy existed.
 *
 * CMP attaches the proxy to its wrapper at a frame boundary, after composition, so the
 * superview isn't there to configure when the factory or the first update runs.
 */
@Composable
private fun attachedToNonInteractiveWrapper(proxy: UIView) {
    LaunchedEffect(proxy) {
        var frames = 0
        while (proxy.superview == null && frames < WRAPPER_ATTACH_FRAME_BUDGET) {
            withFrameNanos { }
            frames++
        }
        proxy.superview?.setUserInteractionEnabled(false)
    }
}

/**
 * Whether VoiceOver is running, kept current for as long as this is composed.
 *
 * Reactive on purpose: VoiceOver can be turned on mid-session from Control Centre or by a
 * triple-click, and the proxy is only created while it is running - no point holding interop
 * views alive for users who will never focus them. This is why the one-shot
 * [org.scottishtecharmy.soundscape.screens.onboarding.accessibility.isScreenReaderEnabled]
 * isn't used here.
 */
@Composable
private fun rememberVoiceOverRunning(): Boolean {
    var running by remember { mutableStateOf(UIAccessibilityIsVoiceOverRunning()) }

    DisposableEffect(Unit) {
        val center = NSNotificationCenter.defaultCenter
        val observer = center.addObserverForName(
            name = UIAccessibilityVoiceOverStatusDidChangeNotification,
            `object` = null,
            queue = NSOperationQueue.mainQueue,
        ) {
            running = UIAccessibilityIsVoiceOverRunning()
        }
        onDispose { center.removeObserver(observer) }
    }

    return running
}
