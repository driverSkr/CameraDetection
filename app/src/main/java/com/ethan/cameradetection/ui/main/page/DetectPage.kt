package com.ethan.cameradetection.ui.main.page

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.ethan.cameradetection.ui.main.context.LocalMainContextEntity
import com.ethan.cameradetection.ui.main.view.DetectCheckView
import com.ethan.cameradetection.ui.main.view.DetectResultView

/**
 * 检测页
 */
@Composable
fun DetectPage() {
    val localMain = LocalMainContextEntity.current

    Box(modifier = Modifier.fillMaxSize().statusBarsPadding()) {
        AnimatedContent(localMain.isShowResult) {
            when(it) {
                true -> DetectResultView()
                false -> DetectCheckView()
            }
        }
    }
}