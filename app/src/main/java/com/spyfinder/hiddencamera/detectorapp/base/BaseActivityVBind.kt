package com.spyfinder.hiddencamera.detectorapp.base

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.viewbinding.ViewBinding
import com.blankj.utilcode.util.BarUtils
import com.ethan.base.component.BaseActivityVB
import com.spyfinder.hiddencamera.detectorapp.utils.AppLanguage
import com.ethan.permission.PermissionUtils
import com.spyfinder.hiddencamera.detectorapp.utils.SubscribeHelper

open class BaseActivityVBind<T: ViewBinding>: BaseActivityVB<T>() {
    var permissionUtils = PermissionUtils()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        permissionUtils.init(this)
        fitStateBarHeight()
        initObserver()
    }

    override fun onResume() {
        super.onResume()
        if (resources.configuration.locales[0].language != AppLanguage.systemLocale().language) {
            recreate()
            return
        }
        // 页面回到前台时刷新订阅状态，覆盖支付完成、退款、取消订阅等外部变化。
        SubscribeHelper.refreshSubscribeState()
    }

    fun edge2EdgeWithCompose() {
        enableEdgeToEdge(SystemBarStyle.dark(Color.TRANSPARENT), SystemBarStyle.dark(Color.TRANSPARENT)) // 这个东西在设置透明的时候会自动顶到边缘
    }

    private fun fitStateBarHeight() {
        setFitStatusBarHeightView()?.let { BarUtils.addMarginTopEqualStatusBarHeight(it) }
    }

    private fun initObserver() {

    }

    open fun setFitStatusBarHeightView(): View? {
        return null
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
    }

    override fun attachBaseContext(newBase: Context) {
        // Ignore legacy manual preferences: language always follows the current system setting.
        super.attachBaseContext(AppLanguage.wrap(newBase))
    }

    fun <T> getIntent(name: String, classF: Class<T>): T? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(name, classF)
        } else {
            intent.getParcelableExtra(name)
        }
    }

}
