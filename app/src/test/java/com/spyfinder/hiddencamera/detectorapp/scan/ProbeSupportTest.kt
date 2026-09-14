package com.spyfinder.hiddencamera.detectorapp.scan

import org.junit.Assert.*
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.util.concurrent.CancellationException

class ProbeSupportTest {
    @Test fun onlyExplicitRefusalCountsAsRefusal() {
        assertEquals(ProbeResult.REFUSED, ProbeSupport.classify(ConnectException(), 111, false))
        listOf(101, 113, 99).forEach { assertEquals(ProbeResult.UNREACHABLE, ProbeSupport.classify(ConnectException(), it, false)) }
        assertEquals(ProbeResult.UNKNOWN, ProbeSupport.classify(ConnectException("Connection refused"), null, false))
        assertEquals(ProbeResult.TIMEOUT, ProbeSupport.classify(SocketTimeoutException(), null, false))
        assertEquals(ProbeResult.CANCELLED, ProbeSupport.classify(IOException(), null, true))
    }
    @Test fun videoPort8554CanBeDiscoveredWithoutHttpOrMulticast() {
        val visited = mutableListOf<Int>()
        val result = ProbeSupport.discover({ true }) { port ->
            visited.add(port)
            if (port == 8554) ProbeResult.OPEN else ProbeResult.TIMEOUT
        }
        assertTrue(result.responded); assertTrue(result.checked)
        assertEquals(listOf(554, 8554), visited)
    }
    @Test fun budgetAndRouteFailureDoNotBecomeNegativeCompleteProbes() {
        assertFalse(ProbeSupport.discover({ false }) { ProbeResult.OPEN }.checked)
        val failed = ProbeSupport.discover({ true }) { ProbeResult.UNREACHABLE }
        assertFalse(failed.responded); assertTrue(failed.uncertain)
    }
    @Test(expected = CancellationException::class) fun cancellationPropagates() {
        ProbeSupport.discover({ true }) { ProbeResult.CANCELLED }
    }
    private fun fragmented(text: String) = object : ByteArrayInputStream(text.toByteArray()) {
        override fun read(b: ByteArray, off: Int, len: Int) = super.read(b, off, minOf(1, len))
    }
    @Test fun statusLineMayArriveOneByteAtATime() {
        listOf("200 OK", "401 Unauthorized").forEach { status ->
            val line = ProbeSupport.readStatusLine(fragmented("RTSP/1.0 $status\r\nCSeq: 1\r\n"), 100, { 0 }, {}, { false })
            assertTrue(DiscoveryProtocols.isRtsp(line))
        }
        val http = ProbeSupport.readStatusLine(fragmented("HTTP/1.1 200 OK\r\nRTSP/1.0 200 OK"), 100, { 0 }, {}, { false })
        assertFalse(DiscoveryProtocols.isRtsp(http))
    }
    @Test(expected = IOException::class) fun truncatedStatusIsIncomplete() {
        ProbeSupport.readStatusLine(fragmented("RTSP/1."), 100, { 0 }, {}, { false })
    }
    @Test(expected = IOException::class) fun oversizedStatusIsBounded() {
        ProbeSupport.readStatusLine(fragmented("a".repeat(30)), 100, { 0 }, {}, { false }, 16)
    }
    @Test(expected = SocketTimeoutException::class) fun slowFragmentsCannotResetDeadline() {
        var clock = 0L
        ProbeSupport.readStatusLine(fragmented("RTSP/1.0 200 OK\r\n"), 5, { clock }, { clock++ }, { false })
    }
    @Test(expected = CancellationException::class) fun cancelledReadStopsImmediately() {
        ProbeSupport.readStatusLine(fragmented("RTSP/1.0 200 OK\r\n"), 100, { 0 }, {}, { true })
    }
}
