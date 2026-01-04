package com.ethan.base.component

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import androidx.annotation.StringRes
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import com.ethan.base.R
import com.ethan.base.widget.progress.ProgressSupport
import com.ethan.base.widget.toast.BaseToast

open class BaseFragment : Fragment(), ProgressSupport {

    private val handler = Handler(Looper.getMainLooper())
    var mActivity: FragmentActivity? = null

    open fun onCreateWrapView(childView: View): View {
        val rootView = LayoutInflater.from(context).inflate(R.layout.fragment_base_state, null)
        val fullContentLayout = rootView.findViewById<FrameLayout>(R.id.full_content_layout)
        fullContentLayout.addView(childView)
        return rootView
    }

    override fun onDetach() {
        super.onDetach()
        if (mActivity is BaseActivity) {
            (mActivity as BaseActivity).dismissLoadingDialog()

        }
        mActivity = null
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        mActivity = context as FragmentActivity
    }

    fun showProgressDialog(@StringRes resId: Int) {
        if (isDestroy()) {
            return
        }
        val content = getString(resId)
        showProgressDialog(content)
    }

    fun showDialogNoBg() {
        if (isDestroy()) {
            return
        }
        if (mActivity is BaseActivity) {
            (mActivity as BaseActivity).showLoadingNoBg()
        }
    }

    fun showProgressDialog(content: String) {
        if (isDestroy()) {
            return
        }
        if (mActivity is BaseActivity) {
            (mActivity as BaseActivity).showLoadingWithBg(content)
        }
    }

    fun dismissProgress() {
        if (isDestroy()) {
            return
        }
        if (mActivity is BaseActivity) {
            (mActivity as BaseActivity).dismissLoadingDialog()
        }
    }

    fun showToast(@StringRes resId: Int) {
        try {
            val content = getString(resId)
            showToast(content)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun showToast(content: String?) {
        if (isDestroy()) {
            return
        }
        content?.let {
            BaseToast.showToast(activity, it)
        }
    }

    override fun isDestroy(): Boolean {
        val act = activity
        if (act != null) {
            return act.isFinishing || act.isDestroyed
        }
        if (context != null) {
            return !isAdded
        }
        return true
    }

    override fun getUIHandler(): Handler {
        return handler
    }
}