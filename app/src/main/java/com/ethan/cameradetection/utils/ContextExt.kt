package com.ethan.cameradetection.utils

import android.content.Context
import android.content.ContextWrapper
import android.content.res.Configuration
import androidx.activity.ComponentActivity
import com.ethan.cameradetection.base.BaseActivityVBind
import java.util.Locale


fun Context.findActivity(): ComponentActivity? = when (this) {
    is ComponentActivity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

fun Context.findBaseActivityVBind(): BaseActivityVBind<*>? = when (this) {
    is BaseActivityVBind<*> -> this
    is ContextWrapper -> baseContext.findBaseActivityVBind()
    else -> null
}

fun Context.getEnglishResources(): Context {
    val config = Configuration(resources.configuration)
    val locale = Locale("en")
    Locale.setDefault(locale)
    config.setLocale(locale)
    return createConfigurationContext(config)
}




