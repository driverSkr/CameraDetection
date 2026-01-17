package com.ethan.cameradetection.ui.main

import android.Manifest
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import com.ethan.cameradetection.base.BaseActivityVBind
import com.ethan.cameradetection.databinding.LayoutComposeContainerBinding
import com.ethan.cameradetection.theme.ComposeProjectTheme
import com.ethan.cameradetection.theme.Transparent
import com.ethan.cameradetection.ui.main.context.LocalMainContextEntity
import com.ethan.cameradetection.ui.main.context.MainContextEntity
import com.ethan.cameradetection.ui.main.page.MainPage
import com.ethan.cameradetection.utils.WifiHelper

class MainActivity : BaseActivityVBind<LayoutComposeContainerBinding>() {

    private val wifiPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
        val granted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true && (Build.VERSION.SDK_INT < 33 || permissions[Manifest.permission.NEARBY_WIFI_DEVICES] == true)
        if (granted) {
            Toast.makeText(this, "权限或得成功", Toast.LENGTH_LONG).show()
        } else {
            Toast.makeText(this, "没有权限", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WifiHelper.checkWifiPermission(this, wifiPermissionLauncher)
//        val isFirstOpen = DataHelper.isFirst(this, "open")
        binding.composeView.apply {
            setContent {
                CompositionLocalProvider(LocalMainContextEntity provides MainContextEntity()) {
                    ComposeProjectTheme {
//                        val localMain = LocalMainContextEntity.current
                        Surface(modifier = Modifier.fillMaxSize(), color = Transparent) {
//                            if (isFirstOpen && !localMain.isOpenMainPage) {
//                                GuidePage()
//                            } else {
//                                MainPage()
//                            }
                            MainPage()
                        }
                    }
                }
            }
        }
    }
}