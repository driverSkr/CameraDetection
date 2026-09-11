package com.spyfinder.hiddencamera.detectorapp.ui.camera.view

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
fun CameraPreview(modifier: Modifier = Modifier, lensFacing: Int = CameraSelector.LENS_FACING_BACK,
    scaleType: PreviewView.ScaleType = PreviewView.ScaleType.FILL_CENTER, onError: () -> Unit = {}) {
    val context = LocalContext.current
    val owner = LocalLifecycleOwner.current
    val error by rememberUpdatedState(onError)
    val view = remember(context) { PreviewView(context).apply {
        this.scaleType = scaleType
        implementationMode = PreviewView.ImplementationMode.COMPATIBLE
    } }
    DisposableEffect(owner, lensFacing, view) {
        var active = true
        var provider: ProcessCameraProvider? = null
        var preview: Preview? = null
        val future = ProcessCameraProvider.getInstance(context)
        future.addListener({
            if (active) try {
                val cameraProvider = future.get()
                provider = cameraProvider
                val useCase = Preview.Builder().build().also { it.setSurfaceProvider(view.surfaceProvider) }
                preview = useCase
                cameraProvider.bindToLifecycle(owner, CameraSelector.Builder().requireLensFacing(lensFacing).build(), useCase)
            } catch (exception: Exception) { error() }
        }, ContextCompat.getMainExecutor(context))
        onDispose {
            active = false
            preview?.let { provider?.unbind(it) }
        }
    }
    AndroidView(factory = { view }, modifier = modifier)
}
