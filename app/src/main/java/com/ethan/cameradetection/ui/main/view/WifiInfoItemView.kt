package com.ethan.cameradetection.ui.main.view

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ethan.cameradetection.R
import com.ethan.cameradetection.model.WifiDevice
import com.ethan.cameradetection.theme.White
import com.ethan.cameradetection.theme.White10
import com.ethan.cameradetection.theme.White60

@Composable
fun WifiInfoItemView(info: WifiDevice, onClick: () -> Unit) {
    val deviceType = when(info.riskLevel) {
        1 -> R.drawable.svg_icon_wifi_info_router
        else -> R.drawable.svg_icon_wifi_info_router
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(71.dp)
            .background(color = White10, shape = RoundedCornerShape(20.dp))
            .padding(horizontal = 16.dp)
            .clickable{ onClick.invoke() },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(painter = painterResource(deviceType), contentDescription = null)
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(info.name, color = White, fontSize = 16.sp, fontWeight = FontWeight.W500)
            Spacer(modifier = Modifier.height(3.dp))
            Text(info.ip, color = White60, fontSize = 12.sp, fontWeight = FontWeight.W400)
        }
        Spacer(modifier = Modifier.weight(1f))
        Image(painter = painterResource(if (info.riskLevel == 0) R.drawable.svg_icon_safety else R.drawable.svg_icon_risk), contentDescription = null)
    }
}