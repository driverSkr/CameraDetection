package com.ethan.cameradetection.ui.main.view

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.ethan.cameradetection.R

@Composable
fun RadarScannerWithRepeatable() {
    // 定义无限重复的动画
    val infiniteTransition = rememberInfiniteTransition()

    // 创建从0到360度循环的动画
    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 2000, // 2秒完成一圈
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Restart
        )
    )

    Box(modifier = Modifier.fillMaxSize()) {
        Image(
            painter = painterResource(R.mipmap.img_radar_bg),
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
            contentDescription = "雷达背景"
        )

        Image(
            painter = painterResource(R.mipmap.img_radar_detect),
            contentDescription = "扫描指针",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize().rotate(rotationAngle)
        )
    }
}

@Composable
fun RadarScannerWithControls() {
    var isAnimating by remember { mutableStateOf(true) }

    // 方法1：使用独立的动画状态
    val infiniteTransition = rememberInfiniteTransition()
    val rotationAngle by if (isAnimating) {
        infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(
                animation = tween(
                    durationMillis = 2000,
                    easing = LinearEasing
                ),
                repeatMode = RepeatMode.Restart
            )
        )
    } else {
        // 停止动画时，返回固定值
        remember { mutableStateOf(0f) }
    }

    Box(modifier = Modifier.size(313.dp).clickable{ isAnimating = !isAnimating }) {
        Image(
            painter = painterResource(R.mipmap.img_radar_bg),
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
            contentDescription = "雷达背景"
        )

        Image(
            painter = painterResource(R.mipmap.img_radar_detect),
            contentDescription = "扫描指针",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize().rotate(rotationAngle)
        )
    }
}