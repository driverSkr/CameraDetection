package com.spyfinder.hiddencamera.detectorapp.ui.main.view

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableIntState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.spyfinder.hiddencamera.detectorapp.R
import com.spyfinder.hiddencamera.detectorapp.model.WifiDevice
import com.spyfinder.hiddencamera.detectorapp.theme.Transparent
import com.spyfinder.hiddencamera.detectorapp.theme.White
import com.spyfinder.hiddencamera.detectorapp.theme.White10
import com.spyfinder.hiddencamera.detectorapp.theme.White60
import com.spyfinder.hiddencamera.detectorapp.ui.main.context.LocalMainContextEntity
import com.spyfinder.hiddencamera.detectorapp.ui.subscribe.SubscribeActivity
import com.spyfinder.hiddencamera.detectorapp.utils.SubscribeHelper
import com.spyfinder.hiddencamera.detectorapp.utils.WifiHelper
import com.stealthcopter.networktools.SubnetDevices
import com.stealthcopter.networktools.subnet.Device
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@SuppressLint("DefaultLocale")
@Composable
fun DetectCheckView() {
    val context = LocalContext.current
    val localMain = LocalMainContextEntity.current
    val isSubscribed = SubscribeHelper.isSubscribedFlow.collectAsState().value
    val wifiSsid = remember { mutableStateOf<String?>(null) }
    val detectProgress = localMain.detectProgress
    val scope = rememberCoroutineScope()
    val activeScanId = remember { androidx.compose.runtime.mutableIntStateOf(0) }
    val actualScanCompletedScanId = remember { androidx.compose.runtime.mutableIntStateOf(-1) }
    val uiProgressCompletedScanId = remember { androidx.compose.runtime.mutableIntStateOf(-1) }
    val displayedSuspiciousCount = remember { androidx.compose.runtime.mutableIntStateOf(0) }
    val finishingScanId = remember { androidx.compose.runtime.mutableIntStateOf(-1) }
    val shouldOpenResultAfterSubscribe = remember { mutableStateOf(false) }
    val subscribeLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        if (!shouldOpenResultAfterSubscribe.value) {
            return@rememberLauncherForActivityResult
        }
        scope.launch {
            val subscribed = SubscribeHelper.isSubscribe()
            if (subscribed) {
                localMain.isShowResult.value = true
            }
            shouldOpenResultAfterSubscribe.value = false
        }
    }

    fun finishScanIfReady(scanId: Int) {
        if (scanId != activeScanId.intValue) {
            return
        }
        if (actualScanCompletedScanId.intValue != scanId) {
            return
        }
        if (uiProgressCompletedScanId.intValue != scanId) {
            return
        }
        if (finishingScanId.intValue == scanId) {
            return
        }
        finishingScanId.intValue = scanId
        scope.launch {
            while (
                scanId == activeScanId.intValue &&
                localMain.isStartDetect.value &&
                displayedSuspiciousCount.intValue < localMain.suspiciousDevices.size
            ) {
                displayedSuspiciousCount.intValue += 1
                delay(250)
            }

            if (scanId != activeScanId.intValue || !localMain.isStartDetect.value) {
                return@launch
            }

            detectProgress.intValue = 100
            localMain.isAnimating.value = false
        }
    }

    fun openResultWithSubscriptionCheck() {
        scope.launch {
            val subscribed = if (isSubscribed) {
                true
            } else {
                SubscribeHelper.isSubscribe()
            }

            if (subscribed) {
                shouldOpenResultAfterSubscribe.value = false
                localMain.isShowResult.value = true
            } else {
                shouldOpenResultAfterSubscribe.value = true
                subscribeLauncher.launch(Intent(context, SubscribeActivity::class.java))
            }
        }
    }

    val startDetectAction = startDetect@{
        val localIp = resolveCurrentWifiLocalIp(context)
        if (localIp == null) {
            Toast.makeText(context, "Please connect to wifi first", Toast.LENGTH_LONG).show()
            return@startDetect
        }

        val scanId = activeScanId.intValue + 1
        activeScanId.intValue = scanId
        actualScanCompletedScanId.intValue = -1
        uiProgressCompletedScanId.intValue = -1
        finishingScanId.intValue = -1
        shouldOpenResultAfterSubscribe.value = false
        // 每次开始扫描前先重置当前态，避免残留上一次展示数据影响本次结果。
        localMain.isShowResult.value = false
        localMain.isStartDetect.value = true
        localMain.isAnimating.value = true
        detectProgress.intValue = 0
        displayedSuspiciousCount.intValue = 0
        localMain.suspiciousDevices.clear()
        localMain.trustedDevices.clear()
        wifiDetect(
            localIp = localIp,
            isScanActive = { scanId == activeScanId.intValue && localMain.isStartDetect.value },
            onDeviceDetected = { wifiDevice ->
                scope.launch {
                    if (scanId != activeScanId.intValue || !localMain.isStartDetect.value) {
                        return@launch
                    }
                    if (actualScanCompletedScanId.intValue == scanId) {
                        return@launch
                    }
                    if (wifiDevice.riskLevel > 0) {
                        localMain.suspiciousDevices.add(wifiDevice)
                    } else {
                        localMain.trustedDevices.add(wifiDevice)
                    }
                }
            }
        ) { suspiciousList, trustedList ->
            scope.launch {
                if (scanId != activeScanId.intValue || !localMain.isStartDetect.value) {
                    return@launch
                }
                actualScanCompletedScanId.intValue = scanId
                localMain.suspiciousDevices.clear()
                localMain.suspiciousDevices.addAll(suspiciousList)
                localMain.trustedDevices.clear()
                localMain.trustedDevices.addAll(trustedList)
                localMain.saveLatestScanResult(suspiciousList, trustedList)
                finishScanIfReady(scanId)
            }
        }
    }

    LaunchedEffect(localMain.pendingWifiAutoScan.value, localMain.selectTabIndex.intValue) {
        if (!localMain.pendingWifiAutoScan.value || localMain.selectTabIndex.intValue != 0) {
            return@LaunchedEffect
        }
        localMain.pendingWifiAutoScan.value = false
        startDetectAction()
    }

    LaunchedEffect(Unit) {
        while (true) {
            wifiSsid.value = WifiHelper.showWifiInfo(context).ssid
            delay(5000) // 每5秒更新一次
        }
    }

    LaunchedEffect(activeScanId.intValue) {
        val scanId = activeScanId.intValue
        if (scanId == 0 || !localMain.isStartDetect.value) {
            return@LaunchedEffect
        }

        runWifiDetectProgressTimeline(
            detectProgress = detectProgress,
            isScanActive = { scanId == activeScanId.intValue && localMain.isStartDetect.value }
        )

        if (scanId != activeScanId.intValue || !localMain.isStartDetect.value) {
            return@LaunchedEffect
        }

        uiProgressCompletedScanId.intValue = scanId
        finishScanIfReady(scanId)
    }

    LaunchedEffect(
        localMain.suspiciousDevices.size,
        localMain.isAnimating.value,
        localMain.isStartDetect.value
    ) {
        val actualSuspiciousCount = localMain.suspiciousDevices.size
        if (displayedSuspiciousCount.intValue > actualSuspiciousCount) {
            displayedSuspiciousCount.intValue = actualSuspiciousCount
        }
        if (!localMain.isAnimating.value && localMain.isStartDetect.value) {
            displayedSuspiciousCount.intValue = actualSuspiciousCount
        }
    }

    Box(modifier = Modifier.fillMaxSize().statusBarsPadding().padding(top = 18.dp)) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("Wifi Scan", color = Color(0xFFFFFFFF), fontSize = 28.sp, fontWeight = FontWeight.W700)
                Spacer(modifier = Modifier.weight(1f))
                if (!isSubscribed) {
                    // 未订阅时展示皇冠入口，订阅后自动隐藏。
                    Image(
                        painter = painterResource(R.mipmap.img_crown),
                        contentDescription = null,
                        modifier = Modifier
                            .size(40.dp)
                            .clickable{ SubscribeActivity.launch(context) }
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text("Connected WI-FI: ${if (wifiSsid.value == null) "未连接" else "\"${wifiSsid.value}\"" }", color = Color(0xFFFFFFFF).copy(0.6f), fontSize = 14.sp, fontWeight = FontWeight.W400)
        }

        Box(modifier = Modifier.size(313.dp).align(Alignment.Center)) {
            RadarScannerWithControls()
            if (localMain.isStartDetect.value) {
                RandomRedDotsWithVisibility(
                    isAnimating = localMain.isAnimating,
                    onDotAppeared = {
                        if (localMain.isAnimating.value && displayedSuspiciousCount.intValue < localMain.suspiciousDevices.size) {
                            displayedSuspiciousCount.intValue += 1
                        }
                    }
                )
                Text(
                    buildAnnotatedString {
                        withStyle(
                            style = SpanStyle(
                                fontSize = 44.sp,
                                color = White,
                                fontWeight = FontWeight.W700,
                                baselineShift = BaselineShift(0f) // 调整符号的垂直位置
                            )
                        ) {
                            append("${detectProgress.intValue}")
                        }
                        withStyle(
                            style = SpanStyle(
                                fontSize = 24.sp,
                                color = White,
                                fontWeight = FontWeight.W700,
                                baselineShift = BaselineShift(0f) // 调整符号的垂直位置
                            )
                        ) {
                            append("%")
                        }
                    },
                    modifier = Modifier.align(Alignment.Center)
                )
            } else {
                Text(
                    text = "Start",
                    color = Color(0xFFFFFFFF),
                    fontSize = 44.sp,
                    fontWeight = FontWeight.W700,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
        }

        Column(modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            if (localMain.isStartDetect.value) {
                Row(modifier = Modifier, verticalAlignment = Alignment.CenterVertically) {
                    Image(painter = painterResource(R.drawable.svg_icon_warning_red), contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Suspicious devices: ", color = White60, fontSize = 16.sp, fontWeight = FontWeight.W500)
                    Text(
                        "${if (localMain.isAnimating.value) displayedSuspiciousCount.intValue else localMain.suspiciousDevices.size}",
                        color = Color(0xFFFE2D3F),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.W500
                    )
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
            if (localMain.isStartDetect.value) {
                if (localMain.isAnimating.value) {
                    Box(modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .padding(horizontal = 24.dp)
                        .background(color = White10, shape = RoundedCornerShape(999.dp))
                        .border(width = 1.dp, shape = RoundedCornerShape(999.dp), brush = Brush.verticalGradient(colorStops = arrayOf(0f to White10, 0.5f to Transparent, 1f to White10)))
                        .clickable{
                            activeScanId.intValue += 1
                            finishingScanId.intValue = -1
                            localMain.isStartDetect.value = false
                            localMain.isAnimating.value = false
                            detectProgress.intValue = 0
                            displayedSuspiciousCount.intValue = 0
                        }
                    ) {
                        Text(
                            text = "Cancel",
                            color = White60,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.W500,
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }
                } else {
                    Row(modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .padding(horizontal = 24.dp)
                    ) {
                        Box(modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .background(color = White10, shape = RoundedCornerShape(999.dp))
                        .border(width = 1.dp, shape = RoundedCornerShape(999.dp), brush = Brush.verticalGradient(colorStops = arrayOf(0f to White10, 0.5f to Transparent, 1f to White10)))
                        .clickable{
                            startDetectAction()
                        }
                    ) {
                        Text(
                            text = "Recheck",
                            color = White60,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.W500,
                                modifier = Modifier.align(Alignment.Center)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Box(modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .background(color = Color(0xFF00C46F), shape = RoundedCornerShape(999.dp))
                            .clickable{
                                openResultWithSubscriptionCheck()
                            }
                        ) {
                            Text(
                                text = "Result",
                                color = Color(0xFFFFFFFF),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.W500,
                                modifier = Modifier.align(Alignment.Center)
                            )
                        }
                    }
                }
            } else {
                if (localMain.hasScanHistory) {
                    Row(modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .padding(horizontal = 24.dp)
                    ) {
                        Box(modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .background(color = White10, shape = RoundedCornerShape(999.dp))
                            .border(width = 1.dp, shape = RoundedCornerShape(999.dp), brush = Brush.verticalGradient(colorStops = arrayOf(0f to White10, 0.5f to Transparent, 1f to White10)))
                            .clickable{
                                localMain.openLatestResult()
                            }
                        ) {
                            Text(
                                text = "History",
                                color = White60,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.W500,
                                modifier = Modifier.align(Alignment.Center)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Box(modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .background(color = Color(0xFF00C46F), shape = RoundedCornerShape(999.dp))
                            .clickable{
                                startDetectAction()
                            }
                        ) {
                            Text(
                                text = "Start",
                                color = Color(0xFFFFFFFF),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.W500,
                                modifier = Modifier.align(Alignment.Center)
                            )
                        }
                    }
                } else {
                    Box(modifier = Modifier
                        .clickable{
                            startDetectAction()
                        }
                        .fillMaxWidth()
                        .height(56.dp)
                        .padding(horizontal = 24.dp)
                        .background(color = Color(0xFF00C46F), shape = RoundedCornerShape(999.dp))
                    ) {
                        Text(
                            text = "Start",
                            color = Color(0xFFFFFFFF),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.W500,
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }
                }
            }
        }
    }
}

private const val WIFI_SCAN_STAGE_ONE_END = 80
private const val WIFI_SCAN_STAGE_TWO_END = 99
private const val WIFI_SCAN_STAGE_THREE_END = 100
private const val WIFI_SCAN_STAGE_ONE_DURATION_MS = 5_000L
private const val WIFI_SCAN_STAGE_TWO_DURATION_MS = 3_000L
private const val WIFI_SCAN_STAGE_THREE_DURATION_MS = 3_000L
private const val WIFI_SCAN_COMPLETE_HOLD_MS = 3_000L
private const val WIFI_SCAN_PROGRESS_TICK_MS = 50L

private suspend fun runWifiDetectProgressTimeline(
    detectProgress: MutableIntState,
    isScanActive: () -> Boolean
) {
    animateWifiDetectProgress(
        detectProgress = detectProgress,
        start = 0,
        end = WIFI_SCAN_STAGE_ONE_END,
        durationMs = WIFI_SCAN_STAGE_ONE_DURATION_MS,
        isScanActive = isScanActive
    )
    animateWifiDetectProgress(
        detectProgress = detectProgress,
        start = WIFI_SCAN_STAGE_ONE_END,
        end = WIFI_SCAN_STAGE_TWO_END,
        durationMs = WIFI_SCAN_STAGE_TWO_DURATION_MS,
        isScanActive = isScanActive
    )
    animateWifiDetectProgress(
        detectProgress = detectProgress,
        start = WIFI_SCAN_STAGE_TWO_END,
        end = WIFI_SCAN_STAGE_THREE_END,
        durationMs = WIFI_SCAN_STAGE_THREE_DURATION_MS,
        isScanActive = isScanActive
    )
    waitForWifiScanStage(
        durationMs = WIFI_SCAN_COMPLETE_HOLD_MS,
        isScanActive = isScanActive
    )
}

private suspend fun animateWifiDetectProgress(
    detectProgress: MutableIntState,
    start: Int,
    end: Int,
    durationMs: Long,
    isScanActive: () -> Boolean
) {
    val safeStart = maxOf(start, detectProgress.intValue)
    detectProgress.intValue = safeStart
    if (safeStart >= end) {
        waitForWifiScanStage(durationMs = durationMs, isScanActive = isScanActive)
        return
    }

    var elapsedMs = 0L
    while (elapsedMs < durationMs) {
        if (!isScanActive()) {
            return
        }
        val progressFraction = elapsedMs.toFloat() / durationMs.toFloat()
        detectProgress.intValue = (safeStart + (end - safeStart) * progressFraction)
            .roundToInt()
            .coerceIn(safeStart, end)
        val nextDelayMs = minOf(WIFI_SCAN_PROGRESS_TICK_MS, durationMs - elapsedMs)
        delay(nextDelayMs)
        elapsedMs += nextDelayMs
    }

    if (isScanActive()) {
        detectProgress.intValue = end
    }
}

private suspend fun waitForWifiScanStage(
    durationMs: Long,
    isScanActive: () -> Boolean
) {
    var elapsedMs = 0L
    while (elapsedMs < durationMs) {
        if (!isScanActive()) {
            return
        }
        val nextDelayMs = minOf(WIFI_SCAN_PROGRESS_TICK_MS, durationMs - elapsedMs)
        delay(nextDelayMs)
        elapsedMs += nextDelayMs
    }
}

fun wifiDetect(
    localIp: String,
    isScanActive: () -> Boolean = { true },
    onDeviceDetected: (WifiDevice) -> Unit = {},
    onDetectFinished: (List<WifiDevice>, List<WifiDevice>) -> Unit = { _, _ -> }
) {
    if (localIp.isBlank() || localIp == "0.0.0.0") {
        if (isScanActive()) {
            onDetectFinished(emptyList(), emptyList())
        }
        return
    }

    val suspiciousDevices = mutableListOf<WifiDevice>()
    val trustedDevices = mutableListOf<WifiDevice>()
    val resultLock = Any()
    try {
        SubnetDevices.fromLocalAddress().findDevices(object : SubnetDevices.OnSubnetDeviceFound {
            override fun onDeviceFound(device: Device?) {
            }

            override fun onFinished(devicesFound: ArrayList<Device?>?) {
            if (devicesFound == null) {
                if (isScanActive()) {
                    onDetectFinished(emptyList(), emptyList())
                }
                return
            }
            // 并发检测每个IP的类型
            val threads = mutableListOf<Thread>()
            for (dev in devicesFound) {
                val targetDevice = dev ?: continue
                val t = Thread {
                    if (!isScanActive()) {
                        return@Thread
                    }
                    val wifiDevice = WifiHelper.detectDeviceType(targetDevice, localIp)
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
                }
                threads.add(t)
                t.start()
                if (threads.size >= 10) {
                    threads.removeAll { !it.isAlive }
                }
            }
            threads.forEach { it.join(3000) }

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
    } catch (_: Throwable) {
        // The subnet scan library may throw IllegalAccessError when no local address is available.
        if (isScanActive()) {
            onDetectFinished(emptyList(), emptyList())
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
