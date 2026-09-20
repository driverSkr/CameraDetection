package com.spyfinder.hiddencamera.detectorapp.scan

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.spyfinder.hiddencamera.detectorapp.R
import com.spyfinder.hiddencamera.detectorapp.ui.main.MainActivity
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.withTimeoutOrNull
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Holds a user-visible foreground notification while a LAN scan runs in [ScanViewModel].
 * Start failures are reported to the caller instead of being swallowed.
 */
class ScanForegroundService : Service() {
    override fun onBind(intent: Intent?) = null

    override fun onCreate() {
        super.onCreate()
        instance = this
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            val handler = stopAction
            stopInternally()
            runCatching { handler?.invoke() }
            return START_NOT_STICKY
        }
        val progress = intent?.getIntExtra(EXTRA_PROGRESS, 0) ?: 0
        if (!applyNotification(progress)) {
            completeStart(false)
            stopInternally()
            return START_NOT_STICKY
        }
        held.set(true)
        completeStart(true)
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        val lostProtection = held.get() && !stoppedByUs.get()
        held.set(false)
        if (instance === this) instance = null
        completeStart(false)
        if (lostProtection) runCatching { lostAction?.invoke() }
        super.onDestroy()
    }

    private fun stopInternally() {
        if (!stoppedByUs.compareAndSet(false, true) && !held.get()) {
            stopSelf()
            return
        }
        held.set(false)
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun applyNotification(progress: Int): Boolean {
        currentProgress = progress.coerceIn(0, 100)
        createChannel()
        val notification = buildNotification(currentProgress)
        val types = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE or
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
        } else {
            0
        }
        return try {
            ServiceCompat.startForeground(
                this, NOTIFICATION_ID, notification,
                types
            )
            true
        } catch (error: Exception) {
            Log.e(TAG, "Unable to start or update the scan foreground service", error)
            false
        }
    }

    private fun buildNotification(progress: Int): Notification {
        val launch = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
                .putExtra(MainActivity.EXTRA_FOCUS_DETECT, true),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val stop = PendingIntent.getForegroundService(
            this, 1,
            Intent(this, ScanForegroundService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val bounded = progress.coerceIn(0, 99)
        return NotificationCompat.Builder(this, CHANNEL)
            .setSmallIcon(R.drawable.svg_icon_detect)
            .setContentTitle(getString(R.string.scan_notification_title))
            .setContentText(getString(R.string.scan_notification_progress, bounded))
            .setContentIntent(launch)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
            .setProgress(100, bounded, false)
            .addAction(R.drawable.svg_icon_detect, getString(R.string.action_cancel), stop)
            .build()
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT < 26) return
        val manager = getSystemService(NotificationManager::class.java) ?: return
        val channel = NotificationChannel(
            CHANNEL,
            getString(R.string.scan_notification_channel),
            NotificationManager.IMPORTANCE_LOW
        )
        channel.setShowBadge(false)
        manager.createNotificationChannel(channel)
    }

    companion object {
        private const val TAG = "ScanForegroundService"
        private const val CHANNEL = "wifi_scan"
        private const val NOTIFICATION_ID = 1001
        private const val ACTION_STOP = "com.spyfinder.hiddencamera.detectorapp.scan.STOP"
        private const val EXTRA_PROGRESS = "progress"
        private val held = AtomicBoolean(false)
        private val stoppedByUs = AtomicBoolean(false)
        @Volatile private var instance: ScanForegroundService? = null
        @Volatile private var stopAction: (() -> Unit)? = null
        @Volatile private var lostAction: (() -> Unit)? = null
        @Volatile private var startWaiter: CompletableDeferred<Boolean>? = null
        @Volatile private var currentProgress = 0
        @Volatile private var lastPostedProgress = -1
        @Volatile private var lastPostedAt = 0L
        private val mainHandler = Handler(Looper.getMainLooper())

        fun isRunning(): Boolean = held.get() && instance != null

        fun setCallbacks(onStop: (() -> Unit)?, onLost: (() -> Unit)?) {
            stopAction = onStop
            lostAction = onLost
        }

        suspend fun ensureRunning(context: Context, progress: Int = 0): Boolean {
            if (isRunning()) {
                instance?.let { service -> mainHandler.post { service.applyNotification(progress) } }
                return true
            }
            stoppedByUs.set(false)
            val waiter = CompletableDeferred<Boolean>()
            startWaiter = waiter
            val launched = runCatching {
                ContextCompat.startForegroundService(
                    context.applicationContext,
                    Intent(context, ScanForegroundService::class.java).putExtra(EXTRA_PROGRESS, progress)
                )
            }.isSuccess
            if (!launched) {
                completeStart(false)
                return false
            }
            return withTimeoutOrNull(5_000) { waiter.await() } ?: isRunning()
        }

        fun update(progress: Int) {
            val service = instance ?: return
            val bounded = progress.coerceIn(0, 99)
            val now = SystemClock.elapsedRealtime()
            if (bounded == lastPostedProgress && now - lastPostedAt < 1_000) return
            lastPostedProgress = bounded
            lastPostedAt = now
            mainHandler.post {
                if (!service.applyNotification(bounded) && isRunning()) {
                    held.set(false)
                    runCatching { lostAction?.invoke() }
                }
            }
        }

        fun stop(context: Context) {
            val service = instance
            if (service != null) {
                mainHandler.post { service.stopInternally() }
            } else {
                held.set(false)
                runCatching { context.stopService(Intent(context, ScanForegroundService::class.java)) }
            }
        }

        private fun completeStart(ok: Boolean) {
            val waiter = startWaiter
            startWaiter = null
            waiter?.takeIf { !it.isCompleted }?.complete(ok)
        }
    }
}
