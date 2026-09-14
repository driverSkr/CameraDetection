package com.ethan.pay.model

data class Goods(val productId: String, val planId: String, val offerId: String, val skuId: String,
    val expectedPrice: String? = null, val expectedPeriod: String? = null)
