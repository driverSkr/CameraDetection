package com.spyfinder.hiddencamera.detectorapp.utils

import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Locale

class AppLanguageTest {
    @Test fun chineseRegionsUseSimplifiedChinese() {
        listOf("zh", "zh-CN", "zh-TW", "zh-HK", "zh-Hans", "zh-Hant").forEach {
            assertEquals(it, Locale.SIMPLIFIED_CHINESE, AppLanguage.localeFor(Locale.forLanguageTag(it)))
        }
    }
    @Test fun otherLanguagesUseEnglish() {
        listOf("en-US", "en-GB", "fr-FR", "ja-JP", "ar", "de", "ko", "und").forEach {
            assertEquals(it, Locale.ENGLISH, AppLanguage.localeFor(Locale.forLanguageTag(it)))
        }
    }
}
