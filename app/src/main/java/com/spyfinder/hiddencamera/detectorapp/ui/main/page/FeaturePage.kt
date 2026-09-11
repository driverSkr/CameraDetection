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
            QuietBadge(context.getString(R.string.toolkit))
            IconButton(onClick = {
                Event.event(context, Event.FEATURE_CLICK, Event.PARAM_FEATURE to "settings")
                SettingActivity.launch(context)
            }) { Icon(painterResource(R.drawable.svg_icon_settings), context.getString(R.string.settings), Modifier.size(24.dp)) }
        }
        QuietHeading(context.getString(R.string.different_checks), context.getString(R.string.calmer_space), context.getString(R.string.choose_method))
        QuietPanel {
            val tools = listOf(
                Triple(R.drawable.svg_icon_wifi, context.getString(R.string.wifi_check), context.getString(R.string.review_network_devices)),
                Triple(R.drawable.svg_icon_magnetic, context.getString(R.string.magnetic_check), context.getString(R.string.observe_magnetic)),
                Triple(R.drawable.svg_icon_scanner, context.getString(R.string.camera_inspection), context.getString(R.string.look_filters)),
                Triple(R.drawable.svg_icon_tips, context.getString(R.string.safety_tips), context.getString(R.string.practical_guide))
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
