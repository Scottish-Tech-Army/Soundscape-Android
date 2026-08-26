package org.scottishtecharmy.soundscape.locationprovider

import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * CompositeHeadTrackingProvider hardcodes `Dispatchers.Default` for its
 * internal scope (rather than accepting an injectable dispatcher), so these
 * tests can't drive it with a virtual-time TestDispatcher. Instead they run
 * under a real `runBlocking` and poll ([awaitUntil]) for the async
 * `combine()`/arbitration effects to land, with a bounded real-time timeout
 * so a broken test fails fast instead of hanging.
 */
class CompositeHeadTrackingProviderTest {

    private val createdProviders = mutableListOf<CompositeHeadTrackingProvider>()

    private fun composite(children: List<HeadTrackingProvider>): CompositeHeadTrackingProvider =
        CompositeHeadTrackingProvider(children).also { createdProviders.add(it) }

    @AfterTest
    fun tearDown() {
        // Belt-and-braces: make sure no test leaves a background scan loop running.
        createdProviders.forEach { it.stop() }
        createdProviders.clear()
    }

    private suspend fun awaitUntil(timeoutMillis: Long = 2_000, condition: () -> Boolean) {
        withTimeout(timeoutMillis) {
            while (!condition()) {
                delay(5)
            }
        }
    }

    private class FakeHeadTrackingProvider : HeadTrackingProvider() {
        var startCalls = 0
            private set
        var stopCalls = 0
            private set
        var destroyCalls = 0
            private set

        override fun start() {
            startCalls++
            // Mirrors real providers: start scanning/connecting.
            if (mutableStatusFlow.value == HeadTrackingStatus.Inactive) {
                mutableStatusFlow.value = HeadTrackingStatus.Disconnected
            }
        }

        override fun stop() {
            stopCalls++
            mutableHeadHeadingFlow.value = null
            mutableStatusFlow.value = HeadTrackingStatus.Inactive
        }

        override fun destroy() {
            destroyCalls++
            super.destroy()
        }

        fun setStatus(status: HeadTrackingStatus) {
            mutableStatusFlow.value = status
        }

        fun setHeading(heading: HeadHeading?) {
            mutableHeadHeadingFlow.value = heading
        }
    }

    @Test
    fun startStartsAllChildren() = runBlocking {
        val a = FakeHeadTrackingProvider()
        val b = FakeHeadTrackingProvider()
        val provider = composite(listOf(a, b))

        provider.start()

        awaitUntil { a.startCalls >= 1 && b.startCalls >= 1 }
    }

    @Test
    fun startIsIdempotentWhileAlreadyRunning() = runBlocking {
        val a = FakeHeadTrackingProvider()
        val provider = composite(listOf(a))

        provider.start()
        awaitUntil { a.startCalls >= 1 }
        val callsAfterFirstStart = a.startCalls

        provider.start() // scope != null already -> should be a no-op
        delay(50)

        assertEquals(callsAfterFirstStart, a.startCalls)
    }

    @Test
    fun headHeadingFlowEmitsMostRecentByTimestampAcrossChildren() = runBlocking {
        val a = FakeHeadTrackingProvider()
        val b = FakeHeadTrackingProvider()
        val provider = composite(listOf(a, b))
        provider.start()

        a.setHeading(HeadHeading(degrees = 10.0, accuracyDegrees = 5.0, timestampMillis = 100L))
        b.setHeading(HeadHeading(degrees = 20.0, accuracyDegrees = 5.0, timestampMillis = 200L))
        awaitUntil { provider.headHeadingFlow.value?.timestampMillis == 200L }
        assertEquals(20.0, provider.headHeadingFlow.value?.degrees)

        // An older sample from A must not override B's newer one.
        a.setHeading(HeadHeading(degrees = 30.0, accuracyDegrees = 5.0, timestampMillis = 150L))
        delay(100)
        assertEquals(20.0, provider.headHeadingFlow.value?.degrees)

        // A genuinely newer sample from A should win.
        a.setHeading(HeadHeading(degrees = 40.0, accuracyDegrees = 5.0, timestampMillis = 300L))
        awaitUntil { provider.headHeadingFlow.value?.degrees == 40.0 }
    }

    @Test
    fun statusFlowReflectsHighestOrdinalAcrossChildren() = runBlocking {
        val a = FakeHeadTrackingProvider()
        val b = FakeHeadTrackingProvider()
        val provider = composite(listOf(a, b))
        provider.start()
        awaitUntil { provider.statusFlow.value != HeadTrackingStatus.Inactive }

        a.setStatus(HeadTrackingStatus.Disconnected)
        b.setStatus(HeadTrackingStatus.Calibrated)
        awaitUntil { provider.statusFlow.value == HeadTrackingStatus.Calibrated }

        b.setStatus(HeadTrackingStatus.Connected)
        awaitUntil { provider.statusFlow.value == HeadTrackingStatus.Connected }
    }

    @Test
    fun arbitrationStopsOtherScanningChildrenOnceOneConnectsAndRestartsThemOnDrop() = runBlocking {
        val a = FakeHeadTrackingProvider()
        val b = FakeHeadTrackingProvider()
        val provider = composite(listOf(a, b))
        provider.start()

        // Both children begin scanning (Inactive -> Disconnected) once started.
        awaitUntil {
            a.statusFlow.value == HeadTrackingStatus.Disconnected &&
                b.statusFlow.value == HeadTrackingStatus.Disconnected
        }
        assertEquals(0, a.stopCalls)
        assertEquals(0, b.stopCalls)

        // A wins the race and connects; B (still scanning) should be stopped.
        a.setStatus(HeadTrackingStatus.Connected)
        awaitUntil { b.stopCalls >= 1 }
        assertEquals(0, a.stopCalls, "The active child should never be stopped by arbitration")
        awaitUntil { b.statusFlow.value == HeadTrackingStatus.Inactive }

        // If A then drops the connection, B should be restarted so it can take over.
        a.setStatus(HeadTrackingStatus.Disconnected)
        awaitUntil { b.startCalls >= 2 }
        awaitUntil { b.statusFlow.value == HeadTrackingStatus.Disconnected }
    }

    @Test
    fun stopClearsCompositeStateAndStopsAllChildren() = runBlocking {
        val a = FakeHeadTrackingProvider()
        val b = FakeHeadTrackingProvider()
        val provider = composite(listOf(a, b))
        provider.start()
        awaitUntil { a.statusFlow.value != HeadTrackingStatus.Inactive }
        a.setHeading(HeadHeading(10.0, 5.0, 1L))
        awaitUntil { provider.headHeadingFlow.value != null }

        provider.stop()

        assertEquals(HeadTrackingStatus.Inactive, provider.statusFlow.value)
        assertNull(provider.headHeadingFlow.value)
        assertTrue(a.stopCalls >= 1)
        assertTrue(b.stopCalls >= 1)
    }

    @Test
    fun destroyStopsAndDestroysAllChildren() = runBlocking {
        val a = FakeHeadTrackingProvider()
        val b = FakeHeadTrackingProvider()
        val provider = composite(listOf(a, b))
        provider.start()
        awaitUntil { a.statusFlow.value != HeadTrackingStatus.Inactive }

        provider.destroy()

        assertEquals(1, a.destroyCalls)
        assertEquals(1, b.destroyCalls)
        assertEquals(HeadTrackingStatus.Inactive, provider.statusFlow.value)
    }
}
