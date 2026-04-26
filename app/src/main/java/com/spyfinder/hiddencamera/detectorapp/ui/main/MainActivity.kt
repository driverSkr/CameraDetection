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
import androidx.lifecycle.lifecycleScope
import com.skydoves.bundler.intentOf
import com.spyfinder.hiddencamera.detectorapp.base.BaseActivityVBind
import com.spyfinder.hiddencamera.detectorapp.databinding.LayoutComposeContainerBinding
import com.spyfinder.hiddencamera.detectorapp.theme.ComposeProjectTheme
import com.spyfinder.hiddencamera.detectorapp.theme.Transparent
import com.spyfinder.hiddencamera.detectorapp.ui.main.context.LocalMainContextEntity
import com.spyfinder.hiddencamera.detectorapp.ui.main.context.MainContextEntity
import com.spyfinder.hiddencamera.detectorapp.ui.main.page.MainPage
import com.spyfinder.hiddencamera.detectorapp.ui.subscribe.SubscribeActivity
import com.spyfinder.hiddencamera.detectorapp.utils.SubscribeHelper
import com.spyfinder.hiddencamera.detectorapp.utils.WifiHelper
import kotlinx.coroutines.launch

class MainActivity : BaseActivityVBind<LayoutComposeContainerBinding>() {

    companion object {
        private const val EXTRA_CHECK_SUBSCRIBE_ON_LAUNCH = "extra_check_subscribe_on_launch"

        fun launch(context: Context, checkSubscribeOnLaunch: Boolean = false) {
            context.intentOf<MainActivity> {
                putExtra(EXTRA_CHECK_SUBSCRIBE_ON_LAUNCH, checkSubscribeOnLaunch)
                startActivity(context)
            }
        }
    }

    private var hasHandledColdStartSubscribeCheck = false

    private val wifiPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
        val granted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true && (Build.VERSION.SDK_INT < 33 || permissions[Manifest.permission.NEARBY_WIFI_DEVICES] == true)
        if (granted) {
            Toast.makeText(this, "Permission granted", Toast.LENGTH_LONG).show()
        } else {
            Toast.makeText(this, "Permission denied", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        checkColdStartSubscribeIfNeeded()
        WifiHelper.checkWifiPermission(this, wifiPermissionLauncher)
        binding.composeView.apply {
            setContent {
                val mainContextEntity = remember {
                    MainContextEntity(applicationContext).apply {
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

    private fun checkColdStartSubscribeIfNeeded() {
        val shouldCheckSubscribeOnLaunch = intent.getBooleanExtra(EXTRA_CHECK_SUBSCRIBE_ON_LAUNCH, false)
        if (!shouldCheckSubscribeOnLaunch || hasHandledColdStartSubscribeCheck) {
            return
        }
        hasHandledColdStartSubscribeCheck = true
        lifecycleScope.launch {
            val isSubscribed = SubscribeHelper.isSubscribe()
            if (!isSubscribed && !isFinishing && !isDestroyed) {
                SubscribeActivity.launch(this@MainActivity)
            }
        }
    }
}
