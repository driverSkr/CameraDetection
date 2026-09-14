package com.spyfinder.hiddencamera.detectorapp.utils

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.spyfinder.hiddencamera.detectorapp.model.WifiInfo
import java.net.Inet4Address

object WifiHelper {
    fun isWifiEnabled(context: Context): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        return cm.allNetworks.any { cm.getNetworkCapabilities(it)?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true }
    }
    fun showWifiInfo(context: Context): WifiInfo {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = cm.allNetworks.firstOrNull { cm.getNetworkCapabilities(it)?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true }
        val ip = network?.let { cm.getLinkProperties(it)?.linkAddresses?.firstOrNull { a -> a.address is Inet4Address }?.address?.hostAddress }
        // Scanning needs the selected network address, not location-sensitive SSID metadata.
        return WifiInfo(if (network == null) "Not connected" else "Wi-Fi", ip ?: "")
    }
}
