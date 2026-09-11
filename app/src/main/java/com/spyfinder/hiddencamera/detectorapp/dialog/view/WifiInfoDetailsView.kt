package com.spyfinder.hiddencamera.detectorapp.dialog.view

import androidx.compose.foundation.layout.*
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
    Column(Modifier.fillMaxWidth().navigationBarsPadding().verticalScroll(rememberScrollState()).padding(22.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        QuietTopBar("Device details") { dialog.dismiss() }
        QuietIcon(R.drawable.svg_icon_wifi_info_router)
        QuietHeading("Device information", device.name.ifBlank { "Unknown" })
        QuietBadge(if (device.riskLevel > 0) "Needs review" else "This phone / confirmed", device.riskLevel > 0)
        QuietPanel {
            listOf("IP address" to device.ip, "MAC address" to device.mac,
                "Device model" to device.brandModel, "Estimated type" to device.type).forEach { (label, value) ->
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    QuietBody(label, true)
                    Text(value.ifBlank { "Not available" })
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            }
        }
        QuietNote("Check that this is a device you recognize before confirming it.")
        if (device.riskLevel > 0) QuietButton("I recognize this device") { onMarkSafe(device); dialog.dismiss() }
    }
}
