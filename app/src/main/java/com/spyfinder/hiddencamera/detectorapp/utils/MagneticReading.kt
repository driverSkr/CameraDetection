package com.spyfinder.hiddencamera.detectorapp.utils

import kotlin.math.sqrt

/** The original scale asset has 15 ticks spanning 144° through 396° clockwise. */
object MagneticScale {
    const val MAX_MICRO_TESLA = 100f
    const val START_ANGLE = 144f
    const val SWEEP_ANGLE = 252f
    const val POINTER_BASE_ANGLE = -45f
    // Rounded base center of the original 209px pointer, not its bitmap corner.
    const val PIVOT_X = 18f / 209f
    const val PIVOT_Y = 190f / 209f
    fun percent(microTesla: Float) = (microTesla / MAX_MICRO_TESLA * 100f).coerceIn(0f, 100f)
    fun direction(microTesla: Float) = START_ANGLE + SWEEP_ANGLE * percent(microTesla) / 100f
    fun rotation(microTesla: Float) = direction(microTesla) - POINTER_BASE_ANGLE
}

data class MagneticReading(val microTesla: Float, val gauge: Float) {
    companion object {
        fun from(x: Float, y: Float, z: Float): MagneticReading? {
            if (!x.isFinite() || !y.isFinite() || !z.isFinite()) return null
            val magnitude = sqrt(x.toDouble() * x + y.toDouble() * y + z.toDouble() * z).toFloat()
            if (!magnitude.isFinite()) return null
            return MagneticReading(magnitude, MagneticScale.percent(magnitude))
        }
    }
}
