package com.spyfinder.hiddencamera.detectorapp.scan

import java.io.File
import java.net.NetworkInterface

/** Best-effort MAC for a LAN address. Modern Android often hides neighbor MACs. */
object MacLookup {
    private val dummy = setOf("02:00:00:00:00:00", "00:00:00:00:00:00", "ff:ff:ff:ff:ff:ff")

    fun forAddress(ip: String): String = localMac(ip) ?: arpMac(ip).orEmpty()

    internal fun format(bytes: ByteArray): String? {
        if (bytes.size != 6 || bytes.all { it == 0.toByte() }) return null
        val value = bytes.joinToString(":") { "%02x".format(it) }
        return value.takeUnless { it in dummy }
    }

    internal fun parseArpMac(table: String, ip: String): String? {
        table.lineSequence().forEach { line ->
            val parts = line.trim().split(Regex("\\s+"))
            if (parts.size < 4 || parts[0] != ip) return@forEach
            val flags = parts[2].removePrefix("0x").toIntOrNull(16) ?: 0
            val mac = parts[3].lowercase()
            if (flags != 0 && mac !in dummy && mac.count { it == ':' } == 5) return mac
        }
        return null
    }

    private fun localMac(ip: String): String? = runCatching {
        NetworkInterface.getNetworkInterfaces()?.toList().orEmpty().firstNotNullOfOrNull { nif ->
            val matches = nif.inetAddresses.toList().any { it.hostAddress?.substringBefore('%') == ip }
            if (matches) format(nif.hardwareAddress ?: return@firstNotNullOfOrNull null) else null
        }
    }.getOrNull()

    private fun arpMac(ip: String): String? = runCatching {
        parseArpMac(File("/proc/net/arp").takeIf { it.canRead() }?.readText().orEmpty(), ip)
    }.getOrNull()
}
