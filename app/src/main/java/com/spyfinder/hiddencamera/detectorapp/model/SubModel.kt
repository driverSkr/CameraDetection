package com.spyfinder.hiddencamera.detectorapp.model

import androidx.annotation.Keep

@Keep
class SubModel {
    var goods: String? = null // 商品id
    var id: String? = null // 基础方案id
    var offerId: String? = null // 优惠id
    var sku: String? = null // sku id
    var price: String? = null
    var offerprice: String? = null

    // var formattedPrice: String? = null
    var currency: String? = null
    var isFreeTrial: Boolean = false
}