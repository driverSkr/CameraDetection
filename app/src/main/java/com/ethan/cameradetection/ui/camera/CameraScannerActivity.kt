package com.ethan.cameradetection.ui.camera

import android.content.Context
import android.os.Bundle
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import com.ethan.cameradetection.base.BaseActivityVBind
import com.ethan.cameradetection.databinding.LayoutComposeContainerBinding
import com.ethan.cameradetection.theme.ComposeProjectTheme
import com.ethan.cameradetection.theme.Transparent
import com.ethan.cameradetection.ui.camera.page.CameraScannerPage
import com.skydoves.bundler.intentOf

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