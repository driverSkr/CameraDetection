package com.spyfinder.hiddencamera.detectorapp.ui.main.view
import com.spyfinder.hiddencamera.detectorapp.theme.AppSpacing
import com.spyfinder.hiddencamera.detectorapp.theme.AppColors

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
import androidx.compose.foundation.layout.size
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
import com.spyfinder.hiddencamera.detectorapp.theme.AppColors.textPrimary
import com.spyfinder.hiddencamera.detectorapp.theme.AppColors.outline
import com.spyfinder.hiddencamera.detectorapp.theme.AppColors.textSecondary

@Composable
fun WifiInfoItemView(info: WifiDevice, onClick: () -> Unit) {
    val context = LocalContext.current
    val identity = com.spyfinder.hiddencamera.detectorapp.scan.DeviceIdentity.forDevice(info)
    val deviceType = com.spyfinder.hiddencamera.detectorapp.utils.DevicePresentation.icon(identity.type)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 88.dp)
            .background(color = AppColors.outline, shape = RoundedCornerShape(20.dp))
            .padding(horizontal = AppSpacing.screen, vertical = 10.dp)
            .clickable{ onClick.invoke() },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(painter = painterResource(deviceType), contentDescription = null, modifier = Modifier.size(32.dp))
        Spacer(modifier = Modifier.width(AppSpacing.screen))
        Column(Modifier.weight(1f)) {
            Text(com.spyfinder.hiddencamera.detectorapp.scan.DeviceIdentity.name(info.details) ?: ScanStrings.text(context, identity.type), color = AppColors.textPrimary, fontSize = 16.sp, fontWeight = FontWeight.W500, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(modifier = Modifier.height(3.dp))
            Text(if (com.spyfinder.hiddencamera.detectorapp.scan.DeviceIdentity.name(info.details) != null)
                "${info.ip} · ${ScanStrings.text(context, identity.type)}" else info.ip,
                color = AppColors.textSecondary, fontSize = 12.sp, fontWeight = FontWeight.W400, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(modifier = Modifier.height(3.dp))
            Text(ScanStrings.text(context, identity.basis), color = AppColors.textSecondary, fontSize = 12.sp)
            if (identity.capabilities.isNotEmpty()) Text(context.getString(R.string.ux_capabilities) + ": " + identity.capabilities.joinToString(" · ") { ScanStrings.text(context, it) }, color = AppColors.textSecondary, fontSize = 12.sp)
            if (info.userTrusted) Text(context.getString(R.string.ux_my_mark), color = AppColors.textSecondary, fontSize = 12.sp)
            if (!info.analysisComplete) Text(context.getString(R.string.analysis_incomplete), color = AppColors.textSecondary, fontSize = 12.sp)
        }
        Spacer(modifier = Modifier.width(AppSpacing.compact))
        Image(painter = painterResource(when {
            !info.analysisComplete -> R.drawable.svg_icon_warning_gray
            else -> R.drawable.svg_icon_next
        }), contentDescription = if (info.userTrusted) context.getString(R.string.user_trusted) else if (info.isCurrentPhone) context.getString(R.string.current_phone) else context.getString(R.string.view_device_details))
    }
}