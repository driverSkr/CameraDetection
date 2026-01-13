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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ethan.cameradetection.theme.White10
import com.ethan.cameradetection.theme.White60

@Composable
fun FeatureItemView(item: Triple<String, Int, String>, onClick: () -> Unit) {
    Row(modifier = Modifier
        .fillMaxWidth()
        .height(100.dp)
        .background(color = White10, shape = RoundedCornerShape(32.dp))
        .clickable { onClick.invoke() }
        .padding(horizontal = 24.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.width(219.dp)) {
            Text(item.first, color = Color(0xFF00C46F), fontSize = 20.sp, fontWeight = FontWeight.W700)
            Spacer(modifier = Modifier.height(4.dp))
            Text(item.third, color = White60, fontSize = 12.sp, fontWeight = FontWeight.W400)
        }
        Spacer(modifier = Modifier.weight(1f))
        Image(painter = painterResource(item.second), contentDescription = null)
    }
}