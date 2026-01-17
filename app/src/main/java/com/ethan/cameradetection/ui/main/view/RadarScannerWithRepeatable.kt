package com.ethan.cameradetection.ui.main.view

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.ethan.cameradetection.R
import com.ethan.cameradetection.ui.main.context.LocalMainContextEntity

@Composable
fun RadarScannerWithControls() {
    val localMain = LocalMainContextEntity.current

    // 使用独立的动画状态
    val infiniteTransition = rememberInfiniteTransition()
    val rotationAngle by if (localMain.isAnimating.value) {
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
        remember { mutableFloatStateOf(0f) }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Image(
            painter = painterResource(R.mipmap.img_radar_bg),
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
            contentDescription = "雷达背景"
        )

        if (localMain.isStartDetect.value) {
            Image(
                painter = painterResource(R.mipmap.img_radar_detect),
                contentDescription = "扫描指针",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize().rotate(rotationAngle)
            )
        }
    }
}