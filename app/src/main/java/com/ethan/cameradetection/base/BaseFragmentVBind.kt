package com.ethan.cameradetection.base

import android.os.Bundle
import androidx.fragment.app.FragmentActivity
import androidx.viewbinding.ViewBinding
import com.ethan.base.component.BaseFragmentVB
import com.ethan.permission.PermissionUtils

open class BaseFragmentVBind<T: ViewBinding>: BaseFragmentVB<T>() {
    lateinit var permissionUtils: PermissionUtils

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        getCurrentActivity()?.let {
            if (it is BaseActivityVBind<*>) {
                permissionUtils = it.permissionUtils
            }
        }
    }

    override fun getCurrentActivity(): FragmentActivity? {
        return this.activity
    }
}