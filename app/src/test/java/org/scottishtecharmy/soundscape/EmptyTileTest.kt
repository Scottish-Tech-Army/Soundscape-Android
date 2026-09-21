package org.scottishtecharmy.soundscape

import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertNotNull
import kotlinx.coroutines.runBlocking
import okio.Path.Companion.toPath
import org.junit.Test
import org.scottishtecharmy.soundscape.geoengine.utils.pmtiles.PmTilesReader
import org.scottishtecharmy.soundscape.network.createAndroidVectorTileClient
import vector_tile.Tile
import java.net.ServerSocket
import kotlin.io.path.createTempDirectory

/**
 * Some protomaps tiles are totally empty, i.e. there's no tile data for them at all. This is
 * different from a tile that's just entirely open sea, or has a layer present with no features
 * in it - both of those still get served as normal, with an HTTP 200. A genuinely empty tile
 * (e.g. z=14, x=5011, y=10976, in a remote part of the Patagonian steppe near Río Gallegos,
 * Argentina, verified directly against the tile server) is missing from a PMTiles offline
 * extract altogether, and the protomaps HTTP server responds to a request for it with an HTTP
 * 204 (No Content) and no body. These tests check that both of those routes to fetching a tile
 * in [org.scottishtecharmy.soundscape.geoengine.ProtomapsGridState] are recognised as a
 * successful, empty tile rather than being treated as a failure that aborts the whole grid
 * update.
 */
class EmptyTileTest {

    @Test
    fun emptyTileMissingFromLocalExtract() {
        // z=14, x=5011, y=10976 is genuinely empty - the protomaps HTTP server responds to it
        // with "204 No Content" (see emptyTileFromHttpServer below). A PMTiles offline extract
        // covering the same area doesn't have a tile at all for it, which is already handled
        // correctly by ProtomapsGridState.updateTile falling through to try other extracts and
        // then the network. This confirms that ground truth against the real fixture.
        val reader = PmTilesReader("$offlineExtractPath/río-gallegos-ar.pmtiles".toPath())

        assertEquals(null, reader.getTile(14, 5011, 10976))
    }

    @Test
    fun emptyTileFromHttpServer() {
        // Start a minimal local HTTP server that responds to a request with "204 No Content" and
        // no body, exactly as the real protomaps tile server does for tiles that are totally
        // empty.
        val serverSocket = ServerSocket(0)
        val serverThread = Thread {
            serverSocket.accept().use { socket ->
                val input = socket.getInputStream().bufferedReader()
                // Consume the request line and headers up to the blank line that terminates them.
                while (true) {
                    val line = input.readLine() ?: break
                    if (line.isEmpty()) break
                }
                val output = socket.getOutputStream()
                output.write(
                    "HTTP/1.1 204 No Content\r\nConnection: close\r\n\r\n".toByteArray()
                )
                output.flush()
            }
        }
        serverThread.start()

        try {
            val tileClient = createAndroidVectorTileClient(
                baseUrl = "http://localhost:${serverSocket.localPort}/",
                cacheDir = createTempDirectory("EmptyTileTest").toFile(),
                userAgent = "EmptyTileTest",
                hasNetwork = { true },
            )

            // A 204 has no body, but it's still a successful response, so this must come back as
            // a non-null (empty) byte array rather than being treated as a failed request that's
            // worth retrying.
            val bytes = runBlocking { tileClient.getTile(5011, 10976, 14) }
            assertNotNull(bytes)
            assertEquals(0, bytes!!.size)

            val tile = Tile.ADAPTER.decode(bytes)
            assertEquals(0, tile.layers.size)
        } finally {
            serverThread.join(2000)
            serverSocket.close()
        }
    }
}
