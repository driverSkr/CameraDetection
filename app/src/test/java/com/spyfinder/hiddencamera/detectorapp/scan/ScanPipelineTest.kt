package com.spyfinder.hiddencamera.detectorapp.scan

import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Test
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicInteger

class ScanPipelineTest {
    @Test fun analyzesBeforeDiscoveryHasFinished() = runBlocking {
        val queue = ConcurrentLinkedQueue<String>()
        val analyzed = CompletableDeferred<Unit>()
        var discoveryFinished = false
        withTimeout(2000) {
            ScanPipeline({ false }, 1, 1).run(2, {}, { index ->
                if (index == 0) queue.add("camera")
                else { analyzed.await(); discoveryFinished = true }
            }, { queue.poll() }, {
                assertFalse(discoveryFinished)
                analyzed.complete(Unit)
            })
        }
        assertTrue(discoveryFinished)
    }
    @Test fun longNetworkScanDrainsEveryAddressAndQueuedDevice() = runBlocking {
        val queue = ConcurrentLinkedQueue<String>()
        var simulatedMillis = 0L
        var probes = 0
        var analyses = 0
        val targets = ScanRules.targets("192.168.2.51", 21, "192.168.0.1")
        assertEquals(2046, targets.addresses.size)
        assertFalse(targets.limited)
        ScanPipeline({ false }, 1, 1).run(targets.addresses.size, {}, {
            probes++; simulatedMillis += 450 * ProbeSupport.ports.size
            queue.add("device$probes")
            yield()
        }, { queue.poll() }, { analyses++; simulatedMillis += 1500 })
        assertTrue(simulatedMillis > 45_000)
        assertEquals(2046, probes)
        assertEquals(2046, analyses)
        assertTrue(queue.isEmpty())
    }
    @Test fun cancellationJoinsWorkers() = runBlocking {
        val entered = CompletableDeferred<Unit>()
        val live = AtomicInteger()
        val task = launch {
            ScanPipeline({ false }, 2, 1).run(100, {}, {
                live.incrementAndGet()
                try { entered.complete(Unit); awaitCancellation() } finally { live.decrementAndGet() }
            }, { null }, {})
        }
        entered.await()
        task.cancelAndJoin()
        assertEquals(0, live.get())
    }
}
