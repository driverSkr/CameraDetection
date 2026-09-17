package com.spyfinder.hiddencamera.detectorapp.ui.camera

import android.content.Context
import android.content.Intent
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
import com.spyfinder.hiddencamera.detectorapp.databinding.LayoutComposeContainerBinding

class CameraScannerActivity : BaseActivityVBind<LayoutComposeContainerBinding>() {

    companion object {
        const val EXTRA_SCENE_TITLE = "extra_scene_title"
        fun intent(context: Context, sceneTitle: String) =
            Intent(context, CameraScannerActivity::class.java).putExtra(EXTRA_SCENE_TITLE, sceneTitle)
        fun launch(context: Context, sceneTitle: String = "") {
            context.startActivity(intent(context, sceneTitle))
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val sceneTitle = intent.getStringExtra(EXTRA_SCENE_TITLE).orEmpty()
        Event.event(this, Event.PAGE_VIEW, Event.PARAM_PAGE to "camera_scanner")
        binding.composeView.apply {
            setContent {
                CompositionLocalProvider {
                    ComposeProjectTheme {
                        Surface(modifier = Modifier.fillMaxSize(), color = Transparent) {
                            CameraScannerPage(sceneTitle = sceneTitle)
                        }
                    }
                }
            }
        }
    }
}
