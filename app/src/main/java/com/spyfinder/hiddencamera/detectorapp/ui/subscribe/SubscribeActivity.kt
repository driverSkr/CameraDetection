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
import com.spyfinder.hiddencamera.detectorapp.ui.subscribe.page.SubscribePage

class SubscribeActivity : BaseActivityVBind<LayoutComposeContainerBinding>() {

    companion object {
        fun launch(context: Context) {
            context.intentOf<SubscribeActivity> {
                startActivity(context)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding.composeView.apply {
            setContent {
                CompositionLocalProvider {
                    ComposeProjectTheme {
                        Surface(modifier = Modifier.fillMaxSize(), color = Transparent) {
                            SubscribePage(
                                onDismiss = {
                                    finish()
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    override fun onBackPressed() {
        finish()
    }
}
