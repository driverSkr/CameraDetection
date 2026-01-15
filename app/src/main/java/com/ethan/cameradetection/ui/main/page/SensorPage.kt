package com.ethan.cameradetection.ui.main.page

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ethan.cameradetection.R
import com.ethan.cameradetection.theme.White
import kotlinx.coroutines.delay

/**
 * 传感器页
 */
@Composable
fun SensorPage() {
    var isRotating by remember { mutableStateOf(true) }
    var value by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(isRotating) {
        if (isRotating) {
            while (isRotating) {
                value = value + 1f
                delay(10)
            }
        }
    }

    // 计算旋转角度并添加动画
    val rotationAngle by animateFloatAsState(
        targetValue = value,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "pointerRotation"
    )

    Box(modifier = Modifier.fillMaxSize().statusBarsPadding().padding(top = 18.dp)) {
        Text("Magnetic Detector", color = Color(0xFFFFFFFF), fontSize = 28.sp, fontWeight = FontWeight.W700, modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp))

        Box(modifier = Modifier
            .size(280.dp)
            .align(Alignment.Center)
            .offset(y = (-60).dp)
        ) {
            Image(
                painter = painterResource(R.mipmap.img_circular_arc),
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize().align(Alignment.TopCenter),
                contentDescription = null
            )

            Image(
                painter = painterResource(R.mipmap.img_circular_scale),
                contentDescription = null,
                modifier = Modifier.height(156.dp).width(196.dp).align(Alignment.Center).offset(y = 10.dp)
            )

            Image(
                painter = painterResource(R.mipmap.img_circular_pointer),
                modifier = Modifier
                    .align(Alignment.Center)
                    .offset(x = 30.dp)
                    .graphicsLayer {
                        // 设置旋转中心为左下角 (0f, 1f)
                        // (0,0) 是左上角，(1,1) 是右下角
                        transformOrigin = TransformOrigin(0f, 1f)
                        rotationZ = rotationAngle
                    },
                contentDescription = null
            )

            Text(
                buildAnnotatedString {
                    withStyle(
                        style = SpanStyle(
                            fontSize = 44.sp,
                            color = White,
                            fontWeight = FontWeight.W700,
                            baselineShift = BaselineShift(0f) // 调整符号的垂直位置
                        )
                    ) {
                        append("${value.toInt()}")
                    }
                    withStyle(
                        style = SpanStyle(
                            fontSize = 16.sp,
                            color = White,
                            fontWeight = FontWeight.W700,
                            baselineShift = BaselineShift(0f) // 调整符号的垂直位置
                        )
                    ) {
                        append(" μT")
                    }
                },
                modifier = Modifier.align(Alignment.BottomCenter).offset(y = 30.dp)
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
                .clickable {
                    // 点击切换旋转状态
                    isRotating = !isRotating
                },
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