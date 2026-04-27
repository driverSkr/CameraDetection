package com.ethan.firebaseAnalytics.analytics

import android.content.Context
import android.os.Bundle
import com.blankj.utilcode.util.AppUtils
import com.blankj.utilcode.util.LanguageUtils
import com.google.firebase.analytics.FirebaseAnalytics
import com.ethan.firebaseAnalytics.attribution.Referrer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.*


object FirebaseEvent {

    private val commonMap: MutableMap<String, String> = mutableMapOf()

    fun event(context: Context?, event: String, value: String) {
        val sharedPreferences = context?.getSharedPreferences("sp_language", Context.MODE_PRIVATE)
        var language = sharedPreferences?.getString("sp_language", null)
        if (language == null) {
            val systemLanguage = LanguageUtils.getSystemLanguage()
            language = if (systemLanguage.language == "zh") { systemLanguage.country.lowercase() } else { systemLanguage.language }
        }
        val bundle = Bundle()
        bundle.putString(FirebaseAnalytics.Param.ITEM_NAME, value)
        bundle.putString("dimension1", AppUtils.getAppVersionName())
        bundle.putString("dimension2", language)
        eventAf(context, event, bundle)
    }

    fun event(context: Context?, event: String, values: Bundle) {
        val sharedPreferences = context?.getSharedPreferences("sp_language", Context.MODE_PRIVATE)
        var language = sharedPreferences?.getString("sp_language", null)
        if (language == null) {
            val systemLanguage = LanguageUtils.getSystemLanguage()
            language = if (systemLanguage.language == "zh") { systemLanguage.country.lowercase() } else { systemLanguage.language }
        }
        values.putString("dimension1", AppUtils.getAppVersionName())
        values.putString("dimension2", language)
        eventAf(context, event, values)
    }

    fun eventAf(context: Context?, event: String, values: Bundle) {
        CoroutineScope(Dispatchers.Default).launch {
            if (event.isEmpty() || context == null) {
                return@launch
            }
            val bundle = Bundle()
            bundle.putAll(values)
            FirebaseAnalytics.getInstance(context).logEvent(event, bundle)
        }
    }

    fun open(context: Context, referrer: Referrer?) { // 上报归因来源
        commonMap.clear()
        commonMap[FirebaseAnalytics.Param.SOURCE] = if (referrer == null || referrer.source.isNullOrEmpty()) "google-play" else referrer.source
        commonMap[FirebaseAnalytics.Param.MEDIUM] = if (referrer == null || referrer.medium.isNullOrEmpty()) "organic" else referrer.medium
        commonMap[FirebaseAnalytics.Param.TERM] = referrer?.term ?: ""
        commonMap[FirebaseAnalytics.Param.CONTENT] = referrer?.content ?: ""
        commonMap[FirebaseAnalytics.Param.CAMPAIGN] = referrer?.campaign ?: ""
        commonMap[FirebaseAnalytics.Param.LOCATION] = Locale.getDefault().country
        event(context, FirebaseAnalytics.Event.APP_OPEN, Bundle())
    }
}