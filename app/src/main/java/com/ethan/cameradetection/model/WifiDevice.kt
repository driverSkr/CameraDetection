package com.ethan.cameradetection.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class WifiDevice(
    var name: String,
    var type: String,
    val ip: String,
    var iconRes: Int,
    var signal: Int,    // 信号强度
    var signalColor: Int, // 信号颜色
    var brandModel: String = "",
    var mac: String = "",
    var connected: Boolean = true,
    var rssi: Int = 0,
    val riskLevel: Int = 0  // 0 = 安全，1 = 高风险
) : Parcelable
