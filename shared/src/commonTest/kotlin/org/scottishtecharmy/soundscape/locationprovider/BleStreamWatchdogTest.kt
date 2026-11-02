package org.scottishtecharmy.soundscape.locationprovider

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.currentTime
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class BleStreamWatchdogTest {

    @Test
    fun aSteadyStreamIsCollectedUntilItStops() = runTest {
        val received = mutableListOf<Int>()
        // 100 notifications 10 ms apart, then silence
        val stream = flow {
            repeat(100) {
                emit(byteArrayOf(it.toByte()))
                delay(10)
            }
            delay(Long.MAX_VALUE)
        }
        assertFailsWith<StreamSilentException> {
            collectUntilSilent(stream, 1_500) { received.add(it[0].toInt()) }
        }
        assertEquals((0 until 100).toList(), received)
        // The last notification came at 990 ms, so the stream is given up 1.5 s after that
        assertEquals(990 + 1_500L, currentTime)
    }

    @Test
    fun aGapShorterThanTheLimitIsAllowed() = runTest {
        val received = mutableListOf<Int>()
        val stream = flow {
            emit(byteArrayOf(1))
            delay(1_400)
            emit(byteArrayOf(2))
            delay(Long.MAX_VALUE)
        }
        assertFailsWith<StreamSilentException> {
            collectUntilSilent(stream, 1_500) { received.add(it[0].toInt()) }
        }
        assertEquals(listOf(1, 2), received)
        assertEquals(1_400 + 1_500L, currentTime)
    }
}
