package org.scottishtecharmy.soundscape

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.scottishtecharmy.soundscape.utils.isPmtilesUsable
import java.io.File
import java.io.RandomAccessFile
import java.nio.file.Files
import java.nio.file.StandardCopyOption

/**
 * JVM unit tests (not instrumented) for [isPmtilesUsable], the gate that stops a corrupt or
 * truncated .pmtiles extract from being handed to MapLibre - which would otherwise abort the
 * process natively with "std::runtime_error: unknown compression method".
 *
 * Each test takes a full copy of a real extract and corrupts it in a specific way to prove a
 * specific layer of the validation rejects it:
 *  - [truncatedExtractIsRejected]      -> the header-declared layout no longer fits the file
 *  - [lastTileBytesZeroedIsRejected]   -> the last tile (physical end of the file) won't decompress
 *  - [leafDirectoryCorruptedIsRejected]-> a child (leaf) directory on the descent path won't decompress
 *
 * The fixtures under src/test/res are developer-local and not committed, so every test is
 * skipped (assumeTrue) when the fixture is absent (e.g. on CI).
 */
class PmtilesValidationTest {

    private val fixture =
        File("src/test/res/org/scottishtecharmy/soundscape/bristol-gb.pmtiles")

    // PMTiles v3 header field offsets (fixed little-endian layout).
    private val leafDirOffsetField = 40L
    private val leafDirLengthField = 48L

    /** Take a fresh, writable full copy of the fixture in a temp file. */
    private fun copyOfFixture(): File {
        val copy = File.createTempFile("pmtiles-test", ".pmtiles")
        Files.copy(fixture.toPath(), copy.toPath(), StandardCopyOption.REPLACE_EXISTING)
        return copy
    }

    private fun readHeaderLong(file: File, offset: Long): Long {
        RandomAccessFile(file, "r").use { raf ->
            val bytes = ByteArray(8)
            raf.seek(offset)
            raf.readFully(bytes)
            var value = 0L
            for (i in 7 downTo 0) value = (value shl 8) or (bytes[i].toLong() and 0xff)
            return value
        }
    }

    /** Overwrite [length] bytes at [offset] with zeros. */
    private fun zeroBytes(file: File, offset: Long, length: Int) {
        RandomAccessFile(file, "rw").use { raf ->
            raf.seek(offset)
            raf.write(ByteArray(length))
        }
    }

    /** A valid extract must pass - this also exercises the full header -> metadata -> last-tile path. */
    @Test
    fun validExtractIsUsable() {
        assumeTrue("fixture not present", fixture.exists())
        assertTrue(isPmtilesUsable(fixture.path))
    }

    /** An unmodified copy must also pass (guards the copy/corrupt machinery itself). */
    @Test
    fun unmodifiedCopyIsUsable() {
        assumeTrue("fixture not present", fixture.exists())
        val copy = copyOfFixture()
        try {
            assertTrue(isPmtilesUsable(copy.path))
        } finally {
            copy.delete()
        }
    }

    /** Truncating even 100 bytes makes the header-declared tile-data section overrun the file. */
    @Test
    fun truncatedExtractIsRejected() {
        assumeTrue("fixture not present", fixture.exists())
        val copy = copyOfFixture()
        try {
            RandomAccessFile(copy, "rw").use { it.setLength(copy.length() - 100) }
            assertFalse(isPmtilesUsable(copy.path))
        } finally {
            copy.delete()
        }
    }

    /**
     * Zero the last 100 bytes while keeping the correct length. The header, root dir, metadata and
     * leaf directories at the front are untouched, so this is only caught by decompressing the last
     * tile - which sits at the physical end of the file (tile data ends at EOF) and whose gzip
     * stream is now corrupt. This is the exact case the old metadata-only check missed.
     */
    @Test
    fun lastTileBytesZeroedIsRejected() {
        assumeTrue("fixture not present", fixture.exists())
        val copy = copyOfFixture()
        try {
            zeroBytes(copy, copy.length() - 100, 100)
            assertTrue(copy.length() == fixture.length()) // length unchanged
            assertFalse(isPmtilesUsable(copy.path))
        } finally {
            copy.delete()
        }
    }

    /**
     * The result is cached per path, but must be re-validated when the file changes. Validate a
     * good copy (caching "usable"), then truncate it on the same path and confirm it is now
     * rejected - proving the cache is invalidated by the change rather than returning the stale pass.
     */
    @Test
    fun cachedResultIsInvalidatedWhenFileChanges() {
        assumeTrue("fixture not present", fixture.exists())
        val copy = copyOfFixture()
        try {
            assertTrue(isPmtilesUsable(copy.path)) // caches "usable" for this path
            RandomAccessFile(copy, "rw").use { it.setLength(copy.length() - 100) }
            assertFalse(isPmtilesUsable(copy.path)) // length changed -> re-validated, now rejected
        } finally {
            copy.delete()
        }
    }

    /**
     * Corrupt a child (leaf) directory. The descent reads the last leaf directory, which in this
     * clustered archive sits at the end of the leaf-directory section (leafDirOffset+leafDirLength).
     * Zeroing the end of that section breaks its gzip stream, so the descent fails before it ever
     * reaches a tile.
     */
    @Test
    fun leafDirectoryCorruptedIsRejected() {
        assumeTrue("fixture not present", fixture.exists())
        val copy = copyOfFixture()
        try {
            val leafDirEnd =
                readHeaderLong(copy, leafDirOffsetField) + readHeaderLong(copy, leafDirLengthField)
            assumeTrue("fixture has no leaf directories", readHeaderLong(copy, leafDirLengthField) >= 100)
            zeroBytes(copy, leafDirEnd - 100, 100)
            assertFalse(isPmtilesUsable(copy.path))
        } finally {
            copy.delete()
        }
    }
}
