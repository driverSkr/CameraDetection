package com.spyfinder.hiddencamera.detectorapp.scan

import org.junit.Assert.*
import org.junit.Test

class AdvertisedIdentityTest {
    private fun advertisement(type: String) = mapOf("ssdp_st" to "urn:schemas-upnp-org:device:$type:1")

    @Test fun announcementIdentifiesPrinterWhenDescriptionIsUnavailable() {
        val details = advertisement("Printer") + ("identity_query" to "Device description unavailable")
        val result = DeviceIdentity.classify(false, false, details, false)
        assertEquals("Printer", result.type)
        assertEquals("SSDP device type", result.basis)
        assertEquals(listOf("Printing service"), DeviceIdentity.capabilities(details))
    }

    @Test fun successiveAnnouncementsPreserveAllCapabilitiesInEitherOrder() {
        val a = advertisement("MediaRenderer")
        val b = advertisement("MediaServer")
        val merged = ServiceMetadata.merge(ServiceMetadata.merge(a, b), mapOf("ssdp_st" to "upnp:rootdevice"))
        assertEquals(ServiceMetadata.merge(b, a)["ssdp_st"], ServiceMetadata.merge(a, b)["ssdp_st"])
        assertEquals(listOf("Media playback service", "Media server service"), DeviceIdentity.capabilities(merged))
        assertEquals("Device type unconfirmed", DeviceIdentity.classify(false, false, merged, true).type)
        assertEquals(merged, ServiceMetadata.merge(merged, a))
    }

    @Test fun deviceAnnouncementsMustBeExactAndDoNotInferFromBanners() {
        listOf("upnp:rootdevice", "urn:schemas-upnp-org:service:Printer:1", "some Printer", "urn:schemas-upnp-org:device:Printer:1-extra").forEach {
            assertEquals("Device type unconfirmed", DeviceIdentity.classify(false, false,
                mapOf("ssdp_st" to it, "ssdp_server" to "Smart TV Camera"), false).type)
        }
    }

    @Test fun contradictoryAnnouncementsStillRequireConfirmation() {
        val details = advertisement("Printer") + ("upnp_type" to "urn:schemas-upnp-org:device:InternetGatewayDevice:1")
        assertEquals("Conflicting identity clues", DeviceIdentity.classify(false, false, details, false).basis)
    }
}
