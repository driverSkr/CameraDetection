package com.spyfinder.hiddencamera.detectorapp.utils

import android.content.Context
import android.util.Log
import androidx.core.content.edit
import com.spyfinder.hiddencamera.detectorapp.model.WifiDevice
import org.json.JSONArray
import org.json.JSONObject

data class LatestScanHistory(
    val suspiciousDevices: List<WifiDevice>,
    val trustedDevices: List<WifiDevice>
)

object ScanHistoryStore {
    private const val TAG = "ScanHistoryStore"
    private const val PREF_NAME = "sp_detect_scan_history"
    private const val KEY_SUSPICIOUS_DEVICES = "key_suspicious_devices"
    private const val KEY_TRUSTED_DEVICES = "key_trusted_devices"

    fun saveLatestScanResult(
        context: Context,
        suspiciousDevices: List<WifiDevice>,
        trustedDevices: List<WifiDevice>
    ) {
        runCatching {
            val sharedPreferences = context.applicationContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            val suspiciousJson = serializeDeviceList(suspiciousDevices).toString()
            val trustedJson = serializeDeviceList(trustedDevices).toString()
            sharedPreferences.edit {
                putString(KEY_SUSPICIOUS_DEVICES, suspiciousJson)
                putString(KEY_TRUSTED_DEVICES, trustedJson)
            }
            Log.d(TAG, "最近一次扫描记录已保存，suspicious=${suspiciousDevices.size} trusted=${trustedDevices.size}")
        }.onFailure { throwable ->
            Log.e(TAG, "保存最近一次扫描记录失败", throwable)
        }
    }

    fun loadLatestScanResult(context: Context): LatestScanHistory? {
        return runCatching {
            val sharedPreferences = context.applicationContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            val suspiciousJson = sharedPreferences.getString(KEY_SUSPICIOUS_DEVICES, null)
            val trustedJson = sharedPreferences.getString(KEY_TRUSTED_DEVICES, null)
            if (suspiciousJson.isNullOrEmpty() || trustedJson.isNullOrEmpty()) {
                return null
            }

            LatestScanHistory(
                suspiciousDevices = deserializeDeviceList(suspiciousJson),
                trustedDevices = deserializeDeviceList(trustedJson)
            )
        }.onFailure { throwable ->
            Log.e(TAG, "读取最近一次扫描记录失败，已忽略损坏数据", throwable)
        }.getOrNull()
    }

    private fun serializeDeviceList(devices: List<WifiDevice>): JSONArray {
        val jsonArray = JSONArray()
        devices.forEach { device ->
            jsonArray.put(
                JSONObject().apply {
                    put("name", device.name)
                    put("type", device.type)
                    put("ip", device.ip)
                    put("iconRes", device.iconRes)
                    put("signal", device.signal)
                    put("signalColor", device.signalColor)
                    put("brandModel", device.brandModel)
                    put("mac", device.mac)
                    put("connected", device.connected)
                    put("rssi", device.rssi)
                    put("riskLevel", device.riskLevel)
                }
            )
        }
        return jsonArray
    }

    private fun deserializeDeviceList(json: String): List<WifiDevice> {
        val result = mutableListOf<WifiDevice>()
        val jsonArray = JSONArray(json)
        for (index in 0 until jsonArray.length()) {
            val jsonObject = jsonArray.optJSONObject(index) ?: continue
            result.add(
                WifiDevice(
                    name = jsonObject.optString("name"),
                    type = jsonObject.optString("type"),
                    ip = jsonObject.optString("ip"),
                    iconRes = jsonObject.optInt("iconRes"),
                    signal = jsonObject.optInt("signal"),
                    signalColor = jsonObject.optInt("signalColor"),
                    brandModel = jsonObject.optString("brandModel"),
                    mac = jsonObject.optString("mac"),
                    connected = jsonObject.optBoolean("connected", true),
                    rssi = jsonObject.optInt("rssi"),
                    riskLevel = jsonObject.optInt("riskLevel")
                )
            )
        }
        return result
    }
}
