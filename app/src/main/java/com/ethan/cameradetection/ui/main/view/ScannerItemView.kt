package com.ethan.cameradetection.ui.main.view

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
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
import com.ethan.cameradetection.theme.White10
import com.ethan.cameradetection.theme.White60

@Composable
fun ScannerItemView(item: Pair<Int, String>) {
    Column(modifier = Modifier
        .height(86.dp)
        .width(106.dp)
        .background(color = White10, shape = RoundedCornerShape(20.dp)),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Image(painter = painterResource(item.first), contentDescription = null)
        Spacer(modifier = Modifier.height(8.dp))
        Text(item.second, color = White60, fontSize = 12.sp, fontWeight = FontWeight.W400)
    }
}