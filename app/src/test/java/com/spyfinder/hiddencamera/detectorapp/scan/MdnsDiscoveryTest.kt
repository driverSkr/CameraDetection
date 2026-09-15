package com.spyfinder.hiddencamera.detectorapp.scan

import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import org.junit.Assert.*
import org.junit.Test

class MdnsDiscoveryTest {
    @Test fun printerAndPlaybackServicesAreNeverCameraEvidence() {
        listOf("_ipp._tcp.local", "_googlecast._tcp.local", "_airplay._tcp.local").forEach { service ->
            val instance = "Room.$service"
            val discovery = MdnsDiscovery()
            val server = ByteArrayOutputStream().also { bytes ->
                DataOutputStream(bytes).apply { writeShort(0); writeShort(0); writeShort(8008); write(name("hallcam.local")) }
            }.toByteArray()
            val response = packet(record(service, 12, name(instance)), record(instance, 33, server), address())
            discovery.accept(response)
            val endpoint = discovery.endpoints("192.168.2.1", 24).single()
            assertFalse(endpoint.evidence.cameraRelated)
            assertFalse(DiscoveryProtocols.mdnsEvidence(response).any { it.cameraRelated })
            assertNotNull(endpoint.details["mdns_device_type"])
        }
    }
    @Test fun lostServiceResponseIsRetriedAndResolvedWithoutFlooding() {
        var clock = 0L
        val discovery = MdnsDiscovery(now = { clock })
        discovery.accept(packet(ptr()))
        assertEquals(1, discovery.questions().size)
        repeat(10) { assertTrue(discovery.questions().isEmpty()) }
        clock = 500
        assertEquals(1, discovery.questions().size)
        discovery.accept(packet(srv()))
        assertEquals(listOf(MdnsPacket.Question("hallcam.local", 1)), discovery.questions())
        clock = 1000
        assertEquals(1, discovery.questions().size)
        discovery.accept(packet(address()))
        clock = 2000
        assertTrue(discovery.questions().isEmpty())
        assertEquals(1, discovery.endpoints("192.168.2.1", 24).size)
    }

    @Test fun unansweredServiceStopsAfterThreeAttempts() {
        var clock = 0L
        val discovery = MdnsDiscovery(now = { clock })
        discovery.accept(packet(ptr()))
        repeat(3) { assertEquals(1, discovery.questions().size); clock += 500 }
        assertTrue(discovery.questions().isEmpty())
        assertEquals(1, discovery.unresolved("192.168.2.1", 24))
    }
    @Test fun literalDotsInAnInstanceStayInsideTheDnsLabelWhenResolving() {
        val wire = ByteArrayOutputStream().also { bytes ->
            DataOutputStream(bytes).apply { writeByte(8); writeBytes("hall.cam"); write(name("_rtsp._tcp.local")) }
        }.toByteArray()
        val discovery = MdnsDiscovery()
        discovery.accept(packet(record("_rtsp._tcp.local", 12, wire)))
        val question = discovery.questions().single()
        assertEquals("hall\\.cam._rtsp._tcp.local", question.name)
        val query = MdnsPacket.query(listOf(question))
        assertArrayEquals(wire, query.copyOfRange(12, 12 + wire.size))
        assertEquals("hall.cam._rtsp._tcp.local", MdnsPacket.displayName(question.name))
    }
    @Test fun advertisedEndpointsMustBeUsableHosts() {
        assertFalse(ScanRules.usableHost("192.168.2.0", "192.168.2.1", 24))
        assertFalse(ScanRules.usableHost("192.168.2.255", "192.168.2.1", 24))
        assertFalse(ScanRules.usableHost("127.0.0.1", "192.168.2.1", 0))
        assertFalse(ScanRules.usableHost("224.0.0.1", "192.168.2.1", 0))
        assertTrue(ScanRules.usableHost("192.168.2.0", "192.168.2.1", 31))
        assertTrue(ScanRules.usableHost("192.168.2.1", "192.168.2.1", 32))
    }
    private fun name(value: String): ByteArray = ByteArrayOutputStream().also { bytes ->
        DataOutputStream(bytes).apply { value.split('.').forEach { writeByte(it.length); writeBytes(it) }; writeByte(0) }
    }.toByteArray()
    private fun record(owner: String, type: Int, data: ByteArray, ttl: Int = 120): ByteArray = ByteArrayOutputStream().also { bytes ->
        DataOutputStream(bytes).apply { write(name(owner)); writeShort(type); writeShort(1); writeInt(ttl); writeShort(data.size); write(data) }
    }.toByteArray()
    private fun packet(vararg records: ByteArray): ByteArray = ByteArrayOutputStream().also { bytes ->
        DataOutputStream(bytes).apply { writeShort(0); writeShort(0x8400); writeShort(0); writeShort(records.size); writeInt(0); records.forEach { write(it) } }
    }.toByteArray()
    private val instance = "HallCam._rtsp._tcp.local"
    private fun ptr(ttl: Int = 120) = record("_rtsp._tcp.local", 12, name(instance), ttl)
    private fun srv() = record(instance, 33, ByteArrayOutputStream().also { bytes ->
        DataOutputStream(bytes).apply { writeShort(0); writeShort(0); writeShort(9554); write(name("hallcam.local")) }
    }.toByteArray())
    private fun address(first: Int = 192) = record("hallcam.local", 1, byteArrayOf(first.toByte(), 168.toByte(), 2, 99))

    @Test fun resolvesAdvertisedHostAndNonstandardPortInsteadOfProxySender() {
        val discovery = MdnsDiscovery()
        discovery.accept(packet(ptr(), srv(), address()))
        val endpoint = discovery.endpoints("192.168.2.1", 24).single()
        assertEquals("192.168.2.99", endpoint.ip)
        assertEquals(ServiceProbe(9554, "RTSP"), endpoint.probe)
        assertEquals("hallcam", endpoint.details["mdns_name"])
        assertEquals("hallcam.local", endpoint.details["mdns_host"])
        assertTrue(endpoint.evidence.cameraRelated)
        assertEquals(0, discovery.unresolved("192.168.2.1", 24))
    }
    @Test fun joinsSplitPacketsAndQueriesOnlyMissingRecords() {
        val discovery = MdnsDiscovery()
        discovery.accept(packet(ptr()))
        assertEquals(listOf(MdnsPacket.Question(instance.lowercase(), 33)), discovery.questions())
        assertTrue(discovery.questions().isEmpty())
        assertTrue(discovery.endpoints("192.168.2.1", 24).isEmpty())
        discovery.accept(packet(srv()))
        assertEquals(listOf(MdnsPacket.Question("hallcam.local", 1)), discovery.questions())
        discovery.accept(packet(address()))
        assertEquals(1, discovery.endpoints("192.168.2.1", 24).size)
    }
    @Test fun doesNotScanOutsideTheSelectedSubnetOrAttributeUnresolvedServices() {
        val discovery = MdnsDiscovery()
        discovery.accept(packet(ptr(), srv(), address(10)))
        assertTrue(discovery.endpoints("192.168.2.1", 24).isEmpty())
        assertEquals(1, discovery.unresolved("192.168.2.1", 24))
    }
    @Test fun goodbyeRemovesAdvertisedService() {
        val discovery = MdnsDiscovery()
        discovery.accept(packet(ptr(), srv(), address()))
        discovery.accept(packet(ptr(ttl = 0)))
        assertTrue(discovery.endpoints("192.168.2.1", 24).isEmpty())
    }
    @Test fun ipv6AdditionalRecordStillRequestsAnIpv4Address() {
        val discovery = MdnsDiscovery()
        discovery.accept(packet(ptr(), srv(), record("hallcam.local", 28, ByteArray(16).also { it[15] = 1 })))
        assertTrue(discovery.endpoints("192.168.2.1", 24).isEmpty())
        assertEquals(listOf(MdnsPacket.Question("hallcam.local", 1)), discovery.questions())
    }
    @Test fun rejectsRdataThatRunsIntoTheFollowingRecord() {
        val bytes = ptr()
        val lengthPosition = name("_rtsp._tcp.local").size + 8
        bytes[lengthPosition] = 0; bytes[lengthPosition + 1] = 1
        assertTrue(MdnsPacket.records(packet(bytes, srv(), address())).isEmpty())
    }
    @Test fun boundsTheSessionCache() {
        val discovery = MdnsDiscovery(limit = 1)
        discovery.accept(packet(ptr(), srv(), address()))
        assertTrue(discovery.limited)
        assertTrue(discovery.endpoints("192.168.2.1", 24).isEmpty())
    }
}
