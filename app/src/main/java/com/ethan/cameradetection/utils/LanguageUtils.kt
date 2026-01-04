package com.ethan.cameradetection.utils

import android.content.Context
import java.util.*
import kotlin.text.contains
import kotlin.text.lowercase

object LanguageUtils {

    fun getPrivacyWithLanguage(context: Context): String {
        val locale = context.resources.configuration.locales[0]
        val language = locale.language
        return if (language.contains("zh")) {
            val country = locale.country.lowercase(Locale.getDefault())
            if (country.contains("hk") || country.contains("tw") || country.contains("mo")) { // 港澳台，繁体中文
                "https://www.hitpaw.tw/company/hitpaw-photo-enhancer-app-privacy-policy.html"
            } else { // 简体中文
                "https://www.hitpaw.com/company/hitpaw-photo-enhancer-app-privacy-policy.html"
            }
        } else if (language.contains("ar")) { // 阿拉伯语
            "https://ar.hitpaw.com/company/hitpaw-photo-enhancer-app-privacy-policy.html"
        } else if (language.contains("de")) { // 德语
            "https://www.hitpaw.de/company/hitpaw-photo-enhancer-app-privacy-policy.html"
        } else if (language.contains("en")) { // 英语
            "https://www.hitpaw.com/company/hitpaw-photo-enhancer-app-privacy-policy.html"
        } else if (language.contains("es")) { // 西班牙语
            "https://www.hitpaw.es/company/hitpaw-photo-enhancer-app-privacy-policy.html"
        } else if (language.contains("fr")) { // 法语
            "https://www.hitpaw.fr/company/hitpaw-photo-enhancer-app-privacy-policy.html"
        } else if (language.contains("ja")) { // 日语
            "https://www.hitpaw.jp/company/hitpaw-photo-enhancer-app-privacy-policy.html"
        } else if (language.contains("pt")) { // 葡萄牙语
            "https://www.hitpaw.com.br/company/hitpaw-photo-enhancer-app-privacy-policy.html"
        } else if (language.contains("in")) {// 印尼
            "https://www.hitpaw.id/company/hitpaw-photo-enhancer-app-privacy-policy.html"
        } else if (language.contains("ko")) {// 韩语
            "https://www.hitpaw.kr/company/hitpaw-photo-enhancer-app-privacy-policy.html"
        } else if (language.contains("ru")) { // 俄语
            "https://www.hitpaw.com/company/hitpaw-photo-enhancer-app-privacy-policy.html"
        } else { // 默认英语
            "https://www.hitpaw.com/company/hitpaw-photo-enhancer-app-privacy-policy.html"
        }
    }

    fun getLicenseWithLanguage(context: Context): String {
        val locale = context.resources.configuration.locales[0]
        val language = locale.language
        return if (language.contains("zh")) {
            val country = locale.country.lowercase(Locale.getDefault())
            if (country.contains("hk") || country.contains("tw") || country.contains("mo")) { // 港澳台，繁体中文
                "https://www.hitpaw.com/terms-and-conditions.html"
            } else { // 简体中文
                "https://www.hitpaw.com/terms-and-conditions.html"
            }
        } else if (language.contains("ar")) { // 阿拉伯语
            "https://www.hitpaw.com/terms-and-conditions.html"
        } else if (language.contains("de")) { // 德语
            "https://www.hitpaw.com/terms-and-conditions.html"
        } else if (language.contains("en")) { // 英语
            "https://www.hitpaw.com/terms-and-conditions.html"
        } else if (language.contains("es")) { // 西班牙语
            "https://www.hitpaw.com/terms-and-conditions.html"
        } else if (language.contains("fr")) { // 法语
            "https://www.hitpaw.com/terms-and-conditions.html"
        } else if (language.contains("it")) { // 意大利语
            "https://www.hitpaw.com/terms-and-conditions.html"
        } else if (language.contains("ja")) { // 日语
            "https://www.hitpaw.com/terms-and-conditions.html"
        } else if (language.contains("pt")) { // 葡萄牙语
            "https://www.hitpaw.com/terms-and-conditions.html"
        } else if (language.contains("ru")) { // 俄语
            "https://www.hitpaw.com/terms-and-conditions.html"
        } else { // 默认英语
            "https://www.hitpaw.com/terms-and-conditions.html"
        }
    }

    fun getLocaleLanguage(context: Context): String {
        val locale = context.resources.configuration.locales[0]
        val language = locale.language
        return if (language.contains("zh")) {
            val country = locale.country.lowercase(Locale.getDefault())
            if (country.contains("hk") || country.contains("tw") || country.contains("mo")) { // 港澳台，繁体中文
                "tw"
            } else { // 简体中文
                "zh"
            }
        } else if (language.contains("ar")) { // 阿拉伯语
            "ar"
        } else if (language.contains("de")) { // 德语
            "de"
        } else if (language.contains("en")) { // 英语
            "en"
        } else if (language.contains("es")) { // 西班牙语
            "es"
        } else if (language.contains("fr")) { // 法语
            "fr"
        } else if (language.contains("ja")) { // 日语
            "ja"
        } else if (language.contains("pt")) { // 葡萄牙语
            "pt"
        } else if (language.contains("ru")) { // 俄语
            "ru"
        } else if (language.contains("ko")) {  // 韩语
            "ko"
        } else if (language.contains("id")) { // 旧印度尼西亚语
            "id"
        } else if (language.contains("in")) { // 新印度尼西亚语
            "id"
        } else { // 默认英语
            "en"
        }
    }

    fun getLocalLanguageName(context: Context): String {
        val locale = context.resources.configuration.locales[0]
        val language = locale.language
        return if (language.contains("zh")) {
            val country = locale.country.lowercase(Locale.getDefault())
            if (country.contains("hk") || country.contains("tw") || country.contains("mo")) { // 港澳台，繁体中文
                "繁體中文"
            } else { // 简体中文
                "简体中文"
            }
        } else if (language.contains("ar")) { // 阿拉伯语
            "العربية"
        } else if (language.contains("de")) { // 德语
            "Deutsch"
        } else if (language.contains("en")) { // 英语
            "English"
        } else if (language.contains("es")) { // 西班牙语
            "Español"
        } else if (language.contains("fr")) { // 法语
            "Français"
        } else if (language.contains("ja")) { // 日语
            "日本語"
        } else if (language.contains("pt")) { // 葡萄牙语
            "Português"
        } else if (language.contains("ru")) { // 俄语
            "русский язык"
        } else if (language.contains("ko")) {  // 韩语
            "한국어"
        } else if (language.contains("id")) { // 旧印度尼西亚语
            "Bahasa Indonesia"
        } else if (language.contains("in")) { // 新印度尼西亚语
            "Bahasa Indonesia"
        } else if (language.contains("th")) { // 泰语
            "ภาษาไทย"
        } else if (language.contains("vi")) { // 越南语
            "Tiếng Việt"
        } else { // 默认英语
            "English"
        }
    }

    fun getNameToLanguage(LanguageName: String): String? {
        when (LanguageName) {
            "简体中文" -> {
                return "zh"
            }

            "繁體中文" -> {
                return "tw"
            }

            "English" -> {
                return "en"
            }

            "Bahasa Indonesia" -> {
                return "id"
            }

            "한국어" -> {
                return "ko"
            }

            "日本語" -> {
                return "ja"
            }

            "Deutsch" -> {
                return "de"
            }

            "Português" -> {
                return "pt"
            }

            "العربية" -> {
                return "ar"
            }

            "Español" -> {
                return "es"
            }

            "Français" -> {
                return "fr"
            }

            "русский язык" -> {
                return "ru"
            }

            "ภาษาไทย" -> {
                return "th"
            }

            "Tiếng Việt" -> {
                return "vi"
            }

            else -> {
                return null
            }
        }
    }
}