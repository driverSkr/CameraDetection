package com.spyfinder.hiddencamera.detectorapp.dialog.view

import androidx.compose.foundation.layout.*
import com.spyfinder.hiddencamera.detectorapp.utils.deviceLabel
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.spyfinder.hiddencamera.detectorapp.R
import com.spyfinder.hiddencamera.detectorapp.model.WifiDevice
import com.spyfinder.hiddencamera.detectorapp.ui.components.*

@Composable
fun WifiInfoDetailsView(dialog: BottomSheetDialog, device: WifiDevice, onMarkSafe: (WifiDevice) -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    Column(Modifier.fillMaxWidth().navigationBarsPadding().verticalScroll(rememberScrollState()).padding(22.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        QuietTopBar(context.getString(R.string.device_details)) { dialog.dismiss() }
        QuietIcon(R.drawable.svg_icon_wifi_info_router)
        QuietHeading(context.getString(R.string.device_information), context.deviceLabel(device.name))
        QuietBadge(if (device.riskLevel > 0) context.getString(R.string.needs_review) else context.getString(R.string.phone_or_confirmed), device.riskLevel > 0)
        QuietPanel {
            listOf(context.getString(R.string.ip_address) to device.ip, context.getString(R.string.mac_address) to device.mac,
                context.getString(R.string.device_model) to device.brandModel, context.getString(R.string.estimated_type) to context.deviceLabel(device.type)).forEach { (label, value) ->
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    QuietBody(label, true)
                    Text(value.ifBlank { context.getString(R.string.not_available) })
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            }
        }
        QuietNote(context.getString(R.string.confirm_device_note))
        if (device.riskLevel > 0) QuietButton(context.getString(R.string.recognize_device)) { onMarkSafe(device); dialog.dismiss() }
    }
}
