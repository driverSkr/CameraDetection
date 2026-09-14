package com.spyfinder.hiddencamera.detectorapp.scan

import java.io.Closeable
import java.util.concurrent.CancellationException

/** Closing a session also closes blocking network operations, not only their UI callbacks. */
class ScanResources : Closeable {
    private val resources = mutableSetOf<Closeable>()
    @Volatile var closed = false
        private set
    @Synchronized fun <T : Closeable> track(resource: T): T {
        if (closed) {
            runCatching { resource.close() }
            throw CancellationException("Scan stopped")
        }
        resources.add(resource)
        return resource
    }
    @Synchronized fun release(resource: Closeable) {
        resources.remove(resource)
        runCatching { resource.close() }
    }
    @Synchronized override fun close() {
        closed = true
        resources.forEach { runCatching { it.close() } }
        resources.clear()
    }
}
