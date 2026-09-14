package com.spyfinder.hiddencamera.detectorapp.scan

import org.junit.Assert.*
import org.junit.Test

class ScanWorkProgressTest {
    @Test fun newDevicesDoNotChangeTheDenominator() {
        val progress = ScanWorkProgress(setOf("a", "b"))
        assertEquals(0, progress.percent())
        progress.discovered("a"); progress.checked("a")
        assertEquals(25, progress.percent())
        progress.analyzed("a")
        assertEquals(50, progress.percent())
        progress.discovered("b")
        assertEquals(50, progress.percent())
        progress.checked("b")
        assertEquals(75, progress.percent())
        progress.analyzed("b"); progress.multicastFinished()
        assertEquals(99, progress.percent()) // Only the scan's terminal event may publish 100.
    }
    @Test fun lateMulticastReplyCannotUndoAnAlreadyRetiredAnalysisSlot() {
        val progress = ScanWorkProgress(setOf("a", "b"))
        progress.checked("a"); progress.checked("b")
        assertEquals(50, progress.percent())
        progress.discovered("b"); progress.multicastFinished()
        assertEquals(75, progress.percent())
        progress.analyzed("b")
        assertEquals(99, progress.percent())
    }
    @Test fun duplicateUpdatesAndOutsideTargetsDoNotInflateProgress() {
        val progress = ScanWorkProgress(setOf("a"))
        repeat(10) { progress.discovered("outside"); progress.analyzed("outside"); progress.checked("outside") }
        assertEquals(0, progress.percent())
        repeat(10) { progress.discovered("a"); progress.checked("a") }
        assertEquals(50, progress.percent())
    }
    @Test fun activeScanCanRunForMinutesButStalledWorkIsDetected() {
        var now = 0L
        val liveness = ScanLiveness({ now })
        repeat(100) { now += 10_000; liveness.activity(); assertFalse(liveness.stalled()) }
        now += 59_999
        assertFalse(liveness.stalled())
        now++
        assertTrue(liveness.stalled())
    }
}
