package com.spyfinder.hiddencamera.detectorapp.scan

import org.junit.Assert.*
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream

class DiscoveryProtocolsTest {
    @Test fun httpAndPortNumbersCannotMasqueradeAsRtsp() {
        assertFalse(DiscoveryProtocols.isRtsp("HTTP/1.1 200 OK\r\nServer: RTSP/1.0"))
        assertTrue(DiscoveryProtocols.isRtsp("RTSP/1.0 401 Unauthorized\r\nCSeq: 1\r\n"))
        assertFalse(DiscoveryProtocols.isRtsp("RTSP/1.0 nonsense"))
    }
    @Test fun ssdpDoesNotInferCameraFromAdvertisingText() {
        val evidence = DiscoveryProtocols.ssdpEvidence("HTTP/1.1 200 OK\r\nST: upnp:rootdevice\r\nSERVER: camera\r\n")
        assertEquals(1, evidence.size)
        assertFalse(evidence.single().cameraRelated)
        assertTrue(DiscoveryProtocols.ssdpEvidence("not HTTP\r\nST: camera").isEmpty())
    }
    @Test fun malformedDnsDoesNotCrashOrProduceFindings() {
        assertTrue(DiscoveryProtocols.mdnsEvidence(byteArrayOf(1, 2)).isEmpty())
        // A compressed name pointer referring to itself must be rejected.
        val packet = byteArrayOf(0,0, -124,0, 0,0, 0,1, 0,0, 0,0, -64,12)
        assertTrue(DiscoveryProtocols.mdnsEvidence(packet).isEmpty())
    }
    @Test fun validMdnsVideoServiceProducesEvidence() {
        val bytes = ByteArrayOutputStream()
        val out = DataOutputStream(bytes)
        out.writeShort(0); out.writeShort(0x8400); out.writeShort(0); out.writeShort(1); out.writeInt(0)
        fun name(text: String) { text.split('.').forEach { out.writeByte(it.length); out.writeBytes(it) }; out.writeByte(0) }
        name("_rtsp._tcp.local")
        out.writeShort(12); out.writeShort(1); out.writeInt(120)
        out.writeShort(7); out.writeByte(4); out.writeBytes("Test"); out.writeShort(0xc00c)
        val evidence = DiscoveryProtocols.mdnsEvidence(bytes.toByteArray())
        assertEquals(1, evidence.size)
        assertTrue(evidence.single().cameraRelated)
    }
}
