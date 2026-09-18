package com.spyfinder.hiddencamera.detectorapp.ui.setting.view
import com.spyfinder.hiddencamera.detectorapp.theme.AppSpacing
import com.spyfinder.hiddencamera.detectorapp.theme.AppColors

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import com.spyfinder.hiddencamera.detectorapp.R
import com.spyfinder.hiddencamera.detectorapp.theme.AppColors.textPrimary
import com.spyfinder.hiddencamera.detectorapp.theme.AppColors.outline

@Composable
fun SettingItemView(item: Pair<Int, String>, onClick: () -> Unit) {
    Row(modifier = Modifier
        .fillMaxWidth()
        .height(AppSpacing.control)
        .background(color = AppColors.outline, shape = RoundedCornerShape(20.dp))
        .clickable{ onClick.invoke() }
        .padding(horizontal = AppSpacing.section, vertical = AppSpacing.screen),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(painter = painterResource(item.first), contentDescription = null)
        Spacer(modifier = Modifier.width(AppSpacing.section))
        Text(item.second, color = AppColors.textPrimary, fontSize = 14.sp, fontWeight = FontWeight.W600)
        Spacer(modifier = Modifier.weight(1f))
        Image(painter = painterResource(R.drawable.svg_icon_next), contentDescription = null)
    }
}