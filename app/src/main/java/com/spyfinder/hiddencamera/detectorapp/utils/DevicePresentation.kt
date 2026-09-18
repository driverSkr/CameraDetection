package com.spyfinder.hiddencamera.detectorapp.utils

import com.spyfinder.hiddencamera.detectorapp.R
import com.spyfinder.hiddencamera.detectorapp.model.WifiDevice
import com.spyfinder.hiddencamera.detectorapp.scan.Finding
import com.spyfinder.hiddencamera.detectorapp.scan.DeviceIdentity

object DevicePresentation {
    fun icon(type: String) = when (type) {
        "Router" -> R.drawable.svg_icon_wifi_info_router
        "Phone", "Likely phone or tablet" -> R.drawable.ic_device_phone
        "Likely computer" -> R.drawable.ic_device_computer
        "Printer" -> R.drawable.ic_device_printer
        "Likely television" -> R.drawable.svg_icon_tv
        "Likely storage device", "Likely video recorder" -> R.drawable.ic_device_storage
        "Likely network camera" -> R.drawable.ic_device_camera
        else -> R.drawable.ic_device_generic
    }
    fun identityStatus(device: WifiDevice, identity: DeviceIdentity.Result): Int? {
        if (identity.type != "Device type unconfirmed") return null
        return when {
            identity.basis == "Conflicting identity clues" -> R.string.identity_status_conflict
            device.details["identity_model"].orEmpty().isNotBlank() -> R.string.identity_status_model
            device.details["identity_query"] == "Authentication required" -> R.string.identity_status_auth
            device.finding == Finding.CAMERA_FEATURES -> R.string.identity_status_video
            identity.capabilities.isNotEmpty() -> R.string.identity_status_services
            else -> R.string.identity_status_unknown
        }
    }
    fun nextStep(device: WifiDevice, identity: DeviceIdentity.Result = DeviceIdentity.forDevice(device)) = when {
        device.isCurrentPhone -> R.string.ux_step_self
        identity.basis == "Conflicting identity clues" -> R.string.ux_step_conflict
        device.details["identity_query"] == "Authentication required" -> R.string.ux_step_auth
        !device.analysisComplete -> R.string.ux_step_incomplete
        device.finding == Finding.CAMERA_FEATURES -> R.string.ux_step_video
        identity.type != "Device type unconfirmed" -> R.string.ux_step_identified
        device.details["identity_model"].orEmpty().isNotBlank() -> R.string.ux_step_model
        else -> R.string.ux_step_unknown
    }

    fun needsLook(device: WifiDevice) =
        device.finding == Finding.CAMERA_FEATURES && !device.userTrusted

    fun listRank(device: WifiDevice) = when {
        needsLook(device) -> 0
        !device.analysisComplete -> 1
        device.isCurrentPhone || device.userTrusted -> 2
        else -> 3
    }

    data class ListLines(val title: String, val caption: String?)

    fun listLines(device: WifiDevice, loc: (Int) -> String, scan: (String) -> String): ListLines {
        val identity = DeviceIdentity.forDevice(device)
        val unconfirmed = identity.type == "Device type unconfirmed"
        val typeLabel = if (unconfirmed) loc(R.string.identity_network_device) else scan(identity.type)
        val reported = DeviceIdentity.name(device.details) ?: hostLabel(device.details) ?: manufacturer(device.details)
        val title = reported ?: typeLabel
        val parts = buildList {
            if (reported != null && !unconfirmed) add(typeLabel)
            if (unconfirmed) {
                when {
                    needsLook(device) -> add(loc(R.string.result_video_service))
                    identity.capabilities.isNotEmpty() -> add(scan(identity.capabilities.first()))
                }
            }
            when {
                device.isCurrentPhone -> add(loc(R.string.current_phone))
                device.userTrusted -> add(loc(R.string.result_marked_known))
            }
            if (isEmpty() && reported == null && unconfirmed) add(device.ip)
        }.distinct().filter { it.isNotBlank() && it != title }.take(2)
        return ListLines(title, parts.joinToString(" · ").ifBlank { null })
    }

    internal fun hostLabel(details: Map<String, String>): String? {
        val host = details["mdns_host"]?.lineSequence()?.firstOrNull(String::isNotBlank) ?: return null
        val short = host.trim().trimEnd('.').substringBefore('.').trim()
        if (short.isBlank() || short.any { it == ':' } || short.all { it.isDigit() || it == '.' }) return null
        return short
    }

    internal fun manufacturer(details: Map<String, String>) =
        details["identity_manufacturer"]?.lineSequence()?.firstOrNull(String::isNotBlank)
}
