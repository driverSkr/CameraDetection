package com.ethan.cameradetection.ui.main.view

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import com.ethan.cameradetection.R
import com.ethan.cameradetection.dialog.DialogHelper
import com.ethan.cameradetection.theme.White
import com.ethan.cameradetection.theme.White10
import com.ethan.cameradetection.theme.White60
import com.ethan.cameradetection.ui.main.context.LocalMainContextEntity

@Composable
fun DetectResultView() {
    val context = LocalContext.current
    val localMain = LocalMainContextEntity.current

    BackHandler { }
    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp)) {
        Box(modifier = Modifier.fillMaxWidth().height(54.dp)) {
            Image(painter = painterResource(R.drawable.svg_icon_back), contentDescription = null, modifier = Modifier.align(Alignment.CenterStart).clickable{
                localMain.isShowResult.value = false
                localMain.isStartDetect.value = false
                localMain.trustedDevices.clear()
                localMain.suspiciousDevices.clear()
            })
            Text("Result", color = White, fontSize = 18.sp, fontWeight = FontWeight.W500, modifier = Modifier.align(Alignment.Center))
        }
        Spacer(modifier = Modifier.height(24.dp))
        Row(modifier = Modifier.align(Alignment.CenterHorizontally), verticalAlignment = Alignment.CenterVertically) {
            Image(painter = painterResource(R.drawable.svg_icon_sensor), modifier = Modifier.size(20.dp), contentDescription = null)
            Spacer(modifier = Modifier.width(4.dp))
            Text("Risky devices Found", color = White, fontSize = 14.sp, fontWeight = FontWeight.W400)
        }
        Spacer(modifier = Modifier.height(16.dp))
        Row(modifier = Modifier.fillMaxWidth().height(92.dp)) {
            Box(modifier = Modifier
                .fillMaxHeight()
                .weight(1f)
                .background(color = Color(0x33FE2D3F), shape = RoundedCornerShape(20.dp))
            ) {
                Column(modifier = Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("${localMain.suspiciousDevices.size}", color = Color(0xFFFE2D3F), fontSize = 32.sp, fontWeight = FontWeight.W700)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Suspicious", color = Color(0xFFFE2D3F), fontSize = 12.sp, fontWeight = FontWeight.W400)
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
            Box(modifier = Modifier
                .fillMaxHeight()
                .weight(1f)
                .background(color = Color(0x3300C46F), shape = RoundedCornerShape(20.dp))
            ) {
                Column(modifier = Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("${localMain.trustedDevices.size}", color = Color(0xFF00C46F), fontSize = 32.sp, fontWeight = FontWeight.W700)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Safe", color = Color(0xFF00C46F), fontSize = 12.sp, fontWeight = FontWeight.W400)
                }
            }
        }
        Spacer(modifier = Modifier.height(21.dp))
        Row(modifier = Modifier.fillMaxWidth().height(24.dp).padding(start = 2.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("All Detection List", color = White60, fontSize = 14.sp, fontWeight = FontWeight.W400)
            Spacer(modifier = Modifier.width(4.dp))
            Box(modifier = Modifier
                .fillMaxHeight()
                .background(color = White10, shape = RoundedCornerShape(999.dp))
                .padding(horizontal = 8.dp)
            ) {
                Text("${localMain.suspiciousDevices.size + localMain.trustedDevices.size}", color = White, fontSize = 12.sp, fontWeight = FontWeight.W400, modifier = Modifier.align(Alignment.Center))
            }
        }
        LazyColumn(
            contentPadding = PaddingValues(vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
        ) {
            val allDevices = localMain.suspiciousDevices + localMain.trustedDevices
            items(allDevices.size) { index ->
                WifiInfoItemView(allDevices[index]) {
                    DialogHelper.showWifiInfoDialog(context as FragmentActivity, allDevices[index])
                }
            }
        }
    }
}