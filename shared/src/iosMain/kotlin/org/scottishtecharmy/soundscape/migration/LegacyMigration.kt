package org.scottishtecharmy.soundscape.migration

import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.newSingleThreadContext
import okio.FileSystem
import okio.Path.Companion.toPath
import org.scottishtecharmy.soundscape.database.local.MarkersAndRoutesDatabaseProvider
import org.scottishtecharmy.soundscape.geoengine.ProtomapsGridState
import org.scottishtecharmy.soundscape.i18n.ComposeLocalizedStrings
import org.scottishtecharmy.soundscape.network.createIosVectorTileClient
import platform.Foundation.NSBundle
import platform.Foundation.NSHomeDirectory

/**
 * The legacy import, in two halves.
 *
 * Reading the legacy Realm database is quick and entirely local, so LegacyMigrator.swift still
 * does it during app launch and hands the result here to be [staged][stageLegacyMigrationPayload]
 * as a file. Writing it into Room is not quick: markers the legacy app never stored a name for
 * have to be looked up in map tiles, which means the network. That half runs later, from the
 * migration screen, where it can take its time and tell the user what it's doing - see
 * [runPendingLegacyMigration] and `LegacyMigrationScreen`.
 *
 * The staged file is the record of work outstanding: it is written before the import and deleted
 * only once the import has been written to the database, so an app killed midway through launch
 * still has everything it needs to try again.
 */
private val payloadPath
    get() = (NSHomeDirectory() + "/Documents/" + PAYLOAD_FILE_NAME).toPath()

/** Where an unparseable payload is parked, so it stops being retried but is still recoverable. */
private val failedPayloadPath
    get() = (NSHomeDirectory() + "/Documents/" + FAILED_PAYLOAD_FILE_NAME).toPath()

private const val PAYLOAD_FILE_NAME = "legacy-migration-payload.json"
private const val FAILED_PAYLOAD_FILE_NAME = "legacy-migration-payload.failed.json"

/**
 * Records the markers and routes read out of the legacy database, for the migration screen to
 * import. Returns false if the file couldn't be written, in which case the caller should leave its
 * "migration done" flag unset so the whole thing is retried on the next launch.
 */
fun stageLegacyMigrationPayload(payloadJson: String): Boolean =
    try {
        FileSystem.SYSTEM.write(payloadPath) { writeUtf8(payloadJson) }
        println("LegacyMigration: staged ${payloadJson.length} chars at $payloadPath")
        true
    } catch (t: Throwable) {
        println("LegacyMigration: could not stage payload: $t")
        false
    }

/** Whether there is a staged import waiting, i.e. whether to show the migration screen. */
fun hasPendingLegacyMigration(): Boolean = FileSystem.SYSTEM.exists(payloadPath)

/**
 * Imports the staged markers and routes into Room, naming the ones the legacy app didn't store a
 * name for from current map tiles (see [TileLegacyMarkerNamer]).
 *
 * Reports progress as (done so far, total). The staged payload is left in place unless the import
 * actually reached the database, so a run that stops for want of map data can simply be tried
 * again - the import writes nothing at all in that case, because a half-imported database can't
 * be resumed without duplicating whatever did land.
 */
@OptIn(ExperimentalCoroutinesApi::class, DelicateCoroutinesApi::class)
suspend fun runPendingLegacyMigration(
    onProgress: (done: Int, total: Int) -> Unit,
): LegacyImportResult {
    val payloadJson = try {
        FileSystem.SYSTEM.read(payloadPath) { readUtf8() }
    } catch (t: Throwable) {
        println("LegacyMigration: could not read staged payload: $t")
        return LegacyImportResult.Unreadable
    }

    val dao = MarkersAndRoutesDatabaseProvider.getInstance().routeDao()

    // A grid of our own rather than the geo engine's: this runs before the engine starts, and
    // tearing it down afterwards keeps its tile readers and worker thread from outliving the
    // import.
    val treeContext = newSingleThreadContext("TreeContext")
    val gridState = ProtomapsGridState(passedInTreeContext = treeContext)
    gridState.tileClient = createIosVectorTileClient(baseUrl = tileProviderUrl())
    // Offline map extracts live alongside the databases in Documents, so a user who has
    // downloaded their area can be migrated with no network connection at all.
    gridState.start(NSHomeDirectory() + "/Documents")

    val namer = TileLegacyMarkerNamer(
        gridState = gridState,
        strings = ComposeLocalizedStrings(),
        log = { message -> println("LegacyMarkerNamer: $message") },
    )

    val result = try {
        importLegacyPayload(payloadJson, dao, namer, onProgress)
    } catch (t: Throwable) {
        println("LegacyMigration: import failed: $t")
        LegacyImportResult.Unreadable
    } finally {
        gridState.stop()
        treeContext.close()
    }

    when (result) {
        is LegacyImportResult.Imported -> {
            // Written to the database, so the staged copy has done its job. The legacy Realm
            // itself is still there - we never delete anything belonging to the old app.
            println("LegacyMigration: imported ${result.count} markers + routes")
            try {
                FileSystem.SYSTEM.delete(payloadPath)
            } catch (t: Throwable) {
                println("LegacyMigration: imported, but could not delete staged payload: $t")
            }
        }

        LegacyImportResult.NeedsMapData -> {
            // Nothing was written. Leave the payload staged so the screen can offer to try again,
            // now or on the next launch.
            println("LegacyMigration: no map data available; import not started")
        }

        LegacyImportResult.Unreadable -> {
            // Retrying is pointless, so park the payload under another name rather than leave the
            // migration screen greeting the user on every launch from here on. The file stays on
            // disk for support, and the legacy database it was built from is untouched.
            println("LegacyMigration: staged payload could not be used")
            try {
                FileSystem.SYSTEM.atomicMove(payloadPath, failedPayloadPath)
            } catch (t: Throwable) {
                println("LegacyMigration: could not park unusable payload: $t")
            }
        }
    }

    return result
}

/**
 * The tile server, read from Info.plist where the build writes it (values come from the gitignored
 * Local.xcconfig). Read here rather than borrowed from IosSoundscapeService because the migration
 * runs before that service exists.
 */
private fun tileProviderUrl(): String =
    NSBundle.mainBundle.objectForInfoDictionaryKey("TileProviderURL") as? String ?: ""
