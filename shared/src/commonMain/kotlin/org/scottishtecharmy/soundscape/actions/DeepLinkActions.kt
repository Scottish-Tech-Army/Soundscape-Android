package org.scottishtecharmy.soundscape.actions

import org.scottishtecharmy.soundscape.services.mediacontrol.MediaControllableService

/**
 * How long an action from a deep link may wait for a position fix and map tiles. The link
 * may be what launched the app, so like an assistant's command it is given a few seconds
 * rather than failing at once - see [SoundscapeActionExecutor.execute].
 */
const val DEEP_LINK_READY_TIMEOUT_MS = 5_000L

/**
 * Runs an action asked for by a soundscape:// link, and speaks the result when the user
 * would otherwise be left with silence.
 *
 * The executor never speaks, because an assistant voices its results. A link has no
 * assistant behind it, so the app has to say why nothing happened itself - see
 * [deepLinkSpeech] for what is said.
 */
suspend fun SoundscapeActionExecutor.executeForDeepLink(
    action: SoundscapeAction,
    service: MediaControllableService?,
) {
    val result = execute(action, DEEP_LINK_READY_TIMEOUT_MS)
    val speech = deepLinkSpeech(action, result) ?: return
    if (service?.requestAudioFocus() == true) {
        service.speak2dText(speech, true)
    }
}

/**
 * What to say after [action] ran with [result], or null to say nothing.
 *
 * Every failure is spoken: a link that silently does nothing is the worst outcome for
 * users who may have no screen to check. A success is spoken only for Stop Beacon, the
 * one action with no audio of its own - a callout, a route or a beacon starting is its
 * own answer, and a confirmation on top would talk over it.
 */
fun deepLinkSpeech(action: SoundscapeAction, result: ActionResult): String? = when (result) {
    is ActionResult.Ok -> result.speech.takeIf { action == SoundscapeAction.StopBeacon }
    is ActionResult.NeedsUi -> result.speech
    is ActionResult.NotReady -> result.speech
    is ActionResult.NotFound -> result.speech
}
