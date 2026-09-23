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
    val level = cycleCalloutDetail() ?: return
    speak2dText(calloutDetailName(ComposeLocalizedStrings(), level), true)
}
