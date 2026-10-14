package org.scottishtecharmy.soundscape.services.mediacontrol

import org.scottishtecharmy.soundscape.actions.calloutDetailName
import org.scottishtecharmy.soundscape.i18n.ComposeLocalizedStrings

/**
 * The headphone button that makes Soundscape quieter: each press steps down a level and says
 * which one it has reached, since a change of setting makes no sound of its own. Silent wraps
 * back to the most detailed, so the button is never a dead end.
 *
 * Just the level's name - "Quiet" - rather than the sentence Siri and Gemini get back. Pressing
 * the button is already the question, so naming the setting again each press is two words in the
 * way of the answer.
 */
fun MediaControllableService.cycleCalloutDetailAndSay() {
    // The same hold-off the audio menu takes before it speaks: without it an auto callout can
    // arrive on top of the level being named, which is the one press somebody reaches for in a
    // hurry and the one answer they need to hear.
    callbackHoldOff()
    val level = cycleCalloutDetail() ?: return
    speak2dText(calloutDetailName(ComposeLocalizedStrings(), level), true)
}
