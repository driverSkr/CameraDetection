package com.spyfinder.hiddencamera.detectorapp.utils

class LatestRequest {
    private var sequence = 0L
    fun begin(): Long = ++sequence
    fun accepts(id: Long): Boolean = sequence == id
    fun invalidate() { sequence++ }
}
