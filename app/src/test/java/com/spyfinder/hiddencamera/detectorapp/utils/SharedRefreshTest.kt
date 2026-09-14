package com.spyfinder.hiddencamera.detectorapp.utils

import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Test

class SharedRefreshTest {
    @Test fun ordinaryCallersShareOneQuery() = runBlocking {
        val scope = CoroutineScope(SupervisorJob() + coroutineContext.minusKey(Job))
        try {
            val refresh = SharedRefresh<Int>(scope)
            val ready = CompletableDeferred<Unit>()
            var count = 0
            val tasks = List(10) { async { refresh.run { count++; ready.await(); 7 } } }
            yield(); yield()
            ready.complete(Unit)
            assertEquals(List(10) { 7 }, tasks.awaitAll())
            assertEquals(1, count)
        } finally { scope.cancel() }
    }
    @Test fun freshCallersQueueOnlyOneSuccessorToAnOldQuery() = runBlocking {
        val scope = CoroutineScope(SupervisorJob() + coroutineContext.minusKey(Job))
        try {
            val refresh = SharedRefresh<Int>(scope)
            val ready = CompletableDeferred<Unit>()
            val entered = CompletableDeferred<Unit>()
            var count = 0
            val old = async { refresh.run { count++; entered.complete(Unit); ready.await(); 1 } }
            entered.await()
            val fresh = List(5) { async { refresh.run(true) { count++; 2 } } }
            yield()
            ready.complete(Unit)
            assertEquals(1, old.await())
            assertEquals(List(5) { 2 }, fresh.awaitAll())
            assertEquals(2, count)
        } finally { scope.cancel() }
    }
    @Test fun cancelledCallerDoesNotCancelSharedQuery() = runBlocking {
        val scope = CoroutineScope(SupervisorJob() + coroutineContext.minusKey(Job))
        try {
            val refresh = SharedRefresh<Int>(scope)
            val ready = CompletableDeferred<Unit>()
            val entered = CompletableDeferred<Unit>()
            val first = async { refresh.run { entered.complete(Unit); ready.await(); 9 } }
            entered.await()
            val second = async { refresh.run { error("must share") } }
            yield()
            first.cancelAndJoin()
            ready.complete(Unit)
            assertEquals(9, second.await())
        } finally { scope.cancel() }
    }
    @Test fun failureDoesNotPoisonFutureRefreshes() = runBlocking {
        val scope = CoroutineScope(SupervisorJob() + coroutineContext.minusKey(Job))
        try {
            val refresh = SharedRefresh<Int>(scope)
            assertTrue(runCatching { refresh.run { error("offline") } }.isFailure)
            assertEquals(3, refresh.run { 3 })
        } finally { scope.cancel() }
    }
}
