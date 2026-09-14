package com.spyfinder.hiddencamera.detectorapp.ui.main.view

import com.spyfinder.hiddencamera.detectorapp.utils.ScanStrings
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

@Composable
fun DetectResultView() {
    val hazeState = remember { HazeState() }
    val context = LocalContext.current
    val localMain = LocalMainContextEntity.current
    val resultSuspiciousDevices = localMain.resultSuspiciousDevices
    val resultTrustedDevices = localMain.resultTrustedDevices
    val allDevices = (resultSuspiciousDevices + resultTrustedDevices).distinctBy { it.ip }
    val scope = rememberCoroutineScope()
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
                    contentDescription = null,
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
            Spacer(modifier = Modifier.height(24.dp))
            Row(modifier = Modifier.align(Alignment.CenterHorizontally), verticalAlignment = Alignment.CenterVertically) {
                Image(painter = painterResource(R.drawable.svg_icon_sensor), modifier = Modifier.size(20.dp), contentDescription = null)
                Spacer(modifier = Modifier.width(4.dp))
                Text(context.getString(R.string.inspection_results), color = White, fontSize = 14.sp, fontWeight = FontWeight.W400)
            }
            Spacer(modifier = Modifier.height(16.dp))
            Row(modifier = Modifier.fillMaxWidth().height(92.dp)) {
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
                        .background(color = Color(0x3300C46F), shape = RoundedCornerShape(20.dp))
                ) {
                    Column(modifier = Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("${resultTrustedDevices.size}", color = Color(0xFF00C46F), fontSize = 32.sp, fontWeight = FontWeight.W700)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(context.getString(R.string.other_devices), color = Color(0xFF00C46F), fontSize = 12.sp, fontWeight = FontWeight.W400)
                    }
                }
            }
            Spacer(modifier = Modifier.height(21.dp))
            Row(
                modifier = Modifier.fillMaxWidth().height(24.dp).padding(start = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(context.getString(R.string.all_detection_list), color = White60, fontSize = 14.sp, fontWeight = FontWeight.W400)
                Spacer(modifier = Modifier.width(4.dp))
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .background(color = White10, shape = RoundedCornerShape(999.dp))
                        .padding(horizontal = 8.dp)
                ) {
                    Text("${allDevices.size}", color = White, fontSize = 12.sp, fontWeight = FontWeight.W400, modifier = Modifier.align(Alignment.Center))
                }
            }
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    Text(ScanStrings.text(context, if (localMain.isShowingLatestHistoryResult) localMain.latestMessage else localMain.scanMessage), color = White60, fontSize = 12.sp)
                    Spacer(Modifier.height(6.dp))
                    Text(context.getString(R.string.result_explanation), color = White60, fontSize = 12.sp)
                    if (localMain.isShowingLatestHistoryResult) {
                        Text(context.getString(R.string.history_offline_note), color = White60, fontSize = 12.sp)
                    }
                }
                items(allDevices.size, key = { allDevices[it].ip }) { index ->
                    WifiInfoItemView(allDevices[index]) {
                        if (!isSubscribed) return@WifiInfoItemView
                        DialogHelper.showWifiInfoDialog(context as? FragmentActivity ?: return@WifiInfoItemView, allDevices[index]) { device ->
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

                    Spacer(modifier = Modifier.height(24.dp))

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
