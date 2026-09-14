package com.spyfinder.hiddencamera.detectorapp.utils

import com.spyfinder.hiddencamera.detectorapp.R

import android.content.Context
import com.ethan.pay.BillFactory
import com.ethan.pay.model.OrderInfo
import com.ethan.pay.utils.SubHelper
import com.spyfinder.hiddencamera.detectorapp.DetectorApp
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

object SubscribeHelper {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val mutex = Mutex()
    private val subscribed = MutableStateFlow(false)
    private val access = MutableStateFlow(AccessStatus.UNKNOWN)
    val isSubscribedFlow = subscribed.asStateFlow()
    val accessFlow = access.asStateFlow()
    val isSubscribed get() = subscribed.value
    val canOfferPurchase get() = access.value == AccessStatus.INACTIVE && !lastQueryFailed
    @Volatile var lastQueryFailed = false
        private set
    private var subscriptionActive: Boolean? = null
    private var lifetimeActive: Boolean? = null
    private var revision = 0L
    private var lastRefresh = 0L
    private var appContext: Context? = null

    fun init(context: Context) { appContext = context.applicationContext; refreshSubscribeState() }
    fun refreshSubscribeState() { scope.launch { refreshSubscribeStateSuspend(force = true) } }
    suspend fun isSubscribe(): Boolean = refreshSubscribeStateSuspend()

    suspend fun refreshSubscribeStateSuspend(force: Boolean = false): Boolean = mutex.withLock {
        val now = android.os.SystemClock.elapsedRealtime()
        if (!force && now - lastRefresh < 2_000) return@withLock isSubscribed
        val queryRevision = synchronized(this) { revision }
        val context = appContext ?: DetectorApp.INSTANCE?.applicationContext
        val connected = if (context == null) false else querySafely { BillFactory.init(context) == 0 } == true
        var sub: Boolean? = null
        var life: Boolean? = null
        if (connected) {
            sub = querySafely { queryPurchaseOnlySub().isNotEmpty() }
            life = querySafely { queryPurchaseOnlyLifeTime().isNotEmpty() }
        }
        synchronized(this) {
            if (queryRevision == revision) {
                if (sub != null) subscriptionActive = sub
                if (life != null) lifetimeActive = life
                lastQueryFailed = sub == null || life == null
                access.value = EntitlementPolicy.resolve(subscriptionActive, lifetimeActive)
                subscribed.value = access.value == AccessStatus.ACTIVE
            }
            lastRefresh = android.os.SystemClock.elapsedRealtime()
        }
        isSubscribed
    }
    private suspend fun querySafely(block: suspend () -> Boolean): Boolean? = try {
        withTimeoutOrNull(8_000) { block() }
    } catch (e: CancellationException) { throw e } catch (_: Exception) { null }

    @Synchronized fun updateSubscribeState(isSubscribed: Boolean) {
        revision++
        subscriptionActive = isSubscribed
        access.value = EntitlementPolicy.resolve(subscriptionActive, lifetimeActive)
        subscribed.value = access.value == AccessStatus.ACTIVE
        lastRefresh = android.os.SystemClock.elapsedRealtime()
    }
    suspend fun queryPurchase(): MutableList<OrderInfo> = (queryPurchaseOnlySub() + queryPurchaseOnlyLifeTime()).toMutableList()
    suspend fun queryPurchaseOnlySub(): MutableList<OrderInfo> {
        val handler = BillFactory.getSubscribe()
        val orders = handler.queryPurchase().filter { it.goodsId == SubHelper.getProductId() || it.goodsId in listOf(SubHelper.getWeekSkuId(), SubHelper.getMonthSkuId(), SubHelper.getYearSkuId()) }.toMutableList()
        acknowledgeOutstanding(orders, handler)
        return orders
    }
    suspend fun queryPurchaseOnlyLifeTime(): MutableList<OrderInfo> {
        val handler = BillFactory.getLifeTime()
        val orders = handler.queryPurchase().filter { it.goodsId in SubHelper.listLifeGoodsList }.toMutableList()
        acknowledgeOutstanding(orders, handler)
        return orders
    }
    private suspend fun acknowledgeOutstanding(orders: List<OrderInfo>, handler: com.ethan.pay.impl.GPayImpl) {
        for (order in orders) {
            if (!order.acknowledged && order.token != null) {
                try { withTimeoutOrNull(2_000) { handler.handlePurchase(order.token!!) } }
                catch (e: CancellationException) { throw e }
                catch (_: Exception) { /* Retry on the next foreground query, without revoking a purchased entitlement. */ }
            }
        }
    }
    fun getProductType(context: Context, planId: String?) = when (planId) {
        SubHelper.getWeekPlanId() -> context.getString(R.string.plan_weekly)
        SubHelper.getMonthPlanId() -> context.getString(R.string.plan_monthly)
        SubHelper.getYearPlanId() -> context.getString(R.string.plan_yearly)
        else -> context.getString(R.string.subscription)
    }
}
