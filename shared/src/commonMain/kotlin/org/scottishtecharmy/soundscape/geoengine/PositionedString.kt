package org.scottishtecharmy.soundscape.geoengine

import org.scottishtecharmy.soundscape.audio.AudioType
import org.scottishtecharmy.soundscape.geojsonparser.geojson.LngLatAlt

/**
 * @param dedupText overrides [text] for callout-history comparison purposes only (see
 * TrackedCallout.equals) - for callouts whose spoken text embeds a value that changes on every
 * update (e.g. a live "distance since X"), using the full text for dedup would mean the callout
 * never repeats exactly and so never gets suppressed as a duplicate. Passing a stable dedupText
 * (the same text with the ever-changing part left out) lets it dedup correctly while the spoken
 * text keeps the live value.
 */
data class PositionedString(
    val text: String,
    val location: LngLatAlt? = null,
    val earcon: String? = null,
    val type: AudioType = AudioType.STANDARD,
    val heading: Double? = null,
    val addDistanceAndHeading: Boolean = false,
    val dedupText: String? = null
)
