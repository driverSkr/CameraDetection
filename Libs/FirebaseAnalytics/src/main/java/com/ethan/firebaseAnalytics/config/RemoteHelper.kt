package com.ethan.firebaseAnalytics.config

import android.app.Activity
import android.content.Context
import android.util.Log
import com.ethan.firebaseAnalytics.config.abTest.ABTextName
import com.ethan.firebaseAnalytics.config.abTest.ABTextValue
import com.google.firebase.ktx.Firebase
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.ktx.remoteConfig
import com.google.firebase.remoteconfig.ktx.remoteConfigSettings
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

object RemoteHelper {

    suspend fun start(activity: Activity): String {
        return suspendCancellableCoroutine { continuation ->
            val remoteConfig: FirebaseRemoteConfig = Firebase.remoteConfig
            val configSettings = remoteConfigSettings {
                minimumFetchIntervalInSeconds = 3600
            }
            remoteConfig.setConfigSettingsAsync(configSettings)
            val def: MutableMap<String, Any> = HashMap()
            def[RemoteKey.BASE_CONFIG] = RemoteConstant.BaseConfig
            def[ABTextName.BOOT_GUIDANCE_TYPE_TEST] = ABTextValue.A
            def[ABTextName.subTest1017] = ABTextValue.A
            def[ABTextName.livePhotoTest1202] = ABTextValue.A
            remoteConfig.setDefaultsAsync(def)
            remoteConfig.fetchAndActivate().addOnCompleteListener(activity) { task ->
                Log.i("RemoteHelper", "start: ${task.isSuccessful}")
                if (task.isSuccessful) {
                    val config = remoteConfig.getString(RemoteKey.BASE_CONFIG)
                    saveBaseConfig(activity, config)
                    saveAbByKey(activity, ABTextName.BOOT_GUIDANCE_TYPE_TEST, remoteConfig.getString(
                        ABTextName.BOOT_GUIDANCE_TYPE_TEST))
                    saveAbByKey(activity, ABTextName.subTest1017, remoteConfig.getString(ABTextName.subTest1017))
                    saveAbByKey(activity, ABTextName.livePhotoTest1202, remoteConfig.getString(
                        ABTextName.livePhotoTest1202))
                    if (continuation.isActive) continuation.resume(config)
                } else {
                    saveBaseConfig(activity, RemoteConstant.BaseConfig)
                    saveAbByKey(activity, ABTextName.BOOT_GUIDANCE_TYPE_TEST, ABTextValue.A)
                    saveAbByKey(activity, ABTextName.livePhotoTest1202, ABTextValue.A)
                }
            }
        }
    }

    private fun saveBaseConfig(context: Context, config: String) {
        val sharedPreferences = context.getSharedPreferences(RemoteKey.BASE_CONFIG, Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.putString(RemoteKey.BASE_CONFIG, config)
        editor.apply()
    }

    private fun saveAbByKey(context: Context, name: String, config: String) {
        val sharedPreferences = context.getSharedPreferences("AB_TEST", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.putString(name, config)
        editor.apply()
    }

}