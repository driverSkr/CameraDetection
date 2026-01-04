package com.ethan.base.dialog

import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.*
import androidx.appcompat.app.AppCompatDialogFragment

abstract class AbsDialogFragment : AppCompatDialogFragment() {

    private val DEFAULT_DIM_AMOUNT = 0.2f

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        var view: View? = null
        if (getLayoutRes() > 0) {
            view = inflater.inflate(getLayoutRes(), container, false)
        }
        if (view == null) {
            view = getDialogView()
        }
        bindView(view)
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        dialog!!.setCanceledOnTouchOutside(isCancelableOutside())
    }

    override fun onStart() {
        super.onStart()
        val window = dialog!!.window
        if (window != null) {
            window.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            val layoutParams = window.attributes
            layoutParams.width = if (getDialogWidth() > 0) getDialogWidth() else WindowManager.LayoutParams.WRAP_CONTENT
            layoutParams.height = if (getDialogHeight() > 0) getDialogHeight() else WindowManager.LayoutParams.WRAP_CONTENT
            layoutParams.dimAmount = getDimAmount()
            layoutParams.gravity = getGravity()
            window.attributes = layoutParams
            if (dialog!!.window != null && getDialogAnimationRes() > 0) {
                dialog!!.window!!.setWindowAnimations(getDialogAnimationRes())
            }
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)
        dialog.window!!.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
        return dialog
    }

    open fun getGravity(): Int {
        return Gravity.CENTER
    }

    open fun getDialogHeight(): Int {
        return WindowManager.LayoutParams.WRAP_CONTENT
    }

    open fun getDialogWidth(): Int {
        return WindowManager.LayoutParams.WRAP_CONTENT
    }

    open fun getDimAmount(): Float {
        return DEFAULT_DIM_AMOUNT
    }

    open fun getFragmentTag(): String? {
        return AbsDialogFragment::class.java.simpleName
    }

    abstract fun show(): BaseDialog?

    abstract fun bindView(view: View?)


    protected open fun isCancelableOutside(): Boolean {
        return true
    }

    protected open fun getDialogAnimationRes(): Int {
        return 0
    }

    protected abstract fun getLayoutRes(): Int

    protected abstract fun getDialogView(): View?
    protected abstract fun setCancelableOutside(b: Boolean)
}