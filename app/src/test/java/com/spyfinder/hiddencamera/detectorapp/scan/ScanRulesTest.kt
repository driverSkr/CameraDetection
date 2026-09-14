package com.spyfinder.hiddencamera.detectorapp.scan

import org.junit.Assert.*
import org.junit.Test

class ScanRulesTest {
    @Test fun unknownHostIsNotAWarning() { assertEquals(Finding.INSUFFICIENT, ScanRules.finding(emptyList(), false)) }
    @Test fun nasPortIsNotACamera() { assertEquals(Finding.INSUFFICIENT, ScanRules.finding(listOf(Evidence("TCP", "5000")), false)) }
    @Test fun webServerIsNotACamera() { assertEquals(Finding.NO_CAMERA_FEATURES, ScanRules.finding(listOf(Evidence("HTTP", "Web service")), false)) }
    @Test fun failedAnalysisDoesNotClaimNoFeatures() { assertEquals(Finding.INSUFFICIENT, ScanRules.finding(listOf(Evidence("HTTP", "Web service")), true)) }
    @Test fun timeoutDoesNotErasePositiveEvidence() { assertEquals(Finding.CAMERA_FEATURES, ScanRules.finding(listOf(Evidence("RTSP", "Video service", true)), true)) }
    @Test fun slash23IncludesAdjacentSegment() {
        val result = ScanRules.targets("192.168.1.4", 23, "192.168.0.1")
        assertEquals(510, result.addresses.size)
        assertTrue(result.addresses.contains("192.168.0.254"))
        assertFalse(result.addresses.contains("192.168.1.255"))
        assertFalse(result.limited)
    }
    @Test fun slash30NeverScansOutsideItsPrefix() {
        assertEquals(setOf("10.0.0.5", "10.0.0.6"), ScanRules.targets("10.0.0.5", 30, "10.0.0.6").addresses.toSet())
    }
    @Test fun largeNetworkIsExplicitlyLimited() {
        val result = ScanRules.targets("10.80.200.20", 8, "10.0.0.1", 1024)
        assertTrue(result.limited)
        assertEquals(1024, result.addresses.size)
        assertTrue(result.addresses.contains("10.0.0.1"))
        assertTrue(result.addresses.all { ScanRules.inSubnet(it, "10.80.200.20", 8) })
    }
    @Test fun hostRouteAndPointToPointHaveNoInvalidExclusions() {
        assertEquals(listOf("10.0.0.7"), ScanRules.targets("10.0.0.7", 32, null).addresses)
        assertEquals(2, ScanRules.targets("10.0.0.6", 31, null).addresses.size)
    }
    @Test fun invalidAndOutOfNetworkAddressesAreRejected() {
        assertNull(ScanRules.ipv4("300.1.1.1"))
        assertFalse(ScanRules.inSubnet("192.168.2.1", "192.168.1.1", 24))
    }
}
