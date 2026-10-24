package org.scottishtecharmy.soundscape.mapstyle

import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okio.Path
import okio.Path.Companion.toPath
import org.maplibre.compose.resource.MapResourceLoad
import org.scottishtecharmy.soundscape.platform.systemFileSystem
import kotlin.random.Random
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertIs

/** Tests for [OfflineExtractTiles], using the hand-built extracts from [PmTilesFixture]. */
class OfflineExtractTilesTest {

    private lateinit var tempDir: Path

    @BeforeTest
    fun setUp() {
        tempDir = "build/offlineExtractTilesTest-${Random.nextLong().let { if (it < 0) -it else it }}".toPath()
        systemFileSystem.createDirectories(tempDir)
    }

    @AfterTest
    fun tearDown() {
        // Open files can't be deleted on Windows.
        runTest { OfflineExtractTiles.closeReaders() }
        systemFileSystem.deleteRecursively(tempDir)
    }

    private fun writeFixture(name: String, bytes: ByteArray): String {
        val path = tempDir / name
        systemFileSystem.write(path) { write(bytes) }
        return path.toString()
    }

    @Test
    fun pathsSurviveEncoding() {
        val path = "/storage/1A2B-3C4D/Download/My maps/glasgow ü?#%{z}.pmtiles"
        val encoded = OfflineExtractTiles.encodePath(path)
        assertEquals(false, encoded.any { it in " ?#{}" })
        assertEquals(path, OfflineExtractTiles.decodePath(encoded))
    }

    @Test
    fun tileJsonPointsTilesBackAtTheExtract() = runTest {
        val path = writeFixture("with space.pmtiles", PmTilesFixture.build(hasTileEntry = true))
        val load = OfflineExtractTiles.load(OfflineExtractTiles.tileJsonUrl(path))
        assertIs<MapResourceLoad.Bytes>(load)
        val tileJson = Json.parseToJsonElement(load.bytes.decodeToString()).jsonObject
        val template = tileJson["tiles"]!!.jsonArray.single().jsonPrimitive.content
        assertEquals(
            "soundscape-extract://tile/{z}/{x}/{y}?file=${OfflineExtractTiles.encodePath(path)}",
            template,
        )
        assertEquals("xyz", tileJson["scheme"]!!.jsonPrimitive.content)
    }

    @Test
    fun tilesComeFromTheExtract() = runTest {
        val bytes = PmTilesFixture.build(hasTileEntry = true)
        val path = writeFixture("tiles.pmtiles", bytes)
        val url = "soundscape-extract://tile/14/8000/5000?file=${OfflineExtractTiles.encodePath(path)}"
        val load = OfflineExtractTiles.load(url)
        assertIs<MapResourceLoad.Bytes>(load)
        // The fixture's only tile is the four bytes at the end of the file, stored uncompressed.
        assertContentEquals(bytes.copyOfRange(bytes.size - 4, bytes.size), load.bytes)
    }

    @Test
    fun tilesTheExtractLacksAreEmpty() = runTest {
        val path = writeFixture("empty.pmtiles", PmTilesFixture.build(hasTileEntry = false))
        val url = "soundscape-extract://tile/14/8000/5000?file=${OfflineExtractTiles.encodePath(path)}"
        assertIs<MapResourceLoad.NoContent>(OfflineExtractTiles.load(url))
    }

    @Test
    fun aMissingExtractFails() = runTest {
        val url = OfflineExtractTiles.tileJsonUrl("$tempDir/gone.pmtiles")
        assertIs<MapResourceLoad.Failed>(OfflineExtractTiles.load(url))
    }

    @Test
    fun aChangedExtractIsReadAfresh() = runTest {
        val path = writeFixture("changed.pmtiles", PmTilesFixture.build(hasTileEntry = true))
        val url = "soundscape-extract://tile/14/8000/5000?file=${OfflineExtractTiles.encodePath(path)}"
        assertIs<MapResourceLoad.Bytes>(OfflineExtractTiles.load(url))
        writeFixture("changed.pmtiles", PmTilesFixture.build(hasTileEntry = false, paddingSize = 10))
        assertIs<MapResourceLoad.NoContent>(OfflineExtractTiles.load(url))
    }
}
