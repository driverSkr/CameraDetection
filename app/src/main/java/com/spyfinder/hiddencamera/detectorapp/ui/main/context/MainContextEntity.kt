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
import com.spyfinder.hiddencamera.detectorapp.scan.ScanStatus

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
    var scanStatus by mutableStateOf(ScanStatus.IDLE)
    var scanMessage by mutableStateOf("Ready to check your Wi-Fi network")
    var latestMessage by mutableStateOf("")
    var networkLabel by mutableStateOf("")

    val resultSuspiciousDevices: SnapshotStateList<WifiDevice>
        get() = if (isShowingLatestHistoryResult) latestSuspiciousDevices else suspiciousDevices

    val resultTrustedDevices: SnapshotStateList<WifiDevice>
        get() = if (isShowingLatestHistoryResult) latestTrustedDevices else trustedDevices

    fun markDeviceAsSafe(device: WifiDevice) {
        val updatedDevice = device.copy(userTrusted = !device.userTrusted)

        // Trust is a user annotation, not evidence that changes the detection conclusion.
        val lists = if (isShowingLatestHistoryResult) listOf(latestSuspiciousDevices, latestTrustedDevices)
            else if (scanStatus == ScanStatus.COMPLETE) listOf(suspiciousDevices, trustedDevices, latestSuspiciousDevices, latestTrustedDevices)
            else listOf(suspiciousDevices, trustedDevices)
        lists.forEach { list ->
            val index = list.indexOfFirst { isSameDevice(it, device) }
            if (index >= 0) list[index] = updatedDevice
        }
        if (hasScanHistory) persistLatestScanResult()
    }

    fun saveLatestScanResult(suspiciousList: List<WifiDevice>, trustedList: List<WifiDevice>) {
        latestSuspiciousDevices.clear()
        latestSuspiciousDevices.addAll(suspiciousList.map { it.copy() })
        latestTrustedDevices.clear()
        latestTrustedDevices.addAll(trustedList.map { it.copy() })
        hasScanHistory = true
        latestMessage = scanMessage
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
        latestMessage = latestScanHistory.summary
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
            trustedDevices = latestTrustedDevices,
            summary = latestMessage
        )
    }
}

val LocalMainContextEntity = compositionLocalOf { MainContextEntity() }
