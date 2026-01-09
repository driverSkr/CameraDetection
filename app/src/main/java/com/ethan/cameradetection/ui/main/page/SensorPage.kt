package com.ethan.cameradetection.ui.main.page

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ethan.cameradetection.R

/**
 * 传感器页
 */
@Composable
fun SensorPage() {
    Box(modifier = Modifier.fillMaxSize().statusBarsPadding().padding(top = 18.dp)) {
        Text("Magnetic Detector", color = Color(0xFFFFFFFF), fontSize = 28.sp, fontWeight = FontWeight.W700, modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp))

        Box(modifier = Modifier
            .size(280.dp)
            .align(Alignment.Center)
            .offset(y = (-50).dp)
        ) {
            Image(
                painter = painterResource(R.mipmap.img_circular_arc),
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize().align(Alignment.TopCenter),
                contentDescription = null
            )
        }

        Column(modifier = Modifier
            .padding(bottom = 24.dp)
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .align(Alignment.BottomCenter)
        ) {
            Row(modifier = Modifier
                .fillMaxWidth()
                .height(60.dp)
                .background(color = Color(0xFFFFFFFF).copy(0.1f), shape = RoundedCornerShape(20.dp))
                .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Image(painter = painterResource(R.drawable.svg_icon_warning_gray), contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Bring your phone close to the suspected area to detect magnetic field changes or hidden devices.",
                    color = Color(0xFFFFFFFF).copy(0.6f),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.W400,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(modifier = Modifier.height(24.dp))
            Box(modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .background(color = Color(0xFFFFFFFF).copy(0.1f), shape = RoundedCornerShape(999.dp))
                .padding(12.dp)
            ) {
                Text(
                    text = "Stop Detection",
                    color = Color(0xFFFFFFFF).copy(0.6f),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.W500,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
        }
    }
}