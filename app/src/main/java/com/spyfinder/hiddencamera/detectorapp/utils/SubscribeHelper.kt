package com.spyfinder.hiddencamera.detectorapp.utils

import android.content.Context
import android.util.Log
import com.ethan.pay.BillFactory
import com.ethan.pay.model.OrderInfo
import com.ethan.pay.utils.SubHelper.listLifeGoodsList
import com.spyfinder.hiddencamera.detectorapp.DetectorApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * 订阅状态工具类
 *
 * 用法：
 * 普通页面直接判断当前缓存状态
 * if (SubscribeHelper.isSubscribed) {
 *     // 已订阅
 * }
 * Compose 页面实时监听
 * val isSubscribed by SubscribeHelper.isSubscribedFlow.collectAsState()
 */
object SubscribeHelper {
    private const val TAG = "SubscribeHelper"
    private val subscribeScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val _isSubscribedFlow = MutableStateFlow(false)

    @Volatile
    private var isBillingInitialized = false

    // 全局实时订阅状态：普通页面可直接读取，Compose 页面可 collectAsState() 自动刷新。
    val isSubscribedFlow: StateFlow<Boolean> = _isSubscribedFlow.asStateFlow()
    val isSubscribed: Boolean
        get() = _isSubscribedFlow.value

    fun init(context: Context) {
        subscribeScope.launch {
            initBillingIfNeeded(context.applicationContext)
            refreshSubscribeStateSuspend()
        }
    }

    fun refreshSubscribeState() {
        subscribeScope.launch {
            refreshSubscribeStateSuspend()
        }
    }

    suspend fun refreshSubscribeStateSuspend(): Boolean {
        initBillingIfNeeded()
        return runCatching {
            val subscribed = queryPurchase().isNotEmpty()
            updateSubscribeState(subscribed)
            subscribed
        }.getOrElse { throwable ->
            // 查询失败时保留上一次状态，避免网络/商店短暂异常导致页面误判为未订阅。
            Log.e(TAG, "刷新订阅状态失败", throwable)
            isSubscribed
        }
    }

    fun updateSubscribeState(isSubscribed: Boolean) {
        _isSubscribedFlow.value = isSubscribed
        Log.d(TAG, "订阅状态更新：$isSubscribed")
    }

    suspend fun isSubscribe(): Boolean {
        return refreshSubscribeStateSuspend()
    }

    suspend fun queryPurchase(): MutableList<OrderInfo> {
        val sub = queryPurchaseOnlySub()
        val lifetime = queryPurchaseOnlyLifeTime()
        return mutableListOf<OrderInfo>().apply {
            this.addAll(sub)
            this.addAll(lifetime)
        }
    }

    suspend fun queryPurchaseOnlySub(): MutableList<OrderInfo> {
        return BillFactory.getSubscribe().queryPurchase()
    }

    suspend fun queryPurchaseOnlyLifeTime(): MutableList<OrderInfo> {
        val list = BillFactory.getLifeTime().queryPurchase().filter {
            return@filter listLifeGoodsList.contains(it.goodsId)
        }.toMutableList()
        return list
    }

    private suspend fun initBillingIfNeeded(context: Context? = DetectorApp.INSTANCE?.applicationContext) {
        if (isBillingInitialized) {
            return
        }
        if (context == null) {
            Log.w(TAG, "初始化订阅状态失败：Context为空")
            return
        }
        val resultCode = BillFactory.init(context)
        isBillingInitialized = resultCode == 0
        Log.d(TAG, "Google Play Billing初始化结果：$resultCode")
    }
}
