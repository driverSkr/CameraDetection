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
        QuietTopBar(context.getString(R.string.settings)) { context.findActivity()?.finish() }
        QuietHeading(context.getString(R.string.brand), context.getString(R.string.make_yours))
        if (!subscribed) QuietPanel(tinted = true) {
            QuietBadge(context.getString(R.string.pro_badge))
            Text(context.getString(R.string.every_check), style = MaterialTheme.typography.headlineSmall)
            QuietButton(context.getString(R.string.explore_pro)) { SubscribeActivity.launch(context) }
        } else QuietBadge(context.getString(R.string.pro_active))
        QuietPanel {
            QuietRow(R.drawable.svg_icon_share_app, context.getString(R.string.share_app), context.getString(R.string.send_link)) {
                ShareUtils.shareTextWithHighlightedLinks(context, context.getString(R.string.brand), "https://play.google.com/store/apps/details?id=${context.packageName}")
            }
            QuietRow(R.drawable.svg_icon_privacy_policy, context.getString(R.string.privacy_policy), context.getString(R.string.privacy_description)) {
                LaunchUtils.launchWeb(context, "https://sites.google.com/view/spycamerafinder-privacy-policy/home", context.getString(R.string.privacy_policy))
            }
            RestorePurchases()
            QuietRow(R.drawable.svg_icon_rate_us, context.getString(R.string.rate_app), context.getString(R.string.share_experience)) {
                context.findActivity()?.let { activity ->
                    val manager = ReviewManagerFactory.create(context)
                    manager.requestReviewFlow().addOnCompleteListener { task ->
                        if (task.isSuccessful) manager.launchReviewFlow(activity, task.result)
                        else message = context.getString(R.string.review_unavailable)
                    }
                }
            }
        }
        message?.let { QuietNote(it) }
    }
}
