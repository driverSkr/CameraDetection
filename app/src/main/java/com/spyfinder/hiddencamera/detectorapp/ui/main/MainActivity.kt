package com.spyfinder.hiddencamera.detectorapp.ui.main

import android.Manifest
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.skydoves.bundler.intentOf
import com.spyfinder.hiddencamera.detectorapp.base.BaseActivityVBind
import com.spyfinder.hiddencamera.detectorapp.databinding.LayoutComposeContainerBinding
import com.spyfinder.hiddencamera.detectorapp.theme.ComposeProjectTheme
import com.spyfinder.hiddencamera.detectorapp.theme.Transparent
import com.spyfinder.hiddencamera.detectorapp.ui.main.context.LocalMainContextEntity
import com.spyfinder.hiddencamera.detectorapp.ui.main.context.MainContextEntity
import com.spyfinder.hiddencamera.detectorapp.ui.main.page.MainPage
import com.spyfinder.hiddencamera.detectorapp.utils.WifiHelper

class MainActivity : BaseActivityVBind<LayoutComposeContainerBinding>() {

    companion object {
        fun launch(context: Context) {
            context.intentOf<MainActivity> {
                startActivity(context)
            }
        }
    }

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
        binding.composeView.apply {
            setContent {
                val mainContextEntity = remember {
                    MainContextEntity(applicationContext).apply {
                        // 进入首页时恢复最近一次扫描记录，支持 History 跨重启保留。
                        restoreLatestScanResult()
                    }
                }
                CompositionLocalProvider(LocalMainContextEntity provides mainContextEntity) {
                    ComposeProjectTheme {
                        Surface(modifier = Modifier.fillMaxSize(), color = Transparent) {
                            MainPage()
                        }
                    }
                }
            }
        }
    }
}
