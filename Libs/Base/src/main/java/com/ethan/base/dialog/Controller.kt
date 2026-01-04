package com.ethan.base.dialog

import android.content.DialogInterface
import android.os.Parcel
import android.os.Parcelable
import android.view.Gravity
import android.view.View
import androidx.fragment.app.FragmentManager

class Controller() : Parcelable {
    private var fragmentManager: FragmentManager? = null
    private var layoutRes = 0
    private var height = 0
    private var width = 0
    private var dimAmount = 0f
    private var gravity = 0
    private var tag: String? = null
    private var ids: IntArray? = null
    private var isCancelableOutside = false
    private var onViewClickListener: OnViewClickListener? = null
    private var dialogAnimationRes = 0
    private var dialogView: View? = null
    private var onDismissListener: DialogInterface.OnDismissListener? = null

    fun getFragmentManager(): FragmentManager? {
        return fragmentManager
    }

    fun getLayoutRes(): Int {
        return layoutRes
    }

    fun getHeight(): Int {
        return height
    }

    fun getWidth(): Int {
        return width
    }

    fun getDimAmount(): Float {
        return dimAmount
    }

    fun getGravity(): Int {
        return gravity
    }

    fun getTag(): String? {
        return if (tag == null) "" else tag
    }

    fun getIds(): IntArray? {
        return ids
    }

    fun isCancelableOutside(): Boolean {
        return isCancelableOutside
    }

    fun getOnViewClickListener(): OnViewClickListener? {
        return onViewClickListener
    }

    fun getDialogAnimationRes(): Int {
        return dialogAnimationRes
    }

    fun getDialogView(): View? {
        return dialogView
    }

    fun getOnDismissListener(): DialogInterface.OnDismissListener? {
        return onDismissListener
    }

    fun setCancelableOutside(b: Boolean) {
        isCancelableOutside = b
    }

    class Params {
        var mFragmentManager: FragmentManager? = null
        var mLayoutRes = 0
        var mWidth = 0
        var mHeight = 0
        var mDimAmount = 0.5f
        var mGravity = Gravity.CENTER
        var mTag = "TSDialog"
        var ids: IntArray? = null
        var mIsCancelableOutside = true
        var mOnViewClickListener: OnViewClickListener? = null
        var mDialogAnimationRes = 0 //弹窗动画
        var mDialogView //直接使用传入进来的View,而不需要通过解析Xml
                : View? = null
        var mOnDismissListener: DialogInterface.OnDismissListener? = null
        fun apply(tController: Controller) {
            tController.fragmentManager = mFragmentManager
            if (mLayoutRes > 0) {
                tController.layoutRes = mLayoutRes
            }
            if (mDialogView != null) {
                tController.dialogView = mDialogView
            }
            if (mWidth > 0) {
                tController.width = mWidth
            }
            if (mHeight > 0) {
                tController.height = mHeight
            }
            tController.dimAmount = mDimAmount
            tController.gravity = mGravity
            tController.tag = mTag
            if (ids != null) {
                tController.ids = ids
            }
            tController.isCancelableOutside = mIsCancelableOutside
            tController.onViewClickListener = mOnViewClickListener
            tController.onDismissListener = mOnDismissListener
            tController.dialogAnimationRes = mDialogAnimationRes
            require(!(tController.getLayoutRes() <= 0 && tController.getDialogView() == null)) { "请先调用setLayoutRes()方法设置弹窗所需的xml布局!" }
            //            if (tController.width <= 0 && tController.height <= 0) {
            //                tController.width = 600;
            //            }
        }
    }

    constructor(parcel: Parcel) : this() {
        layoutRes = parcel.readInt()
        height = parcel.readInt()
        width = parcel.readInt()
        dimAmount = parcel.readFloat()
        gravity = parcel.readInt()
        tag = parcel.readString()
        ids = parcel.createIntArray()
        isCancelableOutside = parcel.readByte() != 0.toByte()
        dialogAnimationRes = parcel.readInt()
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeInt(layoutRes)
        parcel.writeInt(height)
        parcel.writeInt(width)
        parcel.writeFloat(dimAmount)
        parcel.writeInt(gravity)
        parcel.writeString(tag)
        parcel.writeIntArray(ids)
        parcel.writeByte(if (isCancelableOutside) 1 else 0)
        parcel.writeInt(dialogAnimationRes)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<Controller> {
        override fun createFromParcel(parcel: Parcel): Controller {
            return Controller(parcel)
        }

        override fun newArray(size: Int): Array<Controller?> {
            return arrayOfNulls(size)
        }
    }
}