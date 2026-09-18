package com.spyfinder.hiddencamera.detectorapp.ui.main.page
import com.spyfinder.hiddencamera.detectorapp.theme.AppColors
import com.spyfinder.hiddencamera.detectorapp.theme.AppSpacing

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.spyfinder.hiddencamera.detectorapp.R
import com.spyfinder.hiddencamera.detectorapp.event.Event
import com.spyfinder.hiddencamera.detectorapp.ui.camera.CameraScannerActivity
import com.spyfinder.hiddencamera.detectorapp.ui.main.view.ScannerItemView
import com.spyfinder.hiddencamera.detectorapp.ui.subscribe.SubscribeActivity
import com.spyfinder.hiddencamera.detectorapp.utils.SubscribeHelper
import com.spyfinder.hiddencamera.detectorapp.utils.SubscriptionGate
import kotlinx.coroutines.launch

/**
 * 扫描仪页
 */
@Composable
fun ScannerPage() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val isSubscribed = SubscriptionGate.hasAccessFlow.collectAsState().value
    var checking by remember { mutableStateOf(false) }
    var cameraOpen by rememberSaveable { mutableStateOf(false) }
    var pendingScene by rememberSaveable { mutableStateOf("") }
    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        cameraOpen = false
        pendingScene = ""
    }
    fun openCamera(source: String, scene: String = pendingScene) {
        cameraOpen = true
        pendingScene = scene
        try {
            cameraLauncher.launch(CameraScannerActivity.intent(context, scene))
            Event.event(context, Event.CAMERA_SCANNER_OPEN, Event.PARAM_SOURCE to source)
        } catch (_: Exception) {
            cameraOpen = false
            android.widget.Toast.makeText(context, context.getString(R.string.camera_unavailable), android.widget.Toast.LENGTH_LONG).show()
        }
    }
    val shouldLaunchScannerAfterSubscribe = androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf(false) }
    val subscribeLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        if (!shouldLaunchScannerAfterSubscribe.value) {
            return@rememberLauncherForActivityResult
        }
        shouldLaunchScannerAfterSubscribe.value = false
        checking = true
        scope.launch {
            try { if (SubscriptionGate.hasAccess()) openCamera("after_subscribe", pendingScene) }
            finally { checking = false }
        }
    }
    val scannerItemList = listOf(
        Pair(R.drawable.svg_icon_tv, context.getString(R.string.object_tv)),
        Pair(R.drawable.svg_icon_socket, context.getString(R.string.object_socket)),
        Pair(R.drawable.svg_icon_lampshade, context.getString(R.string.object_lampshade)),
        Pair(R.drawable.svg_icon_beside_table, context.getString(R.string.object_bedside_table)),
        Pair(R.drawable.svg_icon_tv_cabinet, context.getString(R.string.object_tv_cabinet)),
        Pair(R.drawable.svg_icon_wardrobe, context.getString(R.string.object_wardrobe)),
        Pair(R.drawable.svg_icon_sofa, context.getString(R.string.object_sofa)),
        Pair(R.drawable.svg_icon_smoke_sensor, context.getString(R.string.object_smoke_sensor)),
        Pair(R.drawable.svg_icon_shower_head, context.getString(R.string.object_shower_head)),
        Pair(R.drawable.svg_icon_vase, context.getString(R.string.object_vase)),
        Pair(R.drawable.svg_icon_air_conditioner, context.getString(R.string.object_air_conditioner)),
        Pair(R.drawable.svg_icon_router, context.getString(R.string.object_router))
    )

    fun openScannerWithSubscriptionCheck(scannerItem: String) {
        if (checking || cameraOpen || shouldLaunchScannerAfterSubscribe.value) return
        checking = true
        // 红外扫描功能点击埋点，item 表示用户选择的检测位置。
        Event.event(context, Event.CAMERA_SCANNER_CLICK, Event.PARAM_ITEM to scannerItem)
        scope.launch {
          try {
            val subscribed = if (isSubscribed) {
                true
            } else {
                SubscriptionGate.hasAccess()
            }

            if (subscribed) {
                shouldLaunchScannerAfterSubscribe.value = false
                openCamera("scanner_grid", scannerItem)
            } else {
                if (!SubscribeHelper.canOfferPurchase) {
                    android.widget.Toast.makeText(context, context.getString(R.string.access_retry), android.widget.Toast.LENGTH_LONG).show()
                    return@launch
                }
                pendingScene = scannerItem
                shouldLaunchScannerAfterSubscribe.value = true
                Event.event(
                    context,
                    Event.SUBSCRIBE_GATE_SHOW,
                    Event.PARAM_SOURCE to "camera_scanner",
                    Event.PARAM_ITEM to scannerItem
                )
                subscribeLauncher.launch(Intent(context, SubscribeActivity::class.java))
            }
          } catch (e: kotlinx.coroutines.CancellationException) { throw e }
            catch (_: Exception) {
                shouldLaunchScannerAfterSubscribe.value = false
                android.widget.Toast.makeText(context, context.getString(R.string.access_retry), android.widget.Toast.LENGTH_LONG).show()
            } finally { checking = false }
        }
    }

    Column(modifier = Modifier.fillMaxSize().statusBarsPadding().padding(top = AppSpacing.pageTop)) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = AppSpacing.screen)) {
            Text(context.getString(R.string.tab_scanner), color = AppColors.textPrimary, fontSize = 28.sp, fontWeight = FontWeight.W700)
            Spacer(modifier = Modifier.height(AppSpacing.compact))
            Text(context.getString(R.string.scanner_description), color = AppColors.textPrimary.copy(0.6f), fontSize = 14.sp, fontWeight = FontWeight.W400)
        }

        Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
          LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            horizontalArrangement = Arrangement.spacedBy(AppSpacing.section),
            verticalArrangement = Arrangement.spacedBy(20.dp),
            contentPadding = PaddingValues(horizontal = AppSpacing.screen, vertical = 20.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(scannerItemList.size) { index ->
                ScannerItemView(scannerItemList[index], enabled = !checking && !cameraOpen && !shouldLaunchScannerAfterSubscribe.value) {
                    openScannerWithSubscriptionCheck(scannerItemList[index].second)
                }
            }
        }
        }
    }
}