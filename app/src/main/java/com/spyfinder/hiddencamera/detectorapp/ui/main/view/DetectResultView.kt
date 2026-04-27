package com.spyfinder.hiddencamera.detectorapp.ui.main.view

import android.content.Intent
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
    val hazeState = HazeState()
    val context = LocalContext.current
    val localMain = LocalMainContextEntity.current
    val resultSuspiciousDevices = localMain.resultSuspiciousDevices
    val resultTrustedDevices = localMain.resultTrustedDevices
    val allDevices = resultSuspiciousDevices + resultTrustedDevices
    val scope = rememberCoroutineScope()
    val isSubscribed = SubscribeHelper.isSubscribedFlow.collectAsState().value
    val shouldRefreshSubscribeStateAfterSubscribe = remember { mutableStateOf(false) }
    val subscribeLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        if (!shouldRefreshSubscribeStateAfterSubscribe.value) {
            return@rememberLauncherForActivityResult
        }
        scope.launch {
            SubscribeHelper.isSubscribe()
            shouldRefreshSubscribeStateAfterSubscribe.value = false
        }
    }

    fun openSubscribeWithResultRefresh() {
        scope.launch {
            val subscribed = if (isSubscribed) {
                true
            } else {
                SubscribeHelper.isSubscribe()
            }

            if (subscribed) {
                shouldRefreshSubscribeStateAfterSubscribe.value = false
                return@launch
            }

            shouldRefreshSubscribeStateAfterSubscribe.value = true
            subscribeLauncher.launch(Intent(context, SubscribeActivity::class.java))
        }
    }

    BackHandler {
        localMain.closeDetectResult()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
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
                    "Result",
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
                Text("Risky devices Found", color = White, fontSize = 14.sp, fontWeight = FontWeight.W400)
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
                        Text("Suspicious", color = Color(0xFFFE2D3F), fontSize = 12.sp, fontWeight = FontWeight.W400)
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
                        Text("Safe", color = Color(0xFF00C46F), fontSize = 12.sp, fontWeight = FontWeight.W400)
                    }
                }
            }
            Spacer(modifier = Modifier.height(21.dp))
            Row(
                modifier = Modifier.fillMaxWidth().height(24.dp).padding(start = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("All Detection List", color = White60, fontSize = 14.sp, fontWeight = FontWeight.W400)
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
                contentPadding = PaddingValues(vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(allDevices.size) { index ->
                    WifiInfoItemView(allDevices[index]) {
                        DialogHelper.showWifiInfoDialog(context as FragmentActivity, allDevices[index]) { device ->
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
                        Text("Warning:", fontSize = 12.sp, fontWeight = FontWeight.W400, color = White)
                        Text("${resultSuspiciousDevices.size}", fontSize = 12.sp, fontWeight = FontWeight.W400, color = Orange)
                        Text("suspicious devices found", fontSize = 12.sp, fontWeight = FontWeight.W400, color = White)
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
                            text = "View Results",
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
