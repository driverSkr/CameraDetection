package com.spyfinder.hiddencamera.detectorapp.ui.camera

import android.content.Context
import android.os.Bundle
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import com.spyfinder.hiddencamera.detectorapp.base.BaseActivityVBind
import com.spyfinder.hiddencamera.detectorapp.event.Event
import com.spyfinder.hiddencamera.detectorapp.theme.ComposeProjectTheme
import com.spyfinder.hiddencamera.detectorapp.theme.Transparent
import com.spyfinder.hiddencamera.detectorapp.ui.camera.page.CameraScannerPage
import com.skydoves.bundler.intentOf
import com.spyfinder.hiddencamera.detectorapp.databinding.LayoutComposeContainerBinding

class CameraScannerActivity : BaseActivityVBind<LayoutComposeContainerBinding>() {

    companion object {
        fun launch(context: Context) {
            context.intentOf<CameraScannerActivity> {
                startActivity(context)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 红外扫描页曝光埋点，统计实际进入扫描页的次数。
        Event.event(this, Event.PAGE_VIEW, Event.PARAM_PAGE to "camera_scanner")
        binding.composeView.apply {
            setContent {
                CompositionLocalProvider {
                    ComposeProjectTheme {
                        Surface(modifier = Modifier.fillMaxSize(), color = Transparent) {
                            CameraScannerPage()
                        }
                    }
                }
            }
        }
    }
}
