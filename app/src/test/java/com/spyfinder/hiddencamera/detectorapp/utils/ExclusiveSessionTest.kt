package com.spyfinder.hiddencamera.detectorapp.utils

import org.junit.Assert.*
import org.junit.Test

class ExclusiveSessionTest {
    @Test fun yieldToScanStopsMagneticWithoutCancellingAnIdleScan() {
        var magnetic = true
        var cancelled = 0
        ExclusiveSession.bind(
            cancelScan = { _, _ -> cancelled++ },
            scanRunning = { false },
            stopMagnetic = { magnetic = false },
            magneticActive = { magnetic }
        )
        try {
            assertTrue(ExclusiveSession.yieldToScan())
            assertFalse(magnetic)
            assertEquals(0, cancelled)
            assertFalse(ExclusiveSession.yieldToScan())
        } finally {
            ExclusiveSession.unbind()
        }
    }

    @Test fun yieldToMagneticCancelsARunningScan() {
        var running = true
        var reason = ""
        ExclusiveSession.bind(
            cancelScan = { value, _ -> running = false; reason = value },
            scanRunning = { running },
            stopMagnetic = {},
            magneticActive = { false }
        )
        try {
            assertTrue(ExclusiveSession.yieldToMagnetic())
            assertFalse(running)
            assertEquals(ExclusiveSession.SCAN_YIELD_MAGNETIC, reason)
        } finally {
            ExclusiveSession.unbind()
        }
    }

    @Test fun yieldToCameraCancelsScanAndMagnetic() {
        var running = true
        var magnetic = true
        ExclusiveSession.bind(
            cancelScan = { _, _ -> running = false },
            scanRunning = { running },
            stopMagnetic = { magnetic = false },
            magneticActive = { magnetic }
        )
        try {
            assertTrue(ExclusiveSession.yieldToCamera())
            assertFalse(running)
            assertFalse(magnetic)
        } finally {
            ExclusiveSession.unbind()
        }
    }
}
