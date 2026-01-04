package com.ethan.base.widget.progress

import android.os.Handler

interface ProgressSupport {
    fun isDestroy(): Boolean
    fun getUIHandler(): Handler
}