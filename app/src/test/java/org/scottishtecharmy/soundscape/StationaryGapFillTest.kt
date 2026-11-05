package org.scottishtecharmy.soundscape

import org.junit.Test
import org.scottishtecharmy.soundscape.geoengine.filters.StationaryDetector
import org.scottishtecharmy.soundscape.geoengine.utils.gpx.parseGpx
import org.scottishtecharmy.soundscape.geoengine.utils.rulers.CheapRuler
import org.scottishtecharmy.soundscape.geojsonparser.geojson.LngLatAlt
import org.scottishtecharmy.soundscape.locationprovider.MAXIMUM_TRUSTED_COURSE_ACCURACY_DEGREES
import org.scottishtecharmy.soundscape.locationprovider.MAXIMUM_USABLE_ACCURACY_METRES
import org.scottishtecharmy.soundscape.platform.parseGpxTimestamp
import java.io.File
import kotlin.test.assertTrue

/**
 * Replays the four journeys StationaryDetector's thresholds were measured on, as recorded and
 * with the fixes put back that the location request's 1m minimum distance stopped the phone
 * delivering.
 *
 * Until 2026-11-05 Accuracy.High asked for no fix until the phone had moved a metre from the
 * last one, so a recording has nothing at all while the user stands still: 9-23 minutes of each
 * of these journeys is gaps. The detector starts its window again after a 60s gap and needs five
 * fixes in 30s, so it never saw much of that stillness. With the minimum distance now 0, the
 * phone reports every second whether or not the user moves, and this is what those recordings
 * would have looked like.
 */
class StationaryGapFillTest {

    private data class Fix(
        val location: LngLatAlt,
        val accuracy: Double?,
        val courseIsTrustworthy: Boolean,
        val millis: Long,
        val filler: Boolean = false,
    )

    private fun load(name: String): List<Fix> {
        val gpx = parseGpx(
            File("src/test/res/org/scottishtecharmy/soundscape/gpxFiles/$name.gpx").readText()
        )
        return gpx.tracks.flatMap { it.trackSegments }.flatMap { it.trackPoints }.mapNotNull { tp ->
            val millis = parseGpxTimestamp(tp.time) ?: return@mapNotNull null
            Fix(
                location = LngLatAlt(tp.longitude, tp.latitude),
                accuracy = tp.accuracy?.toDouble(),
                // As GeoEngine and MvtTileTest build it: the GPS course and its own accuracy.
                courseIsTrustworthy = (tp.bearing != null) &&
                    ((tp.bearingAccuracyDegrees ?: Float.MAX_VALUE) <
                        MAXIMUM_TRUSTED_COURSE_ACCURACY_DEGREES),
                millis = millis,
            )
        }
    }

    /**
     * Puts back a fix a second through every gap the user crossed at under [FILL_BELOW_MPS] - the
     * ones the minimum distance made. The phone only held back fixes within a metre of the last
     * one it delivered, so the filler repeats that one, to within that metre.
     *
     * A faster gap is left alone: a train through a tunnel crosses 1.3km in a 72s gap, and those
     * were fixes the GPS never had to give. The filler carries no course: a copied one would be
     * evidence of movement that nobody saw, and only 2% of stationary fixes carry one anyway.
     */
    private fun fillGaps(fixes: List<Fix>): List<Fix> {
        val ruler = CheapRuler(fixes.first().location.latitude)
        val filled = mutableListOf<Fix>()
        for ((previous, next) in fixes.zipWithNext()) {
            filled.add(previous)
            val gapMillis = next.millis - previous.millis
            if (gapMillis <= 1500L) continue
            val speed = ruler.distance(previous.location, next.location) / (gapMillis / 1000.0)
            if (speed >= FILL_BELOW_MPS) continue
            var t = previous.millis + 1000L
            while (t < next.millis - 500L) {
                filled.add(previous.copy(courseIsTrustworthy = false, millis = t, filler = true))
                t += 1000L
            }
        }
        filled.add(fixes.last())
        return filled
    }

    private data class Spell(val startMillis: Long, val endMillis: Long) {
        val seconds get() = (endMillis - startMillis) / 1000
    }

    /** The detector's stationary spells over [fixes], behind GeoEngine's own accuracy gate. */
    private fun spells(fixes: List<Fix>): Pair<List<Spell>, Long> {
        val detector = StationaryDetector()
        val spells = mutableListOf<Spell>()
        var start: Long? = null
        var last = 0L
        for (fix in fixes) {
            // GeoEngine's own gate hands these to noteUnusableFix rather than update.
            val stationary =
                if ((fix.accuracy != null) && (fix.accuracy > MAXIMUM_USABLE_ACCURACY_METRES))
                    detector.noteUnusableFix(fix.millis)
                else
                    detector.update(fix.location, fix.accuracy, fix.courseIsTrustworthy, fix.millis)
            if (stationary && (start == null)) start = fix.millis
            if (!stationary && (start != null)) {
                spells.add(Spell(start, fix.millis)); start = null
            }
            last = fix.millis
        }
        start?.let { spells.add(Spell(it, last)) }
        return spells to detector.stationaryMillis
    }

    private fun clock(origin: Long, millis: Long): String {
        val s = (millis - origin) / 1000
        return "%d:%02d".format(s / 60, s % 60)
    }

    @Test
    fun recordedJourneysWithTheFixesTheMinimumDistanceHeldBack() {
        val report = StringBuilder()
        for (journey in JOURNEYS) {
            val raw = load(journey)
            val filled = fillGaps(raw)
            val (rawSpells, rawMillis) = spells(raw)
            val (filledSpells, filledMillis) = spells(filled)
            val origin = raw.first().millis

            report.appendLine("$journey: ${raw.size} fixes recorded, " +
                "${filled.count { it.filler }} put back")
            for ((label, list, millis) in listOf(
                Triple("  recorded", rawSpells, rawMillis),
                Triple("  filled  ", filledSpells, filledMillis),
            )) {
                report.appendLine("$label ${list.size} spells, ${millis / 60_000.0} min still: " +
                    list.joinToString { "${clock(origin, it.startMillis)}+${it.seconds}s" })
            }

            // Putting the stillness back can only show more of it.
            assertTrue(filledMillis >= rawMillis,
                "$journey: filling the gaps lost stillness ($rawMillis -> $filledMillis ms)")

            // ToMilngavie's train is stopped by 49:06 and pulls out at 50:18 into four minutes of
            // 46-400m fixes, covering 2.7km before good ones return at 54:17. The stop is still;
            // the ride out of it must not be.
            if (journey == "ToMilngavie") {
                val stop = origin + (49 * 60 + 30) * 1000L
                val pullingOut = origin + (52 * 60) * 1000L
                assertTrue(filledSpells.any { it.startMillis <= stop && it.endMillis > stop },
                    "the stop at 49:30 should be stationary")
                assertTrue(filledSpells.none { it.startMillis <= pullingOut && it.endMillis > pullingOut },
                    "still stationary at 52:00, on a train that left at 50:18")
            }
        }
        println(report)
        File("build/test-output").mkdirs()
        File("build/test-output/stationary-gap-fill.txt").writeText(report.toString())
    }

    private companion object {
        val JOURNEYS = listOf("TransferAtQueenStreet", "ToWaverley", "FromWaverley", "ToMilngavie")

        /** Slower than any walk in the recordings - their 10th percentile is 1.1 m/s. */
        const val FILL_BELOW_MPS = 0.5
    }
}
