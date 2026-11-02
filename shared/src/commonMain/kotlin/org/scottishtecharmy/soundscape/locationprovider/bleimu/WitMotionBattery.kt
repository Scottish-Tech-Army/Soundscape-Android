package org.scottishtecharmy.soundscape.locationprovider.bleimu

/**
 * WitMotion's published table from battery voltage to charge, highest first: a reading above
 * each voltage, in 0.01 V register units, is at least that percentage.
 */
private val BATTERY_TABLE = listOf(
    396 to 100,
    393 to 90,
    387 to 75,
    382 to 60,
    379 to 50,
    377 to 40,
    373 to 30,
    370 to 20,
    368 to 15,
    350 to 10,
    340 to 5,
)

/**
 * The battery percentage for a WitMotion battery voltage register reading, in 0.01 V units
 * (e.g. 371 = 3.71 V). Readings on a boundary take the lower step, as in WitMotion's table
 * (393-396 is 90%), and anything below 3.40 V is 0%.
 */
fun witMotionBatteryPercent(centivolts: Int): Int =
    BATTERY_TABLE.firstOrNull { (lowest, _) ->
        if (lowest == BATTERY_TABLE.first().first) centivolts > lowest else centivolts >= lowest
    }?.second ?: 0
