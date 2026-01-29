package com.ethan.cameradetection.ui.main.view

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.ethan.cameradetection.R
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.random.Random

@Composable
fun RandomRedDotsWithVisibility(
    modifier: Modifier = Modifier,
    maxDots: Int = 5,
    areaWidth: Dp = 313.dp,
    areaHeight: Dp = 313.dp,
    isAnimating: MutableState<Boolean>
) {
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current

    // 存储可见的红点
    val visibleDots = remember { mutableStateListOf<DotInfo>() }

    // 添加新红点的逻辑 - 依赖 isAnimating 状态
    LaunchedEffect(isAnimating.value) {
        // 如果停止动画，清空所有红点
        if (!isAnimating.value) {
            visibleDots.clear()
            return@LaunchedEffect
        }

        // 如果开始动画，启动循环
        while (isAnimating.value) {
            if (visibleDots.size < maxDots) {
                with(density) {
                    // 先转换为像素进行计算
                    val areaWidthPx = areaWidth.toPx()
                    val areaHeightPx = areaHeight.toPx()

                    // 随机大小（8-20dp）
//                    val dotSize = (8 + Random.nextInt(13)).dp
                    // 固定大小
                    val dotSize = 32.dp
                    val dotSizePx = dotSize.toPx()

                    // 确保红点完全在区域内
                    val maxX = areaWidthPx - dotSizePx
                    val maxY = areaHeightPx - dotSizePx

                    // 确保范围有效
                    if (maxX > 0 && maxY > 0) {
                        val xPx = Random.nextFloat() * maxX
                        val yPx = Random.nextFloat() * maxY

                        val dotInfo = DotInfo(
                            id = System.currentTimeMillis(),
                            x = xPx.toDp(),
                            y = yPx.toDp(),
                            size = dotSize,
                            duration = 1000L + Random.nextLong(3000L)
                        )

                        visibleDots.add(dotInfo)

                        // 在单独的协程中处理消失
                        scope.launch {
                            delay(dotInfo.duration)
                            // 检查是否仍在动画状态
                            if (isAnimating.value) {
                                visibleDots.remove(dotInfo)
                            }
                        }
                    }
                }
            }

            // 检查是否仍在动画状态
            if (!isAnimating.value) break

            delay(500L + Random.nextLong(1500L))
        }
    }

    Box(modifier = modifier.size(areaWidth, areaHeight)) {
        visibleDots.forEach { dotInfo ->
            var isVisible by remember(dotInfo.id) { mutableStateOf(true) }

            // 在消失前触发隐藏动画 - 依赖 isAnimating 状态
            LaunchedEffect(dotInfo.id, isAnimating.value) {
                // 如果停止动画，立即隐藏
                if (!isAnimating.value) {
                    isVisible = false
                    return@LaunchedEffect
                }

                delay(dotInfo.duration - 500) // 提前500ms开始消失
                // 检查是否仍在动画状态
                if (isAnimating.value) {
                    isVisible = false
                }
            }

            // 控制AnimatedVisibility的显示状态
            val shouldShow = isVisible && isAnimating.value

            AnimatedVisibility(
                visible = shouldShow,
                enter = fadeIn(animationSpec = tween(500)) +
                        scaleIn(initialScale = 0.5f, animationSpec = tween(500)),
                exit = fadeOut(animationSpec = tween(500)) +
                        scaleOut(targetScale = 0.5f, animationSpec = tween(500)),
                modifier = Modifier
                    .size(dotInfo.size)
                    .offset(dotInfo.x, dotInfo.y)
            ) {
                Image(
                    painter = painterResource(R.mipmap.img_position),
                    modifier = Modifier.matchParentSize(),
                    contentScale = ContentScale.Fit,
                    contentDescription = null
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