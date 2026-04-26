package com.spyfinder.hiddencamera.detectorapp.ui.launch

import android.app.Activity
import android.os.Bundle
import android.util.Log
import com.spyfinder.hiddencamera.detectorapp.ui.guide.GuideActivity
import com.spyfinder.hiddencamera.detectorapp.ui.main.MainActivity
import com.spyfinder.hiddencamera.detectorapp.utils.DataHelper

class LaunchActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        routeToTargetPage()
    }

    private fun routeToTargetPage() {
        try {
            val isFirstOpen = DataHelper.isFirst(this, KEY_FIRST_OPEN)
            if (isFirstOpen) {
                Log.d(TAG, "First open, route to guide page.")
                GuideActivity.launch(this, true)
            } else {
                Log.d(TAG, "Cold start, route to main page.")
                MainActivity.launch(this, checkSubscribeOnLaunch = true)
            }
        } catch (throwable: Throwable) {
            Log.e(TAG, "Route failed, fallback to main page.", throwable)
            MainActivity.launch(this, checkSubscribeOnLaunch = true)
        } finally {
            finish()
        }
    }

    companion object {
        private const val TAG = "LaunchActivity"
        private const val KEY_FIRST_OPEN = "open"
    }
}
