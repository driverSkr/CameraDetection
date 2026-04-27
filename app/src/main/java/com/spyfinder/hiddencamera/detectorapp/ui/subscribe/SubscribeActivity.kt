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
        private const val EXTRA_LAUNCH_MAIN_ON_DISMISS = "extra_launch_main_on_dismiss"

        fun launch(
            context: Context,
            launchMainOnDismiss: Boolean = false
        ) {
            context.intentOf<SubscribeActivity> {
                putExtra(EXTRA_LAUNCH_MAIN_ON_DISMISS, launchMainOnDismiss)
                startActivity(context)
            }
        }
    }

    private var launchMainOnDismiss: Boolean = false
    private var hasHandledDismissNavigation = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        launchMainOnDismiss = intent.getBooleanExtra(EXTRA_LAUNCH_MAIN_ON_DISMISS, false)
        binding.composeView.apply {
            setContent {
                CompositionLocalProvider {
                    ComposeProjectTheme {
                        Surface(modifier = Modifier.fillMaxSize(), color = Transparent) {
                            SubscribePage(
                                onDismiss = {
                                    handleDismiss()
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    override fun onBackPressed() {
        handleDismiss()
    }

    private fun handleDismiss() {
        if (launchMainOnDismiss && !hasHandledDismissNavigation) {
            hasHandledDismissNavigation = true
            MainActivity.launch(context = this)
        }
        finish()
    }
}
