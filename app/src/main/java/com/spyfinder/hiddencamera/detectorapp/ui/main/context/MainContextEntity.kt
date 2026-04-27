package com.spyfinder.hiddencamera.detectorapp.ui.main.context

import android.content.Context
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.setValue
import com.spyfinder.hiddencamera.detectorapp.DetectorApp
import com.spyfinder.hiddencamera.detectorapp.model.WifiDevice
import com.spyfinder.hiddencamera.detectorapp.utils.ScanHistoryStore

class MainContextEntity(
    private val appContext: Context? = DetectorApp.INSTANCE?.applicationContext
) {
    var isOpenMainPage by mutableStateOf(false)
    var isStartDetect = mutableStateOf(false)
    var isAnimating = mutableStateOf(false)
    var isShowResult = mutableStateOf(false)
    var isShowingLatestHistoryResult by mutableStateOf(false)
    val detectProgress = mutableIntStateOf(0)

    val suspiciousDevices = mutableStateListOf<WifiDevice>()
    val trustedDevices = mutableStateListOf<WifiDevice>()
    var hasScanHistory by mutableStateOf(false)
    val latestSuspiciousDevices = mutableStateListOf<WifiDevice>()
    val latestTrustedDevices = mutableStateListOf<WifiDevice>()

    val selectTabIndex = mutableIntStateOf(0)
    val pendingWifiAutoScan = mutableStateOf(false)

    val resultSuspiciousDevices: SnapshotStateList<WifiDevice>
        get() = if (isShowingLatestHistoryResult) latestSuspiciousDevices else suspiciousDevices

    val resultTrustedDevices: SnapshotStateList<WifiDevice>
        get() = if (isShowingLatestHistoryResult) latestTrustedDevices else trustedDevices

    fun markDeviceAsSafe(device: WifiDevice) {
        val updatedDevice = device.copy(riskLevel = 0)

        if (isShowingLatestHistoryResult) {
            moveDeviceToSafeList(device, updatedDevice, latestSuspiciousDevices, latestTrustedDevices)
            persistLatestScanResult()
            return
        }

        moveDeviceToSafeList(device, updatedDevice, suspiciousDevices, trustedDevices)
        if (hasScanHistory) {
            moveDeviceToSafeList(device, updatedDevice, latestSuspiciousDevices, latestTrustedDevices)
            persistLatestScanResult()
        }
    }

    fun saveLatestScanResult(suspiciousList: List<WifiDevice>, trustedList: List<WifiDevice>) {
        latestSuspiciousDevices.clear()
        latestSuspiciousDevices.addAll(suspiciousList.map { it.copy() })
        latestTrustedDevices.clear()
        latestTrustedDevices.addAll(trustedList.map { it.copy() })
        hasScanHistory = true
        persistLatestScanResult()
    }

    fun restoreLatestScanResult() {
        val context = appContext ?: return
        val latestScanHistory = ScanHistoryStore.loadLatestScanResult(context) ?: return

        latestSuspiciousDevices.clear()
        latestSuspiciousDevices.addAll(latestScanHistory.suspiciousDevices.map { it.copy() })
        latestTrustedDevices.clear()
        latestTrustedDevices.addAll(latestScanHistory.trustedDevices.map { it.copy() })
        hasScanHistory = true
    }

    fun openLatestResult() {
        if (!hasScanHistory) {
            return
        }
        isShowingLatestHistoryResult = true
        isShowResult.value = true
    }

    fun openCurrentResult() {
        isShowingLatestHistoryResult = false
        isShowResult.value = true
    }

    fun closeDetectResult() {
        isShowingLatestHistoryResult = false
        isShowResult.value = false
    }

    private fun moveDeviceToSafeList(
        device: WifiDevice,
        updatedDevice: WifiDevice,
        suspiciousList: SnapshotStateList<WifiDevice>,
        trustedList: SnapshotStateList<WifiDevice>
    ) {
        val suspiciousIndex = suspiciousList.indexOfFirst { currentDevice ->
            isSameDevice(currentDevice, device)
        }
        if (suspiciousIndex != -1) {
            suspiciousList.removeAt(suspiciousIndex)
        }

        val trustedIndex = trustedList.indexOfFirst { currentDevice ->
            isSameDevice(currentDevice, updatedDevice)
        }
        if (trustedIndex != -1) {
            trustedList[trustedIndex] = updatedDevice
        } else {
            trustedList.add(updatedDevice)
        }
    }

    private fun isSameDevice(left: WifiDevice, right: WifiDevice): Boolean {
        return when {
            left.mac.isNotBlank() && right.mac.isNotBlank() -> left.mac == right.mac
            left.ip.isNotBlank() && right.ip.isNotBlank() -> left.ip == right.ip
            else -> left.name == right.name && left.type == right.type
        }
    }

    private fun persistLatestScanResult() {
        val context = appContext ?: return
        ScanHistoryStore.saveLatestScanResult(
            context = context,
            suspiciousDevices = latestSuspiciousDevices,
            trustedDevices = latestTrustedDevices
        )
    }
}

val LocalMainContextEntity = compositionLocalOf { MainContextEntity() }
