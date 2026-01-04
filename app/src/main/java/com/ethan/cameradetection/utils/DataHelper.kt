package com.ethan.cameradetection.utils

import android.content.Context

object DataHelper {
    fun getLanguage(context: Context) : String? {
        val sharedPreferences = context.getSharedPreferences("sp_language", Context.MODE_PRIVATE)
        return sharedPreferences.getString("sp_language_data", null)
    }
}