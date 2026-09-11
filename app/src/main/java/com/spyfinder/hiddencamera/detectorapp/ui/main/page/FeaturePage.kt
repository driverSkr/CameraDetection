package com.spyfinder.hiddencamera.detectorapp.ui.main.page

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.spyfinder.hiddencamera.detectorapp.R
import com.spyfinder.hiddencamera.detectorapp.event.Event
import com.spyfinder.hiddencamera.detectorapp.ui.components.*
import com.spyfinder.hiddencamera.detectorapp.ui.main.context.LocalMainContextEntity
import com.spyfinder.hiddencamera.detectorapp.ui.setting.SettingActivity
import com.spyfinder.hiddencamera.detectorapp.ui.tips.TipsActivity

@Composable
fun FeaturePage() {
    val context = LocalContext.current
    val main = LocalMainContextEntity.current
    QuietPage {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            QuietBadge("YOUR TOOLKIT")
            IconButton(onClick = {
                Event.event(context, Event.FEATURE_CLICK, Event.PARAM_FEATURE to "settings")
                SettingActivity.launch(context)
            }) { Icon(painterResource(R.drawable.svg_icon_settings), "Settings", Modifier.size(24.dp)) }
        }
        QuietHeading("Different checks", "One calmer space.", "Choose the method that fits your next step.")
        QuietPanel {
            val tools = listOf(
                Triple(R.drawable.svg_icon_wifi, "Wi-Fi check", "Review devices on your network."),
                Triple(R.drawable.svg_icon_magnetic, "Magnetic check", "Observe magnetic field changes."),
                Triple(R.drawable.svg_icon_scanner, "Camera inspection", "Look closer with color filters."),
                Triple(R.drawable.svg_icon_tips, "Safety tips", "A practical guide to your space.")
            )
            tools.forEachIndexed { index, (icon, title, description) ->
                QuietRow(icon, title, description) {
                    Event.event(context, Event.FEATURE_CLICK, Event.PARAM_FEATURE to listOf("wifi", "magnetic", "scanner", "tips")[index])
                    if (index == 3) TipsActivity.launch(context)
                    else { main.pendingWifiAutoScan.value = index == 0; main.selectTabIndex.intValue = index }
                }
            }
        }
    }
}
