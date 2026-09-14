package com.spyfinder.hiddencamera.detectorapp.dialog.view

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import com.spyfinder.hiddencamera.detectorapp.scan.Finding
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.spyfinder.hiddencamera.detectorapp.model.WifiDevice
import com.spyfinder.hiddencamera.detectorapp.theme.Transparent
import com.spyfinder.hiddencamera.detectorapp.theme.White
import com.spyfinder.hiddencamera.detectorapp.theme.White10
import com.spyfinder.hiddencamera.detectorapp.theme.White60
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.spyfinder.hiddencamera.detectorapp.R

@Composable
fun WifiInfoDetailsView(dialog: BottomSheetDialog, device: WifiDevice, onMarkSafe: (WifiDevice) -> Unit) {
    Column(modifier = Modifier
        .fillMaxWidth()
        .background(color = Color(0xFF161618), shape = RoundedCornerShape(48.dp))
        .heightIn(max = 620.dp)
        .verticalScroll(rememberScrollState())
        .navigationBarsPadding()
        .padding(top = 8.dp, start = 16.dp, end = 16.dp, bottom = 16.dp)
    ) {
        Box(modifier = Modifier
            .width(48.dp)
            .height(6.dp)
            .background(color = Color(0xFF5B5B5E), shape = RoundedCornerShape(4.dp))
            .align(Alignment.CenterHorizontally)
        )
        Spacer(modifier = Modifier.height(10.dp))
        Row(modifier = Modifier.fillMaxWidth().height(64.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(64.dp).background(color = White10, shape = RoundedCornerShape(19.dp))) {
                Image(painter = painterResource(R.drawable.svg_icon_wifi_info_router), modifier = Modifier.size(36.dp).align(Alignment.Center), contentDescription = null)
            }
            Spacer(modifier = Modifier.width(12.dp))
            Text(device.name, modifier = Modifier.weight(1f), color = White, fontSize = 18.sp, fontWeight = FontWeight.W600, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Spacer(modifier = Modifier.width(8.dp))
            Image(painter = painterResource(R.drawable.svg_icon_close_30), contentDescription = null, modifier = Modifier.clickable{ dialog.dismiss() })
        }
        Spacer(modifier = Modifier.height(20.dp))
        Box(modifier = Modifier.fillMaxWidth().height(42.dp)) {
            Text("IP Address", color = White60, fontSize = 14.sp, fontWeight = FontWeight.W400, modifier = Modifier.align(Alignment.CenterStart))
            Text(device.ip, color = White, fontSize = 14.sp, fontWeight = FontWeight.W400, modifier = Modifier.align(Alignment.CenterEnd))
        }
        Box(modifier = Modifier.fillMaxWidth().height(42.dp)) {
            Text("MAC Address", color = White60, fontSize = 14.sp, fontWeight = FontWeight.W400, modifier = Modifier.align(Alignment.CenterStart))
            Text(device.mac.ifBlank { "Unavailable" }, color = White, fontSize = 14.sp, fontWeight = FontWeight.W400, modifier = Modifier.align(Alignment.CenterEnd))
        }
        Box(modifier = Modifier.fillMaxWidth().height(42.dp)) {
            Text("Device Model", color = White60, fontSize = 14.sp, fontWeight = FontWeight.W400, modifier = Modifier.align(Alignment.CenterStart))
            Text("Unknown", color = White, fontSize = 14.sp, fontWeight = FontWeight.W400, modifier = Modifier.align(Alignment.CenterEnd))
        }
        Spacer(Modifier.height(12.dp))
        Text("Detection finding", color = White60, fontSize = 14.sp)
        Spacer(Modifier.height(6.dp))
        Text(if (device.isCurrentPhone) "Current phone" else when (device.finding) {
            Finding.CAMERA_FEATURES -> "Camera-related features — verify manually"
            Finding.NO_CAMERA_FEATURES -> "No camera features in checked services"
            Finding.INSUFFICIENT -> "Insufficient information"
            Finding.LEGACY -> "Legacy result — scan again"
        }, color = White, fontSize = 14.sp)
        if (!device.analysisComplete) Text("Analysis incomplete", color = White60, fontSize = 12.sp)
        device.evidence.forEach {
            Spacer(Modifier.height(6.dp))
            Text(it, color = White60, fontSize = 12.sp)
        }
        if (device.userTrusted) {
            Spacer(Modifier.height(8.dp))
            Text("User trusted · detection evidence is unchanged", color = White60, fontSize = 12.sp)
        }
        if (!device.isCurrentPhone && device.finding != Finding.LEGACY) {
            Spacer(modifier = Modifier.height(20.dp))
            Box(modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .background(color = Color(0xFF00C46F), shape = RoundedCornerShape(999.dp))
                .border(width = 1.dp, shape = RoundedCornerShape(999.dp), brush = Brush.horizontalGradient(colorStops = arrayOf(0f to White10, 0.5f to Transparent, 1f to White10)))
                .clickable{
                    onMarkSafe(device)  // 传递整个设备对象
                    dialog.dismiss()     // 关闭对话框
                }
            ) {
                Row(modifier = Modifier.align(Alignment.Center)) {
                    Image(painter = painterResource(R.drawable.svg_icon_correct_white), contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(if (device.userTrusted) "Remove trust" else "Mark as user trusted", color = White, fontSize = 16.sp, fontWeight = FontWeight.W500)
                }
            }
        }
    }
}
