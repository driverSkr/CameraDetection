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
}
