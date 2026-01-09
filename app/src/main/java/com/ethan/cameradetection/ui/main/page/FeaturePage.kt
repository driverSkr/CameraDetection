package com.ethan.cameradetection.ui.main.page

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ethan.cameradetection.R

/**
 * 功能页
 */
@Composable
fun FeaturePage() {
    val featureItemList = listOf(
        Triple("Wi-Fi", R.drawable.svg_icon_wifi, "Scan for suspicious devices in the current network"),
        Triple("Magnetic", R.drawable.svg_icon_magnetic, "Use mobile phones magnetic snsor to detect sneak shots"),
        Triple("Scanner", R.drawable.svg_icon_scanner_big, "Find the instead point of the camera through the camera"),
        Triple("Tips", R.drawable.svg_icon_tips, "Practical tips to boost your safety awareness"),
    )

    Column(modifier = Modifier.fillMaxSize().statusBarsPadding().padding(top = 18.dp)) {
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column {
                Text("Features", color = Color(0xFFFFFFFF), fontSize = 28.sp, fontWeight = FontWeight.W700)
                Spacer(modifier = Modifier.height(8.dp))
                Text("Detection Method", color = Color(0xFFFFFFFF).copy(0.6f), fontSize = 14.sp, fontWeight = FontWeight.W400)
            }
            Spacer(modifier = Modifier.weight(1f))
            Image(painter = painterResource(R.drawable.svg_icon_settings), contentDescription = null)
        }
        Spacer(modifier = Modifier.height(18.dp))
        LazyColumn(
            contentPadding = PaddingValues(horizontal = 16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {

        }

    }
}