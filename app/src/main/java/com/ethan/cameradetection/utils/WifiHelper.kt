package com.ethan.cameradetection.utils

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import android.os.Build
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.ethan.cameradetection.model.WifiInfo

object WifiHelper {

    // 检测WIFI是否已连接
    fun isWifiEnabled(context: Context): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = cm.activeNetwork ?: return false
        val nc = cm.getNetworkCapabilities(network) ?: return false
        return nc.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
    }

    // 检测WI-FI权限
    fun checkWifiPermission(context: Context) {
        if (!isWifiEnabled(context)) {
            Toast.makeText(context, "Please connect to wifi first", Toast.LENGTH_LONG).show()
            return
        }
        // 权限检查
        val needNearby = Build.VERSION.SDK_INT >= 33
        val hasLocation = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val hasNearby = !needNearby || ContextCompat.checkSelfPermission(context, Manifest.permission.NEARBY_WIFI_DEVICES) == PackageManager.PERMISSION_GRANTED
        if (!hasNearby || !hasLocation) {
            val permission = mutableListOf(Manifest.permission.ACCESS_FINE_LOCATION)
            if (needNearby) permission.add(Manifest.permission.NEARBY_WIFI_DEVICES)
            // todo 写到这里
        }
    }

    fun showWifiInfo(context: Context): WifiInfo {
        val wifiManager = context.getSystemService(Context.WIFI_SERVICE) as WifiManager
        val wifiInfo = wifiManager.connectionInfo
        val ssid = wifiInfo.ssid?.removeSurrounding("\"") ?: "Unknown"
        val ipInt = wifiInfo.ipAddress
        val ip = String.format(
            "%d.%d.%d.%d",
            ipInt and 0xff,
            ipInt shr 8 and 0xff,
            ipInt shr 16 and 0xff,
            ipInt shr 24 and 0xff
        )
        return WifiInfo(ssid, ip)
    }

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