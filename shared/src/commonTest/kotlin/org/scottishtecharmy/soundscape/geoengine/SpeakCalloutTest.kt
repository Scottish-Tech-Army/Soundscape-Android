package org.scottishtecharmy.soundscape.geoengine

import org.scottishtecharmy.soundscape.audio.AudioEngine
import org.scottishtecharmy.soundscape.audio.AudioType
import org.scottishtecharmy.soundscape.audio.EARCON_MODE_ENTER
import org.scottishtecharmy.soundscape.audio.EARCON_MODE_EXIT
import org.scottishtecharmy.soundscape.geoengine.filters.CalloutHistory
import org.scottishtecharmy.soundscape.geoengine.filters.LocationUpdateFilter
import org.scottishtecharmy.soundscape.geoengine.filters.TrackedCallout
import org.scottishtecharmy.soundscape.geoengine.utils.rulers.CheapRuler
import org.scottishtecharmy.soundscape.geojsonparser.geojson.LngLatAlt
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * A minimal, fully in-memory [AudioEngine] fake that records every call so tests can assert on
 * what speakCalloutCommon asked it to play - both what was said/played and in what order and
 * with what parameters. Closely matches the FakeAudioEngine pattern in CalloutControllerTest.kt
 * (same repo), but that one is private to its file, and this file also needs to assert on
 * per-call arguments (type/lat/lon/heading) and interleaving order between earcons and speech,
 * so it carries a couple of extra recording lists.
 */
private class FakeAudioEngine : AudioEngine {
    data class TtsCall(
        val text: String,
        val type: AudioType,
        val latitude: Double,
        val longitude: Double,
        val heading: Double,
    )

    data class EarconCall(
        val asset: String,
        val type: AudioType,
        val latitude: Double,
        val longitude: Double,
        val heading: Double,
    )

    val ttsCalls = mutableListOf<String>()
    val earconCalls = mutableListOf<String>()
    val ttsDetails = mutableListOf<TtsCall>()
    val earconDetails = mutableListOf<EarconCall>()

    // Combined call log, in call order, so tests can assert interleaving between earcons and
    // speech (e.g. "the per-string earcon plays before that string's speech").
    val events = mutableListOf<String>()

    var nextHandle = 1L
    var activeHandle: Long? = null

    override fun createBeacon(location: LngLatAlt, headingOnly: Boolean): Long = 0L
    override fun destroyBeacon(beaconHandle: Long) {}
    override fun toggleBeaconMute(): Boolean = false

    override fun createTextToSpeech(
        text: String,
        type: AudioType,
        latitude: Double,
        longitude: Double,
        heading: Double,
    ): Long {
        ttsCalls.add(text)
        ttsDetails.add(TtsCall(text, type, latitude, longitude, heading))
        events.add("tts:$text")
        return nextHandle++
    }

    override fun createEarcon(
        asset: String,
        type: AudioType,
        latitude: Double,
        longitude: Double,
        heading: Double,
    ): Long {
        earconCalls.add(asset)
        earconDetails.add(EarconCall(asset, type, latitude, longitude, heading))
        events.add("earcon:$asset")
        return nextHandle++
    }

    override fun clearTextToSpeechQueue() {}
    override fun getQueueDepth(): Long = 0L
    override fun isHandleActive(handle: Long): Boolean = activeHandle == handle

    override fun updateGeometry(
        listenerLatitude: Double,
        listenerLongitude: Double,
        listenerHeading: Double?,
        focusGained: Boolean,
        duckingAllowed: Boolean,
        proximityNear: Double,
    ) {}

    override fun setBeaconType(beaconType: String) {}
    override fun getListOfBeaconTypes(): Array<String> = emptyArray()
    override fun setSpeechLanguage(language: String): Boolean = true
    override fun onAllBeaconsCleared() {}
    override fun setHrtfEnabled(enabled: Boolean) {}
}

class SpeakCalloutTest {

    private val ruler = CheapRuler(0.0)

    // ----- null callout -----

    @Test
    fun nullCallout_returnsZeroAndTouchesNothing() {
        val engine = FakeAudioEngine()

        val handle = speakCalloutCommon(
            callout = null,
            addModeEarcon = true,
            audioEngine = engine,
            lastGeometry = null,
            ruler = ruler,
        )

        assertEquals(0L, handle)
        assertTrue(engine.events.isEmpty())
    }

    // ----- addModeEarcon -----

    @Test
    fun addModeEarconTrue_playsEnterAndExitEarconsAroundSpeech() {
        val engine = FakeAudioEngine()
        val callout = TrackedCallout(
            positionedStrings = listOf(PositionedString(text = "hello")),
        )

        speakCalloutCommon(callout, addModeEarcon = true, engine, null, ruler)

        assertEquals(
            listOf("earcon:$EARCON_MODE_ENTER", "tts:hello", "earcon:$EARCON_MODE_EXIT"),
            engine.events,
        )
    }

    @Test
    fun addModeEarconFalse_playsNoModeEarcons() {
        val engine = FakeAudioEngine()
        val callout = TrackedCallout(
            positionedStrings = listOf(PositionedString(text = "hello")),
        )

        speakCalloutCommon(callout, addModeEarcon = false, engine, null, ruler)

        assertTrue(engine.earconCalls.isEmpty())
        assertEquals(listOf("hello"), engine.ttsCalls)
    }

    // ----- multiple positionedStrings -----

    @Test
    fun multiplePositionedStrings_speaksEachInOrder() {
        val engine = FakeAudioEngine()
        val callout = TrackedCallout(
            positionedStrings = listOf(
                PositionedString(text = "first"),
                PositionedString(text = "second"),
                PositionedString(text = "third"),
            ),
        )

        speakCalloutCommon(callout, addModeEarcon = false, engine, null, ruler)

        assertEquals(listOf("first", "second", "third"), engine.ttsCalls)
    }

    @Test
    fun perStringEarcon_playsBeforeThatStringsSpeech_forEachEntry() {
        val engine = FakeAudioEngine()
        val callout = TrackedCallout(
            positionedStrings = listOf(
                PositionedString(
                    text = "poi one",
                    location = LngLatAlt(-4.25, 55.86),
                    earcon = "earcon-one.wav",
                ),
                PositionedString(
                    text = "poi two",
                    location = LngLatAlt(-4.20, 55.90),
                    earcon = "earcon-two.wav",
                ),
            ),
        )

        speakCalloutCommon(callout, addModeEarcon = false, engine, null, ruler)

        assertEquals(
            listOf(
                "earcon:earcon-one.wav", "tts:poi one",
                "earcon:earcon-two.wav", "tts:poi two",
            ),
            engine.events,
        )
    }

    // ----- location-less PositionedString (no `location`) -----

    @Test
    fun positionedStringWithoutLocation_playsEarconThenSpeechAtOriginWithGivenHeading() {
        val engine = FakeAudioEngine()
        val callout = TrackedCallout(
            positionedStrings = listOf(
                PositionedString(text = "ping", earcon = "beep.wav", heading = 45.0),
            ),
        )

        speakCalloutCommon(callout, addModeEarcon = false, engine, null, ruler)

        assertEquals(
            FakeAudioEngine.EarconCall("beep.wav", AudioType.STANDARD, 0.0, 0.0, 45.0),
            engine.earconDetails.single(),
        )
        assertEquals(
            FakeAudioEngine.TtsCall("ping", AudioType.STANDARD, 0.0, 0.0, 45.0),
            engine.ttsDetails.single(),
        )
    }

    @Test
    fun positionedStringWithoutLocation_defaultsHeadingToZeroWhenNotSet() {
        val engine = FakeAudioEngine()
        val callout = TrackedCallout(
            positionedStrings = listOf(PositionedString(text = "no heading")),
        )

        speakCalloutCommon(callout, addModeEarcon = false, engine, null, ruler)

        assertEquals(0.0, engine.ttsDetails.single().heading)
    }

    @Test
    fun positionedStringWithoutLocation_coercesLocalizedTypeToStandard() {
        val engine = FakeAudioEngine()
        val callout = TrackedCallout(
            positionedStrings = listOf(
                PositionedString(
                    text = "local",
                    type = AudioType.LOCALIZED,
                    earcon = "e.wav",
                ),
            ),
        )

        speakCalloutCommon(callout, addModeEarcon = false, engine, null, ruler)

        assertEquals(AudioType.STANDARD, engine.earconDetails.single().type)
        assertEquals(AudioType.STANDARD, engine.ttsDetails.single().type)
    }

    // ----- PositionedString with a location -----

    @Test
    fun positionedStringWithLocation_passesLocationAndTypeThrough() {
        val engine = FakeAudioEngine()
        val location = LngLatAlt(-4.25, 55.86)
        val callout = TrackedCallout(
            positionedStrings = listOf(
                PositionedString(
                    text = "poi",
                    location = location,
                    type = AudioType.LOCALIZED,
                    heading = 10.0,
                ),
            ),
        )

        speakCalloutCommon(callout, addModeEarcon = false, engine, null, ruler)

        // Unlike the no-location branch, a location is present here so the type is *not*
        // coerced away from LOCALIZED - only the location-less branch does that coercion.
        // Note the argument order: createTextToSpeech takes (latitude, longitude), while
        // LngLatAlt's constructor takes (longitude, latitude) - so these are swapped relative
        // to how `location` was constructed above.
        assertEquals(
            FakeAudioEngine.TtsCall("poi", AudioType.LOCALIZED, 55.86, -4.25, 10.0),
            engine.ttsDetails.single(),
        )
    }

    @Test
    fun positionedStringWithLocationAndNoEarcon_doesNotCallCreateEarcon() {
        val engine = FakeAudioEngine()
        val callout = TrackedCallout(
            positionedStrings = listOf(
                PositionedString(text = "poi", location = LngLatAlt(-4.25, 55.86)),
            ),
        )

        speakCalloutCommon(callout, addModeEarcon = false, engine, null, ruler)

        assertTrue(engine.earconCalls.isEmpty())
        assertEquals(listOf("poi"), engine.ttsCalls)
    }

    // ----- addDistanceAndHeading / lastGeometry interaction -----

    @Test
    fun addDistanceAndHeading_withNullLastGeometry_fallsBackToPlainText() {
        val engine = FakeAudioEngine()
        val callout = TrackedCallout(
            positionedStrings = listOf(
                PositionedString(
                    text = "Cafe, 20 metres",
                    location = LngLatAlt(-4.25, 55.86),
                    addDistanceAndHeading = true,
                ),
            ),
        )

        speakCalloutCommon(callout, addModeEarcon = false, engine, lastGeometry = null, ruler = ruler)

        assertEquals(listOf("Cafe, 20 metres"), engine.ttsCalls)
    }

    @Test
    fun addDistanceAndHeadingFalse_withLastGeometryPresent_stillUsesPlainText() {
        val engine = FakeAudioEngine()
        val callout = TrackedCallout(
            positionedStrings = listOf(
                PositionedString(
                    text = "Plain text",
                    location = LngLatAlt(-4.25, 55.86),
                    addDistanceAndHeading = false,
                ),
            ),
        )
        val geometry = UserGeometry(
            location = LngLatAlt(-4.24, 55.86),
            phoneHeading = 90.0,
            timestampMilliseconds = 1000L,
        )

        speakCalloutCommon(callout, addModeEarcon = false, engine, lastGeometry = geometry, ruler = ruler)

        assertEquals(listOf("Plain text"), engine.ttsCalls)
    }

    // ----- returned handle -----

    @Test
    fun returnedHandle_isLastPositionedStringsSpeechHandle_regardlessOfTrailingExitEarcon() {
        val engine = FakeAudioEngine()
        val callout = TrackedCallout(
            positionedStrings = listOf(
                PositionedString(text = "first"),
                PositionedString(text = "second"),
            ),
        )

        // Sequence of engine calls: enter earcon(1), tts "first"(2), tts "second"(3), exit
        // earcon(4). The exit earcon's handle is discarded by speakCalloutCommon, so the
        // returned handle should be 3 (the last *TTS* item), not 4.
        val handle = speakCalloutCommon(callout, addModeEarcon = true, engine, null, ruler)

        assertEquals(3L, handle)
        assertEquals(4, engine.events.size)
    }

    @Test
    fun returnedHandle_withEarconOnLastString_isThatStringsTtsHandleNotItsEarconHandle() {
        val engine = FakeAudioEngine()
        val callout = TrackedCallout(
            positionedStrings = listOf(
                PositionedString(text = "first"),
                PositionedString(text = "second", earcon = "e.wav", location = LngLatAlt(1.0, 2.0)),
            ),
        )

        // Sequence: tts "first"(1), earcon "e.wav"(2), tts "second"(3).
        val handle = speakCalloutCommon(callout, addModeEarcon = false, engine, null, ruler)

        assertEquals(3L, handle)
    }

    @Test
    fun returnedHandle_withNoPositionedStringsAndAddModeEarconTrue_isEnterEarconHandle() {
        // Documents a subtlety in speakCalloutCommon: with an empty callout the loop body never
        // runs, so `lastHandle` is left at whatever the *enter* earcon returned (queued first).
        // The exit earcon is still played afterwards, but its handle is never assigned back into
        // `lastHandle`, so it is not reflected in the return value even though it did get queued.
        val engine = FakeAudioEngine()
        val callout = TrackedCallout(positionedStrings = emptyList())

        val handle = speakCalloutCommon(callout, addModeEarcon = true, engine, null, ruler)

        assertEquals(listOf(EARCON_MODE_ENTER, EARCON_MODE_EXIT), engine.earconCalls)
        assertEquals(1L, handle)
    }

    @Test
    fun returnedHandle_withNoPositionedStringsAndAddModeEarconFalse_isZero() {
        val engine = FakeAudioEngine()
        val callout = TrackedCallout(positionedStrings = emptyList())

        val handle = speakCalloutCommon(callout, addModeEarcon = false, engine, null, ruler)

        assertEquals(0L, handle)
        assertTrue(engine.events.isEmpty())
    }

    // ----- calloutHistory / locationFilter side effects -----

    @Test
    fun callout_isAddedToCalloutHistory_afterSpeaking() {
        val engine = FakeAudioEngine()
        val history = CalloutHistory()
        val callout = TrackedCallout(
            positionedStrings = listOf(PositionedString(text = "hello")),
            calloutHistory = history,
        )

        assertEquals(0, history.size())

        speakCalloutCommon(callout, addModeEarcon = false, engine, null, ruler)

        assertEquals(1, history.size())
        assertTrue(history.find(callout))
    }

    @Test
    fun locationFilter_isUpdatedWithCalloutsUserGeometry_afterSpeaking() {
        val engine = FakeAudioEngine()
        val filter = LocationUpdateFilter(minTimeMilliseconds = 1000L, minDistance = 100.0)
        val userGeometry = UserGeometry(
            location = LngLatAlt(-4.25, 55.86),
            timestampMilliseconds = 1000L,
        )
        val callout = TrackedCallout(
            userGeometry = userGeometry,
            positionedStrings = listOf(PositionedString(text = "hello")),
            locationFilter = filter,
        )

        // Nothing recorded yet, so the filter says "yes, update".
        assertTrue(filter.shouldUpdate(userGeometry))

        speakCalloutCommon(callout, addModeEarcon = false, engine, null, ruler)

        // Now that the filter has been updated with the same time/location, an immediate
        // "should I update again" check for that same geometry says no.
        assertFalse(filter.shouldUpdate(userGeometry))
    }

    @Test
    fun locationFilter_withNullCalloutUserGeometry_isNotUpdated() {
        // TrackedCallout()'s default userGeometry is null; LocationUpdateFilter.update() is a
        // no-op when passed null, so a callout with no userGeometry attached leaves the filter
        // untouched even though calloutHistory still records the callout.
        val engine = FakeAudioEngine()
        val filter = LocationUpdateFilter(minTimeMilliseconds = 1000L, minDistance = 100.0)
        val someGeometry = UserGeometry(location = LngLatAlt(1.0, 1.0), timestampMilliseconds = 1000L)
        val callout = TrackedCallout(
            positionedStrings = listOf(PositionedString(text = "hello")),
            locationFilter = filter,
        )

        speakCalloutCommon(callout, addModeEarcon = false, engine, null, ruler)

        // Filter never saw any geometry, so it still reports "should update".
        assertTrue(filter.shouldUpdate(someGeometry))
    }
}
