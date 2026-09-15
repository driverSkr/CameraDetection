package com.spyfinder.hiddencamera.detectorapp.scan

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit

/** One session-wide socket budget shared by every device, including late service announcements. */
class ServiceProbeRunner(concurrency: Int = 8) {
    private val permits = Semaphore(concurrency)
    suspend fun <T, R> run(probes: List<T>, probe: (T) -> R): List<R> = coroutineScope {
        probes.map { item -> async(Dispatchers.IO) { permits.withPermit { probe(item) } } }.awaitAll()
    }
}
