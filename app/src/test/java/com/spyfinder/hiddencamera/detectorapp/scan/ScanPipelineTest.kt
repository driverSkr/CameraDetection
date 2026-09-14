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
            ScanPipeline({ 0L }, 33, 45, { false }, 1, 1).run(2, {}, { index ->
                if (index == 0) queue.add("camera")
                else { analyzed.await(); discoveryFinished = true }
            }, { queue.poll() }, {
                assertFalse(discoveryFinished)
                analyzed.complete(Unit)
            })
        }
        assertTrue(discoveryFinished)
    }
    @Test fun stopsAddingDiscoveryWhileAnalysisStillHasBudget() = runBlocking {
        val queue = ConcurrentLinkedQueue<String>()
        var clock = 0L
        var probes = 0
        var analyses = 0
        ScanPipeline({ clock }, 33, 45, { false }, 1, 1).run(100, {}, {
            probes++; clock += 20; queue.add("device$probes")
        }, { queue.poll() }, { analyses++ })
        assertEquals(2, probes)
        assertEquals(2, analyses)
    }
    @Test fun cancellationJoinsWorkers() = runBlocking {
        val entered = CompletableDeferred<Unit>()
        val live = AtomicInteger()
        val task = launch {
            ScanPipeline({ 0L }, 33, 45, { false }, 2, 1).run(100, {}, {
                live.incrementAndGet()
                try { entered.complete(Unit); awaitCancellation() } finally { live.decrementAndGet() }
            }, { null }, {})
        }
        entered.await()
        task.cancelAndJoin()
        assertEquals(0, live.get())
    }
}
