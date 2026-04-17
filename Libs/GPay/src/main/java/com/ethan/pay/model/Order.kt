package com.ethan.pay.model

import androidx.annotation.Keep
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchaseHistoryRecord


@Keep
class OrderInfo {
    var orderId: String? = null
    var goodsId: String? = null
    var token: String? = null
    var signature: String? = null
    var json: String? = null

    fun createOrderInfo(purchase: Purchase): OrderInfo {
        this.orderId = purchase.orderId
        this.goodsId = if (purchase.products.isNotEmpty()) purchase.products[0] else null
        this.token = purchase.purchaseToken
        this.signature = purchase.signature
        this.json = purchase.originalJson
        return this
    }

    /**
     * 历史记录是无法查出orderID的
     */
    fun createOrderInfo4His(purchase: PurchaseHistoryRecord): OrderInfo {
        this.orderId = ""
        this.goodsId = if (purchase.products.isNotEmpty()) purchase.products[0] else null
        this.token = purchase.purchaseToken
        this.signature = purchase.signature
        this.json = purchase.originalJson
        return this
    }

    fun createSkuOrderInfo(purchase: Purchase): OrderInfo {
        this.orderId = purchase.orderId
        this.goodsId = if (purchase.skus.isNotEmpty()) purchase.skus[0] else null
        this.token = purchase.purchaseToken
        this.signature = purchase.signature
        this.json = purchase.originalJson
        return this
    }
}

@Keep
data class Receipt(var receipt: String?, var signature: String?)