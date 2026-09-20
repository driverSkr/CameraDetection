package com.spyfinder.hiddencamera.detectorapp.ui.main

import android.Manifest
import android.content.Context
import android.content.Intent
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
import androidx.activity.viewModels
import com.spyfinder.hiddencamera.detectorapp.scan.ScanViewModel
import com.spyfinder.hiddencamera.detectorapp.scan.LocalScanViewModel
import com.skydoves.bundler.intentOf
import com.spyfinder.hiddencamera.detectorapp.base.BaseActivityVBind
import com.spyfinder.hiddencamera.detectorapp.databinding.LayoutComposeContainerBinding
import com.spyfinder.hiddencamera.detectorapp.theme.ComposeProjectTheme
import com.spyfinder.hiddencamera.detectorapp.theme.Transparent
import com.spyfinder.hiddencamera.detectorapp.ui.guide.GuideActivity
import com.spyfinder.hiddencamera.detectorapp.ui.main.context.LocalMainContextEntity
import com.spyfinder.hiddencamera.detectorapp.ui.main.context.MainContextEntity
import com.spyfinder.hiddencamera.detectorapp.ui.main.page.MainPage
import com.spyfinder.hiddencamera.detectorapp.ui.subscribe.SubscribeActivity
import com.spyfinder.hiddencamera.detectorapp.utils.SubscribeHelper
import com.spyfinder.hiddencamera.detectorapp.utils.SubscriptionGate
import com.spyfinder.hiddencamera.detectorapp.utils.WifiHelper
import com.spyfinder.hiddencamera.detectorapp.utils.DataHelper
import kotlinx.coroutines.launch

class MainActivity : BaseActivityVBind<LayoutComposeContainerBinding>() {

    companion object {
        private const val EXTRA_CHECK_SUBSCRIBE_ON_LAUNCH = "extra_check_subscribe_on_launch"
        private const val KEY_FIRST_OPEN = "open"
        const val EXTRA_FOCUS_DETECT = "extra_focus_detect"

        fun launch(context: Context, checkSubscribeOnLaunch: Boolean = false) {
            context.intentOf<MainActivity> {
                putExtra(EXTRA_CHECK_SUBSCRIBE_ON_LAUNCH, checkSubscribeOnLaunch)
                startActivity(context)
            }
        }
    }

    private var hasHandledColdStartSubscribeCheck = false

    private val scanViewModel: ScanViewModel by viewModels()

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        focusDetectIfRequested(intent)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null && routeFirstOpenToGuide()) return
        if (savedInstanceState == null) checkColdStartSubscribeIfNeeded()
        focusDetectIfRequested(intent)

        binding.composeView.apply {
            setContent {
                CompositionLocalProvider(LocalMainContextEntity provides scanViewModel.state, LocalScanViewModel provides scanViewModel) {
                    ComposeProjectTheme {
                        Surface(modifier = Modifier.fillMaxSize(), color = Transparent) {
                            MainPage()
                        }
                    }
                }
            }
        }
    }

    private fun routeFirstOpenToGuide(): Boolean {
        val isFirstOpen = runCatching { DataHelper.isFirst(this, KEY_FIRST_OPEN) }.getOrDefault(false)
        if (!isFirstOpen) return false
        GuideActivity.launch(this, isFirstLaunch = true)
        finish()
        return true
    }

    private fun focusDetectIfRequested(intent: android.content.Intent?) {
        if (intent?.getBooleanExtra(EXTRA_FOCUS_DETECT, false) != true) return
        scanViewModel.state.selectTabIndex.intValue = 0
        scanViewModel.state.closeDetectResult()
        intent.removeExtra(EXTRA_FOCUS_DETECT)
    }

    private fun checkColdStartSubscribeIfNeeded() {
        val launchedFromAppIcon = intent.action == Intent.ACTION_MAIN && intent.hasCategory(Intent.CATEGORY_LAUNCHER)
        val shouldCheckSubscribeOnLaunch = launchedFromAppIcon ||
            intent.getBooleanExtra(EXTRA_CHECK_SUBSCRIBE_ON_LAUNCH, false)
        if (!shouldCheckSubscribeOnLaunch || hasHandledColdStartSubscribeCheck) {
            return
        }
        hasHandledColdStartSubscribeCheck = true
        lifecycleScope.launch {
            val isSubscribed = SubscriptionGate.hasAccess()
            if (!isSubscribed && SubscribeHelper.canOfferPurchase && !isFinishing && !isDestroyed) {
                SubscribeActivity.launch(this@MainActivity)
            }
        }
    }
}
