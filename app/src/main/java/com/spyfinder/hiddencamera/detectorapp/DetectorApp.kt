package com.spyfinder.hiddencamera.detectorapp

import android.content.Context
import androidx.multidex.MultiDex
import com.ethan.base.component.BaseApp
import com.spyfinder.hiddencamera.detectorapp.event.Event
import com.spyfinder.hiddencamera.detectorapp.utils.SubscribeHelper

class DetectorApp: BaseApp() {
    companion object {
        var INSTANCE: DetectorApp? = null
            private set
    }

    override fun initLibs() {
        INSTANCE = this
        // 应用启动后立即初始化订阅状态，保证各页面拿到的是全局同一份数据。
        SubscribeHelper.init(this)
        // 初始化 Firebase 归因与 app_open 基础埋点。
        Event.start(this)
    }

    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(base)
        MultiDex.install(base.applicationContext)
    }
}
