package com.ethan.base.dialog

import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.annotation.LayoutRes
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager

open class BaseDialog : AbsDialogFragment(), View.OnClickListener {
    private val KEY_CONTROLLER = "Controller"
    protected var mController = Controller()
    private var onActivityResult: OnActivityResult? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (null != savedInstanceState) {
            mController = (savedInstanceState.getParcelable<Controller>(KEY_CONTROLLER))!!
        }
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        dialog!!.window!!.attributes.windowAnimations = 0
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putParcelable(KEY_CONTROLLER, mController)
        super.onSaveInstanceState(outState)
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        if (mController.getOnDismissListener() != null) {
            mController.getOnDismissListener()!!.onDismiss(dialog)
        }
    }

    override fun show(): BaseDialog? {
        try {
            mController.getFragmentManager()!!
                .beginTransaction()
                .add(this, mController.getTag())
                .commitAllowingStateLoss()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return this
    }

    override fun dismiss() {
        dismissAllowingStateLoss()
    }

    override fun getGravity(): Int {
        return mController.getGravity()
    }

    override fun getDimAmount(): Float {
        return mController.getDimAmount()
    }

    override fun getDialogHeight(): Int {
        return mController.getHeight()
    }

    override fun getDialogWidth(): Int {
        return mController.getWidth()
    }

    override fun getFragmentTag(): String? {
        return mController.getTag()
    }

    override fun onClick(view: View?) {
        mController.getOnViewClickListener()!!.onViewClick(view, this)
    }


    override fun getLayoutRes(): Int {
        return mController.getLayoutRes()
    }

    override fun getDialogView(): View? {
        return mController.getDialogView()
    }

    public override fun setCancelableOutside(b: Boolean) {
        mController.setCancelableOutside(b)
    }

    override fun isCancelableOutside(): Boolean {
        return mController.isCancelableOutside()
    }

    override fun getDialogAnimationRes(): Int {
        return mController.getDialogAnimationRes()
    }

    class Builder {
        var params: Controller.Params

        constructor(activity: FragmentActivity) {
            params = Controller.Params()
            params.mFragmentManager = activity.supportFragmentManager
        }

        constructor(fragmentManager: FragmentManager?) {
            params = Controller.Params()
            params.mFragmentManager = fragmentManager
        }

        fun setLayoutRes(@LayoutRes layoutRes: Int): Builder {
            params.mLayoutRes = layoutRes
            return this
        }

        fun setView(view: View?): Builder {
            params.mDialogView = view
            return this
        }

        fun setWidth(widthPx: Int): Builder {
            params.mWidth = widthPx
            return this
        }

        fun setHeight(heightPx: Int): Builder {
            params.mHeight = heightPx
            return this
        }

        /**
         * 设置弹窗宽度是屏幕宽度的比例 0 -1
         */
        fun setScreenWidthAspect(context: Context?, widthAspect: Float): Builder {
            params.mWidth = ((context!!.resources.displayMetrics.widthPixels * widthAspect).toInt())
            return this
        }

        /**
         * 设置弹窗高度是屏幕高度的比例 0 -1
         */
        fun setScreenHeightAspect(context: Context?, heightAspect: Float): Builder {
            params.mHeight = ((context!!.resources.displayMetrics.heightPixels * heightAspect).toInt())
            return this
        }

        fun setGravity(gravity: Int): Builder {
            params.mGravity = gravity
            return this
        }

        fun setCancelableOutside(cancel: Boolean): Builder {
            params.mIsCancelableOutside = cancel
            return this
        }

        fun setOnDismissListener(dismissListener: DialogInterface.OnDismissListener?): Builder {
            params.mOnDismissListener = dismissListener
            return this
        }

        /**
         * 设置弹窗背景透明度(0-1f)
         *
         * @param dim
         * @return
         */
        fun setDimAmount(dim: Float): Builder {
            params.mDimAmount = dim
            return this
        }

        fun setTag(tag: String?): Builder {
            params.mTag = tag!!
            return this
        }

        fun addOnClickListener(vararg ids: Int): Builder {
            params.ids = ids
            return this
        }

        fun setOnViewClickListener(listener: OnViewClickListener?): Builder {
            params.mOnViewClickListener = listener
            return this
        }

        fun setDialogAnimationRes(animationRes: Int): Builder {
            params.mDialogAnimationRes = animationRes
            return this
        }

        fun create(): BaseDialog {
            val dialog = BaseDialog()
            params.apply(dialog.mController)
            return dialog
        }
    }

    override fun bindView(view: View?) {
        if (null != mController.getIds() && mController.getIds()!!.isNotEmpty()) {
            for (viewId in mController.getIds()!!) {
                view?.isClickable = true
                view?.findViewById<View>(viewId)?.setOnClickListener(this)
            }
        }
    }

    open fun setOnActivityResult(callback: OnActivityResult?) {
        onActivityResult = callback
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (onActivityResult != null) {
            onActivityResult!!.onActivityResult(this, requestCode, resultCode, data)
        }
    }

    interface OnActivityResult {
        fun onActivityResult(dialog: BaseDialog?, requestCode: Int, resultCode: Int, data: Intent?)
    }
}