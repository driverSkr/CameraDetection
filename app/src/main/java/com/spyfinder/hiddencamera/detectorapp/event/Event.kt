package com.spyfinder.hiddencamera.detectorapp.event

import android.content.Context
import android.os.Bundle
import com.ethan.firebaseAnalytics.analytics.FirebaseEvent
import com.ethan.firebaseAnalytics.attribution.OnReferrerCallback
import com.ethan.firebaseAnalytics.attribution.Referrer
import com.ethan.firebaseAnalytics.attribution.ReferrerSeeker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

object Event {

    fun start(context: Context) {
        ReferrerSeeker.find(context, object : OnReferrerCallback {
            override fun onSuccess(referrer: Referrer) {
                FirebaseEvent.open(context, referrer)
            }

            override fun onFailure(msg: String?) {
                FirebaseEvent.open(context, null)
            }
        })
    }

    fun event(context: Context, event: String, value: String) {
        CoroutineScope(Dispatchers.Default).launch {
            FirebaseEvent.event(context, event, value)
        }
    }

    fun event(context: Context, event: String, bundle: Bundle) {
        CoroutineScope(Dispatchers.Default).launch {
            val bundle2 = Bundle(bundle)
            FirebaseEvent.event(context, event, bundle2)

            var string = ""
            bundle.keySet().forEach { key ->
                string = string + "${key}:${bundle.get(key)},"
            }
        }
    }
}