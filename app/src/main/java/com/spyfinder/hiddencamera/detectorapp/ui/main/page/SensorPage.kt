package com.spyfinder.hiddencamera.detectorapp.ui.main.page

import android.content.Context
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
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
import com.spyfinder.hiddencamera.detectorapp.R
import com.spyfinder.hiddencamera.detectorapp.event.Event
import com.spyfinder.hiddencamera.detectorapp.theme.Transparent
import com.spyfinder.hiddencamera.detectorapp.theme.White
import com.spyfinder.hiddencamera.detectorapp.theme.White10
import com.spyfinder.hiddencamera.detectorapp.theme.White60
import com.spyfinder.hiddencamera.detectorapp.ui.subscribe.SubscribeActivity
import com.spyfinder.hiddencamera.detectorapp.utils.SubscribeHelper
import com.spyfinder.hiddencamera.detectorapp.utils.SubscriptionGate
import kotlinx.coroutines.launch
import com.spyfinder.hiddencamera.detectorapp.utils.MagneticReading
import com.spyfinder.hiddencamera.detectorapp.utils.MagneticScale
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.LinearEasing
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner

/**
 * 传感器页
 */
@Composable
fun SensorPage() {
    val context = LocalContext.current
    val readingLocale = androidx.compose.ui.platform.LocalConfiguration.current.locales[0]
    val scope = rememberCoroutineScope()
    val sensorManager = remember { context.getSystemService(Context.SENSOR_SERVICE) as SensorManager }
    val magneticSensor = remember { sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD) }
    val isSubscribed = SubscriptionGate.hasAccessFlow.collectAsState().value
    var reading by remember { mutableStateOf<MagneticReading?>(null) }
    val magneticGauge = reading?.gauge ?: 0f
    var sensorError by remember { mutableStateOf(false) }
    var sampleStartedAt by remember { mutableStateOf<Long?>(null) }
    var lastSampleAt by remember { mutableStateOf<Long?>(null) }
    var sampleStale by remember { mutableStateOf(false) }
    var sampleAccuracy by remember { mutableStateOf(SensorManager.SENSOR_STATUS_UNRELIABLE) }
    val lifecycleOwner = LocalLifecycleOwner.current
    val selectedTab = com.spyfinder.hiddencamera.detectorapp.ui.main.context.LocalMainContextEntity.current.selectTabIndex.intValue
    var isListening by remember { mutableStateOf(false) } // 控制是否监听传感器

    val shouldStartDetectionAfterSubscribe = androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf(false) }
    val subscribeLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        if (!shouldStartDetectionAfterSubscribe.value) {
            return@rememberLauncherForActivityResult
        }
        shouldStartDetectionAfterSubscribe.value = false
        scope.launch {
            val subscribed = SubscriptionGate.hasAccess()
            if (subscribed) {
                Event.event(context, Event.MAGNETIC_DETECT_START, Event.PARAM_SOURCE to "after_subscribe")
                sensorError = false
                isListening = true
            }
            shouldStartDetectionAfterSubscribe.value = false
        }
    }

    DisposableEffect(lifecycleOwner, isListening, selectedTab) {
        var registered = false
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
                if (registered && event?.sensor?.type == Sensor.TYPE_MAGNETIC_FIELD && event.values.size >= 3) {
                    MagneticReading.from(event.values[0], event.values[1], event.values[2])?.let {
                        reading = it
                        lastSampleAt = android.os.SystemClock.elapsedRealtime()
                        sampleAccuracy = event.accuracy
                        sampleStale = false
                    }
                }
            }
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
                if (registered) sampleAccuracy = accuracy
            }
        }
        fun unregister() {
            registered = false
            sensorManager.unregisterListener(listener)
            reading = null
            sampleStartedAt = null
            lastSampleAt = null
            sampleStale = false
        }
        fun sync() {
            val shouldRegister = isListening && selectedTab == 1 && lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)
            if (!shouldRegister) unregister()
            else if (!registered) {
                reading = null
                sampleStartedAt = android.os.SystemClock.elapsedRealtime()
                lastSampleAt = null
                sampleAccuracy = SensorManager.SENSOR_STATUS_UNRELIABLE
                registered = magneticSensor != null && runCatching {
                    sensorManager.registerListener(listener, magneticSensor, SensorManager.SENSOR_DELAY_NORMAL)
                }.getOrDefault(false)
                sensorError = !registered
                if (!registered) isListening = false
            }
        }
        val observer = LifecycleEventObserver { _, _ -> sync() }
        lifecycleOwner.lifecycle.addObserver(observer)
        sync()
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer); unregister() }
    }

    androidx.compose.runtime.LaunchedEffect(sampleStartedAt) {
        val start = sampleStartedAt ?: return@LaunchedEffect
        while (true) {
            kotlinx.coroutines.delay(500)
            if (com.spyfinder.hiddencamera.detectorapp.utils.MagneticSampleHealth.stale(start, lastSampleAt, android.os.SystemClock.elapsedRealtime())) {
                sampleStale = true
                reading = null
            }
        }
    }

    // One displayed physical value drives both text and needle, with no spring overshoot.
    val displayedMicroTesla by animateFloatAsState(
        targetValue = reading?.microTesla ?: 0f,
        animationSpec = tween(durationMillis = 150, easing = LinearEasing),
        label = "magneticReading"
    )
    val rotationAngle = MagneticScale.rotation(displayedMicroTesla)

    fun toggleDetectionWithSubscriptionCheck() {
        if (magneticSensor == null) {
            android.widget.Toast.makeText(context, context.getString(R.string.no_magnetic_sensor), android.widget.Toast.LENGTH_LONG).show()
            return
        }
        if (isListening) {
            shouldStartDetectionAfterSubscribe.value = false
            // 磁场检测停止埋点，记录用户主动结束检测时的读数。
            Event.event(context, Event.MAGNETIC_DETECT_STOP, Event.PARAM_GAUGE to magneticGauge, "micro_tesla" to reading?.microTesla)
            isListening = false
            return
        }

        scope.launch {
            val subscribed = if (isSubscribed) {
                true
            } else {
                SubscriptionGate.hasAccess()
            }

            if (subscribed) {
                shouldStartDetectionAfterSubscribe.value = false
                Event.event(context, Event.MAGNETIC_DETECT_START, Event.PARAM_SOURCE to "sensor_page")
                sensorError = false
                isListening = true
            } else {
                if (!SubscribeHelper.canOfferPurchase) {
                    android.widget.Toast.makeText(context, context.getString(R.string.access_retry), android.widget.Toast.LENGTH_LONG).show()
                    return@launch
                }
                shouldStartDetectionAfterSubscribe.value = true
                Event.event(context, Event.SUBSCRIBE_GATE_SHOW, Event.PARAM_SOURCE to "magnetic_detector")
                subscribeLauncher.launch(Intent(context, SubscribeActivity::class.java))
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize().statusBarsPadding().padding(top = 18.dp)) {
        Text(context.getString(R.string.title_magnetic), color = Color(0xFFFFFFFF), fontSize = 28.sp, fontWeight = FontWeight.W700, modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp))

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
                    .size(70.dp)
                    .align(Alignment.TopStart)
                    .offset(x = 140.dp - 70.dp * MagneticScale.PIVOT_X,
                        y = 170.dp - 70.dp * MagneticScale.PIVOT_Y)
                    .graphicsLayer {
                        // Anchor the pointer base at the center of the existing scale artwork.
                        transformOrigin = TransformOrigin(MagneticScale.PIVOT_X, MagneticScale.PIVOT_Y)
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
                        append(if (reading != null) String.format(readingLocale, "%.1f", displayedMicroTesla) else "—")
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
            Text(context.getString(if (displayedMicroTesla > MagneticScale.MAX_MICRO_TESLA)
                R.string.magnetic_over_range else R.string.magnetic_scale),
                color = White60, fontSize = 10.sp,
                modifier = Modifier.align(Alignment.BottomCenter).offset(y = 50.dp))
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
                    text = context.getString(when {
                        sensorError -> R.string.magnetic_sensor_failed
                        sampleStale -> R.string.magnetic_sample_stale
                        reading != null && com.spyfinder.hiddencamera.detectorapp.utils.MagneticSampleHealth.unreliable(sampleAccuracy) -> R.string.magnetic_accuracy_low
                        else -> R.string.magnetic_help
                    }),
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
                .background(color = if (isListening) White10 else Color(0xFF00C46F), shape = RoundedCornerShape(999.dp))
                .border(width = 1.dp, shape = RoundedCornerShape(999.dp), brush = Brush.verticalGradient(colorStops = arrayOf(0f to White10, 0.5f to Transparent, 1f to White10)))
                .padding(12.dp)
                .clickable {
                    // 点击切换监听状态
                    toggleDetectionWithSubscriptionCheck()
                },
            ) {
                Text(
                    text = if (isListening) context.getString(R.string.stop_detection) else context.getString(R.string.start_detection),
                    color = if (isListening) White60 else White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.W500,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
        }
    }
}
