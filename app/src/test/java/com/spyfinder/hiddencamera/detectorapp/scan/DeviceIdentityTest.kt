package com.spyfinder.hiddencamera.detectorapp.scan

import org.junit.Assert.*
import org.junit.Test

class DeviceIdentityTest {
    @Test fun incompleteIdentityExplainsTheSpecificReason() {
        fun reason(details: Map<String, String>, video: Boolean = false) = DeviceIdentity.classify(false, false, details, video).basis
        assertEquals("Authentication required", reason(mapOf("identity_query" to "Authentication required"), true))
        assertEquals("Reported identity not matched", reason(mapOf("identity_model" to "XYZ-123")))
        assertEquals("Device description unavailable", reason(mapOf("identity_query" to "Device description unavailable")))
        assertEquals("Network services only; no device identity", reason(mapOf("port_80" to "HTTP")))
        assertEquals("Online response only; no device identity", reason(mapOf("port_80" to "TCP")))
        assertEquals("Video capability detected; hardware type unconfirmed", reason(emptyMap(), true))
    }
    @Test fun additionalModelFamiliesAreTentativeAndUseWordBoundaries() {
        mapOf("MacBook Pro" to "Likely computer", "iPad 10" to "Likely phone or tablet",
            "Smart TV" to "Likely television", "DiskStation DS220" to "Likely storage device").forEach { (model, type) ->
            assertEquals(type, DeviceIdentity.classify(false, false, mapOf("identity_model" to model), false).type)
        }
        listOf("NASAL", "mydesktopapp", "dvrsoftware").forEach {
            assertEquals("Device type unconfirmed", DeviceIdentity.classify(false, false, mapOf("identity_model" to it), false).type)
        }
    }
    @Test fun namesAreWeakHintsAndCannotCreateCameraOrStorageClaims() {
        val pc = DeviceIdentity.classify(false, false, mapOf("mdns_host" to "office-macbook.local"), true)
        assertEquals("Likely computer", pc.type)
        assertEquals("Device name suggests type; verify manually", pc.basis)
        listOf("IP Camera", "NVR", "NAS", "notaniphone", "desktopbackup").forEach {
            assertEquals("Device type unconfirmed", DeviceIdentity.classify(false, false, mapOf("mdns_name" to it), false).type)
        }
    }
    @Test fun conflictingHintsStayUnconfirmedWhileDeclarationsBeatNames() {
        assertEquals("Conflicting identity clues", DeviceIdentity.classify(false, false,
            mapOf("mdns_host" to "macbook.local", "mdns_name" to "iphone"), false).basis)
        assertEquals("Conflicting identity clues", DeviceIdentity.classify(false, false,
            mapOf("identity_model" to "NVR IP Camera"), true).basis)
        assertEquals("Printer", DeviceIdentity.classify(false, false,
            mapOf("mdns_device_type" to "Printer", "mdns_name" to "macbook"), true).type)
    }
    @Test fun advertisedRouterIsNotClaimedAsTheConfiguredGateway() {
        val device = com.spyfinder.hiddencamera.detectorapp.model.WifiDevice("Router", "Router", "192.168.1.3", 0, 0, 0,
            details = mapOf("upnp_type" to "urn:schemas-upnp-org:device:InternetGatewayDevice:1"))
        assertEquals("UPnP device type", DeviceIdentity.forDevice(device).basis)
    }
    @Test fun videoPortsAndGenericSoftwareDoNotProveACamera() {
        assertEquals("Device type unconfirmed", DeviceIdentity.classify(false, false,
            mapOf("port_554" to "RTSP", "server_80" to "webserver"), true).type)
    }
    @Test fun declaredMediaDeviceRetainsItsIdentityEvenWithVideoClues() {
        assertEquals("Media playback device", DeviceIdentity.classify(false, false,
            mapOf("upnp_type" to "urn:schemas-upnp-org:device:MediaRenderer:1"), true).type)
        assertEquals("Media server", DeviceIdentity.classify(false, false,
            mapOf("upnp_type" to "urn:schemas-upnp-org:device:MediaServer:1"), true).type)
    }
    @Test fun explicitCameraAndRecorderModelsAreOnlyTentative() {
        assertEquals("Likely video recorder", DeviceIdentity.classify(false, false, mapOf("identity_model" to "NVR 8-channel"), true).type)
        assertEquals("Likely network camera", DeviceIdentity.classify(false, false, mapOf("identity_model" to "IP Camera"), true).type)
        assertEquals("Device type unconfirmed", DeviceIdentity.classify(false, false, mapOf("mdns_name" to "IP Camera"), true).type)
    }
    @Test fun mdnsPrinterAndPlaybackAnnouncementsHaveTheirOwnTypes() {
        assertEquals("Printer", DeviceIdentity.classify(false, false, mapOf("mdns_device_type" to "Printer"), false).type)
        assertEquals("Media playback device", DeviceIdentity.classify(false, false, mapOf("mdns_device_type" to "Media playback device"), false).type)
    }
    @Test fun descriptionCanOnlyAddressTheAnnouncingDevice() {
        assertNotNull(DeviceDescription.localUrl("http://192.168.1.2:8080/device.xml", "192.168.1.2"))
        listOf("http://example.com/x", "http://192.168.1.3/x", "http://user:pass@192.168.1.2/x", "file:///etc/passwd", "http://192.168.1.2:0/x").forEach {
            assertNull(DeviceDescription.localUrl(it, "192.168.1.2"))
        }
    }
    @Test fun upnpUsesOnlyTheParentDevicesOwnFields() {
        val xml = """<root xmlns="urn:schemas-upnp-org:device-1-0"><device><friendlyName>Living room</friendlyName><deviceType>urn:schemas-upnp-org:device:MediaRenderer:1</deviceType><modelName>Player</modelName><deviceList><device><modelName>IP Camera</modelName></device></deviceList></device></root>"""
        val details = DeviceDescription.parse(xml.toByteArray(), false)
        assertEquals("Living room", details["upnp_name"])
        assertEquals("Player", details["identity_model"])
    }
    @Test fun onvifInformationRequiresCorrectNamespaceAndRejectsEntities() {
        val xml = """<s:Envelope xmlns:s="http://www.w3.org/2003/05/soap-envelope"><s:Body><GetDeviceInformationResponse xmlns="http://www.onvif.org/ver10/device/wsdl"><Manufacturer>Example</Manufacturer><Model>IP Camera</Model></GetDeviceInformationResponse></s:Body></s:Envelope>"""
        assertEquals("Example", DeviceDescription.parse(xml.toByteArray(), true)["identity_manufacturer"])
        assertTrue(DeviceDescription.parse(xml.replace("http://www.onvif.org/ver10/device/wsdl", "wrong").toByteArray(), true).isEmpty())
        assertTrue(DeviceDescription.parse(("<!DOCTYPE x SYSTEM 'file:///etc/passwd'>" + xml).toByteArray(), true).isEmpty())
    }
}
