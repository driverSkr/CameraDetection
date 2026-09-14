package com.spyfinder.hiddencamera.detectorapp.scan

import kotlinx.coroutines.*
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

/** Bounded concurrency; finish all queued work instead of cutting off a scan by elapsed time. */
class ScanPipeline(private val closed: () -> Boolean,
                   private val discoveryWorkers: Int = 32, private val analysisWorkers: Int = 8) {
    suspend fun run(count: Int, multicast: suspend () -> Unit, discover: suspend (Int) -> Unit,
                    nextDevice: () -> String?, analyze: suspend (String) -> Unit) = coroutineScope {
        val done = AtomicBoolean(false)
        val discovery = launch {
            try {
                coroutineScope {
                    launch { multicast() }
                    val next = AtomicInteger()
                    repeat(minOf(discoveryWorkers, count)) {
                        launch {
                            while (isActive && !closed()) {
                                val index = next.getAndIncrement()
                                if (index >= count) break
                                discover(index)
                            }
                        }
                    }
                }
            } finally { done.set(true) }
        }
        val consumers = List(analysisWorkers) {
            launch {
                while (isActive && !closed()) {
                    val device = nextDevice()
                    if (device != null) analyze(device)
                    else if (done.get()) {
                        // Publication can race the first poll and the producer's done flag.
                        val last = nextDevice() ?: break
                        analyze(last)
                    }
                    else delay(20)
                }
            }
        }
        discovery.join()
        consumers.joinAll()
    }
}
