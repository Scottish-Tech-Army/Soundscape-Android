package org.scottishtecharmy.soundscape.platform

/**
 * Ask the platform not to include [path] in the device's cloud backup.
 *
 * Offline map extracts are the reason this exists. They live beside the markers and routes
 * database - in Documents on iOS - which does belong in a backup, so the directory as a whole
 * cannot be excluded and the extracts have to be marked one file at a time. An extract is
 * hundreds of megabytes of map data that the user can download again from the extract server,
 * so backing it up wastes their iCloud allowance restoring something we can refetch, which is
 * exactly what Apple's guidance on the flag says to avoid.
 *
 * Missing files are ignored: callers mark whatever they find on disk, and a file deleted
 * between the scan and the call is not an error.
 */
internal expect fun excludeFromCloudBackup(path: String)
