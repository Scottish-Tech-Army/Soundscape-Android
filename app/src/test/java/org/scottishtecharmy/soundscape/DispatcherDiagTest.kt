package org.scottishtecharmy.soundscape

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.junit.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class DispatcherDiagTest {
    @Test
    fun ioDispatcherRuns() {
        val latch = CountDownLatch(1)
        CoroutineScope(Dispatchers.IO).launch {
            println("DIAG: IO coroutine ran")
            latch.countDown()
        }
        val ok = latch.await(5, TimeUnit.SECONDS)
        println("DIAG: awaited=$ok")
    }
}
