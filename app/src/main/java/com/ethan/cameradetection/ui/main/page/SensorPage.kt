package com.ethan.cameradetection.ui.main.page

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.util.Log
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
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
import kotlin.math.sqrt

/**
 * 传感器页
 */
@Composable
fun SensorPage() {
    val context = LocalContext.current
    val sensorManager = remember { context.getSystemService(Context.SENSOR_SERVICE) as SensorManager }
    val magneticSensor = remember { sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD) }
    var magneticGauge by remember { mutableStateOf(0) }
    var isListening by remember { mutableStateOf(false) } // 控制是否监听传感器

    val magneticSensorListener = remember {
        object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
                event?.let {
                    if (it.sensor.type == Sensor.TYPE_MAGNETIC_FIELD) {
                        val x = it.values[0]
                        val y = it.values[1]
                        val z = it.values[2]

                        // 计算磁场强度（微特斯拉）
                        val magnitude = sqrt(x * x + y * y + z * z)

                        // 将磁场强度转换为0-100的百分比值
                        // 地球磁场通常在25-65 μT之间，我们设置一个合理的范围
                        val normalizedValue = when {
                            magnitude < 20 -> 0 // 低于20 μT认为是异常低
                            magnitude > 1000 -> 100 // 高于200 μT认为是强磁场
                            else -> ((magnitude - 20) / (1000 - 20) * 100).toInt()
                        }

                        Log.e("SensorPage", "磁场强度：${magnitude.toInt()} μT，百分比：$normalizedValue%")

                        magneticGauge = normalizedValue
                    }
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }
    }

    // 根据isListening状态注册或取消注册传感器监听
    DisposableEffect(isListening) {
        if (isListening && magneticSensor != null) {
            sensorManager.registerListener(
                magneticSensorListener,
                magneticSensor,
                SensorManager.SENSOR_DELAY_NORMAL
            )
            Log.d("SensorPage", "开始监听磁场传感器")
        } else {
            if (magneticSensor != null) {
                sensorManager.unregisterListener(magneticSensorListener, magneticSensor)
                magneticGauge = 0
                Log.d("SensorPage", "停止监听磁场传感器")
            }
        }

        onDispose {
            if (magneticSensor != null) {
                sensorManager.unregisterListener(magneticSensorListener, magneticSensor)
                Log.d("SensorPage", "停止监听磁场传感器")
            }
        }
    }

    // 计算旋转角度并添加动画
    // 指针切图默认朝向右上45度（45度）
    // 我们需要顺时针旋转：
    // 0%时：指针朝向左下45度（225度）→ 需要旋转225 - 45 = 180度
    // 100%时：指针朝向右下45度（-45度或315度）→ 从225度顺时针旋转270度
    // 公式：初始旋转180度 + 百分比对应的顺时针旋转角度
    val targetRotationAngle = if (magneticGauge == 0) {
        // 0%时：初始旋转180度使指针朝向左下45度
        180f
    } else {
        // 百分比值转换为角度：初始180度 + 顺时针旋转（每1%旋转2.7度）
        180f + (magneticGauge.toFloat() * 2.7f)
    }

    val rotationAngle by animateFloatAsState(
        targetValue = targetRotationAngle,
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
                        append("$magneticGauge")
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
                    // 点击切换监听状态
                    isListening = !isListening
                },
            ) {
                Text(
                    text = if (isListening) "Stop Detection" else "Start Detection",
                    color = Color(0xFFFFFFFF).copy(0.6f),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.W500,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
        }
    }
}