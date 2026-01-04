package com.ethan.base.widget.progress

import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.graphics.Point
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.TextView
import com.ethan.base.R

class CustomProgressDialog(context: Activity) : Dialog(context, R.style.custom_dialog) {
    private var progressMessage: TextView? = null
    private var cancelable = false

    init {
        setContentView(R.layout.view_progress_dialog)
        val outerView = findViewById<View>(R.id.rl_content_view)
        outerView.setOnClickListener {
            if (cancelable) {
                cancel()
            }
        }
        val progress = findViewById<LoadingView>(R.id.pb_progress)
        progress.start()
        progressMessage = findViewById(R.id.tv_message)
    }

    fun resetContentViewMargin(left: Int, top: Int, right: Int, bottom: Int) {
        val screenWidth = getScreenWidth(context)
        val screenHeight = getScreenHeight(context)
        val attr = window!!.attributes
        attr.gravity = Gravity.NO_GRAVITY
        attr.width = screenWidth - left - right
        attr.height = screenHeight - top - bottom
        attr.x = left
        attr.y = top
    }

    fun setMessage(message: String?) {
        progressMessage!!.text = message
    }

    fun setMessage(message: Int) {
        setMessage(context.getString(message))
    }

    override fun setCancelable(cancelable: Boolean) {
        this.cancelable = cancelable
    }

    private fun getScreenWidth(context: Context): Int {
        val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val point = Point()
        wm.defaultDisplay.getRealSize(point)
        return point.x
    }

    private fun getScreenHeight(context: Context): Int {
        val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val point = Point()
        wm.defaultDisplay.getRealSize(point)
        return point.y
    }
}