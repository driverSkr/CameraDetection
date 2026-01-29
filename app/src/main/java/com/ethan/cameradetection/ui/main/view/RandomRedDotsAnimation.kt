package com.ethan.cameradetection.ui.main.view

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.random.Random

@Composable
fun RandomRedDotsWithVisibility(
    modifier: Modifier = Modifier,
    maxDots: Int = 5,
    areaWidth: Dp = 313.dp,
    areaHeight: Dp = 313.dp
) {
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val areaWidthPx = with(density) { areaWidth.toPx() }
    val areaHeightPx = with(density) { areaHeight.toPx() }

    // 存储可见的红点
    val visibleDots = remember { mutableStateListOf<DotInfo>() }

    // 添加新红点的逻辑
    LaunchedEffect(Unit) {
        while (true) {
            if (visibleDots.size < maxDots) {
                val dotSize = (8 + Random.nextInt(12)).dp
                val dotInfo = DotInfo(
                    id = System.currentTimeMillis(),
                    x = Random.nextInt(0, (areaWidthPx - with(density) { dotSize.toPx() }).toInt()).dp,
                    y = Random.nextInt(0, (areaHeightPx - with(density) { dotSize.toPx() }).toInt()).dp,
                    size = dotSize,
                    duration = 1000L + Random.nextLong(1000L)
                )

                visibleDots.add(dotInfo)

                // 在单独的协程中处理消失
                scope.launch {
                    delay(dotInfo.duration)
                    visibleDots.remove(dotInfo)
                }
            }
            delay(300L + Random.nextLong(700L))
        }
    }

    Box(
        modifier = modifier.size(areaWidth, areaHeight)
    ) {
        visibleDots.forEach { dotInfo ->
            var isVisible by remember(dotInfo.id) { mutableStateOf(true) }

            // 在消失前触发隐藏动画
            LaunchedEffect(dotInfo.id) {
                delay(dotInfo.duration - 500) // 提前500ms开始消失
                isVisible = false
            }

            AnimatedVisibility(
                visible = isVisible,
                enter = fadeIn(animationSpec = tween(500)) +
                        scaleIn(initialScale = 0.5f, animationSpec = tween(500)),
                exit = fadeOut(animationSpec = tween(500)) +
                        scaleOut(targetScale = 0.5f, animationSpec = tween(500)),
                modifier = Modifier
                    .size(dotInfo.size)
                    .offset(dotInfo.x, dotInfo.y)
            ) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(
                            color = Color.Red,
                            shape = androidx.compose.foundation.shape.CircleShape
                        )
                )
            }
        }
    }
}

data class DotInfo(
    val id: Long,
    val x: Dp,
    val y: Dp,
    val size: Dp,
    val duration: Long
)