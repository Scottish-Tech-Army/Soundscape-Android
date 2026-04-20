package org.scottishtecharmy.soundscape.audio

/**
 * Data-driven beacon type definitions ported from DynamicAudioEngineAssets.swift.
 * Each beacon type has a set of WAV asset names, beats per phrase, and a selector
 * function that chooses which asset to play based on the angular relationship
 * between the user's heading and the bearing to the beacon.
 */

/**
 * Result of a beacon asset selection: which asset index to play and at what volume.
 */
data class AssetSelection(val assetIndex: Int, val volume: Float)

/**
 * Selector function type: takes user heading (nullable) and POI bearing,
 * returns which asset to play and at what volume, or null for silence.
 */
typealias BeaconSelector = (userHeading: Double?, poiBearing: Double) -> AssetSelection?

data class BeaconType(
    val name: String,
    val assets: List<String>,
    val beatsInPhrase: Int,
    val selector: BeaconSelector
)

// --- Selector implementations matching the original iOS angular zones ---

/**
 * 2-region selector (Classic beacon):
 *   On-Axis:  central 45° window (337.5° to 22.5°) → asset[0]
 *   Off-Axis: remaining → asset[1]
 */
private fun twoRegionSelector(userHeading: Double?, poiBearing: Double): AssetSelection? {
    if (userHeading == null) return AssetSelection(1, 1f)
    val angle = normalizeAngle(userHeading - poiBearing)
    return if (angle >= 337.5 || angle <= 22.5) {
        AssetSelection(0, 1f)
    } else {
        AssetSelection(1, 1f)
    }
}

/**
 * 3-region selector (Tactile, Drop, Signal, Mallet variants):
 *   A+:     central 30° (345° to 15°) → asset[0]
 *   A:      110° side windows (235°–345° or 15°–125°) → asset[1]
 *   Behind: remaining (125°–235°) → asset[2]
 */
private fun threeRegionSelector(userHeading: Double?, poiBearing: Double): AssetSelection? {
    if (userHeading == null) return AssetSelection(2, 1f)
    val angle = normalizeAngle(userHeading - poiBearing)
    return when {
        angle >= 345 || angle <= 15 -> AssetSelection(0, 1f)
        (angle in 235.0..345.0) || (angle in 15.0..125.0) -> AssetSelection(1, 1f)
        else -> AssetSelection(2, 1f)
    }
}

/**
 * 4-region selector (Current/V2, Flare, Shimmer, Ping):
 *   A+:     central 30° (345°–15°) → asset[0]
 *   A:      40° side windows (305°–345° or 15°–55°) → asset[1]
 *   B:      70° side windows (235°–305° or 55°–125°) → asset[2]
 *   Behind: remaining (125°–235°) → asset[3]
 */
private fun fourRegionSelector(userHeading: Double?, poiBearing: Double): AssetSelection? {
    if (userHeading == null) return AssetSelection(3, 1f)
    val angle = normalizeAngle(userHeading - poiBearing)
    return when {
        angle >= 345 || angle <= 15 -> AssetSelection(0, 1f)
        (angle in 305.0..345.0) || (angle in 15.0..55.0) -> AssetSelection(1, 1f)
        (angle in 235.0..305.0) || (angle in 55.0..125.0) -> AssetSelection(2, 1f)
        else -> AssetSelection(3, 1f)
    }
}

/** Normalize angle to [0, 360) range */
private fun normalizeAngle(degrees: Double): Double {
    var a = degrees % 360.0
    if (a < 0) a += 360.0
    return a
}

// --- All beacon type definitions ---

val BEACON_TYPES: Map<String, BeaconType> = mapOf(
    "Original" to BeaconType(
        name = "Original",
        assets = listOf("Classic_OnAxis", "Classic_OffAxis"),
        beatsInPhrase = 2,
        selector = ::twoRegionSelector
    ),
    "Current" to BeaconType(
        name = "Current",
        assets = listOf("Current_A+", "Current_A", "Current_B", "Current_Behind"),
        beatsInPhrase = 6,
        selector = ::fourRegionSelector
    ),
    "Tactile" to BeaconType(
        name = "Tactile",
        assets = listOf("Tactile_OnAxis", "Tactile_OffAxis", "Tactile_Behind"),
        beatsInPhrase = 6,
        selector = ::threeRegionSelector
    ),
    "Flare" to BeaconType(
        name = "Flare",
        assets = listOf("Flare_A+", "Flare_A", "Flare_B", "Flare_Behind"),
        beatsInPhrase = 6,
        selector = ::fourRegionSelector
    ),
    "Shimmer" to BeaconType(
        name = "Shimmer",
        assets = listOf("Shimmer_A+", "Shimmer_A", "Shimmer_B", "Shimmer_Behind"),
        beatsInPhrase = 6,
        selector = ::fourRegionSelector
    ),
    "Ping" to BeaconType(
        name = "Ping",
        assets = listOf("Ping_A+", "Ping_A", "Ping_B", "Tactile_Behind"),
        beatsInPhrase = 6,
        selector = ::fourRegionSelector
    ),
    "Drop" to BeaconType(
        name = "Drop",
        assets = listOf("Drop_A+", "Drop_A", "Drop_Behind"),
        beatsInPhrase = 6,
        selector = ::threeRegionSelector
    ),
    "Signal" to BeaconType(
        name = "Signal",
        assets = listOf("Signal_A+", "Signal_A", "Drop_Behind"),
        beatsInPhrase = 6,
        selector = ::threeRegionSelector
    ),
    "Signal Slow" to BeaconType(
        name = "Signal Slow",
        assets = listOf("Signal_Slow_A+", "Signal_Slow_A", "Signal_Slow_Behind"),
        beatsInPhrase = 12,
        selector = ::threeRegionSelector
    ),
    "Signal Very Slow" to BeaconType(
        name = "Signal Very Slow",
        assets = listOf("Signal_Very_Slow_A+", "Signal_Very_Slow_A", "Signal_Very_Slow_Behind"),
        beatsInPhrase = 18,
        selector = ::threeRegionSelector
    ),
    "Mallet" to BeaconType(
        name = "Mallet",
        assets = listOf("Mallet_A+", "Mallet_A", "Mallet_Behind"),
        beatsInPhrase = 6,
        selector = ::threeRegionSelector
    ),
    "Mallet Slow" to BeaconType(
        name = "Mallet Slow",
        assets = listOf("Mallet_Slow_A+", "Mallet_Slow_A", "Mallet_Slow_Behind"),
        beatsInPhrase = 12,
        selector = ::threeRegionSelector
    ),
    "Mallet Very Slow" to BeaconType(
        name = "Mallet Very Slow",
        assets = listOf("Mallet_Very_Slow_A+", "Mallet_Very_Slow_A", "Mallet_Very_Slow_Behind"),
        beatsInPhrase = 18,
        selector = ::threeRegionSelector
    ),
)
