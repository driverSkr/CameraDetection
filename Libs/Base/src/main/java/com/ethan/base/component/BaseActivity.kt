package com.ethan.base.component

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.fragment.app.Fragment
import com.ethan.base.R
import com.ethan.base.dialog.BaseDialog
import com.ethan.base.utils.AndroidBarUtils
import com.ethan.base.utils.resumeWithOutError
import com.ethan.base.widget.progress.ProgressSupport
import com.ethan.base.widget.toast.BaseToast
import kotlin.coroutines.suspendCoroutine


private const val TAG = "BaseActivity"

open class BaseActivity : AppCompatActivity(), ProgressSupport {

    private var contentLayout: FrameLayout? = null
    private val handler = Handler(Looper.getMainLooper())
    private var loadingDialog: BaseDialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        supportActionBar?.hide()
        fitStatusBar()
        AndroidBarUtils.transparentNavBar(this)
        setStatusTextDark(false)
        super.onCreate(savedInstanceState)
        super.setContentView(R.layout.layout_activity)
        contentLayout = findViewById(R.id.view_content)
    }

    //透明状态栏
    @Suppress("DEPRECATION")
    private fun fitStatusBar() {
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
        window.statusBarColor = Color.TRANSPARENT
    }

    //设置状态栏文字颜色
    fun setStatusTextDark(dark: Boolean) {
        val controller = ViewCompat.getWindowInsetsController(window.decorView)
        controller?.isAppearanceLightStatusBars = dark
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        val fragments = supportFragmentManager.fragments
        for (f: Fragment in fragments) {
            dispatchActivityResult(f, requestCode, resultCode, data)
        }
    }

    private fun dispatchActivityResult(fragment: Fragment, requestCode: Int, resultCode: Int, data: Intent?) {
        fragment.onActivityResult(requestCode, resultCode, data)
        val fragments = fragment.childFragmentManager.fragments
        for (child: Fragment in fragments) {
            dispatchActivityResult(child, requestCode, resultCode, data)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        dismissLoadingDialog()
        loadingDialog = null
    }

    override fun isDestroy(): Boolean {
        return isDestroyed
    }

    override fun getUIHandler(): Handler {
        return handler
    }

    private fun getAndClearContentLayout(): FrameLayout {
        contentLayout!!.removeAllViewsInLayout()
        return contentLayout!!
    }

    override fun setContentView(layoutResID: Int) {
        super.setContentView(layoutResID)
        LayoutInflater.from(this).inflate(layoutResID, getAndClearContentLayout())
        onContentChanged()
    }

    override fun setContentView(view: View?) {
        super.setContentView(view)
        onContentChanged()
    }

    override fun setContentView(view: View?, params: ViewGroup.LayoutParams?) {
        super.setContentView(view, params)
        getAndClearContentLayout().addView(view, params)
    }

    fun isLoading(): Boolean {
        return loadingDialog?.isAdded != null && loadingDialog?.isAdded!!
    }

    private fun showLoadingDialog(content: String, isWindow: Boolean) {
        runOnUiThread {
            if (isLoading()) {
                return@runOnUiThread
            }
            val view = View.inflate(this, R.layout.dialog_loading, null)
            loadingDialog = BaseDialog.Builder(this).setView(view).setCancelableOutside(false).create()
            loadingDialog?.isCancelable = false
            try {
                Log.i(TAG, "showLoadingDialog: ")
                if (!isLoading()) loadingDialog?.show()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun showLoadingNoBg() {
        showLoadingDialog(getString(R.string.loading), false)
    }

    fun showLoadingWithBg(content: String) {
        showLoadingDialog(content, true)
    }

    fun dismissLoadingDialog() {
        try {
            runOnUiThread {
                Log.i(TAG, "dismissLoadingDialog: ")
                loadingDialog?.dismiss()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun showToast(@StringRes resId: Int) {
        val content = getString(resId)
        showToast(content)
    }

    fun showToast(content: String?) {
        if (content != null) {
            BaseToast.showToast(this, content)
        }
    }

    fun showToastException(@StringRes resId: Int) {
        val content = getString(resId)
        showToastException(content)
    }

    fun showToastException(content: String) {
        BaseToast.toastException(this, content)
    }

    fun startActivity(clz: Class<*>) {
        startActivity(Intent(this, clz))
    }


    private var continuation: (Result<ActivityResult>) -> Unit = {}

    private val launcher: ActivityResultLauncher<Intent> = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        continuation(Result.success(result))
    }

    // 定义扩展方法启动Activity并获取回调
    suspend fun startActivityForResultCommon(intent: Intent) = suspendCoroutine { cont ->
        try {
            continuation = {
                cont.resumeWithOutError(it.getOrThrow())
            }
            launcher.launch(intent)
        } catch (e: Exception) {
            e.printStackTrace()
            cont.resumeWithOutError(null)
        }

    }


    var backPressedPure = false

    /**
     * 这个段代码会影响fragment对返回键的监听，返回事件都应该在Fragment内部进行处理，而不应该交由外部activity干涉
     * fragment应该使用自身获得的父fragment管理器来进行弹栈
     * 因为直接改动涉及范围太广，添加[backPressedPure]变量来进行解决
     */
    override fun onBackPressed() {
        try {
            Log.i(TAG, "onBackPressed: 返回触发,是否纯净返回? -> $backPressedPure")
            if (!backPressedPure) {
                val fragments = supportFragmentManager.fragments
                if (fragments.size > 0) {
                    for (i in fragments.size..0) {
                        val child = fragments[i]
                        if (child.childFragmentManager.popBackStackImmediate()) {
                            return
                        }
                    }
                }
                if (supportFragmentManager.popBackStackImmediate()) {
                    return
                }
            }
            super.onBackPressed()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

}