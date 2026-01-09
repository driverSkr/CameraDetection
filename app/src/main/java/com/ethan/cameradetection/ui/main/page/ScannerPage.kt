package com.ethan.cameradetection.ui.main.page

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ethan.cameradetection.R
import com.ethan.cameradetection.ui.main.view.ScannerItemView

/**
 * 扫描仪页
 */
@Composable
fun ScannerPage() {
    val scannerItemList = listOf(
        Pair(R.drawable.svg_icon_tv, "TV"),
        Pair(R.drawable.svg_icon_socket, "Socket"),
        Pair(R.drawable.svg_icon_lampshade, "Lampshade"),
        Pair(R.drawable.svg_icon_beside_table, "Beside Table"),
        Pair(R.drawable.svg_icon_tv_cabinet, "TV Cabinet"),
        Pair(R.drawable.svg_icon_wardrobe, "Wardrobe"),
        Pair(R.drawable.svg_icon_sofa, "Sofa"),
        Pair(R.drawable.svg_icon_smoke_sensor, "Smoke Sensor"),
        Pair(R.drawable.svg_icon_shower_head, "Shower Head"),
        Pair(R.drawable.svg_icon_vase, "vase"),
        Pair(R.drawable.svg_icon_air_conditioner, "Air Conditioner"),
        Pair(R.drawable.svg_icon_router, "Router")
    )
    Box(modifier = Modifier.fillMaxSize().statusBarsPadding().padding(top = 18.dp)) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
            Text("Scanner", color = Color(0xFFFFFFFF), fontSize = 28.sp, fontWeight = FontWeight.W700)
            Spacer(modifier = Modifier.height(8.dp))
            Text("Click on the following entry to enter the corresponding location for testing.", color = Color(0xFFFFFFFF).copy(0.6f), fontSize = 14.sp, fontWeight = FontWeight.W400)
        }

        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
            contentPadding = PaddingValues(horizontal = 16.dp),
            modifier = Modifier.fillMaxWidth().align(Alignment.Center)
        ) {
            items(scannerItemList.size) { index ->
                ScannerItemView(scannerItemList[index])
            }
        }
    }
}