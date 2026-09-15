package com.spyfinder.hiddencamera.detectorapp.dialog.view

import com.spyfinder.hiddencamera.detectorapp.utils.ScanStrings
import androidx.compose.ui.platform.LocalContext

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import com.spyfinder.hiddencamera.detectorapp.scan.Finding
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.spyfinder.hiddencamera.detectorapp.model.WifiDevice
import com.spyfinder.hiddencamera.detectorapp.theme.Transparent
import com.spyfinder.hiddencamera.detectorapp.theme.White
import com.spyfinder.hiddencamera.detectorapp.theme.White10
import com.spyfinder.hiddencamera.detectorapp.theme.White60
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.spyfinder.hiddencamera.detectorapp.R

@Composable
fun WifiInfoDetailsView(dialog: BottomSheetDialog, device: WifiDevice, onMarkSafe: (WifiDevice) -> Unit) {
    val context = LocalContext.current
    val identity = com.spyfinder.hiddencamera.detectorapp.scan.DeviceIdentity.forDevice(device)
    Column(modifier = Modifier
        .fillMaxWidth()
        .background(color = Color(0xFF161618), shape = RoundedCornerShape(48.dp))
        .heightIn(max = 620.dp)
        .verticalScroll(rememberScrollState())
        .navigationBarsPadding()
        .padding(top = 8.dp, start = 16.dp, end = 16.dp, bottom = 16.dp)
    ) {
        Box(modifier = Modifier
            .width(48.dp)
            .height(6.dp)
            .background(color = Color(0xFF5B5B5E), shape = RoundedCornerShape(4.dp))
            .align(Alignment.CenterHorizontally)
        )
        Spacer(modifier = Modifier.height(10.dp))
        Row(modifier = Modifier.fillMaxWidth().height(64.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(64.dp).background(color = White10, shape = RoundedCornerShape(19.dp))) {
                Image(painter = painterResource(R.drawable.svg_icon_wifi_info_router), modifier = Modifier.size(36.dp).align(Alignment.Center), contentDescription = null)
            }
            Spacer(modifier = Modifier.width(12.dp))
            Text(com.spyfinder.hiddencamera.detectorapp.scan.DeviceIdentity.name(device.details) ?: ScanStrings.text(context, identity.type), modifier = Modifier.weight(1f), color = White, fontSize = 18.sp, fontWeight = FontWeight.W600, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Spacer(modifier = Modifier.width(8.dp))
            Image(painter = painterResource(R.drawable.svg_icon_close_30), contentDescription = context.getString(R.string.a11y_close), modifier = Modifier.clickable{ dialog.dismiss() })
        }
        Spacer(modifier = Modifier.height(20.dp))
        Text(ScanStrings.text(context, identity.type), color = White, fontSize = 14.sp)
        Text(ScanStrings.text(context, identity.basis), color = White60, fontSize = 12.sp)
        listOf(R.string.ip_address to device.ip,
            R.string.mac_address to device.mac.ifBlank { context.getString(R.string.device_mac_unavailable) },
            R.string.device_model to device.brandModel.ifBlank { context.getString(R.string.device_model_unavailable) }).forEach { (label, value) ->
            Column(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                Text(context.getString(label), color = White60, fontSize = 14.sp)
                Spacer(Modifier.height(4.dp))
                Text(value, color = White, fontSize = 14.sp)
            }
        }
        val ports = device.details.filterKeys { it.startsWith("port_") }.entries.sortedBy { it.key.removePrefix("port_").toIntOrNull() ?: 0 }
        if (ports.isNotEmpty()) {
            Text(context.getString(R.string.device_open_ports), color = White60, fontSize = 14.sp)
            Text(ports.joinToString(" · ") { "${it.key.removePrefix("port_")}/${it.value}" }, color = White, fontSize = 14.sp)
            Spacer(Modifier.height(12.dp))
        }
        device.details.filterKeys { !it.startsWith("port_") && it != "identity_basis" && it != "mdns_device_type" }.toSortedMap().forEach { (key, value) ->
            val label = when {
                key == "mdns_name" -> context.getString(R.string.device_advertised_name)
                key == "mdns_host" -> context.getString(R.string.device_hostname)
                key == "ssdp_server" -> context.getString(R.string.device_ssdp_server)
                key == "ssdp_st" -> context.getString(R.string.device_advertised_service)
                key == "upnp_name" -> context.getString(R.string.identity_name)
                key == "upnp_type" -> context.getString(R.string.identity_declared_type)
                key == "identity_model" -> context.getString(R.string.device_model)
                key == "identity_manufacturer" -> context.getString(R.string.identity_manufacturer)
                key == "identity_firmware" -> context.getString(R.string.identity_firmware)
                key == "identity_source" -> context.getString(R.string.identity_source)
                key == "identity_query" -> context.getString(R.string.identity_query)
                key.startsWith("server_") -> context.getString(R.string.device_server_port, key.removePrefix("server_"))
                else -> key
            }
            Text(label, color = White60, fontSize = 14.sp)
            Text(if (key == "identity_query") ScanStrings.text(context, value) else value, color = White, fontSize = 14.sp)
            Spacer(Modifier.height(12.dp))
        }
        Text(context.getString(R.string.device_metadata_note), color = White60, fontSize = 12.sp)
        Spacer(Modifier.height(12.dp))
        Text(context.getString(R.string.detection_finding), color = White60, fontSize = 14.sp)
        Spacer(Modifier.height(6.dp))
        Text(if (device.isCurrentPhone) context.getString(R.string.current_phone) else when (device.finding) {
            Finding.CAMERA_FEATURES -> context.getString(R.string.finding_camera)
            Finding.NO_CAMERA_FEATURES -> context.getString(R.string.finding_no_camera)
            Finding.INSUFFICIENT -> context.getString(R.string.finding_insufficient)
            Finding.LEGACY -> context.getString(R.string.finding_legacy)
        }, color = White, fontSize = 14.sp)
        if (!device.analysisComplete) Text(context.getString(R.string.analysis_incomplete), color = White60, fontSize = 12.sp)
        device.evidence.forEach {
            Spacer(Modifier.height(6.dp))
            Text(ScanStrings.text(context, it), color = White60, fontSize = 12.sp)
        }
        if (device.userTrusted) {
            Spacer(Modifier.height(8.dp))
            Text(context.getString(R.string.trust_annotation), color = White60, fontSize = 12.sp)
        }
        if (!device.isCurrentPhone && device.finding != Finding.LEGACY) {
            Spacer(modifier = Modifier.height(20.dp))
            Box(modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .background(color = Color(0xFF00C46F), shape = RoundedCornerShape(999.dp))
                .border(width = 1.dp, shape = RoundedCornerShape(999.dp), brush = Brush.horizontalGradient(colorStops = arrayOf(0f to White10, 0.5f to Transparent, 1f to White10)))
                .clickable{
                    onMarkSafe(device)  // 传递整个设备对象
                    dialog.dismiss()     // 关闭对话框
                }
            ) {
                Row(modifier = Modifier.align(Alignment.Center)) {
                    Image(painter = painterResource(R.drawable.svg_icon_correct_white), contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(if (device.userTrusted) context.getString(R.string.remove_trust) else context.getString(R.string.mark_trusted), color = White, fontSize = 16.sp, fontWeight = FontWeight.W500)
                }
            }
        }
    }
}
