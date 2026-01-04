package com.ethan.base.progress

import android.os.Handler

interface ProgressSupport {
    fun isDestroy(): Boolean
    fun getUIHandler(): Handler
}