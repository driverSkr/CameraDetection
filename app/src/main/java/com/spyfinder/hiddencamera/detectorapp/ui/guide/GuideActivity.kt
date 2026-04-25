package com.spyfinder.hiddencamera.detectorapp.ui.guide

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
import com.spyfinder.hiddencamera.detectorapp.ui.guide.page.GuidePage
import com.spyfinder.hiddencamera.detectorapp.ui.main.MainActivity
import com.spyfinder.hiddencamera.detectorapp.ui.subscribe.SubscribeActivity

class GuideActivity : BaseActivityVBind<LayoutComposeContainerBinding>() {

    companion object {
        private const val EXTRA_IS_FIRST_LAUNCH = "extra_is_first_launch"

        fun launch(context: Context, isFirstLaunch: Boolean = false) {
            context.intentOf<GuideActivity> {
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
                            GuidePage(
                                onComplete = {
                                    if (isFirstLaunch) {
                                        // 首次启动：引导完成后进入订阅页，关闭引导页
                                        SubscribeActivity.launch(this@GuideActivity, true)
                                        finish()
                                    } else {
                                        // 非首次启动：引导完成后进入主页并关闭引导页，避免返回键回到引导流程。
                                        MainActivity.launch(this@GuideActivity)
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
}
