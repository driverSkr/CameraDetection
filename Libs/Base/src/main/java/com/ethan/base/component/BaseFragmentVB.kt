package com.ethan.base.component

import android.content.ContentValues.TAG
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.ViewCompat
import androidx.fragment.app.FragmentActivity
import androidx.viewbinding.ViewBinding
import com.ethan.base.BuildConfig
import com.ethan.base.utils.AndroidBarUtils
import java.lang.reflect.ParameterizedType

abstract class BaseFragmentVB<T : ViewBinding> : BaseFragment() {
    lateinit var binding: T

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        super.onCreateView(inflater, container, savedInstanceState)
        val type = javaClass.genericSuperclass as ParameterizedType
        val klass = type.actualTypeArguments[0] as Class<*>
        val method = klass.getDeclaredMethod("inflate", LayoutInflater::class.java, ViewGroup::class.java, Boolean::class.java)
        binding = method.invoke(null, layoutInflater, container, false) as T
        return binding.root
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        printAllBundle()
    }

    fun setStatusTextDark(dark: Boolean) {
        val controller = ViewCompat.getWindowInsetsController(requireActivity().window.decorView)
        controller?.isAppearanceLightStatusBars = dark
    }

    protected fun setTopMargin(view: View) {
        getCurrentActivity()?.let {
            val statusBarHeight = AndroidBarUtils.getStatusBarHeight(it)
            view.setPadding(0, statusBarHeight, 0, 0)
        }
    }


    private fun printAllBundle() {
        if (BuildConfig.DEBUG) {
            val bundle = arguments
            val keySet = bundle?.keySet() ?: return

            Log.v(TAG, "Fragment Bundle: ==============================================================================")
            for (key in keySet) {
                Log.v("Fragment Bundle Content", "Key->" + key + ", content->" + bundle.get(key))
            }
            Log.v(TAG, "Fragment Bundle: ==============================================================================")
        }
    }


    open fun getCurrentActivity(): FragmentActivity? {
        return this.activity
    }
}