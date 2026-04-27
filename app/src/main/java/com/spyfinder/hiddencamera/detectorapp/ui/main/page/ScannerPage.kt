package com.spyfinder.hiddencamera.detectorapp.ui.main.page

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
import kotlinx.coroutines.launch

/**
 * 扫描仪页
 */
@Composable
fun ScannerPage() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val isSubscribed = SubscribeHelper.isSubscribedFlow.collectAsState().value
    val shouldLaunchScannerAfterSubscribe = remember { mutableStateOf(false) }
    val subscribeLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        if (!shouldLaunchScannerAfterSubscribe.value) {
            return@rememberLauncherForActivityResult
        }
        scope.launch {
            val subscribed = SubscribeHelper.isSubscribe()
            if (subscribed) {
                // 订阅完成后继续打开红外扫描，记录付费门槛后的转化路径。
                Event.event(context, Event.CAMERA_SCANNER_OPEN, Event.PARAM_SOURCE to "after_subscribe")
                CameraScannerActivity.launch(context)
            }
            shouldLaunchScannerAfterSubscribe.value = false
        }
    }
    val scannerItemList = listOf(
        Pair(R.drawable.svg_icon_tv, "TV"),
        Pair(R.drawable.svg_icon_socket, "Socket"),
        Pair(R.drawable.svg_icon_lampshade, "Lampshade"),
        Pair(R.drawable.svg_icon_beside_table, "Beside Table"),
        Pair(R.drawable.svg_icon_tv_cabinet, "TV Cabinet"),
        Pair(R.drawable.svg_icon_wardrobe, "Wardrobe"),
        Pair(R.drawable.svg_icon_sofa, "Sofa"),
        Pair(R.drawable.svg_icon_smoke_sensor, "Smoke Sensor"),
        Pair(R.drawable.svg_icon_shower_head, "Shower Head"),
        Pair(R.drawable.svg_icon_vase, "vase"),
        Pair(R.drawable.svg_icon_air_conditioner, "Air Conditioner"),
        Pair(R.drawable.svg_icon_router, "Router")
    )

    fun openScannerWithSubscriptionCheck(scannerItem: String) {
        // 红外扫描功能点击埋点，item 表示用户选择的检测位置。
        Event.event(context, Event.CAMERA_SCANNER_CLICK, Event.PARAM_ITEM to scannerItem)
        scope.launch {
            val subscribed = if (isSubscribed) {
                true
            } else {
                SubscribeHelper.isSubscribe()
            }

            if (subscribed) {
                shouldLaunchScannerAfterSubscribe.value = false
                Event.event(context, Event.CAMERA_SCANNER_OPEN, Event.PARAM_SOURCE to "scanner_grid")
                CameraScannerActivity.launch(context)
            } else {
                shouldLaunchScannerAfterSubscribe.value = true
                Event.event(
                    context,
                    Event.SUBSCRIBE_GATE_SHOW,
                    Event.PARAM_SOURCE to "camera_scanner",
                    Event.PARAM_ITEM to scannerItem
                )
                subscribeLauncher.launch(Intent(context, SubscribeActivity::class.java))
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize().statusBarsPadding().padding(top = 18.dp)) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
            Text("Scanner", color = Color(0xFFFFFFFF), fontSize = 28.sp, fontWeight = FontWeight.W700)
            Spacer(modifier = Modifier.height(8.dp))
            Text("Click on the following entry to enter the corresponding location for testing.", color = Color(0xFFFFFFFF).copy(0.6f), fontSize = 14.sp, fontWeight = FontWeight.W400)
        }

        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
            contentPadding = PaddingValues(horizontal = 16.dp),
            modifier = Modifier.fillMaxWidth().align(Alignment.Center)
        ) {
            items(scannerItemList.size) { index ->
                ScannerItemView(scannerItemList[index]) {
                    openScannerWithSubscriptionCheck(scannerItemList[index].second)
                }
            }
        }
    }
}
