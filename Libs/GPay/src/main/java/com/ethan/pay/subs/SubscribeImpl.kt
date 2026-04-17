package com.ethan.pay.subs

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchaseHistoryRecord
import com.android.billingclient.api.acknowledgePurchase
import com.ethan.pay.impl.ClientController
import com.ethan.pay.impl.GPayImpl
import com.ethan.pay.impl.OnPurchaseListener
import com.ethan.pay.impl.PurchaseType
import com.ethan.pay.model.Goods
import com.ethan.pay.model.OnPayResultCallback
import com.ethan.pay.model.OrderInfo
import com.ethan.pay.model.PurchaseState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 针对订阅类商品
 */
class SubscribeImpl : GPayImpl {

    private var payCallback: OnPayResultCallback? = null

    fun init() {
        ClientController.setOnPurchaseListener(object : OnPurchaseListener {
            override fun onPurchase(result: BillingResult, purchases: List<Purchase>?) {
                if (result.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
                    CoroutineScope(Dispatchers.Default).launch {
                        val list = mutableListOf<OrderInfo>()
                        for (purchase in purchases) {
                            if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
                                if (!purchase.isAcknowledged) {
                                    handlePurchase(purchase.purchaseToken)
                                }
                                list.add(OrderInfo().createOrderInfo(purchase))
                            }
                        }
                        if (list.isNotEmpty()) {
                            ClientController.globalSuccessCallBack.invoke(list, PurchaseType.SUB)
                            payCallback?.onSuccess(list)
                        }
                    }
                } else if (result.responseCode == BillingClient.BillingResponseCode.USER_CANCELED) {
                    payCallback?.onCancel()
                } else if (result.responseCode == BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED) { // 已订阅
                    CoroutineScope(Dispatchers.Default).launch {
                        val list = mutableListOf<OrderInfo>()
                        if (purchases != null) {
                            for (purchase in purchases) {
                                if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
                                    if (!purchase.isAcknowledged) {
                                        handlePurchase(purchase.purchaseToken)
                                    }
                                    list.add(OrderInfo().createOrderInfo(purchase))
                                }
                            }
                        }
                        if (list.isNotEmpty()) payCallback?.onOwned(list)
                    }
                } else {
                    payCallback?.onFailed(result.responseCode.toString())
                }
            }
        })
    }

    override suspend fun launchBilling(activity: Activity, goods: Goods, callback: OnPayResultCallback) {
        init()
        payCallback = callback
        withContext(Dispatchers.Main) {
            if (activity is FragmentActivity) {
                activity.lifecycle.addObserver(object : LifecycleEventObserver {
                    override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
                        if (event == Lifecycle.Event.ON_DESTROY) {
                            payCallback = null
                        }
                    }
                })
            }
        }
        if (!ClientController.isConnect(activity)) {
            payCallback?.onDisconnect()
            return
        }
        val productDetails = ClientController.queryProductDetails(goods.productId, BillingClient.ProductType.SUBS) ?: return
        val selectedOfferToken = ClientController.querySubProductOfferToken(productDetails, goods.planId, goods.offerId)
        if (selectedOfferToken.isEmpty()) {
            callback.onFailed("offerToken empty")
            return
        }
        val productDetailsParamsList = listOf(BillingFlowParams.ProductDetailsParams.newBuilder().setProductDetails(productDetails)
            .setOfferToken(selectedOfferToken).build())
        val billingFlowParams = BillingFlowParams.newBuilder().setProductDetailsParamsList(productDetailsParamsList).build()
        withContext(Dispatchers.Main) {
            val responseCode = ClientController.client?.launchBillingFlow(activity, billingFlowParams)?.responseCode
            if (responseCode != BillingClient.BillingResponseCode.OK) {
                payCallback?.onFailed("code $responseCode")
            }
        }
    }

    override suspend fun getGoodsPrice(context: Context, goods: Goods): Array<String?> {
        val array = arrayOfNulls<String>(2)
        if (!ClientController.isConnect(context)) {
            return array
        }
        val productDetails = ClientController.queryProductDetails(goods.productId, BillingClient.ProductType.SUBS) ?: return array
        val price = ClientController.querySubProductPrice(productDetails, goods.planId, "")
        val currency = ClientController.querySubProductCurrency(productDetails, goods.planId)
        array[0] = if (price.contains(",")) price.replace(",", ".") else price // 替换部分国家的浮点数逗号为标准小数点
        array[1] = currency
        return array
    }

    override suspend fun getGoodsFormattedPrice(context: Context, goods: Goods): String {
        if (!ClientController.isConnect(context)) {
            return ""
        }
        val productDetails = ClientController.queryProductDetails(goods.productId, BillingClient.ProductType.SUBS) ?: return ""
        return ClientController.querySubProductFormattedPrice(productDetails, goods.planId, "")
    }

    override suspend fun getDiscountPrice(context: Context, goods: Goods): Array<String?> {
        val array = arrayOfNulls<String>(2)
        if (!ClientController.isConnect(context)) {
            return array
        }
        val productDetails = ClientController.queryProductDetails(goods.productId, BillingClient.ProductType.SUBS) ?: return array
        val price = ClientController.querySubProductPrice(productDetails, goods.planId, goods.offerId)
        val currency = ClientController.querySubProductCurrency(productDetails, goods.planId)
        array[0] = if (price.contains(",")) price.replace(",", ".") else price
        array[1] = currency
        return array
    }

    override suspend fun getOrderInfo(context: Context, goods: Goods): OrderInfo? {
        if (!ClientController.isConnect(context)) {
            return null
        }
        val purchaseList = queryPurchase()
        purchaseList.forEach {
            if (it.goodsId == goods.productId) {
                return it
            }
        }
        return null
    }

    override suspend fun getPurchaseState(context: Context, goods: Goods): PurchaseState {
        if (!ClientController.isConnect(context)) {
            return PurchaseState.DISCONNECT
        }
        val purchaseList = queryPurchase()
        purchaseList.forEach {
            if (it.goodsId == goods.productId) {
                return PurchaseState.GPAY
            }
        }
        return PurchaseState.NOTPAY
    }

    override suspend fun handlePurchase(purchaseToken: String): Boolean {
        val acknowledgePurchaseParams = AcknowledgePurchaseParams.newBuilder().setPurchaseToken(purchaseToken).build() // 确认购买，这里是针对不消耗型商品
        val billingResult = ClientController.client?.acknowledgePurchase(acknowledgePurchaseParams)
        Log.d("pay_point","核销结果：${billingResult?.responseCode}")
        return billingResult?.responseCode == BillingClient.BillingResponseCode.OK
    }

    override suspend fun queryPurchase(): MutableList<OrderInfo> {
        return ClientController.queryPurchase(BillingClient.ProductType.SUBS)
    }

    override suspend fun confirmPlan(context: Context): Boolean {
        if (!ClientController.isConnect(context)) {
            return false
        }
        val purchaseList = queryPurchase()
        return if (purchaseList.isNotEmpty()) {
            purchaseList.forEach {
                if (it.token != null) handlePurchase(it.token!!)
            }
            true
        } else {
            false
        }
    }

    override suspend fun getPurchaseHistory(): MutableList<PurchaseHistoryRecord> {
        return ClientController.queryPurchaseHistory(BillingClient.ProductType.SUBS)
    }

    override suspend fun getPurchaseHistory2OrderInfo(): List<OrderInfo> {
        return ClientController.queryPurchaseHistory(BillingClient.ProductType.SUBS).map { OrderInfo().createOrderInfo4His(it) }
    }

    override suspend fun hasDiscount(goods: Goods): Boolean {
        if (goods.offerId.isNotEmpty()) {
            val productDetails = ClientController.queryProductDetails(goods.productId, BillingClient.ProductType.SUBS)
            productDetails?.subscriptionOfferDetails?.forEach { det ->
                if (det.basePlanId == goods.planId) {
                    if (det.offerId == goods.offerId) {
                        return true
                    }
                }
            }
        }
        return false
    }
}