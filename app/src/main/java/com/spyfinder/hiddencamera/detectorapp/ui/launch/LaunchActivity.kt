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
            // 启动页不展示 UI，只根据首次打开标记分发到引导页或主页。
            if (isFirstOpen) {
                Log.d(TAG, "首次打开应用，跳转引导页")
                GuideActivity.launch(this, true)
            } else {
                Log.d(TAG, "非首次打开应用，跳转主页")
                MainActivity.launch(this)
            }
        } catch (throwable: Throwable) {
            // 分发异常时兜底进入主页，避免启动页卡住影响用户进入应用。
            Log.e(TAG, "启动导航分发失败，兜底跳转主页", throwable)
            MainActivity.launch(this)
        } finally {
            finish()
        }
    }

    companion object {
        private const val TAG = "LaunchActivity"
        private const val KEY_FIRST_OPEN = "open"
    }
}
