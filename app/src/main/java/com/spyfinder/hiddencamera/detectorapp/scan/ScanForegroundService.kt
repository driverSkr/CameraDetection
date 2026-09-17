package com.spyfinder.hiddencamera.detectorapp.scan

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import com.spyfinder.hiddencamera.detectorapp.R
import com.spyfinder.hiddencamera.detectorapp.ui.main.MainActivity

/**
 * Keeps the process eligible to finish an in-flight LAN scan after the UI is backgrounded.
 * Scan work stays in [ScanViewModel]; this service only holds a user-visible foreground notification.
 */
class ScanForegroundService : Service() {
    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopForegroundNotification()
            return START_NOT_STICKY
        }
        createChannel()
        val launch = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(this, CHANNEL)
            .setSmallIcon(R.drawable.svg_icon_detect)
            .setContentTitle(getString(R.string.scan_notification_title))
            .setContentText(getString(R.string.scan_notification_text))
            .setContentIntent(launch)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
            .build()
        val types = ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE or
            ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
        try {
            ServiceCompat.startForeground(this, NOTIFICATION_ID, notification,
                if (Build.VERSION.SDK_INT >= 29) types else 0)
        } catch (_: Exception) {
            stopSelf()
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        stopForegroundNotification()
        super.onDestroy()
    }

    private fun stopForegroundNotification() {
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        stopSelf()
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
        private const val CHANNEL = "wifi_scan"
        private const val NOTIFICATION_ID = 1001
        private const val ACTION_STOP = "com.spyfinder.hiddencamera.detectorapp.scan.STOP"
        fun start(context: Context) {
            val intent = Intent(context, ScanForegroundService::class.java)
            runCatching { context.startForegroundService(intent) }
        }
        fun stop(context: Context) {
            val intent = Intent(context, ScanForegroundService::class.java).setAction(ACTION_STOP)
            runCatching { context.startService(intent) }
            runCatching { context.stopService(Intent(context, ScanForegroundService::class.java)) }
        }
    }
}
