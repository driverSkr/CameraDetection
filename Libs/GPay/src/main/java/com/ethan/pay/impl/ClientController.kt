package com.ethan.pay.impl

import android.content.Context
import android.util.Log
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchaseHistoryRecord
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchaseHistoryParams
import com.android.billingclient.api.QueryPurchasesParams
import com.android.billingclient.api.SkuDetailsParams
import com.android.billingclient.api.SkuDetailsResult
import com.android.billingclient.api.queryProductDetails
import com.android.billingclient.api.queryPurchaseHistory
import com.android.billingclient.api.queryPurchasesAsync
import com.android.billingclient.api.querySkuDetails
import com.ethan.pay.model.OrderInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale
import kotlin.coroutines.resume
import kotlin.math.min

/**
 * Author: loren
 * Date: 2023/1/5
 */
object ClientController {

    var client: BillingClient? = null
    private var onPurchaseListener: OnPurchaseListener? = null

    var globalSuccessCallBack: (orderList: MutableList<OrderInfo>, type: PurchaseType) -> Unit = { _, _ ->
        Log.i("TAG", "globalSuccessCallBack : def callback")
    }

    suspend fun connect(context: Context): Int {
        return suspendCancellableCoroutine { continuation ->
            if (client == null) {
                client = BillingClient.newBuilder(context).setListener { result, purchases ->
                    onPurchaseListener?.onPurchase(result, purchases)
                }.enablePendingPurchases().build()
            }
            client?.startConnection(object : BillingClientStateListener {
                override fun onBillingSetupFinished(billingResult: BillingResult) {
                    if (continuation.isActive) continuation.resume(billingResult.responseCode)
                }

                override fun onBillingServiceDisconnected() {
                    if (continuation.isActive) continuation.resume(-1)
                }
            })
        }
    }

    suspend fun isConnect(context: Context): Boolean {
        if (client == null || client?.isReady == false) {
            val i = connect(context)
            return i == BillingClient.BillingResponseCode.OK
        }
        return true
    }

    fun setOnPurchaseListener(listener: OnPurchaseListener) {
        onPurchaseListener = listener
    }

    fun isSupport(): Boolean {
        val result = client?.isFeatureSupported(BillingClient.FeatureType.PRODUCT_DETAILS)
        return result?.responseCode != BillingClient.BillingResponseCode.FEATURE_NOT_SUPPORTED
    }

    suspend fun queryProductDetails(goodsId: String, productType: String): ProductDetails? { // 新版本product模式
        val productList = mutableListOf<QueryProductDetailsParams.Product>()
        val product = QueryProductDetailsParams.Product.newBuilder()
        product.setProductId(goodsId)
        product.setProductType(productType)
        productList.add(product.build())
        val params = QueryProductDetailsParams.newBuilder()
        params.setProductList(productList)
        val productDetailsResult = withContext(Dispatchers.IO) {
            client?.queryProductDetails(params.build())
        }
        productDetailsResult?.productDetailsList?.forEach {
            if (it.productId == goodsId) {
                return it
            }
        }
        return null
    }

    suspend fun queryPurchase(productType: String): MutableList<OrderInfo> { // 新版本product模式
        val params = QueryPurchasesParams.newBuilder().setProductType(productType)
        val purchasesResult = client?.queryPurchasesAsync(params.build())
        val list = mutableListOf<OrderInfo>()

        Log.d("queryPurchase", (purchasesResult?.purchasesList?.size?:"none").toString())
        purchasesResult?.purchasesList?.forEach {
            if (it.purchaseState == Purchase.PurchaseState.PURCHASED) {
                list.add(OrderInfo().createOrderInfo(it))
            }
        }
        return list
    }

    suspend fun queryPurchaseHistory(productType: String): MutableList<PurchaseHistoryRecord> { // 新版本product模式
        val params = QueryPurchaseHistoryParams.newBuilder().setProductType(productType)
        val list = mutableListOf<PurchaseHistoryRecord>()
        val queryPurchaseHistory = client?.queryPurchaseHistory(params.build())
        queryPurchaseHistory?.purchaseHistoryRecordList?.forEach {
            list.add(it)
        }
        return list
    }

    suspend fun queryPurchaseSkuHistory(productType: String): MutableList<PurchaseHistoryRecord> { // 老版本sku模式
        val list = mutableListOf<PurchaseHistoryRecord>()
        val queryPurchaseHistory = client?.queryPurchaseHistory(productType)
        queryPurchaseHistory?.purchaseHistoryRecordList.let {
            it?.let { it1 -> list.addAll(it1) }
        }
        return list
    }

    suspend fun querySkuDetails(goodsId: String, skuType: String): SkuDetailsResult? { // 老版本sku模式
        val skuList: MutableList<String> = ArrayList()
        skuList.add(goodsId)
        val params = SkuDetailsParams.newBuilder()
        params.setSkusList(skuList).setType(skuType)
        return client?.querySkuDetails(params.build())
    }

    suspend fun querySkuPurchase(skuType: String): MutableList<OrderInfo> { // 老版本sku模式
        val purchaseResult = client?.queryPurchasesAsync(skuType)
        val result = purchaseResult?.billingResult
        val purchaseList = purchaseResult?.purchasesList
        val list = mutableListOf<OrderInfo>()
        if (result?.responseCode == BillingClient.BillingResponseCode.OK && purchaseList != null) {
            for (p in purchaseList) {
                if (p.purchaseState == Purchase.PurchaseState.PURCHASED) {
                    list.add(OrderInfo().createSkuOrderInfo(p))
                }
            }
        }
        return list
    }

    fun querySubProductPrice(details: ProductDetails, planId: String, offerId: String): String {
        val list = details.subscriptionOfferDetails
        if (!list.isNullOrEmpty()) {
            list.forEach { offer ->
                if (offer.basePlanId == planId) {
                    if (offerId.isNotEmpty()) { // 优惠价
                        if (offer.offerId == offerId) {
                            val phaseList = offer.pricingPhases.pricingPhaseList
                            var micros = if (phaseList.size >= 1) phaseList[0].priceAmountMicros else 0L
                            phaseList.forEach { phase ->
                                micros = min(micros, phase.priceAmountMicros) // 取最低价格
                            }
                            return DecimalFormat("0.00", DecimalFormatSymbols.getInstance(Locale.US)).format(micros.toDouble() / 10000.00 / 100.00)
                        }
                    }  // 否则原价
                    if (offer.offerId.isNullOrEmpty()) {
                        val phaseList = offer.pricingPhases.pricingPhaseList
                        var micros = if (phaseList.size >= 1) phaseList[0].priceAmountMicros else 0L
                        phaseList.forEach { phase ->
                            micros = min(micros, phase.priceAmountMicros) // 取最低价格
                            Log.e("TAG", "querySubProductPrice: " + phase.priceAmountMicros)
                            Log.e("TAG", "querySubProductPrice: " + phase.formattedPrice)
                            Log.e("TAG", "querySubProductPrice: " + phase.priceCurrencyCode)
                            Log.e("TAG", "querySubProductPrice: " + phase.billingPeriod)
                            Log.e("TAG", "querySubProductPrice: " + phase.recurrenceMode)
                            Log.e("TAG", "querySubProductPrice: " + phase.billingCycleCount)
                            return DecimalFormat("0.00", DecimalFormatSymbols.getInstance(Locale.US)).format(micros.toDouble() / 10000.00 / 100.00)
                        }
                    }
                }
            }
        }
        return DecimalFormat("0.00", DecimalFormatSymbols.getInstance(Locale.US)).format(0L / 10000.00 / 100.00)
    }

    fun querySubProductFormattedPrice(details: ProductDetails, planId: String, offerId: String): String {
        val list = details.subscriptionOfferDetails
        if (!list.isNullOrEmpty()) {
            list.forEach { offer ->
                if (offer.basePlanId == planId) {
                    if (offerId.isNotEmpty()) { // 优惠价
                        if (offer.offerId == offerId) {
                            val phaseList = offer.pricingPhases.pricingPhaseList
                            val phase = phaseList.minByOrNull { it.priceAmountMicros } // 取最低价格
                            return phase?.formattedPrice ?: ""
                        }
                    }  // 否则原价
                    if (offer.offerId.isNullOrEmpty()) {
                        val phaseList = offer.pricingPhases.pricingPhaseList
                        val phase = phaseList.minByOrNull { it.priceAmountMicros } // 取最低价格
                        return phase?.formattedPrice ?: ""
                    }
                }
            }
        }
        return ""
    }

    fun querySubProductCurrency(details: ProductDetails, planId: String): String {
        val list = details.subscriptionOfferDetails
        if (!list.isNullOrEmpty()) {
            list.forEach { offer ->
                if (offer.basePlanId == planId) {
                    return offer.pricingPhases.pricingPhaseList[0].priceCurrencyCode
                }
            }
        }
        return ""
    }

    fun querySubProductOfferToken(details: ProductDetails, planId: String, offerId: String): String {
        val list = details.subscriptionOfferDetails
        var token = ""
        if (!list.isNullOrEmpty()) {
            list.forEach { offer ->
                if (offer.basePlanId == planId) {
                    token = offer.offerToken
                    if (offerId.isNotEmpty() && offer.offerId == offerId) {
                        return token
                    }
                }
            }
        }
        return token
    }

    fun disconnect() {
        client?.endConnection()
    }
}

enum class PurchaseType { SUB, LIFE_TIME, ONE_TIME }