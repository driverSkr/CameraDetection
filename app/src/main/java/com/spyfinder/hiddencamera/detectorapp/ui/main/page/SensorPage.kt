package com.spyfinder.hiddencamera.detectorapp.ui.main.page

import android.content.Context
import android.content.Intent
import android.hardware.*
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.spyfinder.hiddencamera.detectorapp.R
import com.spyfinder.hiddencamera.detectorapp.event.Event
import com.spyfinder.hiddencamera.detectorapp.ui.components.*
import com.spyfinder.hiddencamera.detectorapp.ui.main.context.LocalMainContextEntity
import com.spyfinder.hiddencamera.detectorapp.ui.subscribe.SubscribeActivity
import com.spyfinder.hiddencamera.detectorapp.utils.SubscribeHelper
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.math.sqrt

@Composable
fun SensorPage() {
    val context = LocalContext.current
    val main = LocalMainContextEntity.current
    val scope = rememberCoroutineScope()
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val manager = remember { context.getSystemService(Context.SENSOR_SERVICE) as SensorManager }
    val sensor = remember { manager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD) }
    var listening by remember { mutableStateOf(false) }
    var reading by remember { mutableStateOf<Float?>(null) }
    var unavailable by remember { mutableStateOf(sensor == null) }
    var pending by remember { mutableStateOf(false) }
    val subscribed by SubscribeHelper.isSubscribedFlow.collectAsState()
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        scope.launch {
            if (pending && SubscribeHelper.isSubscribe() && sensor != null) {
                Event.event(context, Event.MAGNETIC_DETECT_START, Event.PARAM_SOURCE to "after_subscribe")
                listening = true
            }
            pending = false
        }
    }
    DisposableEffect(listening, lifecycle) {
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                val (x, y, z) = event.values
                reading = sqrt(x * x + y * y + z * z)
            }
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
        }
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) { manager.unregisterListener(listener); listening = false }
        }
        lifecycle.addObserver(observer)
        if (listening && sensor != null) {
            if (!manager.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_NORMAL)) {
                unavailable = true; listening = false
            }
        } else reading = null
        onDispose { lifecycle.removeObserver(observer); manager.unregisterListener(listener) }
    }
    fun toggleMeasurement() {
        if (listening) {
            Event.event(context, Event.MAGNETIC_DETECT_STOP, Event.PARAM_GAUGE to reading?.toInt())
            listening = false
        } else scope.launch {
            if (subscribed || SubscribeHelper.isSubscribe()) {
                Event.event(context, Event.MAGNETIC_DETECT_START, Event.PARAM_SOURCE to "sensor_page")
                listening = true
            } else {
                pending = true
                Event.event(context, Event.SUBSCRIBE_GATE_SHOW, Event.PARAM_SOURCE to "magnetic_detector")
                launcher.launch(Intent(context, SubscribeActivity::class.java))
            }
        }
    }
    QuietPage(footer = if (!unavailable) ({
        QuietButton(if (listening) "Stop measurement" else "Start measurement", onClick = ::toggleMeasurement)
    }) else null) {
        if (unavailable) {
            QuietHeading("Magnetic check", "This phone can’t measure magnetic fields.", "A working magnetic sensor wasn’t found on this device.")
            QuietOrbit(R.drawable.svg_icon_magnetic)
            QuietButton("Use Wi-Fi check") { main.selectTabIndex.intValue = 0 }
            QuietButton("Try camera inspection", secondary = true) { main.selectTabIndex.intValue = 2 }
        } else {
            QuietHeading("Magnetic check", "Follow the\nfield changes.", if (listening) "Move slowly near the area you’re checking." else "Use your phone’s magnetic sensor as an extra check.")
            QuietOrbit(value = reading?.let { String.format(Locale.getDefault(), "%.1f", it) } ?: "—", label = "μT · ${if (listening) "Live reading" else "Ready when you are"}")
            QuietPanel(tinted = true) {
                QuietBody(if (listening) "Reading magnetic strength" else "Start close. Move slowly.")
                QuietNote("Magnetic changes alone cannot identify a camera. Nearby electronics and metal can affect readings.")
            }
        }
    }
}
