package com.spyfinder.hiddencamera.detectorapp.utils

import com.spyfinder.hiddencamera.detectorapp.model.WifiDevice
import com.spyfinder.hiddencamera.detectorapp.scan.*
import org.junit.Test
import org.junit.Assert.*
import kotlin.system.measureNanoTime

/** Synthetic JVM measurements, not phone frame-rate or filesystem benchmarks. */
class ScanStoragePerformanceTest {
    @Test fun largeArchivesRoundTrip() {
        for (count in listOf(200, 1000, 4096)) {
            val devices = (1..count).map { i -> WifiDevice("Unknown", "Unknown", ScanRules.address(0xc0a80000L + i), 1, 0, 0,
                connected = false, evidence = List(8) { "HTTP: Service evidence $it for device $i" }) }
            val record = ScanRecord("benchmark", 1, 2, "network", ScanStatus.COMPLETE,
                ScanCoverage(count, count.toLong(), count, count), "summary", devices)
            val archive = ScanArchive(record, record)
            var encoded = ""
            repeat(2) { ScanHistoryStore.decode(ScanHistoryStore.encode(archive)) }
            val nanos = measureNanoTime { repeat(5) {
                encoded = ScanHistoryStore.encode(archive)
                assertEquals(archive, ScanHistoryStore.decode(encoded))
            } }
            println("ARCHIVE count=$count bytes=${encoded.toByteArray().size} roundTripMs=${nanos / 5_000_000.0}")
            val encoder = ScanHistoryStore.ArchiveEncoder()
            val running = record.copy(id = "running", status = ScanStatus.RUNNING, endedAt = null)
            encoder.encode(ScanArchive(running, record))
            val checkpoint = measureNanoTime { repeat(5) { index ->
                val value = ScanArchive(running.copy(coverage = running.coverage.copy(checked = index)), record)
                encoded = encoder.encode(value)
            } }
            assertEquals(4, ScanHistoryStore.decode(encoded).recent!!.coverage.checked)
            println("CHECKPOINT count=$count cachedValidatedEncodeMs=${checkpoint / 5_000_000.0}")
        }
    }
}
