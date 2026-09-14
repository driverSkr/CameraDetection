package com.spyfinder.hiddencamera.detectorapp.scan

enum class Finding { CAMERA_FEATURES, NO_CAMERA_FEATURES, INSUFFICIENT, LEGACY }
enum class ScanStatus { IDLE, RUNNING, COMPLETE, PARTIAL, FAILED, CANCELLED }

data class Evidence(val source: String, val detail: String, val cameraRelated: Boolean = false)

object ScanRules {
    const val VERSION = 2
    const val MAX_TARGETS = 4096
    fun finding(evidence: List<Evidence>, incomplete: Boolean): Finding = when {
        evidence.any { it.cameraRelated } -> Finding.CAMERA_FEATURES
        incomplete || evidence.none { it.source != "TCP" } -> Finding.INSUFFICIENT
        else -> Finding.NO_CAMERA_FEATURES
    }

    fun ipv4(value: String): Long? {
        val parts = value.split('.')
        if (parts.size != 4) return null
        val octets = parts.map { it.toIntOrNull()?.takeIf { n -> n in 0..255 } ?: return null }
        return octets.fold(0L) { acc, n -> (acc shl 8) or n.toLong() }
    }
    fun address(value: Long) = (3 downTo 0).joinToString(".") { ((value ushr (it * 8)) and 255).toString() }
    fun inSubnet(ip: String, local: String, prefix: Int): Boolean {
        if (prefix !in 0..32) return false
        val a = ipv4(ip) ?: return false
        val b = ipv4(local) ?: return false
        val mask = if (prefix == 0) 0L else (0xffffffffL shl (32 - prefix)) and 0xffffffffL
        return a and mask == b and mask
    }
    fun usableHost(ip: String, local: String, prefix: Int): Boolean {
        if (!inSubnet(ip, local, prefix)) return false
        val value = ipv4(ip) ?: return false
        val firstOctet = value ushr 24
        if (firstOctet == 0L || firstOctet == 127L || firstOctet >= 224) return false
        if (prefix >= 31) return true
        val mask = if (prefix == 0) 0L else (0xffffffffL shl (32 - prefix)) and 0xffffffffL
        val network = value and mask
        return value != network && value != (network or (mask xor 0xffffffffL))
    }

    data class Targets(val addresses: List<String>, val total: Long, val limited: Boolean)
    fun targets(local: String, prefix: Int, gateway: String?, limit: Int = MAX_TARGETS): Targets {
        require(prefix in 0..32 && limit > 0)
        val ip = requireNotNull(ipv4(local))
        val mask = if (prefix == 0) 0L else (0xffffffffL shl (32 - prefix)) and 0xffffffffL
        val base = ip and mask
        val end = base or (mask xor 0xffffffffL)
        val first = if (prefix < 31) base + 1 else base
        val last = if (prefix < 31) end - 1 else end
        val total = last - first + 1
        val result = linkedSetOf(local)
        gateway?.takeIf { inSubnet(it, local, prefix) }?.let { result.add(it) }
        // Large networks are explicitly partial; inspect a bounded window around this phone.
        val start = maxOf(first, minOf(ip - limit / 2, last - minOf(total, limit.toLong()) + 1))
        var next = start
        while (result.size < limit && next <= last) result.add(address(next++))
        return Targets(result.toList(), total, result.size.toLong() < total)
    }
}
