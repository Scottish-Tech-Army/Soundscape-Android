package org.scottishtecharmy.soundscape

import android.os.Environment
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.viewinterop.AndroidView
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.After
import org.junit.Assume.assumeTrue
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.maplibre.android.MapLibre
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapView
import org.maplibre.android.style.sources.VectorSource
import org.scottishtecharmy.soundscape.screens.home.home.rememberMapViewWithLifecycle
import java.io.File

/**
 * Manual reproduction of the native SIGABRT this fix addresses - NOT part of the automated test
 * suite (see the [Ignore] on [swappingPmtilesFileInPlaceCrashesMapLibre]).
 *
 * MapLibre's native PMTilesFileSource caches parsed header/directory data per
 * pmtiles://file://<path> URL for the life of the process, with no way to invalidate it (see
 * platform/default/src/mbgl/storage/pmtiles_file_source.cpp in maplibre-native - header_cache,
 * metadata_cache and directory_cache are all keyed by URL and never evicted on file change). If a
 * .pmtiles file already opened by a live map is overwritten in place - same path, different
 * content - the next tile MapLibre requests is looked up using offsets cached from the *old*
 * file's directory, then read from the *new* file's bytes at those offsets. Those bytes are
 * essentially random relative to the new file's actual layout, mbgl::util::decompress fails to
 * gunzip them, and the exception escapes MapLibre's file-source worker thread with no try/catch,
 * hitting std::terminate - a full process abort, not a normal Response::Error.
 *
 * This test reproduces exactly that: load a real map view against glasgow-gb.pmtiles, let MapLibre
 * cache its header/root directory by rendering a tile, overwrite the same file path with a
 * completely different city's pmtiles extract (bristol-gb.pmtiles), then force fresh tile
 * requests in the area MapLibre already has cached header info for. The instrumentation process
 * is expected to die with a native SIGABRT - that IS the test passing. A JUnit assertion can't
 * express "the process should crash", so there is deliberately no pass/fail signal beyond that;
 * confirm reproduction via logcat/tombstone, matching the backtrace through
 * PMTilesFileSource::Impl / mbgl::util::decompress documented in the memory notes for this fix.
 *
 * ## How to run
 * 1. Push two distinct real-world .pmtiles fixtures to the app's external "Download" folder (the
 *    same fixtures/location CI already uses for other tests - see run-tests.yaml):
 *    ```
 *    adb root
 *    adb shell mkdir -p /storage/emulated/0/Android/data/org.scottishtecharmy.soundscape/files/Download
 *    adb push glasgow-gb.pmtiles /storage/emulated/0/Android/data/org.scottishtecharmy.soundscape/files/Download/
 *    adb push bristol-gb.pmtiles /storage/emulated/0/Android/data/org.scottishtecharmy.soundscape/files/Download/
 *    ```
 *    (Both are fetched by CI from the R2 bucket - see the `wget` steps in run-tests.yaml if you
 *    need URLs to download them yourself.)
 * 2. Comment out the `@Ignore` on [swappingPmtilesFileInPlaceCrashesMapLibre].
 * 3. `adb logcat -c && ./gradlew connectedAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=org.scottishtecharmy.soundscape.PmtilesSwapCrashTest`
 * 4. Watch for the instrumentation process dying mid-test, then `adb logcat` for a `libmaplibre.so`
 *    abort whose backtrace includes `PMTilesFileSource` / `mbgl::util::decompress`.
 */
class PmtilesSwapCrashTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private val targetContext = InstrumentationRegistry.getInstrumentation().targetContext
    private val downloadsDir =
        targetContext.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)!!
    private val fixtureA = File(downloadsDir, "glasgow-gb.pmtiles")
    private val fixtureB = File(downloadsDir, "bristol-gb.pmtiles")

    // A scratch path independent of the real app's offline-extract scanning - this test drives
    // its own minimal MapView rather than the app's real map screen, so nothing else needs to
    // treat this as a "real" downloaded extract.
    private val scratchDir = File(downloadsDir, "pmtiles-swap-test")
    private val scratchFile = File(scratchDir, "swap.pmtiles")

    private lateinit var mapView: MapView

    @After
    fun cleanup() {
        scratchDir.deleteRecursively()
    }

    @Ignore(
        "Deliberately reproduces a native SIGABRT in libmaplibre.so - run manually only, see " +
            "class KDoc for setup and how to interpret the result."
    )
    @Test
    fun swappingPmtilesFileInPlaceCrashesMapLibre() {
        assumeTrue("fixture not present: ${fixtureA.path}", fixtureA.exists())
        assumeTrue("fixture not present: ${fixtureB.path}", fixtureB.exists())

        scratchDir.mkdirs()
        fixtureA.copyTo(scratchFile, overwrite = true)

        // A location well within glasgow-gb.pmtiles's coverage.
        val glasgowCenter = LatLng(55.8642, -4.2518)

        composeTestRule.setContent {
            mapView = rememberMapViewWithLifecycle(disposeCode = {})
            AndroidView(factory = { mapView }, modifier = Modifier.fillMaxSize())
        }
        composeTestRule.waitForIdle()

        loadPmtilesSource(scratchFile, glasgowCenter, zoom = 15.0)

        // Let the first load complete: MapLibre fetches glasgow-gb.pmtiles's header and root
        // directory, caches them natively for this URL, and renders at least one tile.
        Thread.sleep(8_000)

        // Overwrite the exact same path with a different city's pmtiles extract - this is the
        // old OfflineDownloader's in-place-overwrite behavior this fix removed. bristol-gb.pmtiles
        // has a completely different internal directory/offset layout, so any subsequent read
        // through Glasgow's cached offsets lands on unrelated bytes.
        fixtureB.copyTo(scratchFile, overwrite = true)

        // Force fresh tile requests in the area MapLibre already cached header/directory info
        // for: a different location and a higher zoom means entirely new tile IDs, so this can't
        // be served from any already-rendered-tile cache - it must go back through
        // PMTilesFileSource::Impl's cached (now-stale) offsets and read the swapped file.
        moveCamera(
            LatLng(glasgowCenter.latitude + 0.01, glasgowCenter.longitude + 0.01),
            zoom = 17.0,
        )

        // Give the native worker thread time to request, mis-decompress, and abort.
        Thread.sleep(10_000)
    }

    private fun loadPmtilesSource(file: File, target: LatLng, zoom: Double) {
        val latch = java.util.concurrent.CountDownLatch(1)
        composeTestRule.activity.runOnUiThread {
            MapLibre.getInstance(targetContext)
            mapView.getMapAsync { mapLibreMap ->
                mapLibreMap.cameraPosition = CameraPosition.Builder()
                    .target(target)
                    .zoom(zoom)
                    .build()

                // Same style asset and "openmaptiles" source name the app's real map uses - its
                // layers are all keyed to a source named "openmaptiles", so adding a VectorSource
                // under that name is what actually makes MapLibre start requesting tiles from our
                // pmtiles file.
                mapLibreMap.setStyle("asset://osm-liberty-accessible/originalStyle.json") { style ->
                    val vectorSource = VectorSource("openmaptiles", "pmtiles://file://${file.path}")
                    vectorSource.isVolatile = true
                    style.addSource(vectorSource)
                    latch.countDown()
                }
            }
        }
        latch.await(10, java.util.concurrent.TimeUnit.SECONDS)
    }

    private fun moveCamera(target: LatLng, zoom: Double) {
        val latch = java.util.concurrent.CountDownLatch(1)
        composeTestRule.activity.runOnUiThread {
            mapView.getMapAsync { mapLibreMap ->
                mapLibreMap.cameraPosition = CameraPosition.Builder()
                    .target(target)
                    .zoom(zoom)
                    .build()
                latch.countDown()
            }
        }
        latch.await(5, java.util.concurrent.TimeUnit.SECONDS)
    }
}
