package com.ethan.cameradetection.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import com.ethan.cameradetection.R

object LaunchUtils {
    fun launchWeb(context: Context?, url: String, title: String) {
        try {
            context?.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        } catch (e: Exception) {
            Log.d("ethan", "e:${e.printStackTrace()}")
        }
    }

    fun launchPlayStore(context: Context) {
        try {
            val intent = Intent(Intent.ACTION_VIEW)
            Log.d("ethan", "包名:${context.packageName}")
            intent.data = Uri.parse("market://details?id=" + context.packageName)
            intent.setPackage("com.android.vending")
            context.startActivity(intent)
        } catch (e: Exception) {
            launchWeb(context, "https://play.google.com/store/apps/details?id=" + context.packageName, context.getString(R.string.app_name))
        }
    }
}