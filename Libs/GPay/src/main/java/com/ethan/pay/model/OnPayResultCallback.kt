package com.ethan.pay.model

interface OnPayResultCallback {
    fun begin()
    fun onSuccess(orderList: MutableList<OrderInfo>)
    fun onOwned(orderList: MutableList<OrderInfo>)
    fun onFailed(msg: String?)
    fun onDisconnect()
    fun onCancel()
    fun onPending() {}
    fun onPriceChanged() { onFailed("Price changed. Please review the updated price.") }
}
