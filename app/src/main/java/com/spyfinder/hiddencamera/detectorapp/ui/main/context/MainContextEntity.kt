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
    // 将扫描进度提升到全局上下文，避免结果页返回后扫描页被重建时进度丢失。
    val detectProgress = mutableIntStateOf(0)

    // DetectPage
    val suspiciousDevices = mutableStateListOf<WifiDevice>()
    val trustedDevices = mutableStateListOf<WifiDevice>()
    var hasScanHistory by mutableStateOf(false)
    val latestSuspiciousDevices = mutableStateListOf<WifiDevice>()
    val latestTrustedDevices = mutableStateListOf<WifiDevice>()

    val selectTabIndex = mutableIntStateOf(0)

    fun markDeviceAsSafe(device: WifiDevice) {
        // 创建更新后的设备副本（因为WifiDevice是 data class）
        val updatedDevice = device.copy(riskLevel = 0)

        // 同步更新当前展示结果和最近一次扫描结果，确保 History 页面展示最新状态。
        moveDeviceToSafeList(device, updatedDevice, suspiciousDevices, trustedDevices)
        if (hasScanHistory) {
            moveDeviceToSafeList(device, updatedDevice, latestSuspiciousDevices, latestTrustedDevices)
            persistLatestScanResult()
        }
    }

    fun saveLatestScanResult(suspiciousList: List<WifiDevice>, trustedList: List<WifiDevice>) {
        // 保存最近一次扫描结果，供首页 History 入口直接回看。
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

        // 应用启动时恢复最近一次扫描记录，供 History 入口跨重启访问。
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

        suspiciousDevices.clear()
        suspiciousDevices.addAll(latestSuspiciousDevices.map { it.copy() })
        trustedDevices.clear()
        trustedDevices.addAll(latestTrustedDevices.map { it.copy() })
        isStartDetect.value = true
        isAnimating.value = false
        // History 展示的是一轮已完成扫描，这里固定为 100%，确保返回扫描页时仍保持完成态。
        detectProgress.intValue = 100
        isShowResult.value = true
    }

    fun closeDetectResult() {
        // 关闭结果页时仅切回扫描页，保留当前扫描态，避免页面看起来像重新初始化。
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
        // 优先使用 mac 匹配；部分设备可能拿不到 mac，此时回退到 ip，避免“Mark as safe”不生效。
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
