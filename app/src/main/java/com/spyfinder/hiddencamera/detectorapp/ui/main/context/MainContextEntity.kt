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
import com.spyfinder.hiddencamera.detectorapp.utils.ScanArchive
import com.spyfinder.hiddencamera.detectorapp.utils.ScanRecord

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

    var currentRecordId = ""
    private var recordSuspicious: List<WifiDevice>? = null
    private var recordTrusted: List<WifiDevice>? = null
    private var recordSnapshot = emptyList<WifiDevice>()
    fun recordDevices(): List<WifiDevice> {
        val suspicious = suspiciousDevices.toList()
        val trusted = trustedDevices.toList()
        if (suspicious !== recordSuspicious || trusted !== recordTrusted) {
            recordSuspicious = suspicious; recordTrusted = trusted
            recordSnapshot = (suspicious + trusted).map { it.copy() }
        }
        return recordSnapshot
    }
    var archive by mutableStateOf(ScanArchive())
        private set
    var showingCompleteHistory by mutableStateOf(false)
        private set
    val hasHistoryChoice get() = archive.recent != null && archive.complete != null && archive.recent?.id != archive.complete?.id
    val displayedHistory get() = if (showingCompleteHistory) archive.complete else archive.recent

    fun markDeviceAsSafe(device: WifiDevice) {
        if (device.isCurrentPhone) return
        val recordId = if (isShowingLatestHistoryResult) displayedHistory?.id else currentRecordId
        if (recordId == null) return
        val trusted = !device.userTrusted
        if (recordId == currentRecordId) {
            listOf(suspiciousDevices, trustedDevices).forEach { list ->
                val index = list.indexOfFirst { it.ip == device.ip }
                if (index >= 0) list[index] = list[index].copy(userTrusted = trusted)
            }
        }
        archive = archive.trust(recordId, device.ip, trusted)
        refreshHistory()
        persist()
    }

    fun saveRecord(record: ScanRecord) {
        val updated = archive.record(record)
        if (updated == archive) return
        archive = updated
        refreshHistory()
        persist()
    }
    suspend fun restoreLatestScanResult() {
        val context = appContext ?: return
        archive = ScanHistoryStore.load(context)
        refreshHistory()
    }
    private fun refreshHistory() {
        hasScanHistory = archive.recent != null || archive.complete != null
        if (archive.recent == null) showingCompleteHistory = true
        val record = displayedHistory
        latestSuspiciousDevices.replaceDevices(record?.devices.orEmpty().filter { it.riskLevel == 1 })
        latestTrustedDevices.replaceDevices(record?.devices.orEmpty().filter { it.riskLevel != 1 })
        latestMessage = record?.summary.orEmpty()
    }
    fun selectHistory(complete: Boolean) {
        showingCompleteHistory = complete && archive.complete != null
        refreshHistory()
    }
    fun openLatestResult() {
        if (!hasScanHistory || scanStatus == ScanStatus.RUNNING) return
        selectHistory(false)
        isShowingLatestHistoryResult = true
        isShowResult.value = true
    }
    fun openCurrentResult() {
        isShowingLatestHistoryResult = false
        isShowResult.value = true
    }
    fun openWifiFeature() {
        pendingWifiAutoScan.value = scanStatus != ScanStatus.RUNNING
        selectTabIndex.intValue = 0
    }
    fun closeDetectResult() {
        isShowingLatestHistoryResult = false
        isShowResult.value = false
    }
    fun retryHistorySave() = persist()
    suspend fun retryHistoryLoad() {
        if (scanStatus == ScanStatus.RUNNING) return
        val previous = archive
        val loaded = appContext?.let { ScanHistoryStore.load(it) } ?: return
        if (scanStatus != ScanStatus.RUNNING && archive === previous &&
            (loaded.recent != null || loaded.complete != null)) {
            archive = loaded
            refreshHistory()
        }
    }
    private fun persist() { appContext?.let { ScanHistoryStore.save(it, archive) } }
}

/** Preserve unchanged rows instead of clearing the entire observable list every update. */
fun SnapshotStateList<WifiDevice>.replaceDevices(devices: List<WifiDevice>) {
    androidx.compose.runtime.snapshots.Snapshot.withMutableSnapshot {
        while (size > devices.size) removeAt(lastIndex)
        devices.forEachIndexed { index, device ->
            if (index >= size) add(device) else if (this[index] != device) this[index] = device
        }
    }
}
val LocalMainContextEntity = compositionLocalOf { MainContextEntity() }
