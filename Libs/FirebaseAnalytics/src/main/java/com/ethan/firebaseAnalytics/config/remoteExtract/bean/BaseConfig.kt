package com.ethan.firebaseAnalytics.config.remoteExtract.bean


import com.google.gson.annotations.SerializedName
import androidx.annotation.Keep
import kotlinx.parcelize.Parcelize
import android.os.Parcelable

@Keep
@Parcelize
data class BaseConfig(
    @SerializedName("homeThumbOssSuffix") val homeThumbOssSuffix: String,
    @SerializedName("isShowGuideDialog")  // 是否展示引导弹窗
    val isShowGuideDialog: Boolean,
    @SerializedName("isShowRetentionSubscription") // 是否显示折扣弹窗
    val isShowRetentionSubscription: Boolean = false,
    @SerializedName("versionCheck")  // 版本审核开关
    val versionCheck: String?,
    @SerializedName("nsfwScoreV")  // 局部重绘鉴黄值
    val nsfwScoreV: Float? = null,
    @SerializedName("isOpenObject")  // 是否开启物体移除开关
    val isOpenObject: Boolean? = null,
    @SerializedName("isOpenShareCopyView")  // 是否开启分享复制弹窗
    val isOpenShareCopyView: Boolean = false,
    @SerializedName("isOpenHttpDns")  // 是否开启ossHttpDNS
    val isOpenHttpDns: Boolean = false,
    @SerializedName("isOpenMusic")  // 是否开启音乐生成功能
    val isOpenMusic: Boolean = false,
    @SerializedName("aiVideoRestyle") val aiVideoRestyle: AiVideoRestyle? = null,
    @SerializedName("aiTattooType") val aiTattooType: Int = 1 // 0 自动 1手动
                     ) : Parcelable


@Keep
@Parcelize
data class AiVideoRestyle(
    @SerializedName("frameRate") var frameRate: Int? = 2,
    @SerializedName("maxDuration") var maxDuration: Int? = 7,
    @SerializedName("minDuration") var minDuration: Int? = 15,
                         ) : Parcelable