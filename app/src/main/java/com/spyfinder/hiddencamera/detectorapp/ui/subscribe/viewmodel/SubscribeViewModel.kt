package com.spyfinder.hiddencamera.detectorapp.ui.subscribe.viewmodel

import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableIntStateOf
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ethan.pay.BillFactory
import com.ethan.pay.model.Goods
import com.ethan.pay.model.OnPayResultCallback
import com.ethan.pay.model.OrderInfo
import com.ethan.pay.utils.SubHelper
import com.spyfinder.hiddencamera.detectorapp.event.Event
import com.spyfinder.hiddencamera.detectorapp.model.SubModel
import com.spyfinder.hiddencamera.detectorapp.utils.SubscribeHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class SubscribeViewModel: ViewModel() {
    var isBuySuccess: MutableState<Int> = mutableIntStateOf(0)
    var isBuyDiscordSuccess: MutableState<Int> = mutableIntStateOf(0)

    // Only offer plans whose current price was returned by the store.
    suspend fun querySubProduct(context: Context): List<SubModel> = withContext(Dispatchers.IO) {
        val plans = listOf(SubHelper.getMonthPlanId(), SubHelper.getWeekPlanId(), SubHelper.getYearPlanId())
        val skus = listOf(SubHelper.getMonthSkuId(), SubHelper.getWeekSkuId(), SubHelper.getYearSkuId())
        plans.mapIndexedNotNull { index, plan ->
            val goods = Goods(SubHelper.getProductId(), plan, "", skus[index])
            val prices = try { BillFactory.getSubscribe().getGoodsPrice(context, goods) }
                catch (exception: kotlinx.coroutines.CancellationException) { throw exception }
                catch (exception: Exception) { null }
            val price = prices?.getOrNull(0)
            if (price.isNullOrBlank() || price == "0.00") null
            else SubModel().apply {
                this.goods = SubHelper.getProductId()
                id = plan; offerId = ""; sku = skus[index]
                this.price = price; currency = prices.getOrNull(1).orEmpty()
                isFreeTrial = false
            }
        }
    }
    fun buySubscribe(model: SubModel?, activity: FragmentActivity, dialog: MutableState<Boolean>) {
        viewModelScope.launch {
            val planId = model?.id.toString()
            Log.d("subscribe", "购买订阅 planId：$planId")
            val goods = Goods(model?.goods ?: SubHelper.getProductId(), planId, model?.offerId ?: "", model?.sku ?: SubHelper.getWeekSkuId())
            withContext(Dispatchers.Main) {
                dialog.value = false
            }
            println("ethan: $goods")
            BillFactory.getSubscribe().launchBilling(activity, goods, object : OnPayResultCallback {
                override fun begin() {
                    Log.d("subscribe", "InApp Billing 购买订阅开始")
                    // 支付开始埋点，记录拉起 Google Play Billing 的商品信息。
                    Event.event(
                        activity,
                        Event.PURCHASE_BEGIN,
                        Event.PARAM_PLAN_ID to planId,
                        Event.PARAM_GOODS_ID to goods.productId,
                        Event.PARAM_SKU to goods.skuId,
                        Event.PARAM_OFFER_ID to goods.offerId
                    )
                }

                override fun onSuccess(orderList: MutableList<OrderInfo>) {
                    Log.d("subscribe", "InApp Billing 购买订阅成功")
                    Event.event(
                        activity,
                        Event.PURCHASE_SUCCESS,
                        Event.PARAM_PLAN_ID to planId,
                        Event.PARAM_GOODS_ID to goods.productId,
                        Event.PARAM_ORDER_COUNT to orderList.size
                    )
                    // 支付成功后立即更新全局订阅状态，避免等待页面重新进入前台。
                    SubscribeHelper.updateSubscribeState(true)
                    viewModelScope.launch(Dispatchers.Main) { isBuySuccess.value = 1 }
                    SubscribeHelper.refreshSubscribeState()
                }

                override fun onOwned(orderList: MutableList<OrderInfo>) {
                    Log.d("subscribe", "InApp Billing 已拥有订阅")
                    Event.event(
                        activity,
                        Event.PURCHASE_OWNED,
                        Event.PARAM_PLAN_ID to planId,
                        Event.PARAM_GOODS_ID to goods.productId,
                        Event.PARAM_ORDER_COUNT to orderList.size
                    )
                    // 已拥有也视为订阅有效，并后台同步一次真实订单列表。
                    SubscribeHelper.updateSubscribeState(true)
                    viewModelScope.launch(Dispatchers.Main) { isBuySuccess.value = 1 }
                    SubscribeHelper.refreshSubscribeState()
                }

                override fun onFailed(msg: String?) {
                    viewModelScope.launch(Dispatchers.Main) {
                        Toast.makeText(activity, "purchase failed", Toast.LENGTH_SHORT).show()
                    }
                    if (model?.offerId.isNullOrBlank()) {
                        isBuySuccess.value = 2
                    } else {
                        isBuyDiscordSuccess.value = 2
                    }

                    Event.event(
                        activity,
                        Event.PURCHASE_FAILED,
                        Event.PARAM_PLAN_ID to planId,
                        Event.PARAM_GOODS_ID to goods.productId,
                        Event.PARAM_REASON to msg.orEmpty()
                    )
                    Log.d("subscribe", "InApp Billing 购买订阅失败: $msg")
                }

                override fun onDisconnect() {
                    viewModelScope.launch(Dispatchers.Main) {
                        Toast.makeText(activity, "store disconnected", Toast.LENGTH_SHORT).show()
                    }
                    if (model?.offerId.isNullOrBlank()) {
                        isBuySuccess.value = 3
                    } else {
                        isBuyDiscordSuccess.value = 3
                    }
                    Event.event(
                        activity,
                        Event.PURCHASE_DISCONNECT,
                        Event.PARAM_PLAN_ID to planId,
                        Event.PARAM_GOODS_ID to goods.productId,
                        Event.PARAM_REASON to "billing_disconnect"
                    )
                    Log.d("subscribe", "InApp Billing GooglePlay连接中断")
                }

                override fun onCancel() {
                    if (model?.offerId.isNullOrBlank()) {
                        isBuySuccess.value = 4
                    } else {
                        isBuyDiscordSuccess.value = 4
                    }
                    viewModelScope.launch(Dispatchers.Main) {
                        Toast.makeText(activity, "purchase cancelled", Toast.LENGTH_SHORT).show()
                    }
                    Event.event(
                        activity,
                        Event.PURCHASE_CANCEL,
                        Event.PARAM_PLAN_ID to planId,
                        Event.PARAM_GOODS_ID to goods.productId
                    )
                    Log.d("subscribe", "InApp Billing 购买订阅取消")
                }
            })
        }
    }
}
