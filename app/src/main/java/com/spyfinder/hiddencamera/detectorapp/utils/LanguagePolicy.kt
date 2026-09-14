package com.spyfinder.hiddencamera.detectorapp.utils

import java.util.Locale

/** Only the system's first language controls app content; secondary languages do not. */
object LanguagePolicy {
    fun resolve(systemLocale: Locale?): Locale =
        if (systemLocale?.language == "zh") Locale.SIMPLIFIED_CHINESE else Locale.ENGLISH
}
