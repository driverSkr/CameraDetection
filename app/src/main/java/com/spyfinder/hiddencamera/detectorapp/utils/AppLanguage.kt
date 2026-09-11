package com.spyfinder.hiddencamera.detectorapp.utils

import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import android.os.LocaleList
import java.util.Locale

/** Use only the system's primary language, never a saved in-app selection. */
object AppLanguage {
    fun localeFor(systemLocale: Locale): Locale =
        if (systemLocale.language == "zh") Locale.SIMPLIFIED_CHINESE else Locale.ENGLISH

    fun systemLocale(): Locale = localeFor(Resources.getSystem().configuration.locales[0])

    fun wrap(context: Context): Context {
        val locale = systemLocale()
        val configuration = Configuration(context.resources.configuration).apply {
            setLocales(LocaleList(locale))
            setLayoutDirection(locale)
        }
        return context.createConfigurationContext(configuration)
    }
}
