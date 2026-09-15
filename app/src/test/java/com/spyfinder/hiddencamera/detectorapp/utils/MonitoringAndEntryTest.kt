package com.spyfinder.hiddencamera.detectorapp.utils

import com.spyfinder.hiddencamera.detectorapp.scan.ScanStatus
import com.spyfinder.hiddencamera.detectorapp.ui.main.context.MainContextEntity
import org.junit.Assert.*
import org.junit.Test

class MonitoringAndEntryTest {
    @Test fun featureEntryKeepsTheActiveSessionAndProgress() {
        val state = MainContextEntity(null)
        state.scanStatus = ScanStatus.RUNNING; state.currentRecordId = "active"
        state.detectProgress.intValue = 62; state.selectTabIndex.intValue = 3
        state.openWifiFeature()
        assertFalse(state.pendingWifiAutoScan.value)
        assertEquals("active", state.currentRecordId)
        assertEquals(62, state.detectProgress.intValue)
        assertEquals(0, state.selectTabIndex.intValue)
        state.scanStatus = ScanStatus.IDLE; state.openWifiFeature()
        assertFalse(state.pendingWifiAutoScan.value)
        assertEquals(ScanStatus.IDLE, state.scanStatus)
    }
    @Test fun sensorSilenceAndRecoveryUseTheLastValidSample() {
        assertFalse(MagneticSampleHealth.stale(1000, null, 3999))
        assertTrue(MagneticSampleHealth.stale(1000, null, 4000))
        assertTrue(MagneticSampleHealth.stale(1000, 5000, 8000))
        assertFalse(MagneticSampleHealth.stale(1000, 8100, 8200))
        assertTrue(MagneticSampleHealth.unreliable(0)); assertTrue(MagneticSampleHealth.unreliable(1))
        assertFalse(MagneticSampleHealth.unreliable(3))
    }
}
