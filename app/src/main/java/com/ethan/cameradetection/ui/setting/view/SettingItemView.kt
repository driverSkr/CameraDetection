package com.ethan.cameradetection.ui.setting.view

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
import com.ethan.cameradetection.R
import com.ethan.cameradetection.theme.White
import com.ethan.cameradetection.theme.White10

@Composable
fun SettingItemView(item: Pair<Int, String>, onClick: () -> Unit) {
    Row(modifier = Modifier
        .fillMaxWidth()
        .height(56.dp)
        .background(color = White10, shape = RoundedCornerShape(20.dp))
        .clickable{ onClick.invoke() }
        .padding(horizontal = 12.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(painter = painterResource(item.first), contentDescription = null)
        Spacer(modifier = Modifier.width(12.dp))
        Text(item.second, color = White, fontSize = 14.sp, fontWeight = FontWeight.W600)
        Spacer(modifier = Modifier.weight(1f))
        Image(painter = painterResource(R.drawable.svg_icon_next), contentDescription = null)
    }
}