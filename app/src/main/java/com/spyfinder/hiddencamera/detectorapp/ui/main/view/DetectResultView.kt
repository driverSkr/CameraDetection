package com.spyfinder.hiddencamera.detectorapp.ui.main.view

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
import com.spyfinder.hiddencamera.detectorapp.theme.Black
import com.spyfinder.hiddencamera.detectorapp.theme.Orange
import com.spyfinder.hiddencamera.detectorapp.theme.White
import com.spyfinder.hiddencamera.detectorapp.theme.White10
import com.spyfinder.hiddencamera.detectorapp.theme.White60
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
    val listState = rememberLazyListState()
    val activeType = selectedType?.takeIf { it in identityGroups }
    val visibleDevices = (activeType?.let { identityGroups.getValue(it) } ?: allDevices).filter { device ->
        (statusFilter != 1 || device.finding == com.spyfinder.hiddencamera.detectorapp.scan.Finding.CAMERA_FEATURES) &&
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
                .padding(horizontal = 12.dp)
                .haze(hazeState)
        ) {
            Box(modifier = Modifier.fillMaxWidth().height(54.dp)) {
                Image(
                    painter = painterResource(R.drawable.svg_icon_back),
                    contentDescription = context.getString(R.string.a11y_back),
                    modifier = Modifier.align(Alignment.CenterStart).clickable {
                        localMain.closeDetectResult()
                    }
                )
                Text(
                    if (localMain.isShowingLatestHistoryResult) context.getString(R.string.action_history) else context.getString(R.string.action_result),
                    color = White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.W500,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.align(Alignment.CenterHorizontally), verticalAlignment = Alignment.CenterVertically) {
                Image(painter = painterResource(R.drawable.svg_icon_sensor), modifier = Modifier.size(20.dp), contentDescription = null)
                Spacer(modifier = Modifier.width(4.dp))
                Text(context.getString(R.string.ux_scan_overview), color = White, fontSize = 14.sp, fontWeight = FontWeight.W400)
            }
            Spacer(modifier = Modifier.height(16.dp))
            Row(modifier = Modifier.fillMaxWidth().height(76.dp)) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .weight(1f)
                        .background(color = Color(0x33FE2D3F), shape = RoundedCornerShape(20.dp))
                ) {
                    Column(modifier = Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("${resultSuspiciousDevices.size}", color = Color(0xFFFE2D3F), fontSize = 32.sp, fontWeight = FontWeight.W700)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(context.getString(R.string.camera_clues), color = Color(0xFFFE2D3F), fontSize = 12.sp, fontWeight = FontWeight.W400)
                    }
                }
                Spacer(modifier = Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .weight(1f)
                        .background(color = White10, shape = RoundedCornerShape(20.dp))
                ) {
                    Column(modifier = Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("${resultTrustedDevices.size}", color = White, fontSize = 32.sp, fontWeight = FontWeight.W700)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(context.getString(R.string.other_devices), color = White60, fontSize = 12.sp, fontWeight = FontWeight.W400)
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(context.getString(R.string.ux_incomplete_count, allDevices.count { !it.analysisComplete }),
                color = White60, fontSize = 12.sp)
            LazyColumn(
                state = listState,
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                stickyHeader {
                    Column(Modifier.fillMaxWidth().background(Black).padding(vertical = 4.dp)) {
                        OutlinedTextField(value = search, onValueChange = { search = it },
                            label = { Text(context.getString(R.string.ux_search)) }, singleLine = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                            keyboardActions = KeyboardActions(onSearch = { keyboard?.hide() }),
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = White, unfocusedTextColor = White,
                                focusedLabelColor = White60, unfocusedLabelColor = White60, cursorColor = White))
                        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            (listOf<String?>(null) + identityGroups.keys.sorted()).forEach { type ->
                                val title = type?.let { ScanStrings.text(context, it) } ?: context.getString(R.string.all_detection_list)
                                val count = type?.let { identityGroups.getValue(it).size } ?: allDevices.size
                                Text("$title ($count)", color = if (activeType == type) White else White60, fontSize = 12.sp,
                                    modifier = Modifier.semantics { selected = activeType == type }
                                        .clickable { selectedType = type; keyboard?.hide() }.padding(horizontal = 8.dp, vertical = 10.dp))
                            }
                        }
                        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(R.string.ux_status_all, R.string.camera_clues, R.string.ux_status_incomplete).forEachIndexed { index, label ->
                                Text(context.getString(label), color = if (statusFilter == index) White else White60, fontSize = 12.sp,
                                    modifier = Modifier.semantics { selected = statusFilter == index }
                                        .clickable { statusFilter = index; keyboard?.hide() }.padding(horizontal = 8.dp, vertical = 10.dp))
                            }
                        }
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            val category = activeType?.let { ScanStrings.text(context, it) } ?: context.getString(R.string.ux_status_all)
                            Text(context.getString(R.string.ux_filter_count, category, visibleDevices.size, allDevices.size),
                                color = White60, fontSize = 12.sp, modifier = Modifier.weight(1f))
                            if (activeType != null || search.isNotBlank() || statusFilter != 0)
                                Text(context.getString(R.string.ux_clear_filter), color = White, fontSize = 12.sp,
                                    modifier = Modifier.clickable { selectedType = null; search = ""; statusFilter = 0; keyboard?.hide() }.padding(8.dp))
                        }
                    }
                }
                item {
                    HistoryReadWarning()
                    if (saveFailed) {
                        Text(context.getString(R.string.history_save_failed), color = White60, fontSize = 12.sp)
                        Text(context.getString(R.string.history_retry_save), color = Color(0xFF00C46F), fontSize = 12.sp,
                            modifier = Modifier.clickable { localMain.retryHistorySave() }.padding(vertical = 8.dp))
                    }
                    if (localMain.isShowingLatestHistoryResult) {
                        if (localMain.hasHistoryChoice) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                listOf(false to R.string.history_recent, true to if (localMain.archive.complete?.status == ScanStatus.COMPLETE) R.string.history_complete else R.string.history_previous).forEach { (complete, label) ->
                                    Text(context.getString(label), fontSize = 12.sp,
                                        color = if (localMain.showingCompleteHistory == complete) Color(0xFF00C46F) else White60,
                                        modifier = Modifier.weight(1f).background(White10, RoundedCornerShape(999.dp))
                                            .clickable { localMain.selectHistory(complete) }.padding(horizontal = 12.dp, vertical = 10.dp))
                                }
                            }
                            Spacer(Modifier.height(8.dp))
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
                            Text(context.getString(label), color = White60, fontSize = 12.sp)
                            if (record.startedAt > 0) Text(context.getString(R.string.history_meta,
                                java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.ROOT).format(java.util.Date(record.startedAt)),
                                record.network, record.coverage.checked, record.coverage.total, record.coverage.analyzed), color = White60, fontSize = 12.sp)
                        }
                    }
                    Text(context.getString(if (showScanDetails) R.string.ux_hide_scan_details else R.string.ux_show_scan_details), color = White, fontSize = 12.sp,
                        modifier = Modifier.clickable { showScanDetails = !showScanDetails }.padding(vertical = 10.dp))
                    if (showScanDetails) Text(ScanStrings.text(context, if (localMain.isShowingLatestHistoryResult) localMain.latestMessage else localMain.scanMessage), color = White60, fontSize = 12.sp)
                    Spacer(Modifier.height(6.dp))
                    Text(context.getString(R.string.result_explanation), color = White60, fontSize = 12.sp)
                    if (localMain.isShowingLatestHistoryResult) {
                        Text(context.getString(R.string.history_offline_note), color = White60, fontSize = 12.sp)
                    }
                }
                if (visibleDevices.isEmpty()) item {
                    Text(context.getString(R.string.ux_no_matches), color = White60, fontSize = 14.sp, modifier = Modifier.padding(16.dp))
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
                    .hazeChild(hazeState, style = HazeStyle(backgroundColor = Black, tint = null, blurRadius = 12.dp))
                    .clickable(enabled = false) { }
            ) {
                Column(modifier = Modifier.align(Alignment.Center).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    Row(
                        modifier = Modifier
                            .wrapContentWidth()
                            .background(color = Color(0x33FFFFFF), shape = RoundedCornerShape(20.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(context.getString(R.string.camera_clues_prefix), fontSize = 12.sp, fontWeight = FontWeight.W400, color = White)
                        Text("${resultSuspiciousDevices.size}", fontSize = 12.sp, fontWeight = FontWeight.W400, color = Orange)
                        Text(context.getString(R.string.review_devices_suffix), fontSize = 12.sp, fontWeight = FontWeight.W400, color = White)
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Box(
                        modifier = Modifier
                            .clickable {
                                openSubscribeWithResultRefresh()
                            }
                            .fillMaxWidth()
                            .height(56.dp)
                            .padding(horizontal = 24.dp)
                            .background(color = Color(0xFF00C46F), shape = RoundedCornerShape(999.dp))
                    ) {
                        Text(
                            text = if (checking) context.getString(R.string.action_checking) else context.getString(R.string.action_view_results),
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
