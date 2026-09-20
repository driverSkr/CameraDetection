package com.spyfinder.hiddencamera.detectorapp.utils

import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat

/** Runtime notification permission for Android 13+, including the usual rationale/settings split. */
object NotificationAccess {
    const val POST_NOTIFICATIONS_PERMISSION = "android.permission.POST_NOTIFICATIONS"
    private const val PREFS = "notification_access"
    private const val KEY_ASKED = "system_asked"

    enum class Step { NOT_REQUIRED, GRANTED, EXPLAIN, SETTINGS }

    fun granted(context: Context): Boolean =
        Build.VERSION.SDK_INT < 33 ||
            ContextCompat.checkSelfPermission(context, POST_NOTIFICATIONS_PERMISSION) ==
            PackageManager.PERMISSION_GRANTED

    fun asked(context: Context): Boolean =
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean(KEY_ASKED, false)

    fun markAsked(context: Context) {
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putBoolean(KEY_ASKED, true).apply()
    }

    fun step(activity: Activity): Step = step(
        sdkInt = Build.VERSION.SDK_INT,
        granted = granted(activity),
        asked = asked(activity),
        canRationale = Build.VERSION.SDK_INT >= 33 &&
            activity.shouldShowRequestPermissionRationale(POST_NOTIFICATIONS_PERMISSION)
    )

    fun step(sdkInt: Int, granted: Boolean, asked: Boolean, canRationale: Boolean): Step = when {
        sdkInt < 33 -> Step.NOT_REQUIRED
        granted -> Step.GRANTED
        !asked || canRationale -> Step.EXPLAIN
        else -> Step.SETTINGS
    }
}
