package com.spyfinder.hiddencamera.detectorapp.ui.camera.view

import com.spyfinder.hiddencamera.detectorapp.R

import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.core.CameraState
import androidx.lifecycle.Observer
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
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
        var switchable = false
        var readyReported = false
        val handler = android.os.Handler(android.os.Looper.getMainLooper())
        val timeout = Runnable {
            if (!disposed && !readyReported && owner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
                error(context.getString(R.string.camera_preview_timeout))
            }
        }
        fun reportReady() {
            val current = camera ?: return
            if (!disposed && !readyReported && current.cameraInfo.cameraState.value?.type == CameraState.Type.OPEN &&
                view.previewStreamState.value == PreviewView.StreamState.STREAMING) {
                readyReported = true
                handler.removeCallbacks(timeout)
                ready(current, switchable)
            }
        }
        val stateObserver = Observer<CameraState> { state ->
            if (!disposed) {
                val failure = state.error
                if (failure != null) {
                    readyReported = false
                    handler.removeCallbacks(timeout)
                    error(context.getString(if (failure.code == CameraState.ERROR_CAMERA_IN_USE || failure.code == CameraState.ERROR_MAX_CAMERAS_IN_USE)
                        R.string.camera_busy else R.string.camera_runtime_error))
                } else reportReady()
            }
        }
        val streamObserver = Observer<PreviewView.StreamState> { reportReady() }
        val lifecycleObserver = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) {
                readyReported = false
                handler.removeCallbacks(timeout)
            } else if (event == Lifecycle.Event.ON_RESUME) {
                reportReady()
                if (!readyReported) { handler.removeCallbacks(timeout); handler.postDelayed(timeout, 10_000) }
            }
        }
        owner.lifecycle.addObserver(lifecycleObserver)
        view.previewStreamState.observe(owner, streamObserver)
        handler.postDelayed(timeout, 10_000)
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
                        switchable = hasFront && hasBack
                        camera!!.cameraInfo.cameraState.observe(owner, stateObserver)
                        reportReady()
                    } catch (_: Exception) { error(context.getString(R.string.camera_unavailable)) }
                }
            }, ContextCompat.getMainExecutor(context))
        } catch (_: Exception) { error(context.getString(R.string.camera_init_error)) }
        onDispose {
            disposed = true
            handler.removeCallbacks(timeout)
            owner.lifecycle.removeObserver(lifecycleObserver)
            view.previewStreamState.removeObserver(streamObserver)
            camera?.cameraInfo?.cameraState?.removeObserver(stateObserver)
            camera?.cameraControl?.enableTorch(false)
            preview?.let { provider?.unbind(it) }
        }
    }
}