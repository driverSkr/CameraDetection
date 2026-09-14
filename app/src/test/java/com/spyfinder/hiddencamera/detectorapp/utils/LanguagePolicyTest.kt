package com.spyfinder.hiddencamera.detectorapp.utils

import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Locale

class LanguagePolicyTest {
    @Test fun allChineseVariantsUseChinese() {
        listOf("zh", "zh-CN", "zh-TW", "zh-HK", "zh-MO", "zh-Hans-SG", "zh-Hant-TW").forEach {
            assertEquals(it, Locale.SIMPLIFIED_CHINESE, LanguagePolicy.resolve(Locale.forLanguageTag(it)))
        }
    }

    @Test fun otherLanguagesUseEnglish() {
        listOf("en-US", "en-GB", "fr-FR", "de-DE", "ja-JP", "ko-KR", "es-ES", "ar-SA", "ru-RU").forEach {
            assertEquals(it, Locale.ENGLISH, LanguagePolicy.resolve(Locale.forLanguageTag(it)))
        }
    }

    @Test fun absentOrUnknownLanguageUsesEnglish() {
        assertEquals(Locale.ENGLISH, LanguagePolicy.resolve(null))
        assertEquals(Locale.ENGLISH, LanguagePolicy.resolve(Locale.ROOT))
        assertEquals(Locale.ENGLISH, LanguagePolicy.resolve(Locale.forLanguageTag("und")))
    }
}
