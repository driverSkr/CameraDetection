package com.spyfinder.hiddencamera.detectorapp.ui.main.view

import com.spyfinder.hiddencamera.detectorapp.utils.ScanStrings
import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.heightIn
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
import com.spyfinder.hiddencamera.detectorapp.event.Event
import com.spyfinder.hiddencamera.detectorapp.theme.Transparent
import com.spyfinder.hiddencamera.detectorapp.theme.White
import com.spyfinder.hiddencamera.detectorapp.theme.White10
import com.spyfinder.hiddencamera.detectorapp.theme.White60
import com.spyfinder.hiddencamera.detectorapp.ui.subscribe.SubscribeActivity
import com.spyfinder.hiddencamera.detectorapp.utils.SubscribeHelper
import kotlinx.coroutines.launch

import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.style.TextOverflow
import com.spyfinder.hiddencamera.detectorapp.scan.LocalScanViewModel
import com.spyfinder.hiddencamera.detectorapp.scan.ScanStatus
import com.spyfinder.hiddencamera.detectorapp.utils.SubscriptionGate


@Composable
fun DetectCheckView() {
    val context = LocalContext.current
    val vm = LocalScanViewModel.current
    val localMain = vm.state
    val isSubscribed = SubscriptionGate.hasAccessFlow.collectAsState().value
    val detectProgress = localMain.detectProgress
    val scope = rememberCoroutineScope()
    var checking by remember { mutableStateOf(false) }
    var waitingForPurchase by rememberSaveable { mutableStateOf(false) }
    val subscribeLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        if (waitingForPurchase) {
            waitingForPurchase = false
            scope.launch { if (SubscriptionGate.hasAccess()) localMain.openCurrentResult() }
        }
    }
    fun openResultWithSubscriptionCheck() {
        if (checking) return
        checking = true
        scope.launch {
            try {
                if (SubscriptionGate.hasAccess()) localMain.openCurrentResult()
                else if (SubscribeHelper.canOfferPurchase) {
                    waitingForPurchase = true
                    subscribeLauncher.launch(Intent(context, SubscribeActivity::class.java))
                } else Toast.makeText(context, context.getString(R.string.access_unconfirmed), Toast.LENGTH_LONG).show()
            } finally { checking = false }
        }
    }
    val startDetectAction = { vm.start() }
    LaunchedEffect(localMain.pendingWifiAutoScan.value, localMain.selectTabIndex.intValue) {
        if (localMain.pendingWifiAutoScan.value && localMain.selectTabIndex.intValue == 0) {
            localMain.pendingWifiAutoScan.value = false
            if (localMain.scanStatus != ScanStatus.RUNNING) vm.start()
        }
    }
    BoxWithConstraints(modifier = Modifier.fillMaxSize().statusBarsPadding().padding(top = 18.dp)) {
        // Preserve the original radar size when space permits; keep text and controls outside it.
        val radarSize = minOf(313.dp, maxWidth, (maxHeight - 304.dp).coerceAtLeast(0.dp))
        val headerHeight = ((maxHeight - radarSize) / 2 - 8.dp).coerceAtLeast(0.dp)
        Column(modifier = Modifier.fillMaxWidth().heightIn(max = headerHeight)
            .verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(context.getString(R.string.title_wifi_scan), color = Color(0xFFFFFFFF), fontSize = 28.sp, fontWeight = FontWeight.W700, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                if (localMain.hasScanHistory && localMain.isStartDetect.value && localMain.scanStatus != ScanStatus.RUNNING) {
                    Text(context.getString(R.string.action_history), color = White60, fontSize = 12.sp,
                        modifier = Modifier.clickable { localMain.openLatestResult() }.padding(horizontal = 8.dp, vertical = 12.dp))
                }
                if (!isSubscribed) {
                    // 未订阅时展示皇冠入口，订阅后自动隐藏。
                    Image(
                        painter = painterResource(R.mipmap.img_crown),
                        contentDescription = null,
                        modifier = Modifier
                            .size(40.dp)
                            .clickable{
                                Event.event(context, Event.SUBSCRIBE_ENTRY_CLICK, Event.PARAM_SOURCE to "wifi_scan_crown")
                                SubscribeActivity.launch(context)
                            }
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(if (localMain.networkLabel.isBlank()) context.getString(R.string.wifi_network_scan) else context.getString(R.string.wifi_network_address, localMain.networkLabel), color = White60, fontSize = 14.sp, fontWeight = FontWeight.W400)
            Spacer(Modifier.height(8.dp))
            Text(ScanStrings.text(context, localMain.scanMessage), color = White60, fontSize = 12.sp,
                lineHeight = 18.sp, softWrap = true, modifier = Modifier.fillMaxWidth())
            if (localMain.scanStatus == ScanStatus.RUNNING) {
                Text(context.getString(R.string.scan_foreground_hint), color = White60, fontSize = 10.sp,
                    lineHeight = 14.sp, modifier = Modifier.fillMaxWidth().padding(top = 4.dp))
            }
        }

        Box(modifier = Modifier.size(radarSize).align(Alignment.Center)) {
            RadarScannerWithControls()
            if (localMain.isStartDetect.value) {
                if (localMain.suspiciousDevices.isNotEmpty()) {
                    RandomRedDotsWithVisibility(isAnimating = localMain.isAnimating, maxDots = localMain.suspiciousDevices.size.coerceAtMost(5), areaWidth = radarSize, areaHeight = radarSize)
                }
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
                            append(if (localMain.scanStatus == ScanStatus.FAILED) context.getString(R.string.action_retry) else "${detectProgress.intValue}")
                        }
                        withStyle(
                            style = SpanStyle(
                                fontSize = 24.sp,
                                color = White,
                                fontWeight = FontWeight.W700,
                                baselineShift = BaselineShift(0f) // 调整符号的垂直位置
                            )
                        ) {
                            if (localMain.scanStatus != ScanStatus.FAILED) append("%")
                        }
                    },
                    modifier = Modifier.align(Alignment.Center)
                )
            } else {
                Text(
                    text = context.getString(R.string.action_start),
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
                    Text(context.getString(R.string.camera_clues_prefix), color = White60, fontSize = 16.sp, fontWeight = FontWeight.W500)
                    Text(
                        "${localMain.suspiciousDevices.size}",
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
                            // 用户主动取消扫描，记录当前进度便于分析中断位置。
                            Event.event(
                                context,
                                Event.WIFI_SCAN_CANCEL,
                                Event.PARAM_SOURCE to "cancel_button",
                                Event.PARAM_PROGRESS to detectProgress.intValue
                            )
                            vm.cancel()
                        }
                    ) {
                        Text(
                            text = context.getString(R.string.action_cancel),
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
                            text = context.getString(R.string.action_recheck),
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
                                Event.event(context, Event.WIFI_RESULT_CLICK, Event.PARAM_SOURCE to "result_button")
                                openResultWithSubscriptionCheck()
                            }
                        ) {
                            Text(
                                text = if (checking) context.getString(R.string.action_checking) else context.getString(R.string.action_result),
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
                                Event.event(
                                    context,
                                    Event.WIFI_HISTORY_CLICK,
                                    Event.PARAM_SUSPICIOUS_COUNT to localMain.latestSuspiciousDevices.size,
                                    Event.PARAM_TRUSTED_COUNT to localMain.latestTrustedDevices.size
                                )
                                localMain.openLatestResult()
                            }
                        ) {
                            Text(
                                text = context.getString(R.string.action_history),
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
                                text = context.getString(R.string.action_start),
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
                            text = context.getString(R.string.action_start),
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
