package com.spyfinder.hiddencamera.detectorapp.scan

import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.net.InetAddress
import java.util.Locale

sealed interface MdnsRecord {
    val owner: String
    val alive: Boolean
    data class Ptr(override val owner: String, val instance: String, override val alive: Boolean) : MdnsRecord
    data class Srv(override val owner: String, val host: String, val port: Int, override val alive: Boolean) : MdnsRecord
    data class Address(override val owner: String, val ip: String, override val alive: Boolean) : MdnsRecord
}

object MdnsPacket {
    val services = setOf("_rtsp._tcp.local", "_onvif._tcp.local", "_http._tcp.local")
    data class Question(val name: String, val type: Int)
    private fun labels(name: String): List<String> {
        val result = mutableListOf<String>()
        val label = StringBuilder()
        var escaped = false
        for (ch in name) {
            if (escaped) { label.append(ch); escaped = false }
            else when (ch) {
                '\\' -> escaped = true
                '.' -> { result.add(label.toString()); label.clear() }
                else -> label.append(ch)
            }
        }
        require(!escaped)
        result.add(label.toString())
        return result
    }
    fun displayName(name: String): String = labels(name).joinToString(".")
    fun query(questions: List<Question>): ByteArray {
        require(questions.size <= 32)
        val bytes = ByteArrayOutputStream()
        DataOutputStream(bytes).use { out ->
            out.writeShort(0); out.writeShort(0); out.writeShort(questions.size); repeat(3) { out.writeShort(0) }
            questions.forEach { question ->
                labels(question.name).forEach { label ->
                    val encoded = label.toByteArray(Charsets.UTF_8)
                    require(encoded.size in 1..63)
                    out.writeByte(encoded.size); out.write(encoded)
                }
                out.writeByte(0); out.writeShort(question.type); out.writeShort(0x8001)
            }
        }
        return bytes.toByteArray()
    }
    fun records(packet: ByteArray): List<MdnsRecord> = runCatching {
        fun u16(p: Int): Int { require(p >= 0 && p + 1 < packet.size); return ((packet[p].toInt() and 255) shl 8) or (packet[p + 1].toInt() and 255) }
        fun name(start: Int): Pair<String, Int> {
            var p = start; var next = -1; var length = 0
            val visited = mutableSetOf<Int>(); val labels = mutableListOf<String>()
            while (true) {
                require(p in packet.indices && visited.add(p) && visited.size <= 128)
                val n = packet[p].toInt() and 255
                if (n == 0) return labels.joinToString(".") { it.replace("\\", "\\\\").replace(".", "\\.") }
                    .lowercase(Locale.ROOT) to if (next >= 0) next else p + 1
                if (n and 0xc0 == 0xc0) {
                    if (next < 0) next = p + 2
                    p = u16(p) and 0x3fff
                } else {
                    require(n <= 63 && p + n < packet.size)
                    length += n + 1; require(length <= 254)
                    labels.add(String(packet, p + 1, n, Charsets.UTF_8)); p += n + 1
                }
            }
            @Suppress("UNREACHABLE_CODE") "" to p
        }
        require(packet.size in 12..16_384 && u16(2) and 0x800f == 0x8000)
        var p = 12
        val questions = u16(4); val count = u16(6) + u16(8) + u16(10)
        require(questions <= 32 && count <= 256)
        repeat(questions) { p = name(p).second + 4; require(p <= packet.size) }
        val result = mutableListOf<MdnsRecord>()
        repeat(count) {
            val owner = name(p); p = owner.second
            val type = u16(p); val internet = u16(p + 2) and 0x7fff == 1
            val alive = u16(p + 4) != 0 || u16(p + 6) != 0
            val len = u16(p + 8); p += 10; val end = p + len
            require(end <= packet.size)
            if (internet) when (type) {
                12 -> { val instance = name(p); require(instance.second == end); result.add(MdnsRecord.Ptr(owner.first, instance.first, alive)) }
                33 -> {
                    require(len >= 7)
                    val host = name(p + 6); require(host.second == end)
                    val port = u16(p + 4)
                    if (port > 0 && host.first.isNotEmpty()) result.add(MdnsRecord.Srv(owner.first, host.first, port, alive))
                }
                1, 28 -> {
                    require(len == if (type == 1) 4 else 16)
                    result.add(MdnsRecord.Address(owner.first, InetAddress.getByAddress(packet.copyOfRange(p, end)).hostAddress!!, alive))
                }
            }
            p = end
        }
        result
    }.getOrDefault(emptyList())
}

data class ServiceProbe(val port: Int, val protocol: String)
data class MdnsEndpoint(val ip: String, val probe: ServiceProbe, val evidence: Evidence)

/** Session-local cache joins split DNS packets without ever assuming the sender hosts the service. */
class MdnsDiscovery(private val limit: Int = 1024) {
    private val records = linkedMapOf<String, MdnsRecord>()
    private val asked = mutableSetOf<MdnsPacket.Question>()
    var limited = false; private set
    fun accept(packet: ByteArray) {
        MdnsPacket.records(packet).forEach { record ->
            val key = when (record) {
                is MdnsRecord.Ptr -> "P:${record.owner}:${record.instance}"
                is MdnsRecord.Srv -> "S:${record.owner}"
                is MdnsRecord.Address -> "A:${record.owner}:${record.ip}"
            }
            if (!record.alive) records.remove(key)
            else if (key in records || records.size < limit) records[key] = record
            else limited = true
        }
    }
    private fun instances() = records.values.filterIsInstance<MdnsRecord.Ptr>().filter { it.owner in MdnsPacket.services }
    fun questions(): List<MdnsPacket.Question> {
        val targets = records.values.filterIsInstance<MdnsRecord.Srv>().associateBy { it.owner }
        val addresses = records.values.filterIsInstance<MdnsRecord.Address>().filter { ScanRules.ipv4(it.ip) != null }.map { it.owner }.toSet()
        return instances().mapNotNull { ptr ->
            val srv = targets[ptr.instance]
            if (srv == null) MdnsPacket.Question(ptr.instance, 33)
            else if (srv.host !in addresses) MdnsPacket.Question(srv.host, 1) else null
        }.distinct().filter { it !in asked }.take(16).also { batch ->
            if (asked.size + batch.size <= limit) asked.addAll(batch) else limited = true
        }.takeIf { !limited }.orEmpty()
    }
    fun endpoints(local: String, prefix: Int): List<MdnsEndpoint> {
        val targets = records.values.filterIsInstance<MdnsRecord.Srv>().associateBy { it.owner }
        val addresses = records.values.filterIsInstance<MdnsRecord.Address>().groupBy { it.owner }
        return instances().flatMap { ptr ->
            val srv = targets[ptr.instance] ?: return@flatMap emptyList()
            addresses[srv.host].orEmpty().filter { ScanRules.usableHost(it.ip, local, prefix) }.map {
                MdnsEndpoint(it.ip, ServiceProbe(srv.port, if (ptr.owner == "_rtsp._tcp.local") "RTSP" else "HTTP"),
                    Evidence("mDNS", "${ptr.owner}: ${MdnsPacket.displayName(ptr.instance)} (${MdnsPacket.displayName(srv.host)}:${srv.port})", ptr.owner != "_http._tcp.local"))
            }
        }.distinct()
    }
    fun unresolved(local: String, prefix: Int): Int {
        val targets = records.values.filterIsInstance<MdnsRecord.Srv>().associateBy { it.owner }
        val addresses = records.values.filterIsInstance<MdnsRecord.Address>().groupBy { it.owner }
        return instances().count { ptr ->
            val srv = targets[ptr.instance]
            srv == null || addresses[srv.host].orEmpty().none { ScanRules.usableHost(it.ip, local, prefix) }
        }
    }
}
