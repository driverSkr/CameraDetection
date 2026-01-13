package com.ethan.cameradetection.dialog

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import com.ethan.cameradetection.dialog.view.WifiInfoDetailsView
import com.ethan.cameradetection.model.WifiInfoModel
import com.ethan.cameradetection.theme.ComposeProjectTheme
import com.ethan.cameradetection.utils.ComposeNativeDialog

object DialogHelper {
    fun showWifiInfoDialog(activity: FragmentActivity, infoModel: WifiInfoModel) {
        val (binding, dialog) = ComposeNativeDialog.composeBottomDialog(activity)
        binding.composeView.apply {
            setContent {
                ComposeProjectTheme {
                    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)) {
                        WifiInfoDetailsView(dialog, infoModel)
                        Spacer(modifier = Modifier.height(42.dp))
                    }
                }
            }
        }
        dialog.show()
    }
}