package com.spyfinder.hiddencamera.detectorapp.scan

import android.net.NetworkCapabilities

/** VPN capabilities can include the transport of their underlying Wi-Fi network. */
fun isLocalWifi(capabilities: NetworkCapabilities?): Boolean = capabilities?.let { isLocalWifi(it::hasTransport) } ?: false

internal fun isLocalWifi(hasTransport: (Int) -> Boolean): Boolean =
    hasTransport(NetworkCapabilities.TRANSPORT_WIFI) && !hasTransport(NetworkCapabilities.TRANSPORT_VPN)
