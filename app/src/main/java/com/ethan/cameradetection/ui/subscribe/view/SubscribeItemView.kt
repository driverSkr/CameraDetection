package com.ethan.cameradetection.ui.subscribe.view

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ethan.cameradetection.theme.Transparent
import com.ethan.cameradetection.theme.White
import com.ethan.cameradetection.theme.White10

@Composable
fun SubscribeItemView(modifier: Modifier = Modifier) {

    Box(modifier = modifier
        .fillMaxWidth()
        .height(120.dp)
        .background(
            brush = Brush.verticalGradient(colorStops = arrayOf(0f to Color(0xFF1E2024), 1f to Color(0xFF242227))),
            shape = RoundedCornerShape(24.dp)
        )
        .border(width = 1.dp, shape = RoundedCornerShape(24.dp), brush = Brush.verticalGradient(colorStops = arrayOf(0f to White10, 0.5f to Transparent, 1f to White10)))
    ) {
        Column(modifier = Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("≈\$3.99/wk", color = Color(0xFF96939E), fontSize = 12.sp, fontWeight = FontWeight.W400)
            Spacer(modifier = Modifier.height(12.dp))
            Text("\$19.99", color = White, fontSize = 20.sp, fontWeight = FontWeight.W700)
            Spacer(modifier = Modifier.height(12.dp))
            Text("Monthly", color = Color(0xFF96939E), fontSize = 12.sp, fontWeight = FontWeight.W400)
        }
    }
}