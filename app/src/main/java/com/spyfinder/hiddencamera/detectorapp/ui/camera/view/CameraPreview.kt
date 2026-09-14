package com.spyfinder.hiddencamera.detectorapp.ui.camera.view

import com.spyfinder.hiddencamera.detectorapp.R

import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner

@Composable
fun CameraPreview(modifier: Modifier = Modifier, lensFacing: Int, retry: Int,
    onReady: (Camera, Boolean) -> Unit, onError: (String) -> Unit) {
    val context = LocalContext.current
    val owner = LocalLifecycleOwner.current
    val view = remember(context) { PreviewView(context).apply { scaleType = PreviewView.ScaleType.FILL_CENTER } }
    val ready by rememberUpdatedState(onReady)
    val error by rememberUpdatedState(onError)
    AndroidView(factory = { view }, modifier = modifier)
    DisposableEffect(owner, lensFacing, retry, view) {
        var disposed = false
        var provider: ProcessCameraProvider? = null
        var preview: Preview? = null
        var camera: Camera? = null
        try {
            val future = ProcessCameraProvider.getInstance(context)
            future.addListener({
                if (!disposed) {
                    try {
                        val p = future.get(); provider = p
                        val hasFront = p.hasCamera(CameraSelector.DEFAULT_FRONT_CAMERA)
                        val hasBack = p.hasCamera(CameraSelector.DEFAULT_BACK_CAMERA)
                        check(hasFront || hasBack) { context.getString(R.string.no_camera) }
                        val selected = when {
                            lensFacing == CameraSelector.LENS_FACING_FRONT && hasFront -> CameraSelector.DEFAULT_FRONT_CAMERA
                            hasBack -> CameraSelector.DEFAULT_BACK_CAMERA
                            else -> CameraSelector.DEFAULT_FRONT_CAMERA
                        }
                        val ownedPreview = Preview.Builder().build().also { it.setSurfaceProvider(view.surfaceProvider) }
                        preview = ownedPreview
                        camera = p.bindToLifecycle(owner, selected, ownedPreview)
                        ready(camera!!, hasFront && hasBack)
                    } catch (_: Exception) { error(context.getString(R.string.camera_unavailable)) }
                }
            }, ContextCompat.getMainExecutor(context))
        } catch (_: Exception) { error(context.getString(R.string.camera_init_error)) }
        onDispose {
            disposed = true
            camera?.cameraControl?.enableTorch(false)
            preview?.let { provider?.unbind(it) }
        }
    }
}
