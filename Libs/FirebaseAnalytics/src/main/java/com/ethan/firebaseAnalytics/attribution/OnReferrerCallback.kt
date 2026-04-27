package com.ethan.firebaseAnalytics.attribution

interface OnReferrerCallback {
    fun onSuccess(referrer: Referrer)
    fun onFailure(msg: String?)
}