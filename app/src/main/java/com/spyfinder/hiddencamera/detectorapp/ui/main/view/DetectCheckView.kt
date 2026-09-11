package com.spyfinder.hiddencamera.detectorapp.ui.main.view

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.BackHandler
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.spyfinder.hiddencamera.detectorapp.R
import com.spyfinder.hiddencamera.detectorapp.event.Event
import com.spyfinder.hiddencamera.detectorapp.model.WifiDevice
import com.spyfinder.hiddencamera.detectorapp.ui.components.*
import com.spyfinder.hiddencamera.detectorapp.ui.main.context.LocalMainContextEntity
import com.spyfinder.hiddencamera.detectorapp.ui.subscribe.SubscribeActivity
import com.spyfinder.hiddencamera.detectorapp.utils.*
import com.stealthcopter.networktools.SubnetDevices
import com.stealthcopter.networktools.subnet.Device
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val WIFI_DETECT_TAG = "WifiDetect"

@Composable
fun DetectCheckView() {
    val context = LocalContext.current
    val main = LocalMainContextEntity.current
    val scope = rememberCoroutineScope()
    val scanToken = remember { AtomicInteger(0) }
    var scanGeneration by remember { mutableIntStateOf(0) }
    var state by remember { mutableStateOf(if (main.isStartDetect.value && !main.isAnimating.value) "complete" else "home") }
    var ssid by remember { mutableStateOf("") }
    var resumeScan by remember { mutableStateOf(false) }
    var openAfterSubscribe by remember { mutableStateOf(false) }
    val subscribed by SubscribeHelper.isSubscribedFlow.collectAsState()
    val permissions = remember { buildList {
        add(Manifest.permission.ACCESS_FINE_LOCATION)
        add(Manifest.permission.ACCESS_COARSE_LOCATION)
        if (Build.VERSION.SDK_INT >= 33) add(Manifest.permission.NEARBY_WIFI_DEVICES)
    }.toTypedArray() }
    fun hasPermissions() = permissions.all { ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        if (hasPermissions()) resumeScan = true else state = "denied"
    }
    val settingsLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        if (hasPermissions() && WifiHelper.isWifiEnabled(context)) resumeScan = true
    }
    val subscribeLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        scope.launch {
            if (openAfterSubscribe && SubscribeHelper.isSubscribe()) main.openCurrentResult()
            openAfterSubscribe = false
        }
    }
    fun failScan(token: Int) {
        if (scanToken.compareAndSet(token, token + 1)) {
            main.isAnimating.value = false
            main.isStartDetect.value = false
            state = "error"
        }
    }
    fun startScan() {
        if (!WifiHelper.isWifiEnabled(context)) { state = "offline"; return }
        if (!hasPermissions()) { state = "permission"; return }
        val ip = resolveCurrentWifiLocalIp(context)
        if (ip == null) { state = "offline"; return }
        ssid = WifiHelper.showWifiInfo(context).ssid
        val token = scanToken.incrementAndGet()
        scanGeneration = token
        state = "scan"
        main.isStartDetect.value = true
        main.isAnimating.value = true
        main.suspiciousDevices.clear()
        main.trustedDevices.clear()
        Event.event(context, Event.WIFI_SCAN_START, Event.PARAM_SOURCE to "detect_page")
        wifiDetect(ip, isScanActive = { scanToken.get() == token }, onDeviceDetected = { device ->
            scope.launch {
                if (scanToken.get() == token && state == "scan") {
                    if (device.riskLevel > 0) main.suspiciousDevices.add(device) else main.trustedDevices.add(device)
                }
            }
        }, onFailure = { scope.launch { failScan(token) } }) { review, trusted ->
            scope.launch {
                if (scanToken.get() == token && state == "scan") {
                    main.suspiciousDevices.clear(); main.suspiciousDevices.addAll(review)
                    main.trustedDevices.clear(); main.trustedDevices.addAll(trusted)
                    main.saveLatestScanResult(review, trusted)
                    main.isAnimating.value = false
                    main.detectProgress.intValue = 100
                    state = "complete"
                    Event.event(context, Event.WIFI_SCAN_COMPLETE, Event.PARAM_SUSPICIOUS_COUNT to review.size,
                        Event.PARAM_TRUSTED_COUNT to trusted.size, Event.PARAM_TOTAL_COUNT to review.size + trusted.size)
                }
            }
        }
    }
    DisposableEffect(Unit) {
        onDispose {
            scanToken.incrementAndGet()
            if (main.isAnimating.value) {
                main.isAnimating.value = false
                main.isStartDetect.value = false
            }
        }
    }
    LaunchedEffect(scanGeneration) {
        if (state == "scan") { delay(60_000); if (state == "scan") failScan(scanGeneration) }
    }
    LaunchedEffect(resumeScan, main.pendingWifiAutoScan.value) {
        if (resumeScan || main.pendingWifiAutoScan.value) {
            resumeScan = false; main.pendingWifiAutoScan.value = false; startScan()
        }
    }
    LaunchedEffect(Unit) {
        while (true) {
            ssid = if (WifiHelper.isWifiEnabled(context) && hasPermissions()) WifiHelper.showWifiInfo(context).ssid else ""
            delay(3000)
        }
    }
    BackHandler(state != "home" && state != "scan" && state != "complete") { state = "home" }
    key(state) {
    QuietPage(footer = if (state == "home") ({
        QuietButton(context.getString(R.string.scan_wifi)) { startScan() }
        TextButton(onClick = {
            if (main.hasScanHistory) {
                Event.event(context, Event.WIFI_HISTORY_CLICK)
                main.openLatestResult()
            } else state = "nohistory"
        }, modifier = Modifier.fillMaxWidth()) { Text(context.getString(R.string.view_last_scan)) }
    }) else null) {
        when (state) {
            "home" -> {
                QuietHeading(context.getString(R.string.brand), context.getString(R.string.home_title), context.getString(R.string.home_description))
                QuietPanel {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        QuietIcon(R.drawable.svg_icon_wifi)
                        Column { Text(ssid.takeIf { it.isNotBlank() && it != "<unknown ssid>" } ?: context.getString(R.string.your_network))
                            QuietBody(if (WifiHelper.isWifiEnabled(context)) context.getString(R.string.connected_ready) else context.getString(R.string.connect_to_begin), true) }
                    }
                }
                QuietOrbit(diameter = 192.dp)
                QuietNote(context.getString(R.string.network_note))
                if (!subscribed) TextButton(onClick = { SubscribeActivity.launch(context) }) { Text(context.getString(R.string.explore_pro)) }
            }
            "scan" -> {
                QuietHeading(context.getString(R.string.network_check), context.getString(R.string.scanning_title), ssid)
                val count = main.suspiciousDevices.size + main.trustedDevices.size
                QuietOrbit(value = count.toString(), label = context.getString(R.string.devices_discovered))
                LinearProgressIndicator(Modifier.fillMaxWidth())
                QuietPanel {
                    Text(context.getString(R.string.looking_for_devices))
                    QuietBody(context.getString(R.string.preparing_results))
                }
                QuietNote(context.getString(R.string.scan_progress_note))
            }
            "complete" -> {
                QuietHeading(context.getString(R.string.check_complete), context.getString(R.string.network_glance), context.getString(R.string.devices_found, main.suspiciousDevices.size + main.trustedDevices.size))
                QuietStats(main.suspiciousDevices.size, main.trustedDevices.size)
                QuietPanel { Text(context.getString(R.string.review_unfamiliar)); QuietBody(context.getString(R.string.unrecognized_note)) }
                QuietButton(context.getString(R.string.view_details)) {
                    scope.launch {
                        if (subscribed || SubscribeHelper.isSubscribe()) main.openCurrentResult()
                        else { openAfterSubscribe = true; subscribeLauncher.launch(Intent(context, SubscribeActivity::class.java)) }
                    }
                }
                QuietButton(context.getString(R.string.scan_again), secondary = true) { startScan() }
            }
            "permission", "denied" -> {
                QuietTopBar(context.getString(R.string.network_access)) { state = "home" }
                QuietOrbit(R.drawable.svg_icon_privacy_policy)
                QuietHeading(context.getString(R.string.your_control), if (state == "denied") context.getString(R.string.access_off) else context.getString(R.string.allow_network),
                    context.getString(R.string.network_permission_description))
                QuietButton(if (state == "denied") context.getString(R.string.open_app_settings) else context.getString(R.string.action_continue)) {
                    if (state == "denied") settingsLauncher.launch(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}")))
                    else permissionLauncher.launch(permissions)
                }
                QuietButton(context.getString(R.string.not_now), secondary = true) { state = "home" }
            }
            "offline" -> {
                QuietTopBar(context.getString(R.string.wifi_check)) { state = "home" }
                QuietOrbit(R.drawable.svg_icon_wifi)
                QuietHeading(context.getString(R.string.connection_needed), context.getString(R.string.connect_wifi), context.getString(R.string.join_network))
                QuietButton(context.getString(R.string.open_wifi_settings)) { settingsLauncher.launch(Intent(Settings.ACTION_WIFI_SETTINGS)) }
                QuietButton(context.getString(R.string.try_again), secondary = true) { startScan() }
            }
            "nohistory" -> {
                QuietTopBar(context.getString(R.string.last_scan)) { state = "home" }
                QuietOrbit(R.drawable.svg_icon_restore)
                QuietHeading(context.getString(R.string.fresh_start), context.getString(R.string.no_history), context.getString(R.string.history_description))
                QuietButton(context.getString(R.string.start_scan)) { startScan() }
            }
            else -> {
                QuietTopBar(context.getString(R.string.wifi_check)) { state = "home" }
                QuietOrbit(R.drawable.svg_icon_warning_gray)
                QuietHeading(context.getString(R.string.check_interrupted), context.getString(R.string.try_again_title), context.getString(R.string.scan_error_description))
                QuietButton(context.getString(R.string.retry_scan)) { startScan() }
                QuietButton(context.getString(R.string.back_wifi), secondary = true) { state = "home" }
            }
        }
    }
    }
}

fun wifiDetect(
    localIp: String,
    isScanActive: () -> Boolean = { true },
    onDeviceDetected: (WifiDevice) -> Unit = {},
    onFailure: () -> Unit = {},
    onDetectFinished: (List<WifiDevice>, List<WifiDevice>) -> Unit = { _, _ -> }
) {
    if (localIp.isBlank() || localIp == "0.0.0.0") {
        if (isScanActive()) {
            onFailure()
        }
        return
    }

    val suspiciousDevices = mutableListOf<WifiDevice>()
    val trustedDevices = mutableListOf<WifiDevice>()
    val resultLock = Any()
    val threadLock = Any()
    val threads = mutableListOf<Thread>()
    val detectedDeviceKeys = mutableSetOf<String>()

    fun buildDeviceKey(device: Device): String {
        return when {
            !device.mac.isNullOrBlank() -> "mac:${device.mac}"
            !device.ip.isNullOrBlank() -> "ip:${device.ip}"
            else -> "host:${device.hostname.orEmpty()}"
        }
    }

    fun analyzeDeviceAsync(device: Device) {
        val deviceKey = buildDeviceKey(device)
        synchronized(threadLock) {
            if (!detectedDeviceKeys.add(deviceKey)) {
                return
            }
        }

        val thread = Thread {
            if (!isScanActive()) {
                return@Thread
            }

            try {
                // 单个设备发现后立即分析并回调，Suspicious 数字就能从扫描开始阶段逐步增长。
                val wifiDevice = WifiHelper.detectDeviceType(device, localIp)
                if (!isScanActive()) {
                    return@Thread
                }
                synchronized(resultLock) {
                    if (wifiDevice.riskLevel > 0) {
                        suspiciousDevices.add(wifiDevice)
                    } else {
                        trustedDevices.add(wifiDevice)
                    }
                }
                onDeviceDetected(wifiDevice)
            } catch (throwable: Throwable) {
                Log.w(WIFI_DETECT_TAG, "分析设备失败: ${device.ip}", throwable)
            }
        }

        synchronized(threadLock) {
            threads.removeAll { !it.isAlive }
            threads.add(thread)
        }
        thread.start()
    }

    fun waitForAnalyzeThreads() {
        val activeThreads = synchronized(threadLock) {
            threads.toList()
        }
        activeThreads.forEach { thread ->
            try {
                thread.join(3_000)
            } catch (interruptedException: InterruptedException) {
                Thread.currentThread().interrupt()
                Log.w(WIFI_DETECT_TAG, "等待设备分析线程被中断", interruptedException)
            }
        }
    }

    try {
        SubnetDevices.fromLocalAddress().findDevices(object : SubnetDevices.OnSubnetDeviceFound {
            override fun onDeviceFound(device: Device?) {
                if (device == null || !isScanActive()) {
                    return
                }
                analyzeDeviceAsync(device)
            }

            override fun onFinished(devicesFound: ArrayList<Device?>?) {
                if (devicesFound != null) {
                    // 部分机型或库版本可能只在完成时返回列表，这里补漏避免漏掉未触发 onDeviceFound 的设备。
                    devicesFound.forEach { device ->
                        if (device != null && isScanActive()) {
                            analyzeDeviceAsync(device)
                        }
                    }
                }

                if (devicesFound == null) {
                    Log.w(WIFI_DETECT_TAG, "子网扫描完成但设备列表为空")
                }

                waitForAnalyzeThreads()

                // 扫描完成后保存一次快照，供首页 History 入口回看最近一次结果。
                if (!isScanActive()) {
                    return
                }

                val suspiciousSnapshot: List<WifiDevice>
                val trustedSnapshot: List<WifiDevice>
                synchronized(resultLock) {
                    suspiciousSnapshot = suspiciousDevices.toList()
                    trustedSnapshot = trustedDevices.toList()
                }

                onDetectFinished(suspiciousSnapshot, trustedSnapshot)
            }

        })
    } catch (throwable: Throwable) {
        // The subnet scan library may throw IllegalAccessError when no local address is available.
        Log.w(WIFI_DETECT_TAG, "子网扫描启动失败", throwable)
        if (isScanActive()) {
            onFailure()
        }
    }
}

private fun resolveCurrentWifiLocalIp(context: Context): String? {
    if (!WifiHelper.isWifiEnabled(context)) {
        return null
    }

    val localIp = WifiHelper.showWifiInfo(context).ip
    return localIp.takeUnless { it.isBlank() || it == "0.0.0.0" }
}
