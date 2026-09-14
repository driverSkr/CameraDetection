package com.spyfinder.hiddencamera.detectorapp.utils

import com.spyfinder.hiddencamera.detectorapp.model.WifiDevice
import com.spyfinder.hiddencamera.detectorapp.scan.*
import com.spyfinder.hiddencamera.detectorapp.ui.main.context.MainContextEntity
import org.junit.Assert.*
import org.junit.Test

class ScanArchiveTest {
    private fun record(id: String, status: ScanStatus) = ScanRecord(id, 123, 456, "192.168.1.2/24",
        status, ScanCoverage(254, 254, 10, 1), "summary",
        listOf(WifiDevice("Device", "Unknown", "192.168.1.3", 1, 0, 0, connected = false, analysisComplete = false)))
    @Test fun legacyMigrationPreservesPreviousRecordWithoutInventingCompletion() {
        val legacy = ScanHistoryStore.legacy("[]", "[]", "Old summary")
        val updated = legacy.record(record("B", ScanStatus.PARTIAL))
        assertEquals("legacy", updated.complete!!.id)
        assertNull(updated.complete!!.status)
        assertEquals("Old summary", updated.complete!!.summary)
        assertEquals(updated, ScanHistoryStore.decode(ScanHistoryStore.encode(updated)))
    }
    @Test fun partialNeverOverwritesLastComplete() {
        val a = record("A", ScanStatus.COMPLETE)
        val b = record("B", ScanStatus.PARTIAL)
        val archive = ScanArchive().record(a).record(b)
        assertEquals(a, archive.complete); assertEquals(b, archive.recent)
        assertEquals(archive, ScanHistoryStore.decode(ScanHistoryStore.encode(archive)))
    }
    @Test fun trustIsScopedToRecordEvenWhenIpIsReused() {
        val archive = ScanArchive().record(record("A", ScanStatus.COMPLETE)).record(record("B", ScanStatus.PARTIAL))
            .trust("B", "192.168.1.3", true)
        assertTrue(archive.recent!!.devices.single().userTrusted)
        assertFalse(archive.complete!!.devices.single().userTrusted)
    }
    @Test fun matchingRecordReferencesReceiveTrustTogether() {
        val archive = ScanArchive().record(record("A", ScanStatus.COMPLETE)).trust("A", "192.168.1.3", true)
        assertEquals(archive.recent, archive.complete)
        assertTrue(archive.complete!!.devices.single().userTrusted)
    }
    @Test fun checkpointRecoversAsInterruptedAndRetainsEvidence() {
        val archive = ScanArchive().record(record("A", ScanStatus.COMPLETE)).record(record("B", ScanStatus.RUNNING))
        val recovered = ScanHistoryStore.decode(ScanHistoryStore.encode(archive)).interrupted()
        assertEquals(ScanStatus.CANCELLED, recovered.recent!!.status)
        assertEquals(archive.recent!!.devices, recovered.recent!!.devices)
        assertEquals(archive.complete, recovered.complete)
    }
    @Test fun emptyAttemptDoesNotEraseUsefulHistory() {
        val archive = ScanArchive().record(record("A", ScanStatus.COMPLETE))
        assertEquals(archive, archive.record(record("B", ScanStatus.CANCELLED).copy(coverage = ScanCoverage(), devices = emptyList())))
    }
    @Test fun historyRemainsAccessibleAfterEachTerminalState() {
        listOf(ScanStatus.CANCELLED, ScanStatus.FAILED, ScanStatus.COMPLETE, ScanStatus.PARTIAL).forEach { status ->
            val state = MainContextEntity(null)
            state.saveRecord(record("A", ScanStatus.COMPLETE))
            state.isStartDetect.value = true
            state.scanStatus = status
            state.openLatestResult()
            assertTrue(state.isShowingLatestHistoryResult)
            state.closeDetectResult()
            assertFalse(state.isShowingLatestHistoryResult)
            assertTrue(state.isStartDetect.value)
        }
    }
}
