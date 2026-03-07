package com.spyfinder.hiddencamera.detectorapp.ui.main.context

import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.spyfinder.hiddencamera.detectorapp.model.WifiDevice

class MainContextEntity {
    var isOpenMainPage by mutableStateOf(false)
    var isStartDetect = mutableStateOf(false)
    var isAnimating = mutableStateOf(false)
    var isShowResult = mutableStateOf(false)

    // DetectPage
    val suspiciousDevices = mutableStateListOf<WifiDevice>()
    val trustedDevices = mutableStateListOf<WifiDevice>()

    val selectTabIndex = mutableIntStateOf(0)

    fun markDeviceAsSafe(device: WifiDevice) {
        // 创建更新后的设备副本（因为WifiDevice是data class）
        val updatedDevice = device.copy(riskLevel = 0)

        // 从suspiciousDevices中移除原设备
        val suspiciousIndex = suspiciousDevices.indexOfFirst { it.mac == device.mac }
        if (suspiciousIndex != -1) {
            suspiciousDevices.removeAt(suspiciousIndex)
        }

        trustedDevices.add(updatedDevice)
    }
}

val LocalMainContextEntity = compositionLocalOf { MainContextEntity() }