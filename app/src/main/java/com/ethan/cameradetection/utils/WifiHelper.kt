package com.ethan.cameradetection.utils

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities

object WifiHelper {

    fun getWifiSSID(context: Context): String? {
        return try {
            val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                val network = connectivityManager.activeNetwork
                val capabilities = connectivityManager.getNetworkCapabilities(network)

                if (capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true) {
                    val wifiManager = context.applicationContext.getSystemService(
                        Context.WIFI_SERVICE
                    ) as android.net.wifi.WifiManager

                    val wifiInfo = wifiManager.connectionInfo
                    val ssid = wifiInfo.ssid

                    // 移除双引号
                    ssid/*.removeSurrounding("\"")*/
                } else {
                    null
                }
            } else {
                // 兼容旧版本
                val wifiManager = context.applicationContext.getSystemService(
                    Context.WIFI_SERVICE
                ) as android.net.wifi.WifiManager

                val wifiInfo = wifiManager.connectionInfo
                val ssid = wifiInfo.ssid
                ssid/*.removeSurrounding("\"")*/
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}