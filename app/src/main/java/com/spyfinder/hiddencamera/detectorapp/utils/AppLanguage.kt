package com.spyfinder.hiddencamera.detectorapp.utils

import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import android.os.LocaleList
import java.util.Locale

object AppLanguage {
    fun systemLocale(): Locale = LanguagePolicy.resolve(Resources.getSystem().configuration.locales[0])

    fun wrap(context: Context): Context {
        val configuration = Configuration(context.resources.configuration)
        configuration.setLocales(LocaleList(systemLocale()))
        return context.createConfigurationContext(configuration)
    }
}
