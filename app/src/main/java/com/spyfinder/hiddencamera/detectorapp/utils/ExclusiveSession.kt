package com.spyfinder.hiddencamera.detectorapp.utils

import android.app.Activity
import java.lang.ref.WeakReference

/**
 * Wi-Fi scan, magnetic detection and the camera inspector cannot run together.
 * Starting one stops the others.
 */
object ExclusiveSession {
    const val SCAN_YIELD_MAGNETIC = "Scan cancelled to start magnetic detection."
    const val SCAN_YIELD_CAMERA = "Scan cancelled to start the camera."

    @Volatile private var cancelScan: ((String, String) -> Unit)? = null
    @Volatile private var scanRunning: (() -> Boolean)? = null
    @Volatile private var stopMagnetic: (() -> Unit)? = null
    @Volatile private var magneticActive: (() -> Boolean)? = null
    private var camera = WeakReference<Activity>(null)

    fun bind(
        cancelScan: (String, String) -> Unit,
        scanRunning: () -> Boolean,
        stopMagnetic: () -> Unit,
        magneticActive: () -> Boolean
    ) {
        this.cancelScan = cancelScan
        this.scanRunning = scanRunning
        this.stopMagnetic = stopMagnetic
        this.magneticActive = magneticActive
    }

    fun unbind() {
        cancelScan = null
        scanRunning = null
        stopMagnetic = null
        magneticActive = null
    }

    fun attachCamera(activity: Activity) {
        camera = WeakReference(activity)
    }

    fun detachCamera(activity: Activity) {
        if (camera.get() === activity) camera.clear()
    }

    fun cameraOpen(): Boolean {
        val activity = camera.get() ?: return false
        return !activity.isFinishing && !activity.isDestroyed
    }

    fun yieldToScan(): Boolean {
        val stopped = (magneticActive?.invoke() == true) || cameraOpen()
        stopMagnetic?.invoke()
        closeCamera()
        return stopped
    }

    fun yieldToMagnetic(): Boolean {
        val stopped = (scanRunning?.invoke() == true) || cameraOpen()
        cancelScan?.invoke(SCAN_YIELD_MAGNETIC, "magnetic")
        closeCamera()
        return stopped
    }

    fun yieldToCamera(): Boolean {
        val stopped = (scanRunning?.invoke() == true) || (magneticActive?.invoke() == true)
        cancelScan?.invoke(SCAN_YIELD_CAMERA, "camera")
        stopMagnetic?.invoke()
        return stopped
    }

    private fun closeCamera() {
        val activity = camera.get() ?: return
        if (!activity.isFinishing) activity.finish()
    }
}
