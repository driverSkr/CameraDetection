package com.ethan.base.widget.toast

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.drawable.Drawable
import android.os.Handler
import android.os.Looper
import android.text.TextUtils
import android.util.Log
import android.widget.Toast
import androidx.annotation.StringRes
import java.lang.ref.WeakReference

/**
 * Author: loren
 * Date: 2021/4/21
 */
class BaseToast {
    init {
        throw Exception()
    }

    companion object {
        private const val Tag = "BaseToast"
        private var sToastRef: WeakReference<Toast>? = null

        fun showToast(context: Context?, @StringRes resId: Int) {
            if (context == null) {
                return
            }
            val msg = context.resources.getString(resId)
            showToast(context, msg, false)
        }

        fun showToast(context: Context?, @StringRes resId: Int, lengthLong: Boolean) {
            var context = context
            if (context == null) {
                return
            }
            context = context.applicationContext
            val msg = context.resources.getString(resId)
            showToast(context, msg, lengthLong)
        }

        fun showToast(context: Context?, msg: String) {
            var context = context
            if (context == null) {
                return
            }
            context = context.applicationContext
            showToast(context, msg, false)
        }

        fun showToast(context: Context?, msg: String, lengthLong: Boolean) {
            if (context == null) {
                return
            }
            toastDefault(context, msg, lengthLong)
        }

        fun showToast(context: Context?, msg: String, leftIcon: Drawable?) {
            if (context == null) {
                return
            }
            showToastInternal(context, msg, ToastLevel.DEFAULT, false, leftIcon)
        }

        fun toastDefault(context: Context, strMsg: String, toastLong: Boolean) {
            showToastInternal(context, strMsg, ToastLevel.DEFAULT, toastLong)
        }

        fun toastDefault(context: Context, @StringRes strResId: Int, toastLong: Boolean) {
            toastDefault(context, context.resources.getString(strResId), toastLong)
        }

        fun toastLoading(context: Context, @StringRes strResId: Int, toastLong: Boolean) {
            toastLoading(context, context.resources.getString(strResId), toastLong)
        }

        fun toastLoading(context: Context, strMsg: String, toastLong: Boolean) {
            showToastInternal(context, strMsg, ToastLevel.LOADING, toastLong)
        }

        fun toastSuccess(context: Context, @StringRes strResId: Int, toastLong: Boolean) {
            toastSuccess(context, context.resources.getString(strResId), toastLong)
        }

        fun toastSuccess(context: Context, strMsg: String, toastLong: Boolean) {
            showToastInternal(context, strMsg, ToastLevel.SUCCESS, toastLong)
        }

        fun toastSuccess(context: Context, strMsg: String) {
            showToastInternal(context, strMsg, ToastLevel.SUCCESS, false)
        }

        fun toastSuccess(context: Context, @StringRes strResId: Int) {
            showToastInternal(context, context.resources.getString(strResId), ToastLevel.SUCCESS, false)
        }

        fun toastException(context: Context, @StringRes strResId: Int, toastLong: Boolean) {
            toastException(context, context.resources.getString(strResId), toastLong)
        }

        fun toastException(context: Context, strMsg: String, toastLong: Boolean) {
            showToastInternal(context, strMsg, ToastLevel.EXCEPTION, toastLong)
        }

        fun toastException(context: Context, @StringRes strResId: Int) {
            showToastInternal(context, context.resources.getString(strResId), ToastLevel.EXCEPTION, false)
        }

        fun toastException(context: Context, strMsg: String) {
            showToastInternal(context, strMsg, ToastLevel.EXCEPTION, false)
        }

        private fun showToastInternal(context: Context, strMsg: String, toastLevel: Int,
                                      toastLong: Boolean) {
            showToastInternal(context, strMsg, toastLevel, toastLong, null)
        }

        @SuppressLint("MissingPermission")
        private fun showToastInternal(context: Context?, msg: String, toastLevel: Int,
                                      toastLong: Boolean, leftIcon: Drawable?) {
            if (context == null || TextUtils.isEmpty(msg)) {
                Log.w(Tag, logWarnMsg(toastLevel, context, msg)!!)
                return
            }
            Handler(Looper.getMainLooper()).post {
                cancelToast()
                val toast = Toast.makeText(context, msg, Toast.LENGTH_SHORT)
                //                Toast toast = new Toast(context);
                //                toast.setGravity(Gravity.CENTER, 0, 0);
                //                toast.setDuration(toastLong ? Toast.LENGTH_LONG : Toast.LENGTH_SHORT);
                //                toast.setText(msg);
                toast.show()
                sToastRef = WeakReference(toast)
            }
        }

        fun cancelToast() {
            val ref: WeakReference<Toast> = sToastRef ?: return
            val toast = ref.get() ?: return
            toast.cancel()
            ref.clear()
        }

        private fun logWarnMsg(toastLevel: Int, context: Context?, msg: String): String? {
            return "Toast level = $toastLevel [ context = $context , msg = $msg ]"
        }
    }
}