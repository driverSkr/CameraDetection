package com.spyfinder.hiddencamera.detectorapp.scan

import java.util.Locale

/** Bounded DNS parsing: compressed names, packet boundaries and pointer loops are checked. */
object DiscoveryProtocols {
    fun mdnsQuery(): ByteArray = MdnsPacket.query(MdnsPacket.services.map { MdnsPacket.Question(it, 12) })

    // Compatibility helper for protocol-only callers. Device attribution uses MdnsDiscovery.
    fun mdnsEvidence(packet: ByteArray): List<Evidence> = MdnsPacket.records(packet)
        .filterIsInstance<MdnsRecord.Ptr>().filter { it.alive && it.owner in MdnsPacket.services }
        .map { Evidence("mDNS", "${it.owner}: ${it.instance}", it.owner in setOf("_rtsp._tcp.local", "_onvif._tcp.local")) }.distinct()

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
