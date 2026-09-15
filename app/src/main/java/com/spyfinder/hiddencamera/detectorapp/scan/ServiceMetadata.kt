package com.spyfinder.hiddencamera.detectorapp.scan

/** Self-reported metadata, never a basis for camera classification or manufacturer inference. */
object ServiceMetadata {
    fun clean(value: String) = value.filter { !it.isISOControl() }.trim().take(200)
    fun headers(response: String): Map<String, String> = response.lineSequence().drop(1)
        .takeWhile { it.isNotBlank() }.take(64).mapNotNull {
            val separator = it.indexOf(':')
            if (separator <= 0) null else it.substring(0, separator).trim().lowercase(java.util.Locale.ROOT) to clean(it.substring(separator + 1))
        }.toMap()
    fun ssdp(response: String): Map<String, String> {
        if (!response.startsWith("HTTP/1.1 200", true)) return emptyMap()
        val headers = headers(response)
        if (headers["st"].isNullOrBlank()) return emptyMap()
        return listOf("server", "st").mapNotNull { key -> headers[key]?.takeIf { it.isNotBlank() }?.let { "ssdp_$key" to it } }.toMap()
    }
}
