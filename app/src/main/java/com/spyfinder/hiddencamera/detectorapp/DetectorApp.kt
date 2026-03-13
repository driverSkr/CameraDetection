package com.spyfinder.hiddencamera.detectorapp

import android.content.Context
import androidx.multidex.MultiDex
import com.ethan.base.component.BaseApp

class DetectorApp: BaseApp() {
    companion object {
        var INSTANCE: DetectorApp? = null
            private set
    }

    override fun initLibs() {
        INSTANCE = this
    }

    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(base)
        MultiDex.install(base.applicationContext)
    }
}