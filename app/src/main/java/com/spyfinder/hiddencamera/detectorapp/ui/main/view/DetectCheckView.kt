package com.spyfinder.hiddencamera.detectorapp.ui.main.view
import com.spyfinder.hiddencamera.detectorapp.theme.AppColors
import com.spyfinder.hiddencamera.detectorapp.theme.AppShapes
import com.spyfinder.hiddencamera.detectorapp.theme.AppSpacing

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
import android.net.Uri
import android.provider.Settings
import com.spyfinder.hiddencamera.detectorapp.R
import com.spyfinder.hiddencamera.detectorapp.event.Event
import com.spyfinder.hiddencamera.detectorapp.theme.Transparent
import com.spyfinder.hiddencamera.detectorapp.theme.AppColors.textPrimary
import com.spyfinder.hiddencamera.detectorapp.theme.AppColors.outline
import com.spyfinder.hiddencamera.detectorapp.theme.AppColors.textSecondary
import com.spyfinder.hiddencamera.detectorapp.ui.subscribe.SubscribeActivity
import com.spyfinder.hiddencamera.detectorapp.utils.SubscribeHelper
import com.spyfinder.hiddencamera.detectorapp.utils.NotificationAccess
import com.spyfinder.hiddencamera.detectorapp.utils.findActivity
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
    val notifyLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        NotificationAccess.markAsked(context)
        vm.start()
    }
    var notifyPrompt by remember { mutableStateOf<NotificationAccess.Step?>(null) }
    fun startScan() { notifyPrompt = null; vm.start() }
    val startDetectAction = {
        val activity = context.findActivity()
        when (val step = activity?.let(NotificationAccess::step) ?: if (NotificationAccess.granted(context)) NotificationAccess.Step.GRANTED else NotificationAccess.Step.EXPLAIN) {
            NotificationAccess.Step.NOT_REQUIRED, NotificationAccess.Step.GRANTED -> startScan()
            NotificationAccess.Step.EXPLAIN, NotificationAccess.Step.SETTINGS -> notifyPrompt = step
        }
    }
    LaunchedEffect(localMain.pendingWifiAutoScan.value, localMain.selectTabIndex.intValue) {
        if (localMain.pendingWifiAutoScan.value && localMain.selectTabIndex.intValue == 0) {
            localMain.pendingWifiAutoScan.value = false
            if (localMain.scanStatus != ScanStatus.RUNNING) startDetectAction()
        }
    }
    BoxWithConstraints(modifier = Modifier.fillMaxSize().statusBarsPadding().padding(top = AppSpacing.pageTop)) {
        // Preserve the original radar size when space permits; keep text and controls outside it.
        val radarSize = minOf(313.dp, maxWidth, (maxHeight - 304.dp).coerceAtLeast(0.dp))
        val headerHeight = ((maxHeight - radarSize) / 2 - AppSpacing.compact).coerceAtLeast(0.dp)
        Column(modifier = Modifier.fillMaxWidth().heightIn(max = headerHeight)
            .verticalScroll(rememberScrollState()).padding(horizontal = AppSpacing.screen)) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(context.getString(R.string.title_wifi_scan), color = AppColors.textPrimary, fontSize = 28.sp, fontWeight = FontWeight.W700, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                if (localMain.hasScanHistory && localMain.isStartDetect.value && localMain.scanStatus != ScanStatus.RUNNING) {
                    Text(context.getString(R.string.action_history), color = AppColors.textSecondary, fontSize = 12.sp,
                        modifier = Modifier.clickable { localMain.openLatestResult() }.padding(horizontal = AppSpacing.compact, vertical = AppSpacing.section))
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
            Spacer(modifier = Modifier.height(AppSpacing.compact))
            Text(
                if (localMain.networkName.isBlank()) context.getString(R.string.wifi_connected_unknown)
                else context.getString(R.string.wifi_connected_network, localMain.networkName),
                color = AppColors.textSecondary, fontSize = 14.sp, fontWeight = FontWeight.W400,
                maxLines = 1, overflow = TextOverflow.Ellipsis
            )
            if (localMain.scanStatus == ScanStatus.COMPLETE) {
                Spacer(Modifier.height(AppSpacing.compact))
                Text(
                    if (localMain.suspiciousDevices.isEmpty()) context.getString(R.string.scan_complete_no_clues)
                    else context.getString(R.string.scan_complete_clues, localMain.suspiciousDevices.size),
                    color = AppColors.textSecondary, fontSize = 12.sp, maxLines = 2,
                    overflow = TextOverflow.Ellipsis, modifier = Modifier.fillMaxWidth()
                )
            } else if (localMain.scanStatus != ScanStatus.IDLE) {
                Spacer(Modifier.height(AppSpacing.compact))
                HistoryReadWarning()
                Text(ScanStrings.text(context, localMain.scanMessage), color = AppColors.textSecondary, fontSize = 12.sp,
                    lineHeight = 18.sp, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.fillMaxWidth())
                if (localMain.scanStatus == ScanStatus.RUNNING && !localMain.scanProtected) {
                    Text(context.getString(R.string.scan_unprotected_hint), color = AppColors.textSecondary, fontSize = 10.sp,
                        lineHeight = 14.sp, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.fillMaxWidth().padding(top = AppSpacing.micro))
                }
            }
        }

        val radarStartsScan = !localMain.isStartDetect.value || localMain.scanStatus == ScanStatus.FAILED
        Box(
            modifier = Modifier
                .size(radarSize)
                .align(Alignment.Center)
                .clickable(enabled = radarStartsScan, onClick = startDetectAction)
        ) {
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
                                color = AppColors.textPrimary,
                                fontWeight = FontWeight.W700,
                                baselineShift = BaselineShift(0f) // 调整符号的垂直位置
                            )
                        ) {
                            append(if (localMain.scanStatus == ScanStatus.FAILED) context.getString(R.string.action_retry) else "${detectProgress.intValue}")
                        }
                        withStyle(
                            style = SpanStyle(
                                fontSize = 24.sp,
                                color = AppColors.textPrimary,
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
                    color = AppColors.textPrimary,
                    fontSize = 44.sp,
                    fontWeight = FontWeight.W700,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
        }

        Column(modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = AppSpacing.large), horizontalAlignment = Alignment.CenterHorizontally) {
            if (localMain.isStartDetect.value) {
                Row(modifier = Modifier, verticalAlignment = Alignment.CenterVertically) {
                    Image(painter = painterResource(R.drawable.svg_icon_warning_red), contentDescription = null)
                    Spacer(modifier = Modifier.width(AppSpacing.micro))
                    Text(context.getString(R.string.camera_clues_prefix), color = AppColors.textSecondary, fontSize = 16.sp, fontWeight = FontWeight.W500)
                    Text(
                        "${localMain.suspiciousDevices.size}",
                        color = AppColors.warning,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.W500
                    )
                }
            }
            Spacer(modifier = Modifier.height(AppSpacing.large))
            if (localMain.isStartDetect.value) {
                if (localMain.isAnimating.value) {
                    Box(modifier = Modifier
                        .fillMaxWidth()
                        .height(AppSpacing.control)
                        .padding(horizontal = AppSpacing.large)
                        .background(color = AppColors.outline, shape = RoundedCornerShape(AppShapes.pill))
                        .border(width = 1.dp, shape = RoundedCornerShape(AppShapes.pill), brush = Brush.verticalGradient(colorStops = arrayOf(0f to AppColors.outline, 0.5f to Transparent, 1f to AppColors.outline)))
                        .clickable{
                            vm.cancel(source = "cancel_button")
                        }
                    ) {
                        Text(
                            text = context.getString(R.string.action_cancel),
                            color = AppColors.textSecondary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.W500,
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }
                } else {
                    Row(modifier = Modifier
                        .fillMaxWidth()
                        .height(AppSpacing.control)
                        .padding(horizontal = AppSpacing.large)
                    ) {
                        Box(modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .background(color = AppColors.outline, shape = RoundedCornerShape(AppShapes.pill))
                            .border(width = 1.dp, shape = RoundedCornerShape(AppShapes.pill), brush = Brush.verticalGradient(colorStops = arrayOf(0f to AppColors.outline, 0.5f to Transparent, 1f to AppColors.outline)))
                            .clickable{
                                startDetectAction()
                            }
                    ) {
                        Text(
                            text = context.getString(R.string.action_recheck),
                            color = AppColors.textSecondary,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.W500,
                                modifier = Modifier.align(Alignment.Center)
                            )
                        }
                        Spacer(modifier = Modifier.width(AppSpacing.section))
                        Box(modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .background(color = AppColors.primary, shape = RoundedCornerShape(AppShapes.pill))
                            .clickable{
                                Event.event(context, Event.WIFI_RESULT_CLICK, Event.PARAM_SOURCE to "result_button")
                                openResultWithSubscriptionCheck()
                            }
                        ) {
                            Text(
                                text = if (checking) context.getString(R.string.action_checking) else context.getString(R.string.action_result),
                                color = AppColors.textPrimary,
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
                        .height(AppSpacing.control)
                        .padding(horizontal = AppSpacing.large)
                    ) {
                        Box(modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .background(color = AppColors.outline, shape = RoundedCornerShape(AppShapes.pill))
                            .border(width = 1.dp, shape = RoundedCornerShape(AppShapes.pill), brush = Brush.verticalGradient(colorStops = arrayOf(0f to AppColors.outline, 0.5f to Transparent, 1f to AppColors.outline)))
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
                                color = AppColors.textSecondary,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.W500,
                                modifier = Modifier.align(Alignment.Center)
                            )
                        }
                        Spacer(modifier = Modifier.width(AppSpacing.section))
                        Box(modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .background(color = AppColors.primary, shape = RoundedCornerShape(AppShapes.pill))
                            .clickable{
                                startDetectAction()
                            }
                        ) {
                            Text(
                                text = context.getString(R.string.action_start),
                                color = AppColors.textPrimary,
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
                        .height(AppSpacing.control)
                        .padding(horizontal = AppSpacing.large)
                        .background(color = AppColors.primary, shape = RoundedCornerShape(AppShapes.pill))
                    ) {
                        Text(
                            text = context.getString(R.string.action_start),
                            color = AppColors.textPrimary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.W500,
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }
                }
            }
        }
        notifyPrompt?.let { prompt ->
            val explain = prompt == NotificationAccess.Step.EXPLAIN
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(AppColors.scrim)
                    .clickable { notifyPrompt = null; startScan() }
            ) {
                Column(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(horizontal = AppSpacing.large)
                        .background(AppColors.dialogSurface, RoundedCornerShape(20.dp))
                        .clickable { }
                        .padding(20.dp)
                ) {
                    Text(
                        context.getString(if (explain) R.string.notify_rationale_title else R.string.notify_settings_title),
                        color = AppColors.textPrimary, fontSize = 18.sp, fontWeight = FontWeight.W600
                    )
                    Spacer(Modifier.height(AppSpacing.compact))
                    Text(
                        context.getString(if (explain) R.string.notify_rationale_body else R.string.notify_settings_body),
                        color = AppColors.textSecondary, fontSize = 14.sp, lineHeight = 20.sp
                    )
                    Spacer(Modifier.height(AppSpacing.screen))
                    Row(modifier = Modifier.fillMaxWidth()) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(AppSpacing.wide)
                                .background(AppColors.outline, RoundedCornerShape(AppShapes.pill))
                                .clickable { notifyPrompt = null; startScan() },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(context.getString(R.string.notify_not_now), color = AppColors.textSecondary, fontSize = 14.sp, fontWeight = FontWeight.W500)
                        }
                        Spacer(Modifier.width(AppSpacing.section))
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(AppSpacing.wide)
                                .background(AppColors.primary, RoundedCornerShape(AppShapes.pill))
                                .clickable {
                                    notifyPrompt = null
                                    if (explain) notifyLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                                    else context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}")))
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                context.getString(if (explain) R.string.notify_allow else R.string.app_settings),
                                color = AppColors.textPrimary, fontSize = 14.sp, fontWeight = FontWeight.W500
                            )
                        }
                    }
                }
            }
        }
    }
}
