package com.spyfinder.hiddencamera.detectorapp.ui.main.view

import android.content.Intent
import com.spyfinder.hiddencamera.detectorapp.utils.deviceLabel
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
        QuietTopBar(if (main.isShowingLatestHistoryResult) context.getString(R.string.last_scan) else context.getString(R.string.scan_results)) { main.closeDetectResult() }
        QuietHeading(if (main.isShowingLatestHistoryResult) context.getString(R.string.saved_on_phone) else context.getString(R.string.network_overview),
            if (all.isEmpty()) context.getString(R.string.no_devices) else context.getString(R.string.review_devices),
            if (main.isShowingLatestHistoryResult) context.getString(R.string.recent_saved_result) else context.getString(R.string.devices_found_check, all.size))
        if (all.isEmpty()) {
            QuietOrbit(R.drawable.svg_icon_wifi)
            QuietNote(context.getString(R.string.empty_result_note))
            QuietButton(context.getString(R.string.back_wifi)) { main.closeDetectResult() }
        } else {
            QuietStats(review.size, trusted.size)
            if (!subscribed) {
                QuietPanel(tinted = true) {
                    QuietIcon(R.drawable.svg_icon_safety)
                    Text(context.getString(R.string.details_pro))
                    QuietBody(context.getString(R.string.unlock_description))
                }
                QuietButton(context.getString(R.string.unlock_details)) { launcher.launch(Intent(context, SubscribeActivity::class.java)) }
                QuietButton(context.getString(R.string.back_wifi), secondary = true) { main.closeDetectResult() }
            } else {
                QuietPanel {
                    all.forEach { device ->
                        QuietRow(R.drawable.svg_icon_wifi_info_router, context.deviceLabel(device.name),
                            "${device.ip} · ${if (device.riskLevel > 0) context.getString(R.string.needs_review) else context.getString(R.string.phone_or_confirmed)}") {
                            (context.findActivity() as? FragmentActivity)?.let { activity ->
                                DialogHelper.showWifiInfoDialog(activity, device) { main.markDeviceAsSafe(it) }
                            }
                        }
                    }
                }
                QuietNote(context.getString(R.string.type_estimate_note))
            }
        }
    }
}
