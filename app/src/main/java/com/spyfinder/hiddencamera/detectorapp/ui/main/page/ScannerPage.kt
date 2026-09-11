package com.spyfinder.hiddencamera.detectorapp.ui.main.page

import com.spyfinder.hiddencamera.detectorapp.ui.components.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*

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
        Pair(R.drawable.svg_icon_beside_table, "Bedside table"),
        Pair(R.drawable.svg_icon_tv_cabinet, "TV Cabinet"),
        Pair(R.drawable.svg_icon_wardrobe, "Wardrobe"),
        Pair(R.drawable.svg_icon_sofa, "Sofa"),
        Pair(R.drawable.svg_icon_smoke_sensor, "Smoke Sensor"),
        Pair(R.drawable.svg_icon_shower_head, "Shower Head"),
        Pair(R.drawable.svg_icon_vase, "Vase"),
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

    QuietPage {
        QuietHeading("Camera inspection", "Where would you\nlike to check?", "Choose a location, then inspect it with your camera.")
        scannerItemList.chunked(3).forEach { items ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                items.forEach { (icon, title) ->
                    OutlinedCard(onClick = { openScannerWithSubscriptionCheck(title) }, modifier = Modifier.weight(1f)) {
                        Column(Modifier.fillMaxWidth().heightIn(min = 108.dp).padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            QuietIcon(icon)
                            Text(title, fontSize = 12.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                        }
                    }
                }
            }
        }
        QuietNote("All locations use the same camera inspection tool.")
    }
}
