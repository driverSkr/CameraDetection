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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.spyfinder.hiddencamera.detectorapp.R
import com.spyfinder.hiddencamera.detectorapp.model.WifiDevice
import com.spyfinder.hiddencamera.detectorapp.theme.AppColors.textPrimary
import com.spyfinder.hiddencamera.detectorapp.theme.AppColors.outline
import com.spyfinder.hiddencamera.detectorapp.theme.AppColors.textSecondary
import com.spyfinder.hiddencamera.detectorapp.utils.DevicePresentation

@Composable
fun WifiInfoItemView(info: WifiDevice, onClick: () -> Unit) {
    val context = LocalContext.current
    val identity = com.spyfinder.hiddencamera.detectorapp.scan.DeviceIdentity.forDevice(info)
    val needsLook = DevicePresentation.needsLook(info)
    val typeLabel = if (identity.type == "Device type unconfirmed") {
        context.getString(R.string.identity_unknown_short)
    } else {
        ScanStrings.text(context, identity.type)
    }
    val name = com.spyfinder.hiddencamera.detectorapp.scan.DeviceIdentity.name(info.details)
    val status = when {
        info.isCurrentPhone -> context.getString(R.string.current_phone)
        needsLook -> context.getString(R.string.result_needs_look)
        !info.analysisComplete -> context.getString(R.string.analysis_incomplete)
        info.userTrusted -> context.getString(R.string.result_marked_known)
        else -> null
    }
    val caption = when {
        name != null && status != null -> "$typeLabel · $status"
        name != null -> typeLabel
        else -> status
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp)
            .background(
                color = if (needsLook) AppColors.warningSurfaceMuted else AppColors.outline,
                shape = RoundedCornerShape(20.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = AppSpacing.screen, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(painter = painterResource(DevicePresentation.icon(identity.type)), contentDescription = null, modifier = Modifier.size(32.dp))
        Spacer(modifier = Modifier.width(AppSpacing.screen))
        Column(Modifier.weight(1f)) {
            Text(name ?: typeLabel, color = AppColors.textPrimary, fontSize = 16.sp, fontWeight = FontWeight.W500, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (caption != null) {
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    caption,
                    color = AppColors.textSecondary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.W400,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        Spacer(modifier = Modifier.width(AppSpacing.compact))
        Image(painter = painterResource(when {
            !info.analysisComplete -> R.drawable.svg_icon_warning_gray
            else -> R.drawable.svg_icon_next
        }), contentDescription = if (info.userTrusted) context.getString(R.string.user_trusted) else if (info.isCurrentPhone) context.getString(R.string.current_phone) else context.getString(R.string.view_device_details))
    }
}
