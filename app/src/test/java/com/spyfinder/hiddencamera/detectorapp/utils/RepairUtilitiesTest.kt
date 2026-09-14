package com.spyfinder.hiddencamera.detectorapp.utils

import org.junit.Assert.*
import org.junit.Test

class RepairUtilitiesTest {
    @Test fun magneticPhysicalValueIsNotGaugePercentage() {
        val reading = MagneticReading.from(30f, 40f, 0f)!!
        assertEquals(50f, reading.microTesla, 0.001f)
        assertEquals(3, reading.gauge)
        val high = MagneticReading.from(0f, 0f, 1200f)!!
        assertEquals(1200f, high.microTesla, 0.001f)
        assertEquals(100, high.gauge)
        assertNull(MagneticReading.from(Float.NaN, 0f, 0f))
        assertNull(MagneticReading.from(Float.POSITIVE_INFINITY, 0f, 0f))
    }
    @Test fun shareIncludesExactlyOneLinkInEitherLanguage() {
        val url = "https://play.google.com/store/apps/details?id=test.app"
        listOf("Share this app", "分享应用").forEach {
            val body = ShareText.body(it, url)
            assertTrue(body.endsWith(url))
            assertEquals(body, ShareText.body(body, url))
        }
    }
    @Test fun obsoleteCameraRequestCannotChangeCurrentState() {
        val gate = LatestRequest()
        val first = gate.begin()
        val second = gate.begin()
        assertFalse(gate.accepts(first)); assertTrue(gate.accepts(second))
        gate.invalidate()
        assertFalse(gate.accepts(second))
    }
}
