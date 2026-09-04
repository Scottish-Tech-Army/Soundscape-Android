package org.scottishtecharmy.soundscape.backup

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.scottishtecharmy.soundscape.database.local.dao.RouteDao
import org.scottishtecharmy.soundscape.utils.buildMarkersAndRoutesArchive
import org.scottishtecharmy.soundscape.utils.decodeArchiveEnvelope
import org.scottishtecharmy.soundscape.utils.encodeArchiveEnvelope
import org.scottishtecharmy.soundscape.utils.restoreMarkersAndRoutesArchive
import platform.Foundation.NSNotification
import platform.Foundation.NSNotificationCenter
import platform.Foundation.NSOperationQueue
import platform.Foundation.NSUbiquitousKeyValueStore
import platform.Foundation.NSUbiquitousKeyValueStoreDidChangeExternallyNotification

/**
 * Keeps a copy of the user's markers and routes in iCloud, so that deleting the app, replacing a
 * phone or restoring a device doesn't lose them.
 *
 * Backup rather than sync. The document is the same archive Settings → Advanced markers and routes
 * exports - see `buildMarkersAndRoutesArchive` - wrapped in a JSON envelope and stored under one
 * key. It is rewritten whole every time anything changes, so a deleted marker is gone from the
 * next backup without any deletion bookkeeping.
 *
 * The whole-document approach only works because Soundscape is a single-device app in practice.
 * Two devices editing between backups would have one overwrite the other's changes wholesale.
 * Making that safe means per-record identity and a merge, which is a great deal of machinery for a
 * case we don't have.
 *
 * ## Restoring
 *
 * Only ever into an empty library, and never over data the user can see. That rule is what makes
 * this safe to run automatically: the worst case is that a restore doesn't happen, not that it
 * overwrites something.
 *
 * The initial download from iCloud is asynchronous and usually lands *after* launch, so a fresh
 * install typically finds nothing on the first look. [start] therefore also listens for the
 * store's change notification and tries again when the data arrives.
 */
class IosCloudBackup(
    private val dao: RouteDao,
    private val scope: CoroutineScope,
    private val store: NSUbiquitousKeyValueStore = NSUbiquitousKeyValueStore.defaultStore,
    /**
     * Whether restoring is allowed at all right now. The legacy import also fills an empty
     * library, from data staged before the app opened, so the two must not both run - the user
     * would end up with each marker twice.
     */
    private val canRestore: () -> Boolean = { true },
    private val onRestored: (Int) -> Unit = {},
) {
    private var observer: Any? = null

    /**
     * Starts watching iCloud, and restores now if there is something there and nothing here.
     *
     * Safe to call when the user has no iCloud account: the store just stays empty, reads return
     * null and writes go nowhere.
     */
    fun start() {
        observer = NSNotificationCenter.defaultCenter.addObserverForName(
            name = NSUbiquitousKeyValueStoreDidChangeExternallyNotification,
            `object` = store,
            queue = NSOperationQueue.mainQueue,
        ) { _: NSNotification? ->
            // Anything arriving from iCloud is either the initial download on a new install or
            // another device's backup. Either way the only thing we do with it is restore into an
            // empty library.
            scope.launch { restoreIfLibraryEmpty() }
        }

        store.synchronize()
        scope.launch { restoreIfLibraryEmpty() }
    }

    fun stop() {
        observer?.let { NSNotificationCenter.defaultCenter.removeObserver(it) }
        observer = null
    }

    /**
     * Replaces the backup with the library as it stands. Called whenever markers or routes change.
     */
    suspend fun backupNow() {
        val archive = try {
            encodeArchiveEnvelope(buildMarkersAndRoutesArchive(dao))
        } catch (t: Throwable) {
            println("IosCloudBackup: could not build archive: $t")
            return
        }

        val size = archive.encodeToByteArray().size
        if (size > MAX_VALUE_BYTES) {
            // iCloud's per-key ceiling is 1 MB and it rejects the write outright, so there is
            // nothing to do but say so. It takes thousands of markers to get here.
            println("IosCloudBackup: archive is $size bytes, over the ${MAX_VALUE_BYTES} limit - not backed up")
            return
        }

        store.setString(archive, forKey = ARCHIVE_KEY)
        store.synchronize()
        println("IosCloudBackup: backed up $size bytes")
    }

    /**
     * Restores the backup if there is one and the library is empty, returning how many GPX
     * documents were restored.
     *
     * The emptiness check is deliberately of the database rather than of a "have I restored yet"
     * flag: a flag can be lost or wrong, whereas an empty library is the actual condition under
     * which restoring can't destroy anything.
     */
    suspend fun restoreIfLibraryEmpty(): Int {
        val json = store.stringForKey(ARCHIVE_KEY) ?: return 0

        if (!canRestore()) return 0
        if (dao.getAllMarkers().isNotEmpty() || dao.getAllRoutes().isNotEmpty()) return 0

        val files = decodeArchiveEnvelope(json)
        if (files == null) {
            println("IosCloudBackup: backup could not be read")
            return 0
        }

        val restored = try {
            restoreMarkersAndRoutesArchive(files, dao)
        } catch (t: Throwable) {
            println("IosCloudBackup: restore failed: $t")
            return 0
        }

        if (restored > 0) {
            println("IosCloudBackup: restored $restored documents from iCloud")
            onRestored(restored)
        }
        return restored
    }

    private companion object {
        /**
         * Deliberately unlike the legacy app's `marker.*` and `route.*` keys. We share a bundle
         * identifier with it and may therefore share its key-value store, and overwriting the
         * legacy app's own backup would break a rollback - the one thing the whole migration is
         * careful not to do.
         */
        const val ARCHIVE_KEY = "soundscape.markersAndRoutes.archive"

        /** iCloud allows 1 MB per key; stay under it with room for the store's own overhead. */
        const val MAX_VALUE_BYTES = 900 * 1024
    }
}
