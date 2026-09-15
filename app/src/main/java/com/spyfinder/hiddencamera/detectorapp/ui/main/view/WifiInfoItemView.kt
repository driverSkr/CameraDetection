package com.spyfinder.hiddencamera.detectorapp.ui.main.view

import com.spyfinder.hiddencamera.detectorapp.utils.ScanStrings
import androidx.compose.ui.platform.LocalContext

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import com.spyfinder.hiddencamera.detectorapp.scan.Finding
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.spyfinder.hiddencamera.detectorapp.R
import com.spyfinder.hiddencamera.detectorapp.model.WifiDevice
import com.spyfinder.hiddencamera.detectorapp.theme.White
import com.spyfinder.hiddencamera.detectorapp.theme.White10
import com.spyfinder.hiddencamera.detectorapp.theme.White60

@Composable
fun WifiInfoItemView(info: WifiDevice, onClick: () -> Unit) {
    val context = LocalContext.current
    val identity = com.spyfinder.hiddencamera.detectorapp.scan.DeviceIdentity.forDevice(info)
    val deviceType = when(info.riskLevel) {
        1 -> R.drawable.svg_icon_wifi_info_router
        else -> R.drawable.svg_icon_wifi_info_router
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 88.dp)
            .background(color = White10, shape = RoundedCornerShape(20.dp))
            .padding(horizontal = 16.dp, vertical = 10.dp)
            .clickable{ onClick.invoke() },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(painter = painterResource(deviceType), contentDescription = null)
        Spacer(modifier = Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(com.spyfinder.hiddencamera.detectorapp.scan.DeviceIdentity.name(info.details) ?: ScanStrings.text(context, identity.type), color = White, fontSize = 16.sp, fontWeight = FontWeight.W500, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(modifier = Modifier.height(3.dp))
            Text(if (com.spyfinder.hiddencamera.detectorapp.scan.DeviceIdentity.name(info.details) != null)
                "${info.ip} · ${ScanStrings.text(context, identity.type)}" else info.ip,
                color = White60, fontSize = 12.sp, fontWeight = FontWeight.W400, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(modifier = Modifier.height(3.dp))
            Text(ScanStrings.text(context, identity.basis), color = White60, fontSize = 12.sp)
        }
        Spacer(modifier = Modifier.width(8.dp))
        Image(painter = painterResource(when {
            info.isCurrentPhone || info.userTrusted -> R.drawable.svg_icon_safety
            info.finding == Finding.CAMERA_FEATURES -> R.drawable.svg_icon_risk
            else -> R.drawable.svg_icon_warning_gray
        }), contentDescription = if (info.userTrusted) context.getString(R.string.user_trusted) else if (info.isCurrentPhone) context.getString(R.string.current_phone) else context.getString(R.string.view_device_details))
    }
}
