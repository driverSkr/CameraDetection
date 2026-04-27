package com.ethan.firebaseAnalytics.config.remoteExtract

import android.content.Context
import com.ethan.firebaseAnalytics.config.abTest.ABTextName
import com.ethan.firebaseAnalytics.config.abTest.ABTextValue
import com.blankj.utilcode.util.SPUtils

object ABTestExtract {
    val BOOT_GUIDANCE_TYPE_TEST: String
        get() {
            val spUtils = SPUtils.getInstance("AB_TEST", Context.MODE_PRIVATE)
            return spUtils.getString(ABTextName.BOOT_GUIDANCE_TYPE_TEST, ABTextValue.A)
        }

    val subTest1017: String
        get() {
            val spUtils = SPUtils.getInstance("AB_TEST", Context.MODE_PRIVATE)
            return spUtils.getString(ABTextName.subTest1017, ABTextValue.A)
        }

    val livePhotoTest1202: String
        get() {
            val spUtils = SPUtils.getInstance("AB_TEST", Context.MODE_PRIVATE)
            return spUtils.getString(ABTextName.livePhotoTest1202, ABTextValue.A)
        }
}