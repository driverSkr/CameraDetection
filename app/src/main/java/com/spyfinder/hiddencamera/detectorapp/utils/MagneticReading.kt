package com.spyfinder.hiddencamera.detectorapp.utils

import kotlin.math.sqrt

data class MagneticReading(val microTesla: Float, val gauge: Int) {
    companion object {
        fun from(x: Float, y: Float, z: Float): MagneticReading? {
            if (!x.isFinite() || !y.isFinite() || !z.isFinite()) return null
            val magnitude = sqrt(x.toDouble() * x + y.toDouble() * y + z.toDouble() * z).toFloat()
            if (!magnitude.isFinite()) return null
            return MagneticReading(magnitude, ((magnitude - 20f) / 980f * 100f).coerceIn(0f, 100f).toInt())
        }
    }
}
