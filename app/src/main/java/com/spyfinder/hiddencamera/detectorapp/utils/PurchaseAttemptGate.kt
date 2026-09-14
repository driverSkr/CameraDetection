package com.spyfinder.hiddencamera.detectorapp.utils

/** A delayed callback from a previous attempt must not navigate or change the current purchase. */
class PurchaseAttemptGate {
    private var current = 0
    private var completed = -1
    @Synchronized fun begin(): Int = ++current
    @Synchronized fun accepts(id: Int) = id == current && completed != id
    @Synchronized fun complete(id: Int): Boolean {
        if (!accepts(id)) return false
        completed = id
        return true
    }
}
