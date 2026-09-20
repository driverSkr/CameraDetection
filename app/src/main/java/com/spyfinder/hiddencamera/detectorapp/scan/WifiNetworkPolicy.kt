package com.spyfinder.hiddencamera.detectorapp.scan

import android.net.NetworkCapabilities
import androidx.annotation.StringRes
import com.spyfinder.hiddencamera.detectorapp.R

/** VPN capabilities can include the transport of their underlying Wi-Fi network. */
fun isLocalWifi(capabilities: NetworkCapabilities?): Boolean = capabilities?.let { isLocalWifi(it::hasTransport) } ?: false

internal fun isLocalWifi(hasTransport: (Int) -> Boolean): Boolean =
    hasTransport(NetworkCapabilities.TRANSPORT_WIFI) && !hasTransport(NetworkCapabilities.TRANSPORT_VPN)

/** Null means a single local Wi-Fi target is ready to resolve. */
@StringRes internal fun scanPreflightMessage(
    airplane: Boolean,
    localWifiCount: Int,
    hasWifi: Boolean,
    hasVpn: Boolean,
): Int? = when {
    localWifiCount == 1 -> null
    localWifiCount > 1 -> R.string.scan_multiple_networks
    airplane -> R.string.scan_airplane
    hasWifi && hasVpn -> R.string.scan_disconnect_vpn
    else -> R.string.scan_connect_wifi
}
