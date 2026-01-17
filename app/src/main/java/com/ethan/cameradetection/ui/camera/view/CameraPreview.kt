package com.ethan.cameradetection.ui.camera.view

import android.util.Size
import android.view.ViewGroup
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner

@Composable
fun CameraPreview(
    modifier: Modifier = Modifier,
    lensFacing: Int = CameraSelector.LENS_FACING_BACK,
    scaleType: PreviewView.ScaleType = PreviewView.ScaleType.FILL_CENTER
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val context = LocalContext.current

    AndroidView(
        factory = { ctx ->
            PreviewView(ctx).apply {
                this.scaleType = scaleType
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )

                // 初始化摄像头
                val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                cameraProviderFuture.addListener({
                    val cameraProvider = cameraProviderFuture.get()

                    val preview = Preview.Builder()
                        .setTargetResolution(Size(1920, 1080))
                        .build()
                        .also {
                            it.setSurfaceProvider(surfaceProvider)
                        }

                    val imageCapture = ImageCapture.Builder()
                        .setTargetResolution(Size(1920, 1080))
                        .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
                        .build()

                    val cameraSelector = CameraSelector.Builder()
                        .requireLensFacing(lensFacing)
                        .build()

                    try {
                        cameraProvider.unbindAll()
                        cameraProvider.bindToLifecycle(
                            lifecycleOwner,
                            cameraSelector,
                            preview,
                            imageCapture
                        )
                    } catch (exc: Exception) {
                        exc.printStackTrace()
                    }
                }, ContextCompat.getMainExecutor(ctx))
            }
        },
        modifier = modifier
    )
}