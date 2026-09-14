package com.spyfinder.hiddencamera.detectorapp.utils

import kotlinx.coroutines.*

/** Caller cancellation does not cancel a shared query. A fresh request queues at most one successor. */
class SharedRefresh<T>(private val scope: CoroutineScope) {
    private val lock = Any()
    private var current: Deferred<T>? = null
    private var next: Deferred<T>? = null

    suspend fun run(fresh: Boolean = false, query: suspend () -> T): T {
        val task = synchronized(lock) {
            if (current?.isCompleted == true) { current = next; next = null }
            val active = current?.takeUnless { it.isCompleted }
            if (active != null && !fresh) return@synchronized next ?: active
            if (active != null && next != null) return@synchronized next!!
            val predecessor = active
            val created = scope.async(start = CoroutineStart.LAZY) {
                if (predecessor != null) {
                    try { predecessor.await() } catch (e: Exception) { currentCoroutineContext().ensureActive() }
                }
                query()
            }
            if (active == null) current = created else next = created
            created.invokeOnCompletion {
                synchronized(lock) {
                    if (current === created) { current = next; next = null }
                    else if (next === created) next = null
                }
            }
            created.start()
            created
        }
        return task.await()
    }
}
