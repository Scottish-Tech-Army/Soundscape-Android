package org.scottishtecharmy.soundscape.logging

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class CappedLineBufferTest {

    // Each line costs its length plus one for the newline it's saved with
    private fun savedSize(lines: List<String>) = lines.sumOf { it.length + 1 }

    @Test
    fun holdsEverythingThatFits() {
        val buffer = CappedLineBuffer(12)
        assertEquals(emptyList(), buffer.snapshot())
        buffer.add("aaa")
        buffer.add("bbb")
        buffer.add("ccc")
        assertEquals(listOf("aaa", "bbb", "ccc"), buffer.snapshot())
    }

    @Test
    fun dropsWholeLinesFromTheFront() {
        val buffer = CappedLineBuffer(12)
        buffer.add("aaa")
        buffer.add("bbb")
        buffer.add("ccc")
        // 4 more characters: "aaa" goes, the rest stay whole
        buffer.add("dd")
        assertEquals(listOf("bbb", "ccc", "dd"), buffer.snapshot())
        // A long line can push out several short ones
        buffer.add("eeeeeeee")
        assertEquals(listOf("dd", "eeeeeeee"), buffer.snapshot())
    }

    @Test
    fun shortLinesMeanMoreOfThem() {
        val buffer = CappedLineBuffer(100)
        for (i in 0 until 100) buffer.add("x")
        assertEquals(50, buffer.snapshot().size)
        for (i in 0 until 100) buffer.add("x".repeat(19))
        assertEquals(5, buffer.snapshot().size)
    }

    @Test
    fun aLineLongerThanTheBudgetIsCutShortNotDropped() {
        val buffer = CappedLineBuffer(40)
        buffer.add("before")
        buffer.add("y".repeat(100))
        val lines = buffer.snapshot()
        assertEquals(1, lines.size)
        assertEquals(39, lines[0].length)
        assertTrue(lines[0].endsWith(" [truncated]"), lines[0])
    }

    @Test
    fun truncatesWithoutTheMarkerWhenTheBudgetIsTiny() {
        val buffer = CappedLineBuffer(4)
        buffer.add("abcdefgh")
        assertEquals(listOf("abc"), buffer.snapshot())
    }

    @Test
    fun clearDropsWhatWasThereButNotWhatComesAfter() {
        val buffer = CappedLineBuffer(12)
        buffer.add("aaa")
        buffer.add("bbb")
        buffer.clear()
        assertEquals(emptyList(), buffer.snapshot())
        buffer.add("ccc")
        buffer.add("ddd")
        buffer.add("eee")
        assertEquals(listOf("ccc", "ddd", "eee"), buffer.snapshot())
    }

    @Test
    fun rejectsAnEmptyBudget() {
        assertFailsWith<IllegalArgumentException> { CappedLineBuffer(0) }
    }

    /**
     * Writers on several threads, with snapshots taken while they run: each writer's lines must
     * come out in the order it wrote them, none repeated, never over budget - and once the writers
     * have finished, the buffer is as full as whole lines allow.
     */
    @Test
    fun concurrentWritersNeverReorderDuplicateOrOverflow() {
        val maxChars = 5_000
        val writers = 8
        val linesEach = 5_000
        val buffer = CappedLineBuffer(maxChars)
        val snapshots = mutableListOf<List<String>>()
        runBlocking(Dispatchers.Default) {
            val jobs = (0 until writers).map { writer ->
                launch { for (i in 0 until linesEach) buffer.add("$writer:$i") }
            }
            while (jobs.any { it.isActive }) snapshots += buffer.snapshot()
        }
        snapshots += buffer.snapshot()

        for (snapshot in snapshots) {
            assertTrue(savedSize(snapshot) <= maxChars, "snapshot of ${savedSize(snapshot)} chars")
            assertEquals(snapshot.size, snapshot.toSet().size, "a line was repeated")
            val lastByWriter = IntArray(writers) { -1 }
            for (line in snapshot) {
                val (writer, i) = line.split(":").map { it.toInt() }
                assertTrue(i > lastByWriter[writer], "writer $writer out of order at $i")
                lastByWriter[writer] = i
            }
        }
        // Full to within one line: the next line out would have fitted otherwise
        val last = snapshots.last()
        assertTrue(savedSize(last) > maxChars - 8, "only ${savedSize(last)} chars held")
    }
}
