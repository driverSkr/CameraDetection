package com.spyfinder.hiddencamera.detectorapp.ui.main.view
import com.spyfinder.hiddencamera.detectorapp.theme.AppSpacing
import com.spyfinder.hiddencamera.detectorapp.theme.AppColors

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.spyfinder.hiddencamera.detectorapp.theme.AppColors.outline
import com.spyfinder.hiddencamera.detectorapp.theme.AppColors.textSecondary

@Composable
fun FeatureItemView(item: Triple<String, Int, String>, onClick: () -> Unit) {
    Row(modifier = Modifier
        .fillMaxWidth()
        .height(100.dp)
        .background(color = AppColors.outline, shape = RoundedCornerShape(32.dp))
        .clickable { onClick.invoke() }
        .padding(horizontal = AppSpacing.large),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.width(219.dp)) {
            Text(item.first, color = AppColors.primary, fontSize = 20.sp, fontWeight = FontWeight.W700)
            Spacer(modifier = Modifier.height(AppSpacing.micro))
            Text(item.third, color = AppColors.textSecondary, fontSize = 12.sp, fontWeight = FontWeight.W400)
        }
        Spacer(modifier = Modifier.weight(1f))
        Image(painter = painterResource(item.second), contentDescription = null)
    }
}