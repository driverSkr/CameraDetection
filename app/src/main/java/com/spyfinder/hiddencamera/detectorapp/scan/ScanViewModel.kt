package com.spyfinder.hiddencamera.detectorapp.scan

import android.app.Application
import androidx.compose.runtime.compositionLocalOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.lifecycle.viewModelScope
import com.spyfinder.hiddencamera.detectorapp.event.Event
import com.spyfinder.hiddencamera.detectorapp.ui.main.context.MainContextEntity
import kotlinx.coroutines.*
import com.spyfinder.hiddencamera.detectorapp.utils.ScanRecord
import com.spyfinder.hiddencamera.detectorapp.ui.main.context.replaceDevices
import java.util.UUID
import android.os.SystemClock
import java.util.Date

class ScanViewModel(application: Application) : AndroidViewModel(application), DefaultLifecycleObserver {
    val state = MainContextEntity(application)
    private val historyLoad = viewModelScope.launch { state.restoreLatestScanResult() }
    private var startedAt = 0L
    private var lastCheckpoint = 0L
    private val prefs = application.getSharedPreferences("scan_session", 0)
    private var job: Job? = null
    private var scanner: NetworkScanner? = null
    private var generation = 0L
    private var lastPublished: List<com.spyfinder.hiddencamera.detectorapp.model.WifiDevice>? = null
    init {
        if (prefs.getBoolean("running", false)) {
            state.scanStatus = ScanStatus.CANCELLED
            state.scanMessage = "The previous scan was interrupted. Start a new scan."
            prefs.edit().putBoolean("running", false).putString("interrupted", state.scanMessage).apply()
        } else if (prefs.contains("interrupted")) {
            state.scanStatus = ScanStatus.CANCELLED
            state.scanMessage = prefs.getString("interrupted", "Scan interrupted. Please retry.").orEmpty()
        }
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
    }
    override fun onStop(owner: LifecycleOwner) { cancel("Scan interrupted while the app was in the background.", source = "background") }
    fun start() {
        cancel("Scan replaced.", source = "replaced")
        lastPublished = null
        val id = ++generation
        startedAt = System.currentTimeMillis()
        lastCheckpoint = SystemClock.elapsedRealtime()
        state.currentRecordId = UUID.randomUUID().toString()
        val worker = NetworkScanner(getApplication())
        scanner = worker
        state.isShowResult.value = false
        state.isStartDetect.value = true
        state.isAnimating.value = true
        state.scanStatus = ScanStatus.RUNNING
        state.detectProgress.intValue = 0
        state.scanMessage = "Preparing Wi-Fi scan…"
        state.networkLabel = ""
        state.suspiciousDevices.clear(); state.trustedDevices.clear()
        prefs.edit().putBoolean("running", true).remove("interrupted").apply()
        Event.event(getApplication(), Event.WIFI_SCAN_START)
        job = viewModelScope.launch {
            var monitor: Job? = null
            var forcedMessage: String? = null
            try {
                historyLoad.join()
                val target = worker.selectNetwork()
                state.networkLabel = "${target.ip}/${target.prefix}"
                val task = async { worker.scan(target) { message, devices, progress ->
                    viewModelScope.launch {
                        if (id == generation && state.scanStatus == ScanStatus.RUNNING) {
                            state.scanMessage = message
                            state.detectProgress.intValue = maxOf(state.detectProgress.intValue, progress.coerceIn(0, 99))
                            publish(devices)
                            if (SystemClock.elapsedRealtime() - lastCheckpoint >= 3_000) {
                                saveSnapshot(worker)
                                lastCheckpoint = SystemClock.elapsedRealtime()
                            }
                        }
                    }
                } }
                monitor = launch {
                    while (task.isActive) {
                        delay(500)
                        if (worker.stalled()) {
                            forcedMessage = "Scan stopped because no probe made progress for 60 seconds. Results are incomplete. Please retry."
                            worker.resources.close(); task.cancel(); break
                        }
                        if (!worker.networkUnchanged(target)) {
                            forcedMessage = "Wi-Fi changed or disconnected. Results are incomplete. Reconnect and retry."
                            worker.resources.close(); task.cancel(); break
                        }
                    }
                }
                val result = task.await()
                if (id != generation) return@launch
                publish(result.devices)
                state.scanStatus = if (result.partial) ScanStatus.PARTIAL else ScanStatus.COMPLETE
                // Work completion is independent of evidence certainty or a declared coverage limit.
                state.detectProgress.intValue = 100
                state.scanMessage = "${java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.ROOT).format(Date())} · ${state.networkLabel}\n${result.message}"
            } catch (e: CancellationException) {
                if (id == generation && forcedMessage != null) {
                    publish(worker.snapshot())
                    state.scanStatus = ScanStatus.PARTIAL
                    state.scanMessage = worker.withWarnings(forcedMessage!!)
                } else throw e
            } catch (e: Exception) {
                if (id == generation) {
                    publish(worker.snapshot())
                    state.scanStatus = ScanStatus.FAILED
                    state.scanMessage = worker.withWarnings(e.message ?: "Scan failed. Reconnect to Wi-Fi and retry.")
                }
            } finally {
                monitor?.cancel(); worker.resources.close()
                if (id == generation) {
                    state.isAnimating.value = false
                    saveSnapshot(worker)
                    prefs.edit().putBoolean("running", false).apply()
                    Event.event(getApplication(), when (state.scanStatus) {
                        ScanStatus.COMPLETE -> Event.WIFI_SCAN_COMPLETE
                        ScanStatus.PARTIAL -> "wifi_scan_partial"
                        else -> "wifi_scan_failed"
                    }, Event.PARAM_REASON to state.scanStatus.name)
                }
            }
        }
    }
    private fun publish(devices: List<com.spyfinder.hiddencamera.detectorapp.model.WifiDevice>) {
        if (devices === lastPublished) return
        lastPublished = devices
        val trust = (state.suspiciousDevices + state.trustedDevices).associate { it.ip to it.userTrusted }
        val annotated = devices.map { it.copy(userTrusted = trust[it.ip] ?: false) }
        state.suspiciousDevices.replaceDevices(annotated.filter { it.riskLevel == 1 })
        state.trustedDevices.replaceDevices(annotated.filter { it.riskLevel != 1 })
    }
    private fun saveSnapshot(worker: NetworkScanner) {
        if (!historyLoad.isCompleted) return
        state.saveRecord(ScanRecord(state.currentRecordId, startedAt,
            if (state.scanStatus == ScanStatus.RUNNING) null else System.currentTimeMillis(),
            state.networkLabel, state.scanStatus, worker.coverage(), state.scanMessage,
            state.recordDevices()))
    }
    fun cancel(reason: String = "Scan cancelled. Results are incomplete.", source: String = "lifecycle") {
        if (state.scanStatus != ScanStatus.RUNNING) return
        ++generation
        scanner?.let { it.resources.close(); publish(it.snapshot()) }
        job?.cancel()
        state.isAnimating.value = false
        state.scanStatus = ScanStatus.CANCELLED
        state.scanMessage = scanner?.withWarnings(reason) ?: reason
        scanner?.let { saveSnapshot(it) }
        prefs.edit().putBoolean("running", false).putString("interrupted", reason).apply()
        Event.event(getApplication(), Event.WIFI_SCAN_CANCEL, Event.PARAM_REASON to reason,
            Event.PARAM_SOURCE to source, Event.PARAM_PROGRESS to state.detectProgress.intValue)
    }
    override fun onCleared() {
        cancel(source = "viewmodel_cleared")
        ProcessLifecycleOwner.get().lifecycle.removeObserver(this)
    }
}
val LocalScanViewModel = compositionLocalOf<ScanViewModel> { error("ScanViewModel was not provided") }
