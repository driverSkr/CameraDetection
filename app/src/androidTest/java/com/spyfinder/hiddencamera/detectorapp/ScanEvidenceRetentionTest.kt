package com.spyfinder.hiddencamera.detectorapp

import androidx.test.platform.app.InstrumentationRegistry
import com.spyfinder.hiddencamera.detectorapp.scan.*
import org.junit.Assert.*
import org.junit.Test

/** Exercise the scanner's publication boundary without depending on LAN timing. */
class ScanEvidenceRetentionTest {
    private class Session {
        val scanner = NetworkScanner(InstrumentationRegistry.getInstrumentation().targetContext)
        val ip = "192.168.1.20"
        val http = ServiceProbe(8080, "HTTP")
        private val discover = NetworkScanner::class.java.declaredMethods.single { it.name == "discovered" }
            .apply { isAccessible = true }
        private val record = NetworkScanner::class.java.declaredMethods.single { it.name == "recordAnalysis" }
            .apply { isAccessible = true }
        private val constructor = Class.forName(NetworkScanner::class.java.name + "\$Analysis")
            .declaredConstructors.single { it.parameterTypes.size == 3 }.apply { isAccessible = true }
        init { discover.invoke(scanner, ip, emptyList<Evidence>(), null, emptyMap<String, String>()) }
        fun announce() { discover.invoke(scanner, ip, listOf(Evidence("mDNS", "HTTP service")), http, emptyMap<String, String>()) }
        fun finish(probes: Set<ServiceProbe>, video: Boolean, complete: Boolean) {
            val evidence = if (video) listOf(Evidence("RTSP", "Video protocol responded on port 554", true)) else emptyList()
            val details = mapOf("port_554" to if (video) "RTSP" else "TCP")
            record.invoke(scanner, ip, probes, constructor.newInstance(evidence, complete, details))
        }
        fun assertRetained(complete: Boolean) {
            val device = scanner.snapshot().single()
            assertEquals(Finding.CAMERA_FEATURES, device.finding)
            assertEquals(1, device.riskLevel)
            assertTrue(device.evidence.any { it.startsWith("RTSP:") })
            assertEquals("RTSP", device.details["port_554"])
            assertEquals(complete, device.analysisComplete)
        }
    }

    @Test fun lateAnnouncementAndFailedRetryPreserveConfirmedProtocol() {
        val session = Session()
        try {
            session.finish(emptySet(), video = true, complete = true)
            session.assertRetained(true)
            session.announce()
            session.assertRetained(false)
            assertEquals(0, session.scanner.coverage().analyzed)
            session.finish(setOf(session.http), video = false, complete = false)
            session.assertRetained(false)
            assertEquals(1, session.scanner.coverage().analyzed)
        } finally { session.scanner.resources.close() }
    }

    @Test fun cancelledSupplementaryAnalysisKeepsSnapshotEvidence() {
        val session = Session()
        session.finish(emptySet(), video = true, complete = true)
        session.announce()
        session.scanner.resources.close()
        session.assertRetained(false)
    }

    @Test fun announcementDuringAnalysisRetainsOldPassObservations() {
        val session = Session()
        try {
            session.announce()
            session.finish(emptySet(), video = true, complete = true)
            session.assertRetained(false)
            assertEquals(0, session.scanner.coverage().analyzed)
            session.finish(setOf(session.http), video = false, complete = true)
            session.assertRetained(true)
        } finally { session.scanner.resources.close() }
    }
}
