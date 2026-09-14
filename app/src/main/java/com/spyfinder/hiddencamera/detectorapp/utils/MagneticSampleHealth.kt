package com.spyfinder.hiddencamera.detectorapp.utils

object MagneticSampleHealth {
    const val STALE_AFTER_MILLIS = 3_000L
    fun stale(startedAt: Long, lastSampleAt: Long?, now: Long) = now - (lastSampleAt ?: startedAt) >= STALE_AFTER_MILLIS
    fun unreliable(accuracy: Int) = accuracy <= 1
}
