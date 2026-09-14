package com.spyfinder.hiddencamera.detectorapp.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import com.spyfinder.hiddencamera.detectorapp.scan.Finding

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
    var riskLevel: Int = 0, // Compatibility only: 1 means camera-related evidence, never confirmed danger.
    val finding: Finding = Finding.INSUFFICIENT,
    val evidence: List<String> = emptyList(),
    val userTrusted: Boolean = false,
    val isCurrentPhone: Boolean = false,
    val analysisComplete: Boolean = true,
    val ruleVersion: Int = 1
) : Parcelable
