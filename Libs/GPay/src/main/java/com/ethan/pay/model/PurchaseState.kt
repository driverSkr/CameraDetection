package com.ethan.pay.model


enum class PurchaseState(val value: Int) {
    GPAY(1), DISCONNECT(2), NOTPAY(3)
}