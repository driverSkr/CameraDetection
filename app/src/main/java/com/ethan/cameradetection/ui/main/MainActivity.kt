package com.ethan.cameradetection.ui.main

import android.os.Bundle
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import com.ethan.cameradetection.base.BaseActivityVBind
import com.ethan.cameradetection.databinding.LayoutComposeContainerBinding
import com.ethan.cameradetection.theme.ComposeProjectTheme
import com.ethan.cameradetection.theme.Transparent
import com.ethan.cameradetection.ui.guide.page.GuidePage
import com.ethan.cameradetection.ui.main.context.LocalMainContextEntity
import com.ethan.cameradetection.ui.main.context.MainContextEntity
import com.ethan.cameradetection.ui.main.page.MainPage
import com.ethan.cameradetection.utils.DataHelper

class MainActivity : BaseActivityVBind<LayoutComposeContainerBinding>() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val isFirstOpen = DataHelper.isFirst(this, "open")
        binding.composeView.apply {
            setContent {
                CompositionLocalProvider(LocalMainContextEntity provides MainContextEntity()) {
                    ComposeProjectTheme {
                        val localMain = LocalMainContextEntity.current
                        Surface(modifier = Modifier.Companion.fillMaxSize(), color = Transparent) {
                            if (isFirstOpen && !localMain.isOpenMainPage) {
                                GuidePage()
                            } else {
                                MainPage()
                            }
                        }
                    }
                }
            }
        }
    }
}