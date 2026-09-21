package org.scottishtecharmy.soundscape.platform

expect fun formatGpxTimestamp(epochMillis: Long): String

/**
 * Reads a track point's `<time>` back to epoch milliseconds, or null if it hasn't got one.
 *
 * Accepts ISO-8601 UTC as [formatGpxTimestamp] writes it ("2026-09-08T14:14:17.605Z"), the same
 * without the fractional seconds, which other apps' exports write, and bare epoch milliseconds,
 * which older Soundscape recordings wrote. Anything else - or a track point with no time at all,
 * as in the RideWithGPS exports - returns null and leaves the caller to make a time up.
 */
expect fun parseGpxTimestamp(time: String?): Long?
