package org.scottishtecharmy.soundscape.mapstyle

import okio.Path
import okio.Path.Companion.toPath
import org.scottishtecharmy.soundscape.geoengine.PROTOMAPS_SERVER_PATH
import org.scottishtecharmy.soundscape.geojsonparser.geojson.LngLatAlt
import org.scottishtecharmy.soundscape.platform.systemFileSystem
import kotlin.random.Random
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

/**
 * Tests for [resolveTileSourceUrl].
 *
 * These write small synthetic ".pmtiles" files to a real temp directory on disk, because
 * [resolveTileSourceUrl] itself does real file I/O (via [org.scottishtecharmy.soundscape.utils.findExtractPaths]
 * and [org.scottishtecharmy.soundscape.utils.isPmtilesUsable]) rather than taking an injectable
 * abstraction.
 *
 * [org.scottishtecharmy.soundscape.utils.isPmtilesUsable] validates a PMTiles v3 file by:
 *  1. reading the 127-byte header (magic + section offsets/lengths) and the root directory,
 *  2. decompressing the JSON metadata section,
 *  3. checking every header-declared section fits within the actual file length, and
 *  4. decompressing the physically-last tile.
 * It does NOT validate actual map content, so a minimal hand-built file that only satisfies the
 * above (using "no compression" for both the internal and tile compression, so steps 2 and 4 are
 * trivial) is enough to be considered "usable" - see [PmTilesFixture].
 *
 * To make a fixture "contain" (or not contain) an arbitrary test location without having to
 * replicate resolveTileSourceUrl's hilbert/zoom math, [PmTilesFixture.build] with
 * `hasTileEntry = true` writes a single root-directory entry starting at tile id 0 with an
 * enormous run-length, so [org.scottishtecharmy.soundscape.geoengine.utils.pmtiles.PmTilesReader.getTile]
 * matches virtually any zoom/x/y id computed for [org.scottishtecharmy.soundscape.geoengine.MAX_ZOOM_LEVEL].
 * `hasTileEntry = false` writes an empty root directory, so no location is ever "contained".
 */
class TileSourceResolverTest {

    private lateinit var tempDir: Path

    @BeforeTest
    fun setUp() {
        tempDir = "build/tileSourceResolverTest-${Random.nextLong().let { if (it < 0) -it else it }}".toPath()
        systemFileSystem.createDirectories(tempDir)
    }

    @AfterTest
    fun tearDown() {
        systemFileSystem.deleteRecursively(tempDir)
    }

    private fun writeFixture(name: String, bytes: ByteArray): String {
        val path = tempDir / name
        systemFileSystem.write(path) { write(bytes) }
        return path.toString()
    }

    private fun expectedNetworkUrl(networkTileUrl: String): String =
        "${networkTileUrl.trimEnd('/')}/$PROTOMAPS_SERVER_PATH.json"

    private val glasgow = LngLatAlt(-4.2518, 55.8642)
    private val networkTileUrl = "https://tiles.example.com"

    @Test
    fun emptyExtractsPathReturnsNetworkUrlImmediately() {
        val result = resolveTileSourceUrl(glasgow, "", networkTileUrl)
        assertEquals(expectedNetworkUrl(networkTileUrl), result)
    }

    @Test
    fun noLocalExtractsFoundInEmptyDirectoryReturnsNetworkUrl() {
        // tempDir exists but contains no .pmtiles files.
        val result = resolveTileSourceUrl(glasgow, tempDir.toString(), networkTileUrl)
        assertEquals(expectedNetworkUrl(networkTileUrl), result)
    }

    @Test
    fun nonExistentExtractsPathReturnsNetworkUrl() {
        val missingDir = (tempDir / "does-not-exist").toString()
        val result = resolveTileSourceUrl(glasgow, missingDir, networkTileUrl)
        assertEquals(expectedNetworkUrl(networkTileUrl), result)
    }

    @Test
    fun nonPmtilesFilesInExtractsPathAreIgnored() {
        writeFixture("notes.txt", "hello".encodeToByteArray())
        val result = resolveTileSourceUrl(glasgow, tempDir.toString(), networkTileUrl)
        assertEquals(expectedNetworkUrl(networkTileUrl), result)
    }

    @Test
    fun corruptExtractsAreExcludedAndFallsBackToNetwork() {
        writeFixture("empty.pmtiles", ByteArray(0))
        writeFixture("bad-magic.pmtiles", PmTilesFixture.corruptMagic())
        writeFixture("truncated.pmtiles", PmTilesFixture.truncated())

        val result = resolveTileSourceUrl(glasgow, tempDir.toString(), networkTileUrl)
        assertEquals(expectedNetworkUrl(networkTileUrl), result)
    }

    @Test
    fun corruptExtractsExcludedEvenWhenLocationIsNull() {
        // Regression coverage for the bug described in resolveTileSourceUrl's doc comment: the
        // location == null branch used to return offlineExtractPaths[0] *before* filtering out
        // unusable extracts. Put the corrupt file first alphabetically so an unfiltered pick
        // would return it.
        writeFixture("a-corrupt.pmtiles", PmTilesFixture.corruptMagic())

        val result = resolveTileSourceUrl(null, tempDir.toString(), networkTileUrl)
        assertEquals(expectedNetworkUrl(networkTileUrl), result)
    }

    @Test
    fun locationNullWithSingleUsableExtractReturnsIt() {
        val path = writeFixture("world.pmtiles", PmTilesFixture.build(hasTileEntry = true))

        val result = resolveTileSourceUrl(null, tempDir.toString(), networkTileUrl)
        assertEquals("pmtiles://file://$path", result)
    }

    @Test
    fun locationNullPicksAUsableExtractEvenWhenACorruptOneSortsFirst() {
        writeFixture("a-corrupt.pmtiles", PmTilesFixture.corruptMagic())
        val usablePath = writeFixture("z-usable.pmtiles", PmTilesFixture.build(hasTileEntry = true))

        val result = resolveTileSourceUrl(null, tempDir.toString(), networkTileUrl)
        assertEquals("pmtiles://file://$usablePath", result)
    }

    @Test
    fun locationNullPicksLargestExtractNotJustTheFirstOne() {
        // Regression coverage: the location == null branch used to just return
        // offlineExtractPaths[0] (whatever order the filesystem lists them in), contradicting
        // this function's own doc comment promising the "best (largest)" extract. Name the
        // smaller one so it sorts first, so a naive "first" pick would fail this assertion.
        val smallerPath =
            writeFixture("a-small.pmtiles", PmTilesFixture.build(hasTileEntry = true, paddingSize = 0))
        val largerPath =
            writeFixture("z-large.pmtiles", PmTilesFixture.build(hasTileEntry = true, paddingSize = 5_000))

        val result = resolveTileSourceUrl(null, tempDir.toString(), networkTileUrl)
        assertEquals("pmtiles://file://$largerPath", result)
        assertTrue(
            systemFileSystem.metadata(smallerPath.toPath()).size!! <
                systemFileSystem.metadata(largerPath.toPath()).size!!,
        )
    }

    @Test
    fun extractContainingLocationIsPicked() {
        val path = writeFixture("world.pmtiles", PmTilesFixture.build(hasTileEntry = true))

        val result = resolveTileSourceUrl(glasgow, tempDir.toString(), networkTileUrl)
        assertEquals("pmtiles://file://$path", result)
    }

    @Test
    fun usableExtractNotContainingLocationFallsBackToNetwork() {
        // Usable (passes validation) but its root directory has no entries, so it never
        // "contains" any tile.
        writeFixture("empty-coverage.pmtiles", PmTilesFixture.build(hasTileEntry = false))

        val result = resolveTileSourceUrl(glasgow, tempDir.toString(), networkTileUrl)
        assertEquals(expectedNetworkUrl(networkTileUrl), result)
    }

    @Test
    fun largestContainingExtractIsPreferredOverSmallerOne() {
        val smallerPath = writeFixture("small.pmtiles", PmTilesFixture.build(hasTileEntry = true, paddingSize = 0))
        val largerPath = writeFixture("large.pmtiles", PmTilesFixture.build(hasTileEntry = true, paddingSize = 5_000))

        val result = resolveTileSourceUrl(glasgow, tempDir.toString(), networkTileUrl)
        assertEquals("pmtiles://file://$largerPath", result)
        // Sanity check that the two fixtures actually differ in size, i.e. the test is exercising
        // the "largest wins" comparison and not just picking the only match.
        assertTrue(
            systemFileSystem.metadata(smallerPath.toPath()).size!! <
                systemFileSystem.metadata(largerPath.toPath()).size!!,
        )
    }

    @Test
    fun mixOfContainingCoveringAndCorruptExtractsPicksLargestUsableContaining() {
        writeFixture("corrupt.pmtiles", PmTilesFixture.corruptMagic())
        writeFixture("no-coverage.pmtiles", PmTilesFixture.build(hasTileEntry = false))
        val smallContaining =
            writeFixture("small-containing.pmtiles", PmTilesFixture.build(hasTileEntry = true, paddingSize = 0))
        val bigContaining =
            writeFixture("big-containing.pmtiles", PmTilesFixture.build(hasTileEntry = true, paddingSize = 2_000))

        val result = resolveTileSourceUrl(glasgow, tempDir.toString(), networkTileUrl)
        assertEquals("pmtiles://file://$bigContaining", result)
        assertNotEquals("pmtiles://file://$smallContaining", result)
    }
}

/**
 * Builds minimal, hand-crafted PMTiles v3 byte arrays for [TileSourceResolverTest].
 *
 * Mirrors just enough of the format read by
 * [org.scottishtecharmy.soundscape.geoengine.utils.pmtiles.PmTilesReader] to pass
 * [org.scottishtecharmy.soundscape.utils.isPmtilesUsable]'s validation: a 127-byte header, a
 * root directory (varint-encoded, uncompressed), a JSON metadata blob (uncompressed) and a tile
 * data section. Both "internal" and "tile" compression are set to NONE so the reader's
 * decompress calls are no-ops - real gzip data isn't needed to exercise resolveTileSourceUrl's
 * branching, only the presence/validity of each section.
 */
private object PmTilesFixture {
    private const val HEADER_LENGTH = 127
    private val MAGIC = byteArrayOf(0x50, 0x4D, 0x54, 0x69, 0x6C, 0x65, 0x73) // "PMTiles"
    private const val VERSION: Byte = 3
    private const val COMPRESSION_NONE: Byte = 1
    private const val CLUSTERED: Byte = 1

    /**
     * @param hasTileEntry when true, the root directory has one entry at tile id 0 with a
     *   run-length of 1,000,000,000 (comfortably larger than the id space at any zoom level this
     *   codebase uses), so [org.scottishtecharmy.soundscape.geoengine.utils.pmtiles.PmTilesReader.getTile]
     *   matches any zoom/x/y - i.e. the fixture "contains" any test location. When false, the root
     *   directory is empty and no location is ever matched.
     * @param paddingSize extra trailing bytes appended to the tile payload, only to vary the
     *   resulting file's size for "pick the largest extract" tests.
     */
    fun build(hasTileEntry: Boolean, paddingSize: Int = 0): ByteArray {
        val tileData = ByteArray(4 + paddingSize) { (it % 251).toByte() }

        val rootDir = if (hasTileEntry) {
            directoryBytes(
                ids = longArrayOf(0L),
                runLengths = longArrayOf(1_000_000_000L),
                lengths = longArrayOf(tileData.size.toLong()),
                offsetValues = longArrayOf(1L), // offsets[0] = value - 1 = 0
            )
        } else {
            directoryBytes(LongArray(0), LongArray(0), LongArray(0), LongArray(0))
        }

        val jsonMetadata = "{}".encodeToByteArray()

        val rootDirOffset = HEADER_LENGTH.toLong()
        val rootDirLength = rootDir.size.toLong()
        val jsonMetadataOffset = rootDirOffset + rootDirLength
        val jsonMetadataLength = jsonMetadata.size.toLong()
        val leafDirOffset = jsonMetadataOffset + jsonMetadataLength
        val leafDirLength = 0L
        val tileDataOffset = leafDirOffset + leafDirLength
        val tileDataLength = tileData.size.toLong()

        val header = ByteArray(HEADER_LENGTH)
        MAGIC.copyInto(header, 0)
        header[7] = VERSION
        writeLELong(header, 8, rootDirOffset)
        writeLELong(header, 16, rootDirLength)
        writeLELong(header, 24, jsonMetadataOffset)
        writeLELong(header, 32, jsonMetadataLength)
        writeLELong(header, 40, leafDirOffset)
        writeLELong(header, 48, leafDirLength)
        writeLELong(header, 56, tileDataOffset)
        writeLELong(header, 64, tileDataLength)
        writeLELong(header, 80, if (hasTileEntry) 1L else 0L) // tileEntries
        header[96] = CLUSTERED
        header[97] = COMPRESSION_NONE // internalCompression
        header[98] = COMPRESSION_NONE // tileCompression

        return header + rootDir + jsonMetadata + tileData
    }

    /** A file with the right length header but a bad magic number - fails at the very first read. */
    fun corruptMagic(): ByteArray {
        val bytes = build(hasTileEntry = true)
        bytes[0] = 0x00
        return bytes
    }

    /**
     * A structurally-valid header (correct magic, root directory and JSON metadata all present
     * and intact) whose declared tile-data section runs past the end of the (truncated) file.
     * Exercises the "front intact, tail corrupt" truncation case called out in isPmtilesUsable's
     * doc comment.
     */
    fun truncated(): ByteArray {
        val full = build(hasTileEntry = true, paddingSize = 100)
        return full.copyOf(full.size - 50)
    }

    private fun directoryBytes(
        ids: LongArray,
        runLengths: LongArray,
        lengths: LongArray,
        offsetValues: LongArray,
    ): ByteArray {
        val out = mutableListOf<Byte>()
        writeVarLong(out, ids.size.toLong())
        var lastId = 0L
        for (id in ids) {
            writeVarLong(out, id - lastId)
            lastId = id
        }
        for (r in runLengths) writeVarLong(out, r)
        for (l in lengths) writeVarLong(out, l)
        for (o in offsetValues) writeVarLong(out, o)
        return out.toByteArray()
    }

    private fun writeVarLong(out: MutableList<Byte>, value: Long) {
        var v = value
        while (true) {
            val b = (v and 0x7F).toInt()
            v = v ushr 7
            if (v != 0L) {
                out.add((b or 0x80).toByte())
            } else {
                out.add(b.toByte())
                return
            }
        }
    }

    private fun writeLELong(arr: ByteArray, offset: Int, value: Long) {
        for (i in 0 until 8) {
            arr[offset + i] = ((value shr (i * 8)) and 0xFF).toByte()
        }
    }
}
