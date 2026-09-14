package com.spyfinder.hiddencamera.detectorapp.utils

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Feature access only; actual purchase and restore state remains in SubscribeHelper. */
object SubscriptionGate {
    // TODO(subscription-gate): 全流程联调临时取消订阅拦截；测试结束、发布前改为 false 恢复。
    // 此开关统一控制引导/冷启动弹窗、相机、磁场检测及扫描/历史详情，无需删除原拦截代码。
    const val TEMPORARILY_BYPASS_SUBSCRIPTION = true

    val hasAccessFlow: StateFlow<Boolean> = if (TEMPORARILY_BYPASS_SUBSCRIPTION) {
        MutableStateFlow(true).asStateFlow()
    } else {
        SubscribeHelper.isSubscribedFlow
    }

    suspend fun hasAccess(): Boolean =
        TEMPORARILY_BYPASS_SUBSCRIPTION || SubscribeHelper.isSubscribe()
}
