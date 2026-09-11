package com.spyfinder.hiddencamera.detectorapp.ui.guide.page

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import com.spyfinder.hiddencamera.detectorapp.R
import com.spyfinder.hiddencamera.detectorapp.ui.components.*

@Composable
fun GuidePage(onComplete: (() -> Unit)? = null) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var page by rememberSaveable { mutableIntStateOf(0) }
    val icons = listOf(R.drawable.svg_icon_wifi, R.drawable.svg_icon_scanner, R.drawable.svg_icon_magnetic)
    val titles = listOf(context.getString(R.string.guide_network_title), context.getString(R.string.guide_camera_title), context.getString(R.string.guide_sensor_title))
    val descriptions = listOf(context.getString(R.string.guide_network_description), context.getString(R.string.guide_camera_description), context.getString(R.string.guide_sensor_description))
    key(page) {
        QuietPage(navigationPadding = true) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text(context.getString(R.string.brand)); QuietBadge(context.getString(R.string.getting_started)) }
            QuietOrbit(icons[page])
            QuietHeading(listOf(context.getString(R.string.guide_network), context.getString(R.string.guide_camera), context.getString(R.string.guide_magnetic))[page], titles[page], descriptions[page])
            LinearProgressIndicator(progress = { (page + 1) / 3f }, modifier = Modifier.fillMaxWidth())
            QuietBody(context.getString(R.string.guide_progress, page + 1, 3), true)
            QuietButton(if (page == 2) context.getString(R.string.get_started) else context.getString(R.string.action_continue)) { if (page < 2) page++ else onComplete?.invoke() }
            if (page > 0) QuietButton(context.getString(R.string.back), secondary = true) { page-- }
        }
    }
}
