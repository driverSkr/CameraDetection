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
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

class SubscribeSkuImpl : GPayImpl {

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
                                list.add(OrderInfo().createSkuOrderInfo(purchase))
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
                                    list.add(OrderInfo().createSkuOrderInfo(purchase))
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
            callback.onDisconnect()
            return
        }
        val result = ClientController.querySkuDetails(goods.skuId, BillingClient.SkuType.SUBS)
        val billingResult = result?.billingResult
        val skuDetailsList = result?.skuDetailsList
        if (billingResult?.responseCode == BillingClient.BillingResponseCode.OK && skuDetailsList != null) {
            for (skuDetail in skuDetailsList) {
                if (skuDetail.sku == goods.skuId) {
                    val flowParams = BillingFlowParams.newBuilder().setSkuDetails(skuDetail).build()
                    withContext(Dispatchers.Main) {
                        val responseCode = ClientController.client?.launchBillingFlow(activity, flowParams)?.responseCode
                        if (responseCode != 0) {
                            callback.onFailed("not support")
                        }
                    }
                    break
                }
            }
        }
    }

    override suspend fun getGoodsPrice(context: Context, goods: Goods): Array<String?> {
        val array = arrayOfNulls<String>(2)
        if (!ClientController.isConnect(context)) {
            return array
        }
        val result = ClientController.querySkuDetails(goods.skuId, BillingClient.SkuType.SUBS)
        val billingResult = result?.billingResult
        val skuDetailsList = result?.skuDetailsList
        if (billingResult?.responseCode == BillingClient.BillingResponseCode.OK && skuDetailsList != null) {
            if (skuDetailsList.isNotEmpty()) {
                for (skuDetail in skuDetailsList) {
                    if (skuDetail.sku == goods.skuId) {
                        Log.e("TAG", "getGoodsPrice: " + skuDetail.price)
                        val price = DecimalFormat("0.00", DecimalFormatSymbols.getInstance(Locale.US)).format(skuDetail.priceAmountMicros.toDouble() / 10000.00 / 100.00)
                        val currency = skuDetail.priceCurrencyCode
                        array[0] = if (price.contains(",")) price.replace(",", ".") else price // 替换部分国家的浮点数逗号为标准小数点
                        array[1] = currency
                        return array
                    }
                }
            }
        }
        return array
    }

    override suspend fun getGoodsFormattedPrice(context: Context, goods: Goods): String {
        if (!ClientController.isConnect(context)) {
            return ""
        }
        val result = ClientController.querySkuDetails(goods.skuId, BillingClient.SkuType.SUBS)
        val billingResult = result?.billingResult
        val skuDetailsList = result?.skuDetailsList
        if (billingResult?.responseCode == BillingClient.BillingResponseCode.OK && skuDetailsList != null) {
            if (skuDetailsList.isNotEmpty()) {
                for (skuDetail in skuDetailsList) {
                    if (skuDetail.sku == goods.skuId) {
                        return skuDetail.price
                    }
                }
            }
        }
        return ""
    }

    override suspend fun getDiscountPrice(context: Context, goods: Goods): Array<String?> {
        val array = arrayOfNulls<String>(2)
        if (!ClientController.isConnect(context)) {
            return array
        }
        val result = ClientController.querySkuDetails(goods.skuId, BillingClient.SkuType.SUBS)
        val billingResult = result?.billingResult
        val skuDetailsList = result?.skuDetailsList
        if (billingResult?.responseCode == BillingClient.BillingResponseCode.OK && skuDetailsList != null) {
            if (skuDetailsList.isNotEmpty()) {
                for (skuDetail in skuDetailsList) {
                    if (skuDetail.sku == goods.skuId) {
                        val price = DecimalFormat("0.00", DecimalFormatSymbols.getInstance(Locale.US)).format(skuDetail.introductoryPriceAmountMicros.toDouble() / 10000.00 / 100.00)
                        val currency = skuDetail.priceCurrencyCode
                        array[0] = if (price.contains(",")) price.replace(",", ".") else price
                        array[1] = currency
                        return array
                    }
                }
            }
        }
        return array
    }

    override suspend fun getOrderInfo(context: Context, goods: Goods): OrderInfo? {
        if (!ClientController.isConnect(context)) {
            return null
        }
        val purchaseList = queryPurchase()
        purchaseList.forEach {
            if (it.goodsId == goods.skuId) {
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
            if (it.goodsId == goods.skuId) {
                return PurchaseState.GPAY
            }
        }
        return PurchaseState.NOTPAY
    }

    override suspend fun handlePurchase(purchaseToken: String): Boolean {
        val acknowledgePurchaseParams = AcknowledgePurchaseParams.newBuilder().setPurchaseToken(purchaseToken).build() // 确认购买，这里是针对不消耗型商品
        val billingResult = ClientController.client?.acknowledgePurchase(acknowledgePurchaseParams)
        return billingResult?.responseCode == BillingClient.BillingResponseCode.OK
    }

    override suspend fun queryPurchase(): MutableList<OrderInfo> {
        return ClientController.querySkuPurchase(BillingClient.SkuType.SUBS)
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
        return ClientController.queryPurchaseSkuHistory(BillingClient.SkuType.SUBS)
    }

    override suspend fun getPurchaseHistory2OrderInfo(): List<OrderInfo> {
        val skuHistory = ClientController.queryPurchaseSkuHistory(BillingClient.SkuType.SUBS)
        return skuHistory.map { OrderInfo().createOrderInfo4His(it) }
    }

    override suspend fun hasDiscount(goods: Goods): Boolean {
        return true // sku模式无法查询到是否有免费试订的资格，全部以有资格返回
    }
}