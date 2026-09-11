package com.spyfinder.hiddencamera.detectorapp.ui.camera.page

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.spyfinder.hiddencamera.detectorapp.R
import com.spyfinder.hiddencamera.detectorapp.ui.camera.view.CameraPreview
import com.spyfinder.hiddencamera.detectorapp.ui.components.*
import com.spyfinder.hiddencamera.detectorapp.utils.findActivity

@Composable
fun CameraScannerPage() {
    val context = LocalContext.current
    fun hasAccess() = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
    var granted by remember { mutableStateOf(hasAccess()) }
    var denied by remember { mutableStateOf(false) }
    var cameraError by remember { mutableStateOf(false) }
    var retry by remember { mutableIntStateOf(0) }
    var filter by remember { mutableIntStateOf(0) }
    val colors = listOf(Color(0xFFE84141), Color(0xFF38BD67), Color(0xFF4389ED))
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted = it; denied = !it }
    val settingsLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { granted = hasAccess() }
    QuietPage(navigationPadding = true) {
        QuietTopBar(context.getString(R.string.camera_inspection)) { context.findActivity()?.finish() }
        if (!granted) {
            QuietOrbit(R.drawable.svg_icon_scanner)
            QuietHeading(context.getString(R.string.camera_access), if (denied) context.getString(R.string.camera_access_off) else context.getString(R.string.see_space), context.getString(R.string.allow_camera_description))
            QuietButton(if (denied) context.getString(R.string.open_app_settings) else context.getString(R.string.allow_camera)) {
                if (denied) settingsLauncher.launch(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}")))
                else permissionLauncher.launch(Manifest.permission.CAMERA)
            }
            QuietButton(context.getString(R.string.not_now), secondary = true) { context.findActivity()?.finish() }
        } else if (cameraError) {
            QuietHeading(context.getString(R.string.camera_unavailable), context.getString(R.string.camera_open_error), context.getString(R.string.close_other_cameras))
            QuietButton(context.getString(R.string.retry_camera)) { retry++; cameraError = false }
        } else {
            Box(Modifier.fillMaxWidth().height(355.dp).clip(RoundedCornerShape(24.dp)).background(Color.Black)) {
                key(retry) {
                    CameraPreview(Modifier.fillMaxSize(), lensFacing = CameraSelector.LENS_FACING_BACK, onError = { cameraError = true })
                }
                Box(Modifier.fillMaxSize().background(colors[filter].copy(alpha = .25f)))
                Box(Modifier.align(Alignment.Center).padding(40.dp).fillMaxWidth().height(190.dp).border(1.dp, Color.White.copy(alpha = .6f), RoundedCornerShape(22.dp)))
                Text(context.getString(R.string.inspect_bright_points), Modifier.align(Alignment.BottomCenter).background(Color.Black.copy(alpha = .65f)).padding(12.dp), color = Color.White)
            }
            Text(context.getString(R.string.color_filter), style = MaterialTheme.typography.titleMedium)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(context.getString(R.string.red), context.getString(R.string.green), context.getString(R.string.blue)).forEachIndexed { index, label ->
                    FilterChip(selected = filter == index, onClick = { filter = index }, label = { Text(label) }, modifier = Modifier.weight(1f).heightIn(min = 48.dp))
                }
            }
            QuietButton(context.getString(R.string.reset_filter), secondary = true) { filter = 0 }
            QuietNote(context.getString(R.string.camera_note))
        }
    }
}
