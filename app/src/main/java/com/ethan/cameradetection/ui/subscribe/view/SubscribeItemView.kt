package com.ethan.cameradetection.ui.subscribe.view

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
fun SubscribeItemView(modifier: Modifier = Modifier, product: Triple<String, String, String>, isSelected: Boolean, onClick: () -> Unit) {

    Column(modifier = modifier.fillMaxWidth()) {
        if (isSelected) {
            Box(modifier = Modifier
                .fillMaxWidth()
                .height(28.dp)
                .background(brush = Brush.horizontalGradient(colorStops = arrayOf(0f to Color(0xFF01C587), 1f to Color(0xFFBCF085))), shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
            ) {
                Text("Best Choose", color = Color(0xFF010101), fontSize = 12.sp, fontWeight = FontWeight.W600, modifier = Modifier.align(Alignment.Center))
            }
        }
        Box(modifier = Modifier
            .fillMaxWidth()
            .height(120.dp)
            .background(
                brush = Brush.verticalGradient(colorStops = arrayOf(0f to Color(0xFF1E2024), 1f to Color(0xFF242227))),
                shape = if (isSelected) RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp) else RoundedCornerShape(24.dp)
            )
            .then(
                if (isSelected) {
                    Modifier.border(width = 2.dp, shape = RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp), brush = Brush.horizontalGradient(colorStops = arrayOf(0f to Color(0xFF01C587), 1f to Color(0xFFBCF085))))
                } else {
                    Modifier.border(width = 1.dp, shape = RoundedCornerShape(24.dp), brush = Brush.verticalGradient(colorStops = arrayOf(0f to White10, 0.5f to Transparent, 1f to White10)))
                }
            )
            .clickable{ onClick.invoke() }
        ) {
            Column(modifier = Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(product.first, color = Color(0xFF96939E), fontSize = 12.sp, fontWeight = FontWeight.W400)
                Spacer(modifier = Modifier.height(12.dp))
                Text(product.second, color = White, fontSize = 20.sp, fontWeight = FontWeight.W700)
                Spacer(modifier = Modifier.height(12.dp))
                Text(product.third, color = Color(0xFF96939E), fontSize = 12.sp, fontWeight = FontWeight.W400)
            }
        }
    }
}