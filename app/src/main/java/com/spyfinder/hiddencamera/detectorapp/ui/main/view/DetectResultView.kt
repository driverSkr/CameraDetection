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
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.selected
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import com.spyfinder.hiddencamera.detectorapp.R
import com.spyfinder.hiddencamera.detectorapp.dialog.DialogHelper
import com.spyfinder.hiddencamera.detectorapp.theme.AppColors.background
import com.spyfinder.hiddencamera.detectorapp.theme.AppColors.resultAccent
import com.spyfinder.hiddencamera.detectorapp.theme.AppColors.textPrimary
import com.spyfinder.hiddencamera.detectorapp.theme.AppColors.outline
import com.spyfinder.hiddencamera.detectorapp.theme.AppColors.textSecondary
import com.spyfinder.hiddencamera.detectorapp.ui.main.context.LocalMainContextEntity
import com.spyfinder.hiddencamera.detectorapp.ui.subscribe.SubscribeActivity
import com.spyfinder.hiddencamera.detectorapp.utils.SubscribeHelper
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.haze
import dev.chrisbanes.haze.hazeChild
import kotlinx.coroutines.launch

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun DetectResultView() {
    val hazeState = remember { HazeState() }
    val context = LocalContext.current
    val keyboard = LocalSoftwareKeyboardController.current
    val localMain = LocalMainContextEntity.current
    val resultSuspiciousDevices = localMain.resultSuspiciousDevices
    val resultTrustedDevices = localMain.resultTrustedDevices
    val allDevices = (resultSuspiciousDevices + resultTrustedDevices).distinctBy { it.ip }
    val identityGroups = allDevices.groupBy { com.spyfinder.hiddencamera.detectorapp.scan.DeviceIdentity.forDevice(it).type }
    val recordKey = if (localMain.isShowingLatestHistoryResult) localMain.displayedHistory?.id else localMain.currentRecordId
    var selectedType by rememberSaveable(recordKey) { mutableStateOf<String?>(null) }
    var search by rememberSaveable(recordKey) { mutableStateOf("") }
    var statusFilter by rememberSaveable(recordKey) { mutableStateOf(0) }
    var showScanDetails by rememberSaveable(recordKey) { mutableStateOf(false) }
    val resultStatus = if (localMain.isShowingLatestHistoryResult) localMain.displayedHistory?.status else localMain.scanStatus
    val resultMessage = ScanStrings.text(context, if (localMain.isShowingLatestHistoryResult) localMain.latestMessage else localMain.scanMessage)
    val needsAttention = resultStatus in setOf(ScanStatus.FAILED, ScanStatus.CANCELLED, ScanStatus.PARTIAL)
    val listState = rememberLazyListState()
    val activeType = selectedType?.takeIf { it in identityGroups }
    val visibleDevices = (activeType?.let { identityGroups.getValue(it) } ?: allDevices).filter { device ->
        (statusFilter != 1 || (device.finding == com.spyfinder.hiddencamera.detectorapp.scan.Finding.CAMERA_FEATURES && !device.userTrusted)) &&
        (statusFilter != 2 || !device.analysisComplete) &&
        (search.isBlank() || (listOf(device.ip, device.name, device.brandModel) +
            listOf("upnp_name", "mdns_name", "mdns_host", "identity_model").mapNotNull { device.details[it] })
            .any { it.contains(search.trim(), ignoreCase = true) })
    }
    LaunchedEffect(recordKey, activeType, search, statusFilter) { listState.scrollToItem(0) }
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
            Row(modifier = Modifier.align(Alignment.CenterHorizontally), verticalAlignment = Alignment.CenterVertically) {
                Image(painter = painterResource(R.drawable.svg_icon_sensor), modifier = Modifier.size(20.dp), contentDescription = null)
                Spacer(modifier = Modifier.width(AppSpacing.micro))
                Text(context.getString(R.string.ux_scan_overview), color = AppColors.textPrimary, fontSize = 14.sp, fontWeight = FontWeight.W400)
            }
            Spacer(modifier = Modifier.height(AppSpacing.screen))
            Row(modifier = Modifier.fillMaxWidth().height(76.dp)) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .weight(1f)
                        .background(color = AppColors.warningSurface, shape = RoundedCornerShape(20.dp))
                ) {
                    Column(modifier = Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("${resultSuspiciousDevices.size}", color = AppColors.warning, fontSize = 32.sp, fontWeight = FontWeight.W700)
                        Spacer(modifier = Modifier.height(AppSpacing.micro))
                        Text(context.getString(R.string.camera_clues), color = AppColors.warning, fontSize = 12.sp, fontWeight = FontWeight.W400)
                    }
                }
                Spacer(modifier = Modifier.width(AppSpacing.compact))
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .weight(1f)
                        .background(color = AppColors.outline, shape = RoundedCornerShape(20.dp))
                ) {
                    Column(modifier = Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("${resultTrustedDevices.size}", color = AppColors.textPrimary, fontSize = 32.sp, fontWeight = FontWeight.W700)
                        Spacer(modifier = Modifier.height(AppSpacing.micro))
                        Text(context.getString(R.string.other_devices), color = AppColors.textSecondary, fontSize = 12.sp, fontWeight = FontWeight.W400)
                    }
                }
            }
            Spacer(modifier = Modifier.height(AppSpacing.compact))
            Text(context.getString(R.string.ux_incomplete_count, allDevices.count { !it.analysisComplete }),
                color = AppColors.textSecondary, fontSize = 12.sp)
            LazyColumn(
                state = listState,
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(vertical = AppSpacing.section),
                verticalArrangement = Arrangement.spacedBy(AppSpacing.compact)
            ) {
                stickyHeader {
                    Column(Modifier.fillMaxWidth().background(AppColors.background).padding(vertical = AppSpacing.micro)) {
                        OutlinedTextField(value = search, onValueChange = { search = it },
                            label = { Text(context.getString(R.string.ux_search)) }, singleLine = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                            keyboardActions = KeyboardActions(onSearch = { keyboard?.hide() }),
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = AppColors.textPrimary, unfocusedTextColor = AppColors.textPrimary,
                                focusedLabelColor = AppColors.textSecondary, unfocusedLabelColor = AppColors.textSecondary, cursorColor = AppColors.textPrimary))
                        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(AppSpacing.compact)) {
                            (listOf<String?>(null) + identityGroups.keys.sorted()).forEach { type ->
                                val title = type?.let { ScanStrings.text(context, it) } ?: context.getString(R.string.all_detection_list)
                                val count = type?.let { identityGroups.getValue(it).size } ?: allDevices.size
                                Text("$title ($count)", color = if (activeType == type) AppColors.textPrimary else AppColors.textSecondary, fontSize = 12.sp,
                                    modifier = Modifier.semantics { selected = activeType == type }
                                        .clickable { selectedType = type; keyboard?.hide() }.padding(horizontal = AppSpacing.compact, vertical = 10.dp))
                            }
                        }
                        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(AppSpacing.compact)) {
                            listOf(R.string.ux_status_all, R.string.camera_clues, R.string.ux_status_incomplete).forEachIndexed { index, label ->
                                Text(context.getString(label), color = if (statusFilter == index) AppColors.textPrimary else AppColors.textSecondary, fontSize = 12.sp,
                                    modifier = Modifier.semantics { selected = statusFilter == index }
                                        .clickable { statusFilter = index; keyboard?.hide() }.padding(horizontal = AppSpacing.compact, vertical = 10.dp))
                            }
                        }
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            val category = activeType?.let { ScanStrings.text(context, it) } ?: context.getString(R.string.ux_status_all)
                            Text(context.getString(R.string.ux_filter_count, category, visibleDevices.size, allDevices.size),
                                color = AppColors.textSecondary, fontSize = 12.sp, modifier = Modifier.weight(1f))
                            if (activeType != null || search.isNotBlank() || statusFilter != 0)
                                Text(context.getString(R.string.ux_clear_filter), color = AppColors.textPrimary, fontSize = 12.sp,
                                    modifier = Modifier.clickable { selectedType = null; search = ""; statusFilter = 0; keyboard?.hide() }.padding(AppSpacing.compact))
                        }
                    }
                }
                item {
                    HistoryReadWarning()
                    if (saveFailed) {
                        Text(context.getString(R.string.history_save_failed), color = AppColors.textSecondary, fontSize = 12.sp)
                        Text(context.getString(R.string.history_retry_save), color = AppColors.primary, fontSize = 12.sp,
                            modifier = Modifier.clickable { localMain.retryHistorySave() }.padding(vertical = AppSpacing.compact))
                    }
                    if (localMain.isShowingLatestHistoryResult) {
                        if (localMain.hasHistoryChoice) {
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
                        localMain.displayedHistory?.let { record ->
                            val label = when (record.status) {
                                ScanStatus.COMPLETE -> R.string.history_status_complete
                                ScanStatus.PARTIAL -> R.string.history_status_partial
                                ScanStatus.CANCELLED -> R.string.history_status_cancelled
                                ScanStatus.FAILED -> R.string.history_status_failed
                                ScanStatus.RUNNING -> R.string.history_status_running
                                else -> R.string.history_status_legacy
                            }
                            Text(context.getString(label), color = AppColors.textSecondary, fontSize = 12.sp)
                            if (record.startedAt > 0) Text(context.getString(R.string.history_meta,
                                java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.ROOT).format(java.util.Date(record.startedAt)),
                                record.network, record.coverage.checked, record.coverage.total, record.coverage.analyzed), color = AppColors.textSecondary, fontSize = 12.sp)
                        }
                    }
                    if (!localMain.isShowingLatestHistoryResult) {
                        val label = when (resultStatus) {
                            ScanStatus.COMPLETE -> R.string.history_status_complete
                            ScanStatus.PARTIAL -> R.string.history_status_partial
                            ScanStatus.CANCELLED -> R.string.history_status_cancelled
                            ScanStatus.FAILED -> R.string.history_status_failed
                            ScanStatus.RUNNING -> R.string.history_status_running
                            else -> R.string.history_status_legacy
                        }
                        Text(context.getString(label), color = AppColors.textPrimary, fontSize = 14.sp)
                    }
                    if (needsAttention) {
                        Text(resultMessage, color = AppColors.textSecondary, fontSize = 12.sp)
                        Text(context.getString(R.string.ux_return_to_scan), color = AppColors.primary, fontSize = 14.sp,
                            modifier = Modifier.clickable {
                                localMain.closeDetectResult()
                                localMain.openWifiFeature()
                            }.padding(vertical = AppSpacing.section))
                    }
                    if (!needsAttention) Text(context.getString(if (showScanDetails) R.string.ux_hide_scan_details else R.string.ux_show_scan_details), color = AppColors.textPrimary, fontSize = 12.sp,
                        modifier = Modifier.clickable { showScanDetails = !showScanDetails }.padding(vertical = 10.dp))
                    if (showScanDetails && !needsAttention) Text(resultMessage, color = AppColors.textSecondary, fontSize = 12.sp)
                    Spacer(Modifier.height(6.dp))
                    Text(context.getString(R.string.result_explanation), color = AppColors.textSecondary, fontSize = 12.sp)
                    if (localMain.isShowingLatestHistoryResult) {
                        Text(context.getString(R.string.history_offline_note), color = AppColors.textSecondary, fontSize = 12.sp)
                    }
                }
                if (visibleDevices.isEmpty()) item {
                    val emptyLabel = when {
                        allDevices.isNotEmpty() -> R.string.ux_no_matches
                        resultStatus == ScanStatus.FAILED -> R.string.ux_scan_failed_empty
                        resultStatus == ScanStatus.CANCELLED || resultStatus == ScanStatus.PARTIAL -> R.string.ux_scan_incomplete_empty
                        resultStatus == ScanStatus.RUNNING -> R.string.ux_scan_running_empty
                        else -> R.string.ux_scan_empty
                    }
                    Text(context.getString(emptyLabel), color = AppColors.textSecondary, fontSize = 14.sp, modifier = Modifier.padding(AppSpacing.screen))
                }
                items(visibleDevices.size, key = { visibleDevices[it].ip }) { index ->
                    WifiInfoItemView(visibleDevices[index]) {
                        if (!isSubscribed) return@WifiInfoItemView
                        DialogHelper.showWifiInfoDialog(context as? FragmentActivity ?: return@WifiInfoItemView, visibleDevices[index]) { device ->
                            localMain.markDeviceAsSafe(device)
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