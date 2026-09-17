package com.spyfinder.hiddencamera.detectorapp.ui.camera.page

import android.Manifest
import com.spyfinder.hiddencamera.detectorapp.utils.LatestRequest
import androidx.camera.core.CameraSelector
import androidx.camera.core.Camera
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.spyfinder.hiddencamera.detectorapp.R
import com.spyfinder.hiddencamera.detectorapp.theme.Black
import com.spyfinder.hiddencamera.detectorapp.theme.Transparent
import com.spyfinder.hiddencamera.detectorapp.theme.White
import com.spyfinder.hiddencamera.detectorapp.theme.White10
import com.spyfinder.hiddencamera.detectorapp.theme.White60
import com.spyfinder.hiddencamera.detectorapp.ui.camera.view.CameraPreview
import com.spyfinder.hiddencamera.detectorapp.utils.findBaseActivityVBind

import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner

@Composable
fun CameraScannerPage(sceneTitle: String = "") {
    val context = LocalContext.current
    val owner = LocalLifecycleOwner.current
    var granted by remember { mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) }
    var requested by rememberSaveable { mutableStateOf(false) }
    var lens by rememberSaveable { mutableIntStateOf(CameraSelector.LENS_FACING_BACK) }
    var retry by remember { mutableIntStateOf(0) }
    var camera by remember { mutableStateOf<Camera?>(null) }
    var canSwitch by remember { mutableStateOf(false) }
    var torch by remember { mutableStateOf(false) }
    val colors = listOf(Color(0xFFDD1313), Color(0xFF00C424), Color(0xFF1C73FF))
    var currentFilterColorIndex by rememberSaveable { mutableIntStateOf(-1) }
    var zoom by remember { mutableFloatStateOf(1f) }
    var minZoom by remember { mutableFloatStateOf(1f) }
    var maxZoom by remember { mutableFloatStateOf(1f) }
    var error by remember { mutableStateOf("") }
    var zoomError by remember { mutableStateOf("") }
    var torchError by remember { mutableStateOf("") }
    val zoomRequests = remember { LatestRequest() }
    val torchRequests = remember { LatestRequest() }
    fun invalidateControls() { zoomRequests.invalidate(); torchRequests.invalidate(); zoomError = ""; torchError = "" }
    fun cancelledOperation(error: Throwable): Boolean = generateSequence(error) { it.cause }.take(16).any {
        it is androidx.camera.core.CameraControl.OperationCanceledException || it is java.util.concurrent.CancellationException
    }
    fun setZoom(value: Float) {
        zoom = value.coerceIn(minZoom, maxZoom)
        val activeCamera = camera ?: return
        val request = zoomRequests.begin()
        val future = activeCamera.cameraControl.setZoomRatio(zoom)
        future.addListener({
            if (camera === activeCamera && zoomRequests.accepts(request) && owner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
                runCatching { future.get() }.onSuccess { zoomError = "" }.onFailure {
                    if (!cancelledOperation(it)) zoomError = context.getString(R.string.zoom_error)
                }
            }
        }, ContextCompat.getMainExecutor(context))
    }
    var showHelp by rememberSaveable { mutableStateOf(true) }
    fun updatePermission(value: Boolean) {
        if (granted != value) { invalidateControls(); camera = null; error = ""; torch = false; canSwitch = false }
        granted = value
    }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { updatePermission(it) }
    LaunchedEffect(Unit) { if (!granted && !requested) { requested = true; launcher.launch(Manifest.permission.CAMERA) } }
    DisposableEffect(owner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) updatePermission(ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED)
            if (event == Lifecycle.Event.ON_STOP) { invalidateControls(); camera?.cameraControl?.enableTorch(false); torch = false }
        }
        owner.lifecycle.addObserver(observer)
        onDispose { invalidateControls(); owner.lifecycle.removeObserver(observer) }
    }
    Column(modifier = Modifier.fillMaxSize().background(color = Black).statusBarsPadding().navigationBarsPadding()) {
        Box(modifier = Modifier.fillMaxWidth().height(54.dp).padding(horizontal = 12.dp)) {
            Image(painter = painterResource(R.drawable.svg_icon_back), contentDescription = context.getString(R.string.a11y_back), modifier = Modifier.align(Alignment.CenterStart).clickable{
                context.findBaseActivityVBind()?.finish()
            })
            Image(painterResource(R.drawable.svg_icon_warning_gray), contentDescription = context.getString(R.string.inspection_tips), modifier = Modifier.align(Alignment.CenterEnd).size(24.dp).clickable { showHelp = !showHelp })
            Text(if (sceneTitle.isBlank()) context.getString(R.string.tab_scanner) else sceneTitle, color = White, fontSize = 18.sp, fontWeight = FontWeight.W500, modifier = Modifier.align(Alignment.Center))
        }

        Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
            if (granted) CameraPreview(Modifier.fillMaxSize(), lens, retry,
                onReady = { value, switch ->
                    invalidateControls(); camera = value; canSwitch = switch; error = ""; torch = false
                    val range = value.cameraInfo.zoomState.value
                    minZoom = range?.minZoomRatio ?: 1f; maxZoom = range?.maxZoomRatio ?: 1f
                    zoom = range?.zoomRatio ?: 1f
                }, onError = { invalidateControls(); error = it; camera = null })

            // 滤镜层
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(if (currentFilterColorIndex >= 0) colors[currentFilterColorIndex].copy(alpha = 0.3f) else Transparent)
            )

            if (granted && camera == null && error.isEmpty()) {
                Text(context.getString(R.string.camera_starting), color = White60, fontSize = 14.sp,
                    modifier = Modifier.align(Alignment.Center))
            }

            if (!granted || error.isNotEmpty()) {
                Column(Modifier.align(Alignment.Center).padding(16.dp).fillMaxWidth().background(Color(0xFF161618), RoundedCornerShape(20.dp)).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(if (!granted) context.getString(R.string.camera_permission_required) else error, color = White60, fontSize = 14.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        CameraControl(context.getString(R.string.action_retry)) { if (!granted) launcher.launch(Manifest.permission.CAMERA) else if (error.isNotEmpty()) { invalidateControls(); camera = null; error = ""; torch = false; canSwitch = false; retry++ } }
                        CameraControl(context.getString(R.string.app_settings)) { context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}"))) }
                    }
                }
            }
            // 顶部遮罩
            Box(modifier = Modifier.fillMaxWidth().height(140.dp).background(brush = Brush.verticalGradient(colorStops = arrayOf(0f to Black, 1f to Transparent))))
            // 底部遮罩
            Box(modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(140.dp).background(brush = Brush.verticalGradient(colorStops = arrayOf(0f to Transparent, 1f to Black))))

            if (showHelp) {
                Row(modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth()
                    .heightIn(min = 60.dp)
                    .background(color = White10, shape = RoundedCornerShape(20.dp))
                    .padding(horizontal = 12.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Image(painter = painterResource(R.drawable.svg_icon_warning_gray), contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (sceneTitle.isBlank()) context.getString(R.string.camera_help)
                        else context.getString(R.string.scanner_scene_tip, sceneTitle),
                        color = White60,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.W400, modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Image(painter = painterResource(R.drawable.svg_icon_close), contentDescription = context.getString(R.string.a11y_close), modifier = Modifier.clickable{ showHelp = false })
                }
            }

            Image(painter = painterResource(R.drawable.svg_icon_retry), contentDescription = context.getString(R.string.a11y_reset_camera), modifier = Modifier.align(Alignment.BottomEnd).padding(bottom = 118.dp, end = 8.dp).clickable{
                currentFilterColorIndex = -1
                setZoom(1f)
            })

            Column(modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 42.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(context.getString(R.string.filter_visual_aid), color = White60, fontSize = 11.sp, fontWeight = FontWeight.W400,
                    modifier = Modifier.padding(bottom = 8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    FilterSwatch(
                        selected = currentFilterColorIndex == -1,
                        fill = White10,
                        label = context.getString(R.string.original),
                        description = context.getString(R.string.original_view)
                    ) { currentFilterColorIndex = -1 }
                    val captions = listOf(R.string.filter_caption_red, R.string.filter_caption_green, R.string.filter_caption_blue)
                    val descriptions = listOf(R.string.colour_red, R.string.colour_green, R.string.colour_blue)
                    colors.forEachIndexed { index, color ->
                        FilterSwatch(
                            selected = currentFilterColorIndex == index,
                            fill = color,
                            label = context.getString(captions[index]),
                            description = context.getString(descriptions[index])
                        ) { currentFilterColorIndex = index }
                    }
                }
            }
        }
        Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally)) {
                if (canSwitch) CameraControl(context.getString(R.string.switch_camera), enabled = camera != null) {
                    invalidateControls(); camera = null; torch = false
                    lens = if (lens == CameraSelector.LENS_FACING_BACK) CameraSelector.LENS_FACING_FRONT else CameraSelector.LENS_FACING_BACK
                }
                if (camera?.cameraInfo?.hasFlashUnit() == true) CameraControl(if (torch) context.getString(R.string.light_off) else context.getString(R.string.light_on)) {
                    val next = !torch
                    val activeCamera = camera
                    val request = torchRequests.begin()
                    activeCamera?.cameraControl?.enableTorch(next)?.let { future ->
                        future.addListener({
                            if (camera === activeCamera && torchRequests.accepts(request) && owner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
                                runCatching { future.get() }.onSuccess { torch = next; torchError = "" }.onFailure {
                                    if (!cancelledOperation(it)) torchError = context.getString(R.string.flashlight_error)
                                }
                            }
                        }, ContextCompat.getMainExecutor(context))
                    }
                }
            }
            if (torchError.isNotEmpty()) Text(torchError, color = White60, fontSize = 12.sp)
            if (zoomError.isNotEmpty()) Text(zoomError, color = White60, fontSize = 12.sp)
            if (camera != null && maxZoom > minZoom) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(context.getString(R.string.zoom), color = White60, fontSize = 12.sp)
                    Spacer(Modifier.width(12.dp))
                    Slider(modifier = Modifier.weight(1f), value = zoom.coerceIn(minZoom, maxZoom), valueRange = minZoom..maxZoom,
                        colors = SliderDefaults.colors(thumbColor = Color(0xFF00C46F), activeTrackColor = Color(0xFF00C46F), inactiveTrackColor = White10),
                        onValueChange = { setZoom(it) })
                }
            }
        }
    }
}
@Composable
private fun FilterSwatch(selected: Boolean, fill: Color, label: String, description: String, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable(onClick = onClick)
            .semantics { contentDescription = description; this.selected = selected }
    ) {
        Box(
            modifier = Modifier
                .size(58.dp)
                .border(2.dp, if (selected) White else Transparent, RoundedCornerShape(999.dp))
                .padding(5.dp)
                .background(fill, RoundedCornerShape(999.dp))
        )
        Spacer(Modifier.height(4.dp))
        Text(label, color = if (selected) White else White60, fontSize = 10.sp, fontWeight = FontWeight.W400)
    }
}
@Composable
private fun CameraControl(text: String, enabled: Boolean = true, onClick: () -> Unit) {
    Box(Modifier.background(White10, RoundedCornerShape(999.dp)).border(1.dp, White10, RoundedCornerShape(999.dp)).clickable(enabled = enabled, onClick = onClick).padding(horizontal = 16.dp, vertical = 12.dp), contentAlignment = Alignment.Center) {
        Text(text, color = if (enabled) White else White60, fontSize = 12.sp, fontWeight = FontWeight.W500)
    }
}
