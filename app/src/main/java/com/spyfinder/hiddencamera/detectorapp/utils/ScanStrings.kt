package com.spyfinder.hiddencamera.detectorapp.utils

import android.content.Context
import com.spyfinder.hiddencamera.detectorapp.R

/**
 * Formats the scanner's canonical records at the display boundary. Existing history stays readable
 * in either language; translating a view never rewrites evidence, addresses or classification input.
 */
object ScanStrings {
    private val exact = mapOf(
        "Scan stopped because no probe made progress for 60 seconds. Results are incomplete. Please retry." to R.string.scan_stalled,
        " Coverage or evidence storage was limited; review the recorded scope." to R.string.scan_scope_limited,
        "Ready to check your Wi-Fi network" to R.string.scan_ready,
        "Preparing Wi-Fi scan…" to R.string.scan_preparing,
        "The previous scan was interrupted. Start a new scan." to R.string.scan_previous_interrupted,
        "Scan interrupted. Please retry." to R.string.scan_interrupted,
        "Scan interrupted while the app was in the background." to R.string.scan_background,
        "Scan replaced." to R.string.scan_replaced,
        "Wi-Fi changed or disconnected. Results are incomplete. Reconnect and retry." to R.string.scan_network_changed,
        "Scan timed out after 45 seconds. Results are incomplete; try again on a smaller network." to R.string.scan_timeout,
        "Scan failed. Reconnect to Wi-Fi and retry." to R.string.scan_failed,
        "Scan cancelled. Results are incomplete." to R.string.scan_cancelled,
        "Connect to Wi-Fi before scanning." to R.string.scan_connect_wifi,
        "Multiple Wi-Fi networks are available. Select one network and retry." to R.string.scan_multiple_networks,
        "Wi-Fi address is unavailable. Reconnect and retry." to R.string.scan_address_unavailable,
        "This network has no IPv4 address. IPv6 scanning is not supported yet." to R.string.scan_ipv6_unsupported,
        "Legacy scan record — rescan to obtain evidence." to R.string.scan_legacy_history,
        "Partially completed. " to R.string.scan_partial_prefix,
        "Scan completed. " to R.string.scan_complete_prefix,
        " Devices that do not respond or are isolated by the network may be missed. No result proves a room is safe." to R.string.scan_limits_note,
        "Video service" to R.string.device_video_service,
        "Phone" to R.string.device_phone,
        "Configured Wi-Fi gateway" to R.string.evidence_gateway,
        "Device advertises a network video transmitter; verify manually" to R.string.evidence_onvif,
        "Local" to R.string.evidence_source_local,
        "Network" to R.string.evidence_source_network,
        "Unknown" to R.string.unknown,
        "Router" to R.string.object_router,
        "Current phone" to R.string.current_phone
    )
    private val templates = listOf(
        Regex(" (\\d+) addresses could not be verified due to network errors\\.") to R.string.scan_unverified_addresses,
        Regex("Discovering devices in (.+)…") to R.string.scan_discovering_network,
        Regex("Discovering: (\\d+)/(\\d+) addresses checked") to R.string.scan_discovery_progress,
        Regex("Analyzing services: (\\d+)/(\\d+) devices") to R.string.scan_analysis_progress,
        Regex("Checked (\\d+) of (\\d+) IPv4 addresses; (\\d+) devices responded\\.") to R.string.scan_summary,
        Regex(" (\\d+) devices could not be fully analyzed\\.") to R.string.scan_incomplete_count,
        Regex("Port (\\d+) is open \\(service not confirmed\\)") to R.string.evidence_open_port,
        Regex("Video protocol responded on port (\\d+); verify the device manually") to R.string.evidence_rtsp,
        Regex("Web service responded on port (\\d+)") to R.string.evidence_http,
        Regex("Advertised service: (.+)") to R.string.evidence_ssdp
    )

    fun text(context: Context, value: String): String {
        if (context.resources.configuration.locales[0].language != "zh") return value
        return format(value) { id, args -> context.getString(id, *args.toTypedArray()) }
    }

    internal fun format(value: String, lookup: (Int, List<String>) -> String): String {
        exact[value]?.let { return lookup(it, emptyList()) }
        templates.forEach { (pattern, id) ->
            pattern.matchEntire(value)?.let { return lookup(id, it.groupValues.drop(1)) }
        }
        // Timestamp/network metadata is retained verbatim; only the summary is translated.
        if ('\n' in value) return value.split('\n').joinToString("\n") { format(it, lookup) }
        if (value.startsWith("Scan completed. ") || value.startsWith("Partially completed. ")) {
            var result = value
            templates.forEach { (pattern, id) ->
                result = pattern.replace(result) { lookup(id, it.groupValues.drop(1)) }
            }
            listOf("Scan completed. ", "Partially completed. ", " Coverage or evidence storage was limited; review the recorded scope.",
                " Devices that do not respond or are isolated by the network may be missed. No result proves a room is safe.").forEach {
                if (it in result) result = result.replace(it, lookup(exact.getValue(it), emptyList()))
            }
            return result
        }
        val source = value.substringBefore(": ", "")
        if (source in setOf("Local", "Network", "TCP", "RTSP", "HTTP", "SSDP", "ONVIF", "mDNS")) {
            val label = exact[source]?.let { lookup(it, emptyList()) } ?: source
            return "$label: ${format(value.substringAfter(": "), lookup)}"
        }
        return value // External device names, mDNS names, URLs and protocol identifiers are data.
    }
}
