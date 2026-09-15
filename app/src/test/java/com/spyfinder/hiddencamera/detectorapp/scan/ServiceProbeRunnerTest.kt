package com.spyfinder.hiddencamera.detectorapp.scan

import kotlinx.coroutines.*
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import org.junit.Assert.*
import org.junit.Test

class ServiceProbeRunnerTest {
    @Test fun delayedPortBenchmarkRetainsEveryResult() = runBlocking {
        val ports = (1..5).toList()
        val serialStart = System.nanoTime()
        val serial = ports.map { Thread.sleep(40); it }
        val serialMs = (System.nanoTime() - serialStart) / 1_000_000
        val parallelStart = System.nanoTime()
        val parallel = ServiceProbeRunner().run(ports) { Thread.sleep(40); it }
        val parallelMs = (System.nanoTime() - parallelStart) / 1_000_000
        assertEquals(serial, parallel)
        println("Five simulated 40ms ports: serial=${serialMs}ms shared-budget=${parallelMs}ms; excludes real network/hardware.")
    }
    @Test fun tailDeviceUsesAllSlotsAndPreservesResultOrder() = runBlocking {
        val entered = CountDownLatch(4)
        val runner = ServiceProbeRunner(4)
        val results = withTimeout(5000) {
            runner.run((0..3).toList()) {
                entered.countDown()
                check(entered.await(2, TimeUnit.SECONDS)) { "Ports were serialized" }
                it * 2
            }
        }
        assertEquals(listOf(0, 2, 4, 6), results)
    }
    @Test fun concurrentDevicesShareOneBudget() = runBlocking {
        val runner = ServiceProbeRunner(3)
        val live = AtomicInteger()
        val peak = AtomicInteger()
        coroutineScope {
            List(8) { async { runner.run((1..5).toList()) {
                val count = live.incrementAndGet()
                peak.updateAndGet { maxOf(it, count) }
                try { Thread.sleep(10); it } finally { live.decrementAndGet() }
            } } }.awaitAll()
        }
        assertTrue(peak.get() <= 3)
        assertEquals(0, live.get())
    }
    @Test fun cancellingQueuedPortsReleasesBudgetForNextRun() = runBlocking {
        val runner = ServiceProbeRunner(1)
        val entered = CompletableDeferred<Unit>()
        val task = launch { runner.run((1..40).toList()) { entered.complete(Unit); Thread.sleep(30) } }
        entered.await()
        task.cancelAndJoin()
        assertEquals(listOf(7), withTimeout(1000) { runner.run(listOf(7)) { it } })
    }
}
