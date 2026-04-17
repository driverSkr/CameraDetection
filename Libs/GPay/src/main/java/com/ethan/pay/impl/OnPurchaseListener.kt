package com.ethan.pay.impl

import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.Purchase

interface OnPurchaseListener {
    fun onPurchase(result: BillingResult, purchases: List<Purchase>?)
}