package com.spyfinder.hiddencamera.detectorapp.scan

/** Two fixed work slots per planned address: discovery, then analysis or a confirmed no-reply attempt. */
class ScanWorkProgress(private val targets: Set<String>) {
    private val checked = mutableSetOf<String>()
    private val found = mutableSetOf<String>()
    private val analyzed = mutableSetOf<String>()
    private var multicastFinished = false
    @Synchronized fun discovered(ip: String) { if (ip in targets) found.add(ip) }
    @Synchronized fun checked(ip: String) { if (ip in targets) checked.add(ip) }
    @Synchronized fun analyzed(ip: String) { if (ip in targets) analyzed.add(ip) }
    @Synchronized fun multicastFinished() { multicastFinished = true }
    @Synchronized fun percent(): Int {
        if (targets.isEmpty()) return 0
        // Do not retire unanswered addresses until multicast has finished publishing devices.
        val noReply = if (multicastFinished) checked.count { it !in found } else 0
        return ((checked.size + analyzed.size + noReply).toLong() * 100 / (targets.size * 2)).toInt().coerceIn(0, 99)
    }
}

/** Measures actual completed probe activity, never the total duration of a scan. */
class ScanLiveness(private val now: () -> Long, private val idleLimitMillis: Long = 60_000) {
    @Volatile private var lastActivity = now()
    fun activity() { lastActivity = now() }
    fun stalled() = now() - lastActivity >= idleLimitMillis
}
