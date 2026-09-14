package com.spyfinder.hiddencamera.detectorapp.scan

import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.util.Locale

/** Bounded DNS parsing: compressed names, packet boundaries and pointer loops are checked. */
object DiscoveryProtocols {
    fun mdnsQuery(): ByteArray {
        val bytes = ByteArrayOutputStream()
        val out = DataOutputStream(bytes)
        out.writeShort(0); out.writeShort(0); out.writeShort(3)
        repeat(3) { out.writeShort(0) }
        for (service in listOf("_rtsp._tcp.local", "_onvif._tcp.local", "_http._tcp.local")) {
            service.split('.').forEach { label -> out.writeByte(label.length); out.writeBytes(label) }
            out.writeByte(0); out.writeShort(12); out.writeShort(0x8001) // PTR, unicast reply requested
        }
        return bytes.toByteArray()
    }

    fun mdnsEvidence(packet: ByteArray): List<Evidence> = runCatching {
        fun u16(p: Int): Int { require(p >= 0 && p + 1 < packet.size); return ((packet[p].toInt() and 255) shl 8) or (packet[p + 1].toInt() and 255) }
        fun name(start: Int): Pair<String, Int> {
            var p = start
            var next = -1
            val visited = mutableSetOf<Int>()
            val labels = mutableListOf<String>()
            while (true) {
                require(p in packet.indices && visited.add(p) && visited.size <= 128)
                val n = packet[p].toInt() and 255
                if (n == 0) return labels.joinToString(".") to if (next >= 0) next else p + 1
                if (n and 0xc0 == 0xc0) {
                    if (next < 0) next = p + 2
                    p = u16(p) and 0x3fff
                } else {
                    require(n <= 63 && p + n < packet.size)
                    labels.add(String(packet, p + 1, n, Charsets.UTF_8)); p += n + 1
                }
            }
            @Suppress("UNREACHABLE_CODE") "" to p
        }
        require(packet.size >= 12 && u16(2) and 0x8000 != 0)
        var p = 12
        val questions = u16(4)
        val count = u16(6) + u16(8) + u16(10)
        require(questions <= 32 && count <= 256)
        repeat(questions) { p = name(p).second + 4; require(p <= packet.size) }
        val evidence = mutableListOf<Evidence>()
        repeat(count) {
            val owner = name(p); p = owner.second
            val type = u16(p); val len = u16(p + 8); p += 10
            require(p + len <= packet.size)
            if (type == 12 && len > 0) {
                val service = owner.first.lowercase(Locale.ROOT)
                val instance = name(p).first.take(160)
                if (service in setOf("_rtsp._tcp.local", "_onvif._tcp.local", "_http._tcp.local")) {
                    evidence.add(Evidence("mDNS", "$service: $instance", service != "_http._tcp.local"))
                }
            }
            p += len
        }
        evidence.distinct()
    }.getOrDefault(emptyList())

    fun ssdpEvidence(response: String): List<Evidence> {
        if (!response.startsWith("HTTP/1.1 200", ignoreCase = true)) return emptyList()
        val headers = response.lineSequence().drop(1).mapNotNull {
            val index = it.indexOf(':')
            if (index <= 0) null else it.substring(0, index).trim().lowercase(Locale.ROOT) to it.substring(index + 1).trim().take(200)
        }.toMap()
        val service = headers["st"] ?: return emptyList()
        return listOf(Evidence("SSDP", "Advertised service: $service"))
    }

    fun isRtsp(response: String): Boolean = Regex("^RTSP/1\\.0 [1-5][0-9]{2}(?: |\\r|\\n)").containsMatchIn(response)
}
