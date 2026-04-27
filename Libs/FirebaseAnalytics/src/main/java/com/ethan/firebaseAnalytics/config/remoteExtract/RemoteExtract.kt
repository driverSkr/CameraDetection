package com.ethan.firebaseAnalytics.config.remoteExtract

import android.content.Context
import com.ethan.firebaseAnalytics.config.RemoteConstant
import com.ethan.firebaseAnalytics.config.RemoteKey
import com.ethan.firebaseAnalytics.config.remoteExtract.bean.BaseConfig
import com.blankj.utilcode.util.SPUtils
import com.google.gson.Gson

object RemoteExtract {
    private fun baseConfig(): BaseConfig {
        val spUtils = SPUtils.getInstance(RemoteKey.BASE_CONFIG, Context.MODE_PRIVATE)
        var config = spUtils.getString(RemoteKey.BASE_CONFIG, "")
        if (config.isBlank()) {
            config = RemoteConstant.BaseConfig
        }
        return Gson().fromJson(config, BaseConfig::class.java)
    }

    val homeThumbOssSuffix by lazy {
        baseConfig().homeThumbOssSuffix
    }

    val isShowGuideDialog by lazy {
        baseConfig().isShowGuideDialog
    }

    val versionCheck by lazy {
        baseConfig().versionCheck
    }

    val nsfwScoreV by lazy {
        baseConfig().nsfwScoreV
    }
    val isOpenObject by lazy {
        baseConfig().isOpenObject
    }

    val isShowRetentionSubscription by lazy {
        baseConfig().isShowRetentionSubscription
    }

    val isOpenHttpDns by lazy {
        baseConfig().isOpenShareCopyView
    }
    val isOpenMusic by lazy {
        baseConfig().isOpenMusic
    }
    val aiVideoRestyle by lazy {
        baseConfig().aiVideoRestyle
    }
    val aiTattooType by lazy {
        baseConfig().aiTattooType
    }
}