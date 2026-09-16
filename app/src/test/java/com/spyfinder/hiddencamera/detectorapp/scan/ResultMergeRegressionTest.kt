package com.spyfinder.hiddencamera.detectorapp.scan

import org.junit.Assert.*
import org.junit.Test

class ResultMergeRegressionTest {
    @Test fun compatibleMediaCapabilitiesPreserveHardwareIdentityInEitherOrder() {
        val base = mapOf("identity_source" to "UPnP", "identity_manufacturer" to "Example", "identity_model" to "Smart TV")
        val reports = listOf("MediaRenderer", "MediaServer").map {
            base + ("upnp_type" to "urn:schemas-upnp-org:device:$it:1")
        }
        val merged = DeviceDescription.mergeReports(reports)
        assertEquals(merged, DeviceDescription.mergeReports(reports.reversed()))
        assertNull(merged["identity_conflict"])
        assertEquals("Likely television", DeviceIdentity.classify(false, false, merged, false).type)
        assertEquals(listOf("Media playback service", "Media server service"), DeviceIdentity.capabilities(merged))
    }

    @Test fun multipleCapabilitiesStillHonorExplicitHardwareDeclarations() {
        fun report(type: String) = mapOf("identity_source" to "UPnP", "upnp_type" to "urn:schemas-upnp-org:device:$type:1")
        val printer = DeviceDescription.mergeReports(listOf(report("Printer"), report("MediaServer")))
        assertEquals("Printer", DeviceIdentity.classify(false, false, printer, false).type)
        val conflict = DeviceDescription.mergeReports(listOf(report("Printer"), report("InternetGatewayDevice")))
        assertEquals("Conflicting identity clues", DeviceIdentity.classify(false, false, conflict, false).basis)
    }

    @Test fun conflictingManufacturersRemainUnconfirmed() {
        val base = mapOf("identity_source" to "UPnP", "identity_model" to "Smart TV")
        val merged = DeviceDescription.mergeReports(listOf(base + ("identity_manufacturer" to "A"), base + ("identity_manufacturer" to "B")))
        assertEquals("true", merged["identity_conflict"])
        assertEquals("Conflicting identity clues", DeviceIdentity.classify(false, false, merged, false).basis)
    }

    @Test fun repeatedProbeMergesDoNotDuplicateServerObservations() {
        val reports = listOf(mapOf("server_80" to "A"), mapOf("server_80" to "B"))
        val merged = ServiceMetadata.mergeProbeDetails(reports)
        assertEquals(merged, ServiceMetadata.mergeProbeDetails(listOf(merged) + reports))
    }
    @Test fun compatibleReportsPreserveNameCapabilitiesAndCoherentHardware() {
        val onvif = mapOf("identity_source" to "ONVIF", "identity_model" to "Model-100", "identity_manufacturer" to "Example")
        val upnp = mapOf("identity_source" to "UPnP", "identity_model" to "Model-100", "upnp_name" to "Living room",
            "upnp_type" to "urn:schemas-upnp-org:device:MediaRenderer:1", "identity_firmware" to "Other firmware")
        val merged = DeviceDescription.mergeReports(listOf(onvif, upnp))
        assertEquals(merged, DeviceDescription.mergeReports(listOf(upnp, onvif)))
        assertEquals("Living room", DeviceIdentity.name(merged))
        assertEquals(listOf("Media playback service"), DeviceIdentity.capabilities(merged))
        assertEquals("Example", merged["identity_manufacturer"])
        assertNull(merged["identity_firmware"])
        assertNull(merged["identity_conflict"])
    }

    @Test fun conflictingReportsRetainCapabilitiesWithoutInventingHardwareIdentity() {
        val reports = listOf(
            mapOf("identity_source" to "ONVIF", "identity_model" to "NVR"),
            mapOf("identity_source" to "UPnP", "identity_model" to "IP Camera", "upnp_name" to "Room",
                "upnp_type" to "urn:schemas-upnp-org:device:MediaRenderer:1"),
            mapOf("identity_source" to "UPnP", "upnp_type" to "urn:schemas-upnp-org:device:MediaServer:1"))
        val merged = DeviceDescription.mergeReports(reports)
        assertEquals("Device type unconfirmed", DeviceIdentity.classify(false, false, merged, true).type)
        assertEquals(listOf("Media playback service", "Media server service"), DeviceIdentity.capabilities(merged))
        assertEquals("NVR", merged["identity_model"])
        assertEquals(merged, DeviceDescription.mergeReports(reports.reversed()))
    }

    @Test fun confirmedProtocolsSurviveWeakerResultsAndRemainOrderIndependent() {
        val reports = listOf(mapOf("port_554" to "HTTP"), mapOf("port_554" to "TCP"),
            mapOf("port_554" to "RTSP"), mapOf("port_80" to "TCP"), emptyMap())
        val merged = ServiceMetadata.mergeProbeDetails(reports)
        assertEquals("HTTP / RTSP", merged["port_554"])
        assertEquals("TCP", merged["port_80"])
        assertEquals(merged, ServiceMetadata.mergeProbeDetails(reports.reversed()))
        assertEquals("HTTP", ServiceMetadata.mergeProbeDetails(reports.take(2))["port_554"])
        assertEquals(merged, ServiceMetadata.mergeProbeDetails(listOf(merged, merged)))
    }
}
