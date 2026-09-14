package com.spyfinder.hiddencamera.detectorapp.scan

import java.io.IOException
import java.io.InputStream
import java.net.SocketTimeoutException
import java.util.concurrent.CancellationException

enum class ProbeResult { OPEN, REFUSED, TIMEOUT, UNREACHABLE, CANCELLED, UNKNOWN }

object ProbeSupport {
    val ports = listOf(554, 8554, 80, 5000, 443)

    data class DiscoveryAttempt(val responded: Boolean, val checked: Boolean, val uncertain: Boolean)
    fun discover(shouldContinue: () -> Boolean, probe: (Int) -> ProbeResult): DiscoveryAttempt {
        var uncertain = false
        for (port in ports) {
            if (!shouldContinue()) return DiscoveryAttempt(false, false, true)
            when (probe(port)) {
                ProbeResult.OPEN, ProbeResult.REFUSED -> return DiscoveryAttempt(true, true, uncertain)
                ProbeResult.CANCELLED -> throw CancellationException("Discovery cancelled")
                ProbeResult.UNREACHABLE, ProbeResult.UNKNOWN -> uncertain = true
                ProbeResult.TIMEOUT -> Unit
            }
        }
        return DiscoveryAttempt(false, true, uncertain)
    }

    // errno is supplied by the Android adapter; unknown exceptions must not imply a reply.
    fun classify(error: Throwable, errno: Int?, closed: Boolean): ProbeResult = when {
        closed || error is CancellationException -> ProbeResult.CANCELLED
        error is SocketTimeoutException || errno == 110 -> ProbeResult.TIMEOUT
        errno == 111 -> ProbeResult.REFUSED // Linux ECONNREFUSED
        errno in listOf(101, 113, 99) -> ProbeResult.UNREACHABLE
        else -> ProbeResult.UNKNOWN
    }

    fun readStatusLine(input: InputStream, deadline: Long, now: () -> Long,
                       beforeRead: (Int) -> Unit, cancelled: () -> Boolean, limit: Int = 4096): String {
        val line = StringBuilder()
        val buffer = ByteArray(256)
        while (line.length < limit) {
            if (cancelled()) throw CancellationException("Probe cancelled")
            val remaining = deadline - now()
            if (remaining <= 0) throw SocketTimeoutException("Status line timed out")
            beforeRead(remaining.coerceAtMost(Int.MAX_VALUE.toLong()).toInt())
            val count = input.read(buffer, 0, minOf(buffer.size, limit - line.length))
            if (count < 0) throw IOException("Truncated status line")
            for (i in 0 until count) {
                val ch = (buffer[i].toInt() and 255).toChar()
                if (ch == '\n') return line.toString().removeSuffix("\r") + "\r\n"
                line.append(ch)
            }
        }
        throw IOException("Status line exceeds limit")
    }
}
