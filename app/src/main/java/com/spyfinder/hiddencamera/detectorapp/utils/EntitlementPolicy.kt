package com.spyfinder.hiddencamera.detectorapp.utils

enum class AccessStatus { UNKNOWN, ACTIVE, INACTIVE }

object EntitlementPolicy {
    fun resolve(subscription: Boolean?, lifetime: Boolean?): AccessStatus = when {
        subscription == true || lifetime == true -> AccessStatus.ACTIVE
        subscription == false && lifetime == false -> AccessStatus.INACTIVE
        else -> AccessStatus.UNKNOWN
    }
}
