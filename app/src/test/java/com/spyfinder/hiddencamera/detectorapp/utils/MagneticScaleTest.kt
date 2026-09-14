package com.spyfinder.hiddencamera.detectorapp.utils

import org.junit.Assert.*
import org.junit.Test

class MagneticScaleTest {
    @Test fun zeroMidpointAndMaximumMatchTheOriginalArtwork() {
        assertEquals(144f, MagneticScale.direction(0f), 0.001f)
        assertEquals(270f, MagneticScale.direction(50f), 0.001f)
        assertEquals(396f, MagneticScale.direction(100f), 0.001f)
        assertEquals(270f, MagneticScale.rotation(50f) + MagneticScale.POINTER_BASE_ANGLE, 0.001f)
    }
    @Test fun everyOriginalTickHasAnEqualPhysicalIncrement() {
        for (tick in 0..14) {
            assertEquals(144f + tick * 18f, MagneticScale.direction(100f * tick / 14f), 0.001f)
        }
    }
    @Test fun smallChangesAreNotRoundedToWholePercentages() {
        val a = MagneticScale.rotation(50.1f)
        val b = MagneticScale.rotation(50.2f)
        assertEquals(0.252f, b - a, 0.001f)
    }
    @Test fun outOfRangeNeedleClampsButPhysicalReadingRemainsTrue() {
        assertEquals(MagneticScale.rotation(100f), MagneticScale.rotation(1200f), 0.001f)
        assertEquals(1200f, MagneticReading.from(0f, 0f, 1200f)!!.microTesla, 0.001f)
    }
}
