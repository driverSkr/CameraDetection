package com.spyfinder.hiddencamera.detectorapp.ui.main.view

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.fragment.app.FragmentActivity
import com.spyfinder.hiddencamera.detectorapp.R
import com.spyfinder.hiddencamera.detectorapp.dialog.DialogHelper
import com.spyfinder.hiddencamera.detectorapp.ui.components.*
import com.spyfinder.hiddencamera.detectorapp.ui.main.context.LocalMainContextEntity
import com.spyfinder.hiddencamera.detectorapp.ui.subscribe.SubscribeActivity
import com.spyfinder.hiddencamera.detectorapp.utils.SubscribeHelper
import com.spyfinder.hiddencamera.detectorapp.utils.findActivity
import kotlinx.coroutines.launch

@Composable
fun DetectResultView() {
    val context = LocalContext.current
    val main = LocalMainContextEntity.current
    val subscribed by SubscribeHelper.isSubscribedFlow.collectAsState()
    val scope = rememberCoroutineScope()
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        scope.launch { SubscribeHelper.isSubscribe() }
    }
    val review = main.resultSuspiciousDevices
    val trusted = main.resultTrustedDevices
    val all = review + trusted
    BackHandler { main.closeDetectResult() }
    QuietPage(navigationPadding = true) {
        QuietTopBar(if (main.isShowingLatestHistoryResult) "Last scan" else "Scan results") { main.closeDetectResult() }
        QuietHeading(if (main.isShowingLatestHistoryResult) "Saved on this phone" else "Network overview",
            if (all.isEmpty()) "No devices found." else "Review your devices.",
            if (main.isShowingLatestHistoryResult) "Your most recent saved result." else "${all.size} devices found in this check.")
        if (all.isEmpty()) {
            QuietOrbit(R.drawable.svg_icon_wifi)
            QuietNote("No reachable devices were returned. This does not confirm that a space is free of cameras. Devices may be offline or on another network.")
            QuietButton("Back to Wi-Fi") { main.closeDetectResult() }
        } else {
            QuietStats(review.size, trusted.size)
            if (!subscribed) {
                QuietPanel(tinted = true) {
                    QuietIcon(R.drawable.svg_icon_safety)
                    Text("Device details are Pro")
                    QuietBody("Unlock device names, addresses and manual confirmation.")
                }
                QuietButton("Unlock device details") { launcher.launch(Intent(context, SubscribeActivity::class.java)) }
                QuietButton("Back to Wi-Fi", secondary = true) { main.closeDetectResult() }
            } else {
                QuietPanel {
                    all.forEach { device ->
                        QuietRow(R.drawable.svg_icon_wifi_info_router, device.name.ifBlank { "Unknown" },
                            "${device.ip} · ${if (device.riskLevel > 0) "Needs review" else "This phone / confirmed"}") {
                            (context.findActivity() as? FragmentActivity)?.let { activity ->
                                DialogHelper.showWifiInfoDialog(activity, device) { main.markDeviceAsSafe(it) }
                            }
                        }
                    }
                }
                QuietNote("Device types are estimates. Confirmation is your own assessment, not a safety certification.")
            }
        }
    }
}
