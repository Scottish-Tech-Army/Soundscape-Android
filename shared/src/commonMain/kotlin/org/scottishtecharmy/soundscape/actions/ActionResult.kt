package org.scottishtecharmy.soundscape.actions

import org.scottishtecharmy.soundscape.intents.IncomingIntent

/**
 * The outcome of a [SoundscapeAction], in a form an assistant can report back.
 *
 * Every branch that isn't success carries non-null [speech]: a command that fails
 * silently is the worst outcome for this app's users, who may have no screen to
 * check. Success is the one case allowed to say nothing, because the app's own
 * spatial callout is a better answer than any confirmation could be.
 */
sealed class ActionResult {

    /**
     * Executed. [speech] is a short confirmation for the assistant to speak, or null
     * when the app's own spatial audio *is* the response — a callout says far more
     * than "OK" could, and its positioning carries information plain speech can't.
     */
    data class Ok(val speech: String? = null) : ActionResult()

    /**
     * Can't be done headlessly. The platform publishes [intent] into the existing
     * IncomingIntent pipeline and foregrounds the app.
     *
     * Unused by the current action set — every one of those runs against the
     * service — but the escalation path is defined here so the first action that
     * needs UI doesn't have to reshape the result type to get it.
     */
    data class NeedsUi(val intent: IncomingIntent, val speech: String? = null) : ActionResult()

    /** The service isn't in a state to carry the action out. */
    data class NotReady(val reason: Reason, val speech: String) : ActionResult()

    /**
     * [query] named nothing the user has saved. Distinct from [NotReady] with
     * NO_ROUTES_SAVED / NO_MARKERS_SAVED: this one means "you have some, but not
     * that one", which is usually a mis-hearing worth reporting back verbatim.
     */
    data class NotFound(val query: String, val speech: String) : ActionResult()

    enum class Reason {
        NO_LOCATION_FIX,
        NO_MAP_DATA,
        NO_ROUTE_ACTIVE,
        NO_ROUTES_SAVED,
        NO_MARKERS_SAVED,
    }
}
