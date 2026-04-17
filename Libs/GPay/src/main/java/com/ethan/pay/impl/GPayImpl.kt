package com.ethan.pay.impl

import android.app.Activity
import android.content.Context
import com.android.billingclient.api.PurchaseHistoryRecord
import com.ethan.pay.model.Goods
import com.ethan.pay.model.OnPayResultCallback
import com.ethan.pay.model.OrderInfo
import com.ethan.pay.model.PurchaseState

interface GPayImpl {

    suspend fun launchBilling(activity: Activity, goods: Goods, callback: OnPayResultCallback)

    suspend fun getGoodsPrice(context: Context, goods: Goods): Array<String?>


    suspend fun getGoodsFormattedPrice(context: Context, goods: Goods): String

    suspend fun getDiscountPrice(context: Context, goods: Goods): Array<String?>

    suspend fun getOrderInfo(context: Context, goods: Goods): OrderInfo?

    suspend fun getPurchaseState(context: Context, goods: Goods): PurchaseState

    suspend fun handlePurchase(purchaseToken: String): Boolean

    suspend fun queryPurchase(): MutableList<OrderInfo>

    suspend fun confirmPlan(context: Context): Boolean

    suspend fun getPurchaseHistory(): MutableList<PurchaseHistoryRecord>

    suspend fun getPurchaseHistory2OrderInfo(): List<OrderInfo>

    suspend fun hasDiscount(goods: Goods): Boolean
}