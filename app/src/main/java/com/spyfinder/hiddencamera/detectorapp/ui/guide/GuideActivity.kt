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
import com.spyfinder.hiddencamera.detectorapp.utils.DataHelper

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
                                    DataHelper.setFirstCompleted(this@GuideActivity, "open")
                                    if (isFirstLaunch) {
                                        SubscribeActivity.launch(
                                            context = this@GuideActivity,
                                            launchMainOnDismiss = true
                                        )
                                    } else {
                                        MainActivity.launch(context = this@GuideActivity)
                                    }
                                    finish()
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
