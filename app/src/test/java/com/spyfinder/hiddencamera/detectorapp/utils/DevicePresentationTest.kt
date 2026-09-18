package com.spyfinder.hiddencamera.detectorapp.utils

import com.spyfinder.hiddencamera.detectorapp.R
import com.spyfinder.hiddencamera.detectorapp.model.WifiDevice
import com.spyfinder.hiddencamera.detectorapp.scan.*
import org.junit.Assert.*
import org.junit.Test

class DevicePresentationTest {
    private fun device(details: Map<String, String> = emptyMap()) = WifiDevice("", "", "192.168.1.2", 0, 0, 0, details = details)
    @Test fun unknownModelHasSpecificExplanationAndGuidance() {
        val device = device(mapOf("identity_model" to "XYZ-123"))
        assertEquals(R.string.identity_status_model, DevicePresentation.identityStatus(device, DeviceIdentity.forDevice(device)))
        assertEquals(R.string.ux_step_model, DevicePresentation.nextStep(device))
    }
    @Test fun conflictsTakePrecedenceOverKnownModel() {
        val device = device(mapOf("identity_model" to "Smart TV", "identity_conflict" to "true"))
        assertEquals(R.string.identity_status_conflict, DevicePresentation.identityStatus(device, DeviceIdentity.forDevice(device)))
        assertEquals(R.string.ux_step_conflict, DevicePresentation.nextStep(device))
    }
    @Test fun recognizedDeviceDoesNotGetMissingIdentityAdvice() {
        val device = device(mapOf("identity_model" to "Smart TV"))
        assertNull(DevicePresentation.identityStatus(device, DeviceIdentity.forDevice(device)))
        assertEquals(R.string.ux_step_identified, DevicePresentation.nextStep(device))
        assertEquals(R.string.ux_step_incomplete, DevicePresentation.nextStep(device.copy(analysisComplete = false)))
    }
    @Test fun accessRestrictionsAndVideoCluesStayDistinct() {
        val auth = device(mapOf("identity_query" to "Authentication required"))
        assertEquals(R.string.identity_status_auth, DevicePresentation.identityStatus(auth, DeviceIdentity.forDevice(auth)))
        val video = device().copy(finding = Finding.CAMERA_FEATURES)
        assertEquals(R.string.identity_status_video, DevicePresentation.identityStatus(video, DeviceIdentity.forDevice(video)))
    }
    @Test fun listRanksCameraCluesAheadOfOtherDevices() {
        val clue = device().copy(finding = Finding.CAMERA_FEATURES)
        assertTrue(DevicePresentation.needsLook(clue))
        assertFalse(DevicePresentation.needsLook(clue.copy(userTrusted = true)))
        assertEquals(0, DevicePresentation.listRank(clue))
        assertEquals(1, DevicePresentation.listRank(device().copy(analysisComplete = false)))
        assertEquals(2, DevicePresentation.listRank(device().copy(userTrusted = true)))
        assertEquals(2, DevicePresentation.listRank(device().copy(isCurrentPhone = true)))
        assertEquals(3, DevicePresentation.listRank(device()))
    }
    @Test fun listUsesReportedIdentityInsteadOfUnknownDevice() {
        assertEquals("客厅", lines(device(mapOf("upnp_name" to "客厅"))).title)
        assertEquals("Example", lines(device(mapOf("identity_manufacturer" to "Example"))).title)
        assertEquals("hallcam", lines(device(mapOf("mdns_host" to "hallcam.local"))).title)
        val empty = lines(device())
        assertEquals("Network device", empty.title)
        assertEquals("192.168.1.2", empty.caption)
        val video = lines(device().copy(finding = Finding.CAMERA_FEATURES))
        assertEquals("Network device", video.title)
        assertEquals("Possible camera-related clue", video.caption)
        val named = lines(device(mapOf("identity_model" to "NVR")).copy(finding = Finding.CAMERA_FEATURES))
        assertEquals("NVR", named.title)
        assertEquals("Likely video recorder", named.caption)
        val printer = lines(device(mapOf("mdns_device_type" to "Printer")))
        assertEquals("Printer", printer.title)
        assertNull(printer.caption)
    }
    private fun lines(device: WifiDevice) = DevicePresentation.listLines(device, { id ->
        when (id) {
            R.string.identity_network_device -> "Network device"
            R.string.current_phone -> "Current phone"
            R.string.result_video_service -> "Possible camera-related clue"
            R.string.result_marked_known -> "Marked known"
            else -> "res:$id"
        }
    }) { it }
}
