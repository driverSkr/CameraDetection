package com.spyfinder.hiddencamera.detectorapp.scan

import android.net.NetworkCapabilities

const val SCAN_CONNECT_WIFI = "Connect to Wi-Fi before scanning."
const val SCAN_MULTIPLE_NETWORKS = "Multiple Wi-Fi networks are available. Select one network and retry."
const val SCAN_AIRPLANE_MODE = "Turn off Airplane mode and connect to Wi-Fi before scanning."
const val SCAN_DISCONNECT_VPN = "Disconnect VPN to scan the local network."

/** VPN capabilities can include the transport of their underlying Wi-Fi network. */
fun isLocalWifi(capabilities: NetworkCapabilities?): Boolean = capabilities?.let { isLocalWifi(it::hasTransport) } ?: false

internal fun isLocalWifi(hasTransport: (Int) -> Boolean): Boolean =
    hasTransport(NetworkCapabilities.TRANSPORT_WIFI) && !hasTransport(NetworkCapabilities.TRANSPORT_VPN)

/** Null means a single local Wi-Fi target is ready to resolve. */
internal fun scanPreflightMessage(
    airplane: Boolean,
    localWifiCount: Int,
    hasWifi: Boolean,
    hasVpn: Boolean,
): String? = when {
    localWifiCount == 1 -> null
    localWifiCount > 1 -> SCAN_MULTIPLE_NETWORKS
    airplane -> SCAN_AIRPLANE_MODE
    hasWifi && hasVpn -> SCAN_DISCONNECT_VPN
    else -> SCAN_CONNECT_WIFI
}
