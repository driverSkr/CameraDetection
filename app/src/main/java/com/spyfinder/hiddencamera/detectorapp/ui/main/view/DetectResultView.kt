package com.spyfinder.hiddencamera.detectorapp.ui.main.view
import com.spyfinder.hiddencamera.detectorapp.theme.AppColors
import com.spyfinder.hiddencamera.detectorapp.theme.AppShapes
import com.spyfinder.hiddencamera.detectorapp.theme.AppSpacing

import com.spyfinder.hiddencamera.detectorapp.utils.ScanStrings
import com.spyfinder.hiddencamera.detectorapp.utils.ScanHistoryStore
import com.spyfinder.hiddencamera.detectorapp.scan.ScanStatus
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import com.spyfinder.hiddencamera.detectorapp.utils.SubscriptionGate
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import com.spyfinder.hiddencamera.detectorapp.R
import com.spyfinder.hiddencamera.detectorapp.dialog.DialogHelper
import com.spyfinder.hiddencamera.detectorapp.model.WifiDevice
import com.spyfinder.hiddencamera.detectorapp.scan.DeviceIdentity
import com.spyfinder.hiddencamera.detectorapp.theme.AppColors.resultAccent
import com.spyfinder.hiddencamera.detectorapp.ui.main.context.LocalMainContextEntity
import com.spyfinder.hiddencamera.detectorapp.ui.subscribe.SubscribeActivity
import com.spyfinder.hiddencamera.detectorapp.utils.DevicePresentation
import com.spyfinder.hiddencamera.detectorapp.utils.SubscribeHelper
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.haze
import dev.chrisbanes.haze.hazeChild
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@Composable
fun DetectResultView() {
    val hazeState = remember { HazeState() }
    val context = LocalContext.current
    val localMain = LocalMainContextEntity.current
    val resultSuspiciousDevices = localMain.resultSuspiciousDevices
    val resultTrustedDevices = localMain.resultTrustedDevices
    val allDevices = (resultSuspiciousDevices + resultTrustedDevices).distinctBy { it.ip }
    val identityGroups = allDevices.groupBy { DeviceIdentity.forDevice(it).type }
    val recordKey = if (localMain.isShowingLatestHistoryResult) localMain.displayedHistory?.id else localMain.currentRecordId
    var selectedType by rememberSaveable(recordKey) { mutableStateOf<String?>(null) }
    var lookExpanded by rememberSaveable(recordKey) { mutableStateOf(true) }
    var otherExpanded by rememberSaveable(recordKey) { mutableStateOf(true) }
    var showScanDetails by rememberSaveable(recordKey) { mutableStateOf(false) }
    val resultStatus = if (localMain.isShowingLatestHistoryResult) localMain.displayedHistory?.status else localMain.scanStatus
    val resultMessage = ScanStrings.text(context, if (localMain.isShowingLatestHistoryResult) localMain.latestMessage else localMain.scanMessage)
    val needsAttention = resultStatus in setOf(ScanStatus.FAILED, ScanStatus.CANCELLED, ScanStatus.PARTIAL)
    val listState = rememberLazyListState()
    val activeType = selectedType?.takeIf { it in identityGroups }
    fun inType(device: WifiDevice) = activeType == null || DeviceIdentity.forDevice(device).type == activeType
    val lookDevices = remember(allDevices, activeType) {
        resultSuspiciousDevices.filter(::inType).sortedWith(compareBy({ DevicePresentation.listRank(it) }, { it.ip }))
    }
    val otherDevices = remember(allDevices, activeType) {
        resultTrustedDevices.filter(::inType).sortedWith(compareBy({ DevicePresentation.listRank(it) }, { it.ip }))
    }
    LaunchedEffect(recordKey, activeType) { listState.scrollToItem(0) }
    val scope = rememberCoroutineScope()
    val saveFailed by ScanHistoryStore.saveFailed.collectAsState()
    val isSubscribed = SubscriptionGate.hasAccessFlow.collectAsState().value
    var checking by remember { mutableStateOf(false) }
    val shouldRefreshSubscribeStateAfterSubscribe = rememberSaveable { mutableStateOf(false) }
    val subscribeLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        if (!shouldRefreshSubscribeStateAfterSubscribe.value) {
            return@rememberLauncherForActivityResult
        }
        scope.launch {
            SubscriptionGate.hasAccess()
            shouldRefreshSubscribeStateAfterSubscribe.value = false
        }
    }

    fun openSubscribeWithResultRefresh() {
        if (checking) return
        checking = true
        scope.launch {
            try {
                val subscribed = if (isSubscribed) {
                    true
                } else {
                    SubscriptionGate.hasAccess()
                }

                if (subscribed) {
                    shouldRefreshSubscribeStateAfterSubscribe.value = false
                    return@launch
                }

                if (!SubscribeHelper.canOfferPurchase) {
                    Toast.makeText(context, context.getString(R.string.store_access_unavailable), Toast.LENGTH_LONG).show()
                    return@launch
                }
                shouldRefreshSubscribeStateAfterSubscribe.value = true
                subscribeLauncher.launch(Intent(context, SubscribeActivity::class.java))
            } finally { checking = false }
        }
    }

    BackHandler {
        localMain.closeDetectResult()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding().navigationBarsPadding()
                .padding(horizontal = AppSpacing.section)
                .haze(hazeState)
        ) {
            Box(modifier = Modifier.fillMaxWidth().height(AppSpacing.topBar)) {
                Image(
                    painter = painterResource(R.drawable.svg_icon_back),
                    contentDescription = context.getString(R.string.a11y_back),
                    modifier = Modifier.align(Alignment.CenterStart).clickable {
                        localMain.closeDetectResult()
                    }
                )
                Text(
                    if (localMain.isShowingLatestHistoryResult) context.getString(R.string.action_history) else context.getString(R.string.action_result),
                    color = AppColors.textPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.W500,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
            Spacer(modifier = Modifier.height(AppSpacing.compact))
            Row(modifier = Modifier.fillMaxWidth().height(76.dp)) {
                ResultCountCard(
                    count = resultSuspiciousDevices.size,
                    label = context.getString(R.string.camera_clues),
                    background = AppColors.warningSurface,
                    countColor = AppColors.warning,
                    labelColor = AppColors.warning,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(AppSpacing.compact))
                ResultCountCard(
                    count = resultTrustedDevices.size,
                    label = context.getString(R.string.other_devices),
                    background = AppColors.outline,
                    countColor = AppColors.textPrimary,
                    labelColor = AppColors.textSecondary,
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(modifier = Modifier.height(AppSpacing.compact))
            ResultTypeTabs(
                types = listOf<String?>(null) + identityGroups.keys.sorted(),
                activeType = activeType,
                typeTitle = { type ->
                    type?.let { ScanStrings.text(context, it) } ?: context.getString(R.string.all_detection_list)
                },
                typeCount = { type ->
                    type?.let { identityGroups.getValue(it).size } ?: allDevices.size
                },
                onSelect = { selectedType = it }
            )
            LazyColumn(
                state = listState,
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(vertical = AppSpacing.section),
                verticalArrangement = Arrangement.spacedBy(AppSpacing.compact)
            ) {
                item {
                    HistoryReadWarning()
                    if (saveFailed) {
                        Text(context.getString(R.string.history_save_failed), color = AppColors.textSecondary, fontSize = 12.sp)
                        Text(context.getString(R.string.history_retry_save), color = AppColors.primary, fontSize = 12.sp,
                            modifier = Modifier.clickable { localMain.retryHistorySave() }.padding(vertical = AppSpacing.compact))
                    }
                    if (localMain.isShowingLatestHistoryResult && localMain.hasHistoryChoice) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(AppSpacing.compact)) {
                            listOf(false to R.string.history_recent, true to if (localMain.archive.complete?.status == ScanStatus.COMPLETE) R.string.history_complete else R.string.history_previous).forEach { (complete, label) ->
                                Text(context.getString(label), fontSize = 12.sp,
                                    color = if (localMain.showingCompleteHistory == complete) AppColors.primary else AppColors.textSecondary,
                                    modifier = Modifier.weight(1f).background(AppColors.outline, RoundedCornerShape(AppShapes.pill))
                                        .clickable { localMain.selectHistory(complete) }.padding(horizontal = AppSpacing.section, vertical = 10.dp))
                            }
                        }
                        Spacer(Modifier.height(AppSpacing.compact))
                    }
                    if (needsAttention) {
                        val label = statusLabel(resultStatus)
                        Text(context.getString(label), color = AppColors.textPrimary, fontSize = 14.sp)
                        Text(resultMessage, color = AppColors.textSecondary, fontSize = 12.sp)
                        Text(context.getString(R.string.ux_return_to_scan), color = AppColors.primary, fontSize = 14.sp,
                            modifier = Modifier.clickable {
                                localMain.closeDetectResult()
                                localMain.openWifiFeature()
                            }.padding(vertical = AppSpacing.section))
                    } else {
                        Text(context.getString(if (showScanDetails) R.string.ux_hide_scan_details else R.string.ux_about_this_scan),
                            color = AppColors.textPrimary, fontSize = 12.sp,
                            modifier = Modifier.clickable { showScanDetails = !showScanDetails }.padding(vertical = 10.dp))
                        if (showScanDetails) {
                            val label = statusLabel(resultStatus)
                            Text(context.getString(label), color = AppColors.textSecondary, fontSize = 12.sp)
                            if (localMain.isShowingLatestHistoryResult) {
                                localMain.displayedHistory?.let { record ->
                                    if (record.startedAt > 0) {
                                        val header = context.getString(
                                            R.string.history_meta_header,
                                            java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.ROOT)
                                                .format(java.util.Date(record.startedAt)),
                                            record.network
                                        )
                                        val addresses = context.resources.getQuantityString(
                                            R.plurals.history_addresses_checked,
                                            record.coverage.checked,
                                            record.coverage.checked,
                                            record.coverage.total
                                        )
                                        val devices = context.resources.getQuantityString(
                                            R.plurals.history_devices_analyzed,
                                            record.coverage.analyzed,
                                            record.coverage.analyzed
                                        )
                                        Text(
                                            "$header\n${context.getString(R.string.history_meta_details, addresses, devices)}",
                                            color = AppColors.textSecondary,
                                            fontSize = 12.sp
                                        )
                                    }
                                }
                            }
                            Text(resultMessage, color = AppColors.textSecondary, fontSize = 12.sp)
                            Text(context.getString(R.string.result_explanation), color = AppColors.textSecondary, fontSize = 12.sp)
                            if (localMain.isShowingLatestHistoryResult) {
                                Text(context.getString(R.string.history_offline_note), color = AppColors.textSecondary, fontSize = 12.sp)
                            }
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                }
                if (allDevices.isEmpty()) item {
                    val emptyLabel = when {
                        resultStatus == ScanStatus.FAILED -> R.string.ux_scan_failed_empty
                        resultStatus == ScanStatus.CANCELLED || resultStatus == ScanStatus.PARTIAL -> R.string.ux_scan_incomplete_empty
                        resultStatus == ScanStatus.RUNNING -> R.string.ux_scan_running_empty
                        else -> R.string.ux_scan_empty
                    }
                    Text(context.getString(emptyLabel), color = AppColors.textSecondary, fontSize = 14.sp, modifier = Modifier.padding(AppSpacing.screen))
                } else if (lookDevices.isEmpty() && otherDevices.isEmpty()) item {
                    Text(context.getString(R.string.ux_no_matches), color = AppColors.textSecondary, fontSize = 14.sp, modifier = Modifier.padding(AppSpacing.screen))
                }
                if (lookDevices.isNotEmpty()) {
                    item(key = "section-look") {
                        ResultSectionHeader(
                            title = context.getString(R.string.camera_clues),
                            count = lookDevices.size,
                            expanded = lookExpanded,
                            onToggle = { lookExpanded = !lookExpanded }
                        )
                    }
                    if (lookExpanded) {
                        items(lookDevices.size, key = { lookDevices[it].ip }) { index ->
                            WifiInfoItemView(lookDevices[index]) {
                                if (!isSubscribed) return@WifiInfoItemView
                                DialogHelper.showWifiInfoDialog(context as? FragmentActivity ?: return@WifiInfoItemView, lookDevices[index]) { device ->
                                    localMain.markDeviceAsSafe(device)
                                }
                            }
                        }
                    }
                }
                if (otherDevices.isNotEmpty()) {
                    item(key = "section-other") {
                        ResultSectionHeader(
                            title = context.getString(R.string.other_devices),
                            count = otherDevices.size,
                            expanded = otherExpanded,
                            onToggle = { otherExpanded = !otherExpanded }
                        )
                    }
                    if (otherExpanded) {
                        items(otherDevices.size, key = { otherDevices[it].ip }) { index ->
                            WifiInfoItemView(otherDevices[index]) {
                                if (!isSubscribed) return@WifiInfoItemView
                                DialogHelper.showWifiInfoDialog(context as? FragmentActivity ?: return@WifiInfoItemView, otherDevices[index]) { device ->
                                    localMain.markDeviceAsSafe(device)
                                }
                            }
                        }
                    }
                }
            }
        }

        if (!isSubscribed) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .hazeChild(hazeState, style = HazeStyle(backgroundColor = AppColors.background, tint = null, blurRadius = AppSpacing.section))
                    .clickable(enabled = false) { }
            ) {
                Column(modifier = Modifier.align(Alignment.Center).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    Row(
                        modifier = Modifier
                            .wrapContentWidth()
                            .background(color = AppColors.overlayLight, shape = RoundedCornerShape(20.dp))
                            .padding(horizontal = AppSpacing.compact, vertical = AppSpacing.micro),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(context.getString(R.string.camera_clues_prefix), fontSize = 12.sp, fontWeight = FontWeight.W400, color = AppColors.textPrimary)
                        Text("${resultSuspiciousDevices.size}", fontSize = 12.sp, fontWeight = FontWeight.W400, color = AppColors.resultAccent)
                        Text(context.getString(R.string.review_devices_suffix), fontSize = 12.sp, fontWeight = FontWeight.W400, color = AppColors.textPrimary)
                    }

                    Spacer(modifier = Modifier.height(AppSpacing.compact))

                    Box(
                        modifier = Modifier
                            .clickable {
                                openSubscribeWithResultRefresh()
                            }
                            .fillMaxWidth()
                            .height(AppSpacing.control)
                            .padding(horizontal = AppSpacing.large)
                            .background(color = AppColors.primary, shape = RoundedCornerShape(AppShapes.pill))
                    ) {
                        Text(
                            text = if (checking) context.getString(R.string.action_checking) else context.getString(R.string.action_view_results),
                            color = AppColors.textPrimary,
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

private fun statusLabel(status: ScanStatus?) = when (status) {
    ScanStatus.COMPLETE -> R.string.history_status_complete
    ScanStatus.PARTIAL -> R.string.history_status_partial
    ScanStatus.CANCELLED -> R.string.history_status_cancelled
    ScanStatus.FAILED -> R.string.history_status_failed
    ScanStatus.RUNNING -> R.string.history_status_running
    else -> R.string.history_status_legacy
}

@Composable
private fun ResultTypeTabs(
    types: List<String?>,
    activeType: String?,
    typeTitle: (String?) -> String,
    typeCount: (String?) -> Int,
    onSelect: (String?) -> Unit
) {
    val scroll = rememberScrollState()
    var viewport by remember { mutableIntStateOf(0) }
    val origins = remember { mutableStateMapOf<String?, Int>() }
    val widths = remember { mutableStateMapOf<String?, Int>() }
    LaunchedEffect(activeType) {
        val target = snapshotFlow {
            val x = origins[activeType]
            val width = widths[activeType]
            if (x == null || width == null || width == 0 || viewport <= 0) null
            else (x + width / 2 - viewport / 2).coerceIn(0, scroll.maxValue.coerceAtLeast(0))
        }.filterNotNull().first()
        scroll.animateScrollTo(target)
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .onSizeChanged { viewport = it.width }
            .horizontalScroll(scroll),
        horizontalArrangement = Arrangement.spacedBy(AppSpacing.compact),
        verticalAlignment = Alignment.CenterVertically
    ) {
        types.forEach { type ->
            val selected = activeType == type
            Text(
                "${typeTitle(type)} (${typeCount(type)})",
                color = if (selected) AppColors.textPrimary else AppColors.textSecondary,
                fontSize = 12.sp,
                modifier = Modifier
                    .onGloballyPositioned {
                        origins[type] = it.positionInParent().x.toInt()
                        widths[type] = it.size.width
                    }
                    .semantics { this.selected = selected }
                    .background(
                        color = if (selected) AppColors.tabSurfaceSelected else AppColors.tabSurface,
                        shape = RoundedCornerShape(AppShapes.pill)
                    )
                    .clickable { onSelect(type) }
                    .padding(horizontal = AppSpacing.section, vertical = AppSpacing.compact)
            )
        }
    }
}

@Composable
private fun ResultSectionHeader(
    title: String,
    count: Int,
    expanded: Boolean,
    onToggle: () -> Unit
) {
    val context = LocalContext.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) {
                stateDescription = context.getString(if (expanded) R.string.tips_expanded else R.string.tips_collapsed)
            }
            .clickable(
                role = Role.Button,
                onClickLabel = context.getString(if (expanded) R.string.tips_collapse else R.string.tips_expand),
                onClick = onToggle
            )
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            "$title ($count)",
            color = AppColors.textSecondary,
            fontSize = 12.sp,
            fontWeight = FontWeight.W500,
            modifier = Modifier.weight(1f)
        )
        Image(
            painter = painterResource(if (expanded) R.drawable.svg_icon_down else R.drawable.svg_icon_next),
            contentDescription = null,
            modifier = Modifier.size(16.dp)
        )
    }
}

@Composable
private fun ResultCountCard(
    count: Int,
    label: String,
    background: Color,
    countColor: Color,
    labelColor: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxHeight()
            .background(color = background, shape = RoundedCornerShape(20.dp))
    ) {
        Column(modifier = Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("$count", color = countColor, fontSize = 32.sp, fontWeight = FontWeight.W700)
            Spacer(modifier = Modifier.height(AppSpacing.micro))
            Text(label, color = labelColor, fontSize = 12.sp, fontWeight = FontWeight.W400)
        }
    }
}
