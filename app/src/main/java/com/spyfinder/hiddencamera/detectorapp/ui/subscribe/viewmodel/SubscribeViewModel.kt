package com.spyfinder.hiddencamera.detectorapp.ui.subscribe.viewmodel

import com.spyfinder.hiddencamera.detectorapp.R

import android.content.Context
import androidx.compose.runtime.*
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.billingclient.api.BillingClient
import com.ethan.pay.BillFactory
import com.ethan.pay.impl.ClientController
import com.ethan.pay.model.Goods
import com.ethan.pay.model.OnPayResultCallback
import com.ethan.pay.model.OrderInfo
import com.ethan.pay.utils.SubHelper
import com.spyfinder.hiddencamera.detectorapp.event.Event
import com.spyfinder.hiddencamera.detectorapp.model.SubModel
import com.spyfinder.hiddencamera.detectorapp.utils.SubscribeHelper
import com.spyfinder.hiddencamera.detectorapp.utils.PurchaseAttemptGate
import kotlinx.coroutines.*

enum class PurchaseUiState { IDLE, LAUNCHING, PENDING, SUCCESS, CANCELLED, FAILED }

class SubscribeViewModel(private val productLoader: (suspend (Context) -> List<SubModel>)? = null) : ViewModel() {
    var products by mutableStateOf<List<SubModel>>(emptyList())
        private set
    var selected by mutableStateOf<SubModel?>(null)
        private set
    var loading by mutableStateOf(false)
        private set
    var purchaseState by mutableStateOf(PurchaseUiState.IDLE)
        private set
    var messageRes by mutableIntStateOf(0)
        private set
    var productMessageRes by mutableIntStateOf(0)
        private set
    private var loaded = false
    private val attempts = PurchaseAttemptGate()
    fun select(model: SubModel) { if (purchaseState != PurchaseUiState.LAUNCHING) selected = model }
    fun load(context: Context, force: Boolean = false) {
        if (loading || (loaded && !force)) return
        loaded = true
        loading = true
        productMessageRes = 0
        products = emptyList(); selected = null
        viewModelScope.launch {
            try {
                products = withTimeout(15_000) { productLoader?.invoke(context) ?: queryProducts(context) }
                selected = products.firstOrNull { it.id == SubHelper.getWeekPlanId() } ?: products.firstOrNull()
                productMessageRes = if (products.isEmpty()) R.string.store_no_plans else 0
            } catch (e: CancellationException) {
                if (e !is TimeoutCancellationException) throw e
                productMessageRes = R.string.store_timeout
            } catch (_: Exception) { productMessageRes = R.string.store_prices_error }
            finally { loading = false }
        }
    }
    private suspend fun queryProducts(context: Context): List<SubModel> {
        check(BillFactory.init(context.applicationContext) == BillingClient.BillingResponseCode.OK)
        val plans = listOf(SubHelper.getMonthPlanId(), SubHelper.getWeekPlanId(), SubHelper.getYearPlanId())
        val skus = listOf(SubHelper.getMonthSkuId(), SubHelper.getWeekSkuId(), SubHelper.getYearSkuId())
        if (ClientController.isSupport()) {
            val details = ClientController.queryProductDetails(SubHelper.getProductId(), BillingClient.ProductType.SUBS) ?: return emptyList()
            return plans.mapIndexedNotNull { i, plan ->
                val offer = details.subscriptionOfferDetails?.firstOrNull { it.basePlanId == plan && it.offerId == null } ?: return@mapIndexedNotNull null
                // The current product UI sells base plans only. Do not silently display one phase of a multi-phase offer.
                val phase = offer.pricingPhases.pricingPhaseList.singleOrNull() ?: return@mapIndexedNotNull null
                SubModel().apply {
                    goods = details.productId; id = plan; offerId = ""; sku = skus[i]
                    formattedPrice = phase.formattedPrice; price = phase.formattedPrice
                    currency = phase.priceCurrencyCode; billingPeriod = phase.billingPeriod
                }
            }
        }
        return skus.mapIndexedNotNull { i, skuId ->
            val result = ClientController.querySkuDetails(skuId, BillingClient.SkuType.SUBS)
            check(result?.billingResult?.responseCode == BillingClient.BillingResponseCode.OK)
            val details = result?.skuDetailsList?.firstOrNull { it.sku == skuId } ?: return@mapIndexedNotNull null
            // Avoid hiding legacy trials or introductory phases behind a base-price card.
            if (details.freeTrialPeriod.isNotEmpty() || details.introductoryPrice.isNotEmpty()) return@mapIndexedNotNull null
            SubModel().apply {
                goods = SubHelper.getProductId(); id = plans[i]; offerId = ""; sku = skuId
                formattedPrice = details.price; price = details.price; currency = details.priceCurrencyCode; billingPeriod = details.subscriptionPeriod
            }
        }
    }
    fun buy(activity: FragmentActivity) {
        val model = selected ?: return
        if (loading || purchaseState == PurchaseUiState.LAUNCHING || purchaseState == PurchaseUiState.PENDING || purchaseState == PurchaseUiState.SUCCESS) return
        val id = attempts.begin()
        purchaseState = PurchaseUiState.LAUNCHING
        messageRes = R.string.opening_store
        fun update(state: PurchaseUiState, text: Int) { viewModelScope.launch {
            if (attempts.accepts(id)) { purchaseState = state; messageRes = text }
        } }
        fun success() { viewModelScope.launch {
            if (!attempts.complete(id)) return@launch
            SubscribeHelper.updateSubscribeState(true)
            purchaseState = PurchaseUiState.SUCCESS
            Event.event(activity.applicationContext, Event.PURCHASE_SUCCESS, Event.PARAM_PLAN_ID to model.id)
        } }
        viewModelScope.launch {
            try {
                withTimeout(15_000) {
                    val goods = Goods(model.goods!!, model.id!!, model.offerId.orEmpty(), model.sku!!, model.formattedPrice, model.billingPeriod)
                    BillFactory.getSubscribe().launchBilling(activity, goods, object : OnPayResultCallback {
                        override fun begin() { Event.event(activity.applicationContext, Event.PURCHASE_BEGIN, Event.PARAM_PLAN_ID to model.id) }
                        override fun onSuccess(orderList: MutableList<OrderInfo>) {
                            if (orderList.any { it.goodsId == model.goods || it.goodsId == model.sku }) success()
                            else update(PurchaseUiState.FAILED, R.string.purchase_mismatch)
                        }
                        override fun onOwned(orderList: MutableList<OrderInfo>) { viewModelScope.launch {
                            if (!attempts.accepts(id)) return@launch
                            if (SubscribeHelper.refreshSubscribeStateSuspend(force = true)) success()
                            else update(PurchaseUiState.FAILED, R.string.purchase_unconfirmed)
                        } }
                        override fun onFailed(msg: String?) { update(PurchaseUiState.FAILED, R.string.purchase_failed) }
                        override fun onDisconnect() { update(PurchaseUiState.FAILED, R.string.store_disconnected) }
                        override fun onCancel() { update(PurchaseUiState.CANCELLED, R.string.purchase_cancelled) }
                        override fun onPending() { update(PurchaseUiState.PENDING, R.string.purchase_pending) }
                        override fun onPriceChanged() { viewModelScope.launch {
                            if (!attempts.accepts(id)) return@launch
                            purchaseState = PurchaseUiState.IDLE
                            messageRes = R.string.purchase_price_changed
                            load(activity.applicationContext, true)
                        } }
                    })
                }
            } catch (e: CancellationException) {
                if (e !is TimeoutCancellationException) throw e
                update(PurchaseUiState.FAILED, R.string.purchase_timeout)
            } catch (_: Exception) { update(PurchaseUiState.FAILED, R.string.store_unavailable) }
        }
    }
    fun restore(context: Context) { viewModelScope.launch {
        if (SubscribeHelper.refreshSubscribeStateSuspend(force = true)) {
            purchaseState = PurchaseUiState.SUCCESS
        } else {
            purchaseState = PurchaseUiState.IDLE
            messageRes = if (SubscribeHelper.lastQueryFailed) R.string.restore_query_error else R.string.purchase_not_found
        }
    } }
    fun refreshOnResume() { viewModelScope.launch {
        if (SubscribeHelper.refreshSubscribeStateSuspend()) purchaseState = PurchaseUiState.SUCCESS
        else if (purchaseState == PurchaseUiState.LAUNCHING) {
            // A missing callback must not leave the button locked indefinitely.
            delay(1500)
            if (purchaseState == PurchaseUiState.LAUNCHING) {
                purchaseState = PurchaseUiState.IDLE
                messageRes = R.string.purchase_restore_hint
            }
        }
    } }
}
