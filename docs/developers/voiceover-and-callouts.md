---
title: VoiceOver and callout audio
layout: page
parent: Information for developers
has_toc: false
---

# VoiceOver and callout audio on iOS

Soundscape's callouts and a screen reader both want to talk, and on iOS they used
to talk at the same time. This document explains why, how the app stops it, and
which Compose Multiplatform internals the fix leans on — because those are what
will need re-checking the next time CMP is upgraded.

## The collision

Double-tapping a callout button — My Location, say — made VoiceOver speak the
button's label and play its activation click over the top of the callout that the
same tap had just started.

Nothing in the system serialises the two, because the app's speech is not on the
system speech channel. `TtsRenderer`
(`shared/src/iosMain/kotlin/org/scottishtecharmy/soundscape/audio/TtsRenderer.kt`)
builds `AVSpeechUtterance`s but renders them to PCM buffers with
`synthesizer.write()` and plays them through the app's own `AVAudioEngine` graph,
so that HRTF spatialisation can be applied. To iOS it is ordinary app audio, and
VoiceOver treats it accordingly.

Android has no equivalent problem: TalkBack stops speaking as soon as the app
speaks.

## The fix: two accessibility traits

iOS has traits for exactly this, and the app sets both on the affected controls:

* `UIAccessibilityTraitStartsMediaSession` — the element starts a media session
  that should not be interrupted, so VoiceOver silences its speech output when
  the element is activated. This is what stops the label being read over the
  callout.
* `UIAccessibilityTraitPlaysSound` — the element makes its own sound when
  activated. Apple documents it for silencing assistive-technology audio during
  an action that produces sound, and it is aimed at the activation click rather
  than the speech. It was added after the media-session trait alone left a click
  that ducked the start of the callout.

Both are verified working on a device.

What the app *cannot* control: VoiceOver's **Audio Ducking** and its sound-effect
volume are user settings under Settings → Accessibility → VoiceOver → Audio.
There is no API for either. If a residual duck ever needs addressing, the option
considered but not built is to delay the first audio of a callout by ~200 ms
while VoiceOver is running — note that for My Location the first sound is the
mode-enter earcon in `CalloutController.myLocation()`, not the speech, so a delay
has to sit ahead of that.

## Why this needs UIKit interop

Compose Multiplatform derives iOS accessibility traits itself, from semantics,
and offers no hook for adding arbitrary ones:

* `SemanticsConfiguration.accessibilityTraits()`
  (`compose ui` iOS source, `platform/accessibility/SemanticConfigurationUtils.ios.kt`)
  maps only LiveRegion, Disabled, Selected, Heading, ToggleableState,
  ProgressBarRangeInfo, EditableText, OnClick and Role onto traits. Neither trait
  above has a semantics property that would produce it.
* `AccessibilityElement.accessibilityTraits()` (`platform/Accessibility.ios.kt`)
  is an override computed from the semantics node, so a trait assigned to the
  element from outside cannot stick. The same override is why the iOS 17
  `setAccessibilityTraitsBlock` is no use here — UIKit never consults the block
  when the getter has been overridden.

So the traits have to live on a native view. `StartsSpeechControl` puts a
transparent `UIView` over the control and lets VoiceOver traverse that instead of
the Compose element.

## How StartsSpeechControl works

* `shared/src/commonMain/.../screens/StartsSpeechControl.kt` — `expect` wrapper
  taking the control as a content slot that receives a `Modifier`.
* `shared/src/androidMain/.../screens/StartsSpeechControl.android.kt` — a plain
  centred `Box`. The `Box` is kept on Android deliberately, so both platforms
  have the same layout tree and a sizing regression shows up in Android previews
  and the Maestro run rather than only on a device.
* `shared/src/iosMain/.../screens/StartsSpeechControl.ios.kt` — a `Box`
  containing the control, hidden from accessibility with `talkbackHidden()`, plus
  a sibling `UIKitView` sized with `matchParentSize()` whose `UIView` carries the
  traits, the label, the hint and the identifier.

Two details of the iOS side worth knowing:

* **Activation** goes through `setAccessibilityActivateBlock`.
  `accessibilityActivate()` is exposed by the Kotlin/Native bindings as a *final*
  extension function on `NSObject`, so it cannot be overridden from a Kotlin
  `UIView` subclass. The block-based API can be set on any plain `UIView`, which
  is why no subclass and no Swift bridge is involved. The block reads the current
  callback through a `rememberUpdatedState`, because it is installed once while
  the Compose lambda is recreated on every recomposition.
* **The proxy only exists while VoiceOver is running**, tracked reactively via
  `UIAccessibilityVoiceOverStatusDidChangeNotification` — VoiceOver can be turned
  on mid-session from Control Centre. The one-shot `isScreenReaderEnabled()` in
  `ScreenReaderDetection.ios.kt` is not suitable for this and is left alone.

There is a `USE_NATIVE_A11Y_PROXY` constant at the top of the iOS file. Setting
it to `false` returns iOS behaviour to stock Compose in one line.

## Four CMP behaviours the proxy depends on

These are the load-bearing details. All four were read out of the Compose
Multiplatform 1.11.1 iOS sources, none of them are public API, and each one fails
in a way that is easy to misdiagnose.

1. **`interactionMode` must be non-null.**
   `InteropWrappingView.updateAccessibilityElements()` publishes the interop
   subviews to accessibility only while `interactionMode != null`; with `null` it
   publishes an empty list. The obvious spelling for a non-interactive overlay
   therefore makes the proxy invisible to VoiceOver, and the change appears to do
   nothing at all.
2. **`userInteractionEnabled` must be cleared on the wrapping view.** Touch
   handling reads that flag, while accessibility exposure reads
   `interactionMode`, so this is how the proxy stays out of the touch path. Leave
   the wrapper interactive and `TouchesGestureRecognizer` records a hit-test
   result, then cancels the touch sequence when a tap ends — meaning the Compose
   control underneath never fires for sighted users. CMP attaches the proxy to
   its wrapper at a frame boundary, after composition, so the flag is cleared
   from an effect that waits for the superview to appear.
3. **The proxy is a sibling of the control, not a child.** Inside a merged
   subtree — a Material `Button`, say — the button's element would be both a
   focusable element and a container for the interop view, which
   `Accessibility.ios.kt` states is not possible. A non-merging `Box` holding the
   control and the proxy side by side avoids the question entirely.
4. **`placedAsOverlay = true`.** Otherwise CMP punches a transparent cut-out
   through the Metal canvas for the interop view and the Compose control
   underneath disappears.

## What the proxy owns, and what it drops

Native accessibility resolution replaces the Compose element wholesale, so the
proxy has to supply everything VoiceOver reads. It carries the label, the hint
and the identifier, and nothing else.

The hint is phrased by `activationHint()` in `TalkbackHelpers.ios.kt`, shared
with `Modifier.talkbackHint`. Callers pass a bare fragment ("hear about your
current location") and iOS turns it into "Double tap to hear about your current
location", because VoiceOver reads a hint verbatim while TalkBack composes that
phrasing itself from the control's own `onClick` label.

What is lost, and therefore which controls should **not** be wrapped as things
stand:

* **Custom accessibility actions** — `startPlayback` on `LocationItem` is a
  `CustomAccessibilityAction`, not a button, and would vanish. It would need
  `UIAccessibilityCustomAction`s rebuilt on the proxy.
* **Selection and toggle state** — `AudioBeaconItem`'s `selectable` row would
  lose its selected state. It would need `accessibilityValue` and
  `UIAccessibilityTraitSelected` plumbed through first.
* **Adjustable controls** — the speaking-rate slider announces a value on every
  change; the trait model does not fit.
* Anything from the preference library, such as the beacon-style
  `ListPreferenceItem`, where there is no modifier seam to wrap.

Controls that only *stop* audio — Stop, Mute, Stop route — have nothing to talk
over and are not worth wrapping.

## Where it is applied

* The four home bottom-bar callout buttons (`SharedHomeBottomAppBar.kt`).
* `IconWithTextButton` has an opt-in `startsSpeech` parameter, used by Start
  beacon and Street Preview (`SharedLocationDetailsScreen.kt`) and Start route
  and Start route reversed (`SharedRouteDetailsScreen.kt`).

Remaining candidates, not yet done: the Go button in `StreetPreview.kt`, route
Skip previous/next in `SharedHomeContent.kt`, and the "play example" button in
the onboarding hearing screen.

Note that a wrapped control on `IconWithTextButton` should pass its test tag as
`buttonTestTag` rather than via `modifier`: the tag carries over to the proxy as
its `accessibilityIdentifier`, whereas a tag on the modifier lands on the Compose
node that iOS hides.

Each wrapped control is a live interop view while VoiceOver runs. A handful per
screen is fine; a *list* of them — a beacon per row in Places Nearby, say — is
the point at which they should be collapsed into a single `UIKitView` hosting one
child view per row.

## Testing a change to any of this

Use a real device. Simulator VoiceOver is not trustworthy for this, and the
interesting failures are all behavioural.

Xcode's Accessibility Inspector is the quickest structural check: over the bottom
bar you should see exactly four elements, each with its label, its "Double tap
to ..." hint, its identifier and traits including Starts Media Session and Plays
Sound. The Inspector's Activate action should start a callout.

Then, on a device with VoiceOver on:

* Double-tap a wrapped control: the callout plays with no label spoken over it.
* Swipe to the next element afterwards: VoiceOver announces normally, i.e. it
  recovered from the silencing.
* Touch a wrapped control directly rather than swiping to it: it should take
  focus. This exercises a different path — the accessibility hit-test — through
  the wrapping view that touch handling is deliberately kept out of.
* Swipe across a screen's controls: the focus order and element count are
  unchanged, with no duplicate stops.

With VoiceOver off, tap the controls and check the ripple, press state and any
active-state animation are exactly as before; that is the assumption behaviour 2
above protects. Finally run the Android Maestro flows, which tap several of these
controls by test tag.
