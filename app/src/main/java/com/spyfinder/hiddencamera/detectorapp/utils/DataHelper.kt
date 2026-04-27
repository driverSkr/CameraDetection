package com.spyfinder.hiddencamera.detectorapp.utils

import android.content.Context
import androidx.core.content.edit

object DataHelper {
    fun getLanguage(context: Context) : String? {
        val sharedPreferences = context.getSharedPreferences("sp_language", Context.MODE_PRIVATE)
        return sharedPreferences.getString("sp_language_data", null)
    }

    fun isFirst(context: Context, key: String) : Boolean {
        val sharedPreferences = context.getSharedPreferences("sp_first_$key", Context.MODE_PRIVATE)
        return sharedPreferences.getBoolean("key", true)
    }

    fun setFirstCompleted(context: Context, key: String) {
        val sharedPreferences = context.getSharedPreferences("sp_first_$key", Context.MODE_PRIVATE)
        sharedPreferences.edit {
            putBoolean("key", false)
        }
    }
}