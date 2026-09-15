package com.spyfinder.hiddencamera.detectorapp.scan

/** Scale bounded discovery concurrency with subnet size, preserving full probe timeouts. */
class DiscoveryPolicy {
    fun workers(addresses: Int): Int = when {
        addresses <= 256 -> 32
        addresses <= 1024 -> 48
        // Higher fan-out increased unreachable results on the validation Wi-Fi network.
        else -> 64
    }.coerceAtMost(addresses.coerceAtLeast(1))

    fun discover(continueScanning: () -> Boolean, probe: (Int, Int) -> ProbeResult): ProbeSupport.DiscoveryAttempt {
        return ProbeSupport.discover(continueScanning) { probe(it, 450) }
    }
}
