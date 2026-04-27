package com.ethan.firebaseAnalytics.attribution

import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.util.Log
import com.ethan.firebaseAnalytics.analytics.FirebaseEvent
import com.ethan.firebaseAnalytics.analytics.Name
import com.android.installreferrer.api.InstallReferrerClient
import com.android.installreferrer.api.InstallReferrerStateListener

object ReferrerSeeker {
    private var referrerClient: InstallReferrerClient? = null

    fun find(context: Context, callback: OnReferrerCallback) {
        referrerClient = InstallReferrerClient.newBuilder(context).build()
        referrerClient?.startConnection(object : InstallReferrerStateListener {
            override fun onInstallReferrerSetupFinished(responseCode: Int) {
                if (responseCode == InstallReferrerClient.InstallReferrerResponse.OK && referrerClient?.isReady == true) {
                    try {
                        val response = referrerClient?.installReferrer
                        val referrerUrl = response?.installReferrer
                        val referrerClickTime = response?.referrerClickTimestampSeconds
                        val appInstallTime = response?.installBeginTimestampSeconds
                        val instantExperienceLaunched = response?.googlePlayInstantParam

                        val url = if (referrerUrl.isNullOrEmpty() || !referrerUrl.startsWith("http")) "http://test.com?$referrerUrl" else referrerUrl
                        val uri = Uri.parse(url)
                        val source = uri.getQueryParameter("utm_source")
                        val medium = uri.getQueryParameter("utm_medium")
                        val term = uri.getQueryParameter("utm_term")
                        val content = uri.getQueryParameter("utm_content")
                        val campaign = uri.getQueryParameter("utm_campaign")
                        val anid = uri.getQueryParameter("anid")
                        val referrer = Referrer(referrerUrl, source, medium, term, content, campaign, anid, referrerClickTime, appInstallTime, instantExperienceLaunched)
                        callback.onSuccess(referrer)
                        disconnect()
                    } catch (e: Exception) {
                        FirebaseEvent.event(context, Name.ReferrerClientError, Bundle().apply {
                            putString("Source", e.message?.substring(0,100))
                        })
                        Log.e("Firebase", "ReferrerClientError error:${e.message}")
                    }
                } else {
                    callback.onFailure(responseCode.toString())
                }
            }

            override fun onInstallReferrerServiceDisconnected() {
                callback.onFailure(InstallReferrerClient.InstallReferrerResponse.SERVICE_DISCONNECTED.toString())
            }
        })
    }

    private fun disconnect() {
        referrerClient?.endConnection()
    }
}