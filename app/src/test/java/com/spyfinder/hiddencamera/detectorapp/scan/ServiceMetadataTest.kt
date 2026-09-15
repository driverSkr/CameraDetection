package com.spyfinder.hiddencamera.detectorapp.scan

import org.junit.Assert.*
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.IOException
import java.net.SocketTimeoutException

class ServiceMetadataTest {
    @Test fun statusReadPreservesHeadersAndStopsBeforeBody() {
        val input = ByteArrayInputStream("HTTP/1.1 200 OK\r\nServer: Example/2\r\n\r\nBODY".toByteArray()).buffered()
        val status = ProbeSupport.readLine(input, 100, { 0 }, {}, { false })
        val headers = ProbeSupport.readHeaders(input, 100, { 0 }, {}, { false })
        assertEquals("Example/2", ServiceMetadata.headers(status + headers)["server"])
        assertEquals('B'.code, input.read())
    }
    @Test(expected = IOException::class) fun excessiveHeadersAreBounded() {
        ProbeSupport.readHeaders(ByteArrayInputStream(("X: " + "a".repeat(250) + "\r\n").repeat(50).toByteArray()), 100, { 0 }, {}, { false })
    }
    @Test(expected = SocketTimeoutException::class) fun slowHeadersShareOneDeadline() {
        var clock = 0L
        ProbeSupport.readHeaders(ByteArrayInputStream("Server: slow\r\n\r\n".toByteArray()), 5, { clock++ }, {}, { false })
    }
    @Test fun ssdpMetadataDoesNotFollowUrlsOrClaimADeviceModel() {
        val values = ServiceMetadata.ssdp("HTTP/1.1 200 OK\r\nST: upnp:rootdevice\r\nSERVER: Linux UPnP/1.0\r\nLOCATION: http://outside.invalid/private\r\n\r\n")
        assertEquals(mapOf("ssdp_server" to "Linux UPnP/1.0", "ssdp_st" to "upnp:rootdevice"), values)
        assertTrue(ServiceMetadata.ssdp("bad").isEmpty())
    }
}
