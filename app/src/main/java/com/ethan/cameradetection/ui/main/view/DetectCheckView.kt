package com.ethan.cameradetection.ui.main.view

import android.content.Context
import android.net.wifi.WifiManager
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableIntState
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshots.SnapshotStateList
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
import com.ethan.cameradetection.R
import com.ethan.cameradetection.model.WifiDevice
import com.ethan.cameradetection.theme.Transparent
import com.ethan.cameradetection.theme.White
import com.ethan.cameradetection.theme.White10
import com.ethan.cameradetection.theme.White60
import com.ethan.cameradetection.ui.main.context.LocalMainContextEntity
import com.ethan.cameradetection.utils.WifiHelper
import com.stealthcopter.networktools.SubnetDevices
import com.stealthcopter.networktools.subnet.Device
import kotlinx.coroutines.delay

@Composable
fun DetectCheckView() {
    val context = LocalContext.current
    val localMain = LocalMainContextEntity.current
    val wifiSsid = remember { mutableStateOf<String?>(null) }
    val detectProgress = remember { mutableIntStateOf(0) }
    val localIp = remember {
        val wifiManager = context.getSystemService(Context.WIFI_SERVICE) as WifiManager
        val wifiInfo = wifiManager.connectionInfo
        String.format(
            "%d.%d.%d.%d",
            wifiInfo.ipAddress and 0xff,
            wifiInfo.ipAddress shr 8 and 0xff,
            wifiInfo.ipAddress shr 16 and 0xff,
            wifiInfo.ipAddress shr 24 and 0xff
        )
    }

    LaunchedEffect(Unit) {
        while (true) {
            wifiSsid.value = WifiHelper.showWifiInfo(context).ssid
            delay(5000) // 每5秒更新一次
        }
    }

    Box(modifier = Modifier.fillMaxSize().padding(top = 18.dp)) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
            Text("Wifi Scan", color = Color(0xFFFFFFFF), fontSize = 28.sp, fontWeight = FontWeight.W700)
            Spacer(modifier = Modifier.height(8.dp))
            Text("Connected WI-FI: ${if (wifiSsid.value == null) "未连接" else wifiSsid.value }", color = Color(0xFFFFFFFF).copy(0.6f), fontSize = 14.sp, fontWeight = FontWeight.W400)
        }

        Box(modifier = Modifier.size(313.dp).align(Alignment.Center)) {
            RadarScannerWithControls()
            if (localMain.isStartDetect.value) {
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
                            append("${detectProgress.intValue}")
                        }
                        withStyle(
                            style = SpanStyle(
                                fontSize = 24.sp,
                                color = White,
                                fontWeight = FontWeight.W700,
                                baselineShift = BaselineShift(0f) // 调整符号的垂直位置
                            )
                        ) {
                            append("%")
                        }
                    },
                    modifier = Modifier.align(Alignment.Center)
                )
            } else {
                Text(
                    text = "Start",
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
                    Text("Suspicious devices: ", color = White60, fontSize = 16.sp, fontWeight = FontWeight.W500)
                    Text("${localMain.suspiciousDevices.size}", color = Color(0xFFFE2D3F), fontSize = 16.sp, fontWeight = FontWeight.W500)
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
                            localMain.isStartDetect.value = false
                            localMain.isAnimating.value = false
                        }
                    ) {
                        Text(
                            text = "Cancel",
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
                                localMain.isStartDetect.value = true
                                localMain.isAnimating.value = true
                                detectProgress.intValue = 0
                                localMain.suspiciousDevices.clear()
                                localMain.trustedDevices.clear()
                                wifiDetect(localIp, localMain.suspiciousDevices, localMain.trustedDevices, localMain.isAnimating, detectProgress)
                            }
                        ) {
                            Text(
                                text = "Recheck",
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
                                localMain.isShowResult.value = true
                            }
                        ) {
                            Text(
                                text = "Result",
                                color = Color(0xFFFFFFFF),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.W500,
                                modifier = Modifier.align(Alignment.Center)
                            )
                        }
                    }
                }
            } else {
                Box(modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .padding(horizontal = 24.dp)
                    .background(color = Color(0xFF00C46F), shape = RoundedCornerShape(999.dp))
                    .clickable{
                        localMain.isStartDetect.value = true
                        localMain.isAnimating.value = true

                        wifiDetect(localIp, localMain.suspiciousDevices, localMain.trustedDevices, localMain.isAnimating, detectProgress)
                    }
                ) {
                    Text(
                        text = "Start",
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

fun wifiDetect(
    localIp: String,
    suspiciousDevices: SnapshotStateList<WifiDevice>,
    trustedDevices: SnapshotStateList<WifiDevice>,
    isAnimating: MutableState<Boolean>,
    detectProgress: MutableIntState
) {
    SubnetDevices.fromLocalAddress().findDevices(object : SubnetDevices.OnSubnetDeviceFound {
        override fun onDeviceFound(device: Device?) {
            if (detectProgress.intValue < 100) {
                detectProgress.intValue += 1
            }
        }

        override fun onFinished(devicesFound: ArrayList<Device?>?) {
            if (devicesFound == null) {
                return
            }
            // 并发检测每个IP的类型
            val threads = mutableListOf<Thread>()
            for (dev in devicesFound) {
                val t = Thread {
                    val wifiDevice = WifiHelper.detectDeviceType(dev!!, localIp)
                    if (wifiDevice.riskLevel > 0) {
                        suspiciousDevices.add(wifiDevice)
                    } else {
                        trustedDevices.add(wifiDevice)
                    }
                }
                threads.add(t)
                t.start()
                if (threads.size >= 10) {
                    threads.removeAll { !it.isAlive }
                }
            }
            threads.forEach { it.join(3000) }

            isAnimating.value = false
            detectProgress.intValue = 100
        }

    })
}