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
import com.spyfinder.hiddencamera.detectorapp.ui.main.context.bucketDevices
import com.spyfinder.hiddencamera.detectorapp.utils.ScanRecord
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Date
import java.util.UUID
import java.util.concurrent.atomic.AtomicLong
import android.os.SystemClock
import android.widget.Toast
import com.spyfinder.hiddencamera.detectorapp.R
import com.spyfinder.hiddencamera.detectorapp.ui.main.context.replaceDevices
import com.spyfinder.hiddencamera.detectorapp.utils.ExclusiveSession
import com.spyfinder.hiddencamera.detectorapp.utils.WifiHelper
import com.spyfinder.hiddencamera.detectorapp.utils.ScanStrings

class ScanViewModel(application: Application) : AndroidViewModel(application) {
    private fun message(id: Int, vararg args: Any) = ScanStrings.canonical(getApplication(), id, *args)
    val state = MainContextEntity(application)
    private val historyLoad = viewModelScope.launch { state.restoreLatestScanResult() }
    private var startedAt = 0L
    private var lastCheckpoint = 0L
    private val prefs = application.getSharedPreferences("scan_session", 0)
    private var job: Job? = null
    private var scanner: NetworkScanner? = null
    private var generation = 0L
    private var lastPublished: List<com.spyfinder.hiddencamera.detectorapp.model.WifiDevice>? = null
    private val processObserver = object : DefaultLifecycleObserver {
        override fun onStop(owner: LifecycleOwner) {
            if (state.scanStatus == ScanStatus.RUNNING && !ScanForegroundService.isRunning()) {
                cancel(message(R.string.scan_unprotected_background), source = "unprotected_background")
            }
        }
    }
    init {
        state.networkName = WifiHelper.connectedSsid(application).orEmpty()
        ExclusiveSession.bind(
            cancelScan = { reason, source -> cancel(reason, source) },
            scanRunning = { state.scanStatus == ScanStatus.RUNNING },
            stopMagnetic = { state.magneticListening.value = false },
            magneticActive = { state.magneticListening.value }
        )
        ProcessLifecycleOwner.get().lifecycle.addObserver(processObserver)
        ScanForegroundService.setCallbacks(
            onStop = { cancel(source = "notification") },
            onLost = {
                if (state.scanStatus == ScanStatus.RUNNING) {
                    state.scanProtected = false
                    if (!ProcessLifecycleOwner.get().lifecycle.currentState.isAtLeast(androidx.lifecycle.Lifecycle.State.STARTED)) {
                        cancel(message(R.string.scan_unprotected_background), source = "service_lost")
                    }
                }
            }
        )
        if (prefs.getBoolean("running", false)) {
            state.scanStatus = ScanStatus.CANCELLED
            state.scanMessage = message(R.string.scan_previous_interrupted)
            prefs.edit().putBoolean("running", false).putString("interrupted", state.scanMessage).apply()
        } else if (prefs.contains("interrupted")) {
            state.scanStatus = ScanStatus.CANCELLED
            state.scanMessage = prefs.getString("interrupted", message(R.string.scan_interrupted)).orEmpty()
        }
    }
    fun start() {
        if (state.scanStatus == ScanStatus.RUNNING) cancel(message(R.string.scan_replaced), source = "replaced")
        if (ExclusiveSession.yieldToScan()) {
            Toast.makeText(getApplication(), getApplication<Application>().getString(R.string.feature_preempted), Toast.LENGTH_SHORT).show()
        }
        val id = ++generation
        val worker = NetworkScanner(getApplication())
        job = viewModelScope.launch {
            var sessionStarted = false
            var monitor: Job? = null
            var forcedMessage: String? = null
            val updates = Channel<ScanUiUpdate>(Channel.UNLIMITED)
            val seq = AtomicLong()
            val publisher = launch {
                var lastSeq = 0L
                for (update in updates) {
                    if (id != generation || state.scanStatus != ScanStatus.RUNNING) continue
                    if (update.seq < lastSeq) continue
                    lastSeq = update.seq
                    state.scanMessage = update.message
                    state.detectProgress.intValue = maxOf(state.detectProgress.intValue, update.progress.coerceIn(0, 99))
                    publish(update.devices)
                    ScanForegroundService.update(state.detectProgress.intValue)
                    if (SystemClock.elapsedRealtime() - lastCheckpoint >= 3_000) {
                        saveSnapshot(worker)
                        lastCheckpoint = SystemClock.elapsedRealtime()
                    }
                }
            }
            try {
                historyLoad.join()
                if (id != generation) return@launch
                val target = try {
                    worker.selectNetwork()
                } catch (e: CancellationException) {
                    throw e
                } catch (e: IllegalStateException) {
                    if (id == generation) state.scanMessage = e.message ?: message(R.string.scan_connect_wifi)
                    return@launch
                }
                if (id != generation) return@launch
                sessionStarted = true
                scanner = worker
                lastPublished = null
                startedAt = System.currentTimeMillis()
                lastCheckpoint = SystemClock.elapsedRealtime()
                state.currentRecordId = UUID.randomUUID().toString()
                state.isShowResult.value = false
                state.isStartDetect.value = true
                state.isAnimating.value = true
                state.scanStatus = ScanStatus.RUNNING
                state.detectProgress.intValue = 0
                state.scanMessage = message(R.string.scan_preparing)
                state.networkLabel = "${target.ip}/${target.prefix}"
                state.networkName = worker.connectedWifiName(target.network).orEmpty()
                state.suspiciousDevices.clear(); state.trustedDevices.clear()
                prefs.edit().putBoolean("running", true).remove("interrupted").apply()
                Event.event(getApplication(), Event.WIFI_SCAN_START)
                state.scanProtected = ScanForegroundService.ensureRunning(getApplication())
                if (!state.scanProtected) {
                    state.scanMessage = message(R.string.scan_unprotected_keep_open)
                }
                val task = async { worker.scan(target) { message, devices, progress ->
                    updates.trySend(ScanUiUpdate(seq.incrementAndGet(), message, devices, progress))
                } }
                monitor = launch {
                    while (task.isActive) {
                        delay(500)
                        if (worker.stalled()) {
                            forcedMessage = message(R.string.scan_stalled)
                            worker.resources.close(); task.cancel(); break
                        }
                        if (!worker.networkUnchanged(target)) {
                            forcedMessage = message(R.string.scan_network_changed)
                            worker.resources.close(); task.cancel(); break
                        }
                    }
                }
                val result = task.await()
                updates.close()
                publisher.join()
                if (id != generation) return@launch
                publish(result.devices)
                state.scanStatus = if (result.partial) ScanStatus.PARTIAL else ScanStatus.COMPLETE
                state.detectProgress.intValue = 100
                state.scanMessage = "${java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.ROOT).format(Date())} · ${state.networkLabel}\n${result.message}"
            } catch (e: CancellationException) {
                if (sessionStarted && id == generation && forcedMessage != null) {
                    publish(worker.snapshot())
                    state.scanStatus = ScanStatus.PARTIAL
                    state.scanMessage = worker.withWarnings(forcedMessage!!)
                } else throw e
            } catch (e: Exception) {
                if (sessionStarted && id == generation) {
                    publish(worker.snapshot())
                    state.scanStatus = ScanStatus.FAILED
                    state.scanMessage = worker.withWarnings(e.message ?: message(R.string.scan_failed))
                }
            } finally {
                updates.close()
                publisher.cancel()
                monitor?.cancel(); worker.resources.close()
                if (sessionStarted && id == generation) {
                    state.isAnimating.value = false
                    saveSnapshot(worker)
                    prefs.edit().putBoolean("running", false).apply()
                    ScanForegroundService.stop(getApplication())
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
        val buckets = bucketDevices(annotated)
        state.suspiciousDevices.replaceDevices(buckets.first)
        state.trustedDevices.replaceDevices(buckets.second)
    }
    private fun saveSnapshot(worker: NetworkScanner) {
        if (!historyLoad.isCompleted) return
        state.saveRecord(ScanRecord(state.currentRecordId, startedAt,
            if (state.scanStatus == ScanStatus.RUNNING) null else System.currentTimeMillis(),
            state.networkLabel, state.scanStatus, worker.coverage(), state.scanMessage,
            state.recordDevices()))
    }
    fun cancel(reason: String = message(R.string.scan_cancelled), source: String = "lifecycle") {
        val preparing = job?.isActive == true && state.scanStatus != ScanStatus.RUNNING
        if (state.scanStatus != ScanStatus.RUNNING && !preparing) return
        ++generation
        if (preparing) {
            job?.cancel()
            ScanForegroundService.stop(getApplication())
            return
        }
        scanner?.let { it.resources.close(); publish(it.snapshot()) }
        job?.cancel()
        state.isAnimating.value = false
        state.scanStatus = ScanStatus.CANCELLED
        state.scanMessage = scanner?.withWarnings(reason) ?: reason
        scanner?.let { saveSnapshot(it) }
        prefs.edit().putBoolean("running", false).putString("interrupted", reason).apply()
        ScanForegroundService.stop(getApplication())
        Event.event(getApplication(), Event.WIFI_SCAN_CANCEL, Event.PARAM_REASON to reason,
            Event.PARAM_SOURCE to source, Event.PARAM_PROGRESS to state.detectProgress.intValue)
    }
    override fun onCleared() {
        ProcessLifecycleOwner.get().lifecycle.removeObserver(processObserver)
        ExclusiveSession.unbind()
        ScanForegroundService.setCallbacks(null, null)
        cancel(source = "viewmodel_cleared")
    }
}

private data class ScanUiUpdate(
    val seq: Long,
    val message: String,
    val devices: List<com.spyfinder.hiddencamera.detectorapp.model.WifiDevice>,
    val progress: Int
)

val LocalScanViewModel = compositionLocalOf<ScanViewModel> { error("ScanViewModel was not provided") }
