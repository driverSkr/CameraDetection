package com.spyfinder.hiddencamera.detectorapp.ui.setting.page

import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import com.google.android.play.core.review.ReviewManagerFactory
import com.spyfinder.hiddencamera.detectorapp.R
import com.spyfinder.hiddencamera.detectorapp.ui.components.*
import com.spyfinder.hiddencamera.detectorapp.ui.subscribe.SubscribeActivity
import com.spyfinder.hiddencamera.detectorapp.utils.*

@Composable
fun SettingPage() {
    val context = LocalContext.current
    val subscribed by SubscribeHelper.isSubscribedFlow.collectAsState()
    var message by remember { mutableStateOf<String?>(null) }
    QuietPage(navigationPadding = true) {
        QuietTopBar("Settings") { context.findActivity()?.finish() }
        QuietHeading("SpyFinder", "Make it yours.")
        if (!subscribed) QuietPanel(tinted = true) {
            QuietBadge("SPYFINDER PRO")
            Text("Every check.\nMore detail.", style = MaterialTheme.typography.headlineSmall)
            QuietButton("Explore Pro") { SubscribeActivity.launch(context) }
        } else QuietBadge("PRO ACTIVE")
        QuietPanel {
            QuietRow(R.drawable.svg_icon_share_app, "Share app", "Send the app link.") {
                ShareUtils.shareTextWithHighlightedLinks(context, "SpyFinder", "https://play.google.com/store/apps/details?id=${context.packageName}")
            }
            QuietRow(R.drawable.svg_icon_privacy_policy, "Privacy policy", "Read how data is handled.") {
                LaunchUtils.launchWeb(context, "https://sites.google.com/view/spycamerafinder-privacy-policy/home", "Privacy policy")
            }
            RestorePurchases()
            QuietRow(R.drawable.svg_icon_rate_us, "Rate SpyFinder", "Share your experience.") {
                context.findActivity()?.let { activity ->
                    val manager = ReviewManagerFactory.create(context)
                    manager.requestReviewFlow().addOnCompleteListener { task ->
                        if (task.isSuccessful) manager.launchReviewFlow(activity, task.result)
                        else message = "The review prompt is unavailable right now. Please try again later."
                    }
                }
            }
        }
        message?.let { QuietNote(it) }
    }
}
