package com.spyfinder.hiddencamera.detectorapp.scan

import java.net.ServerSocket
import java.net.Socket
import java.util.concurrent.CancellationException
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import org.junit.Assert.*
import org.junit.Test

class ScanResourcesTest {
    @Test fun cancelClosesBlockedSocketRead() {
        val executor = Executors.newSingleThreadExecutor()
        ServerSocket(0).use { server ->
            val session = ScanResources()
            val client = session.track(Socket("127.0.0.1", server.localPort))
            server.accept().use {
                val task = executor.submit<Boolean> { try { client.getInputStream().read(); false } catch (_: Exception) { true } }
                session.close()
                assertTrue(task.get(2, TimeUnit.SECONDS))
                assertTrue(client.isClosed)
            }
        }
        executor.shutdownNow()
    }
    @Test fun oldSessionCannotStartNewConnections() {
        val session = ScanResources()
        session.close()
        val socket = Socket()
        try { session.track(socket); fail("Expected cancellation") } catch (_: CancellationException) { assertTrue(socket.isClosed) }
    }
    @Test fun repeatedCancellationDoesNotAffectAnotherSession() {
        val old = ScanResources(); val next = ScanResources()
        val socket = next.track(Socket())
        old.close(); old.close()
        assertFalse(socket.isClosed)
        next.close(); assertTrue(socket.isClosed)
    }
}
