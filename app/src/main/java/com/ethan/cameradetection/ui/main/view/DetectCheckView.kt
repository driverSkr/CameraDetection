package com.ethan.cameradetection.ui.main.view

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ethan.cameradetection.R
import com.ethan.cameradetection.utils.WifiHelper
import kotlinx.coroutines.delay

@Composable
fun DetectCheckView() {
    val context = LocalContext.current
    val wifiSsid = remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        while (true) {
            wifiSsid.value = WifiHelper.getWifiSSID(context)
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
            Image(
                painter = painterResource(R.mipmap.img_radar_bg),
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
                contentDescription = null
            )
            Image(painterResource(R.mipmap.img_radar_detect), contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
            Text(
                text = "Start",
                color = Color(0xFFFFFFFF),
                fontSize = 44.sp,
                fontWeight = FontWeight.W700,
                modifier = Modifier.align(Alignment.Center)
            )
        }

        Box(modifier = Modifier
            .padding(bottom = 24.dp)
            .fillMaxWidth()
            .height(56.dp)
            .padding(horizontal = 24.dp)
            .background(color = Color(0xFF00C46F), shape = RoundedCornerShape(999.dp))
            .align(Alignment.BottomCenter)
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