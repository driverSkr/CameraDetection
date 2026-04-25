package com.spyfinder.hiddencamera.detectorapp.ui.subscribe

import android.content.Context
import android.os.Bundle
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import com.skydoves.bundler.intentOf
import com.spyfinder.hiddencamera.detectorapp.base.BaseActivityVBind
import com.spyfinder.hiddencamera.detectorapp.databinding.LayoutComposeContainerBinding
import com.spyfinder.hiddencamera.detectorapp.theme.ComposeProjectTheme
import com.spyfinder.hiddencamera.detectorapp.theme.Transparent
import com.spyfinder.hiddencamera.detectorapp.ui.main.MainActivity
import com.spyfinder.hiddencamera.detectorapp.ui.subscribe.page.SubscribePage

class SubscribeActivity : BaseActivityVBind<LayoutComposeContainerBinding>() {

    companion object {
        private const val EXTRA_IS_FIRST_LAUNCH = "extra_is_first_launch"

        fun launch(context: Context, isFirstLaunch: Boolean = false) {
            context.intentOf<SubscribeActivity> {
                putExtra(EXTRA_IS_FIRST_LAUNCH, isFirstLaunch)
                startActivity(context)
            }
        }
    }

    private var isFirstLaunch: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        isFirstLaunch = intent.getBooleanExtra(EXTRA_IS_FIRST_LAUNCH, false)
        binding.composeView.apply {
            setContent {
                CompositionLocalProvider {
                    ComposeProjectTheme {
                        Surface(modifier = Modifier.fillMaxSize(), color = Transparent) {
                            SubscribePage(
                                onDismiss = {
                                    if (isFirstLaunch) {
                                        // 首次启动：订阅页关闭后进入主页，并关闭订阅页
                                        MainActivity.launch(this@SubscribeActivity)
                                        finish()
                                    } else {
                                        // 非首次启动：直接关闭订阅页，返回上一级
                                        finish()
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    override fun onBackPressed() {
        // 拦截返回键和侧滑返回，统一使用自定义的关闭逻辑
        if (isFirstLaunch) {
            // 首次启动：订阅页关闭后进入主页，并关闭订阅页
            MainActivity.launch(this)
            finish()
        } else {
            // 非首次启动：直接关闭订阅页，返回上一级
            super.onBackPressed()
        }
    }

    private fun handleDismiss() {
        if (isFirstLaunch) {
            // 首次启动：订阅页关闭后进入主页，并关闭订阅页
            MainActivity.launch(this)
            finish()
        } else {
            // 非首次启动：直接关闭订阅页，返回上一级
            finish()
        }
    }
}
