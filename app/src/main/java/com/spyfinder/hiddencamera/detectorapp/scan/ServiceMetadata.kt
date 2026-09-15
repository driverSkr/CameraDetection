package com.spyfinder.hiddencamera.detectorapp.scan

/** Self-reported metadata, never a basis for camera classification or manufacturer inference. */
object ServiceMetadata {
    /** A connectivity-only result must never erase an observed application protocol. */
    fun mergeProbeDetails(reports: List<Map<String, String>>): Map<String, String> =
        reports.flatMap { it.entries }.groupBy({ it.key }, { it.value }).mapValues { (key, values) ->
            if (key.startsWith("port_")) {
                val protocols = values.flatMap { it.split(" / ") }.filter(String::isNotBlank).distinct().sorted()
                protocols.filter { it != "TCP" }.ifEmpty { protocols }.joinToString(" / ")
            } else values.filter(String::isNotBlank).distinct().sorted().joinToString("\n")
        }
    fun merge(previous: Map<String, String>, incoming: Map<String, String>): Map<String, String> = previous + incoming.mapValues { (key, value) ->
        if (key in setOf("mdns_name", "mdns_host", "mdns_device_type"))
            (previous[key].orEmpty().lines() + value.lines()).filter { it.isNotBlank() }.map(::clean).distinct().sorted().take(16).joinToString("\n")
        else clean(value)
    }
    fun clean(value: String) = value.filter { !it.isISOControl() }.trim().take(200)
    fun headers(response: String): Map<String, String> = response.lineSequence().drop(1)
        .takeWhile { it.isNotBlank() }.take(64).mapNotNull {
            val separator = it.indexOf(':')
            if (separator <= 0) null else {
                val key = it.substring(0, separator).trim().lowercase(java.util.Locale.ROOT)
                val value = it.substring(separator + 1).trim()
                if (key == "location") {
                    if (value.length > 2048 || value.any(Char::isISOControl)) null else key to value
                } else key to clean(value)
            }
        }.toMap()
    fun ssdp(response: String): Map<String, String> {
        if (!response.startsWith("HTTP/1.1 200", true)) return emptyMap()
        val headers = headers(response)
        if (headers["st"].isNullOrBlank()) return emptyMap()
        return listOf("server", "st").mapNotNull { key -> headers[key]?.takeIf { it.isNotBlank() }?.let { "ssdp_$key" to it } }.toMap()
    }
}
