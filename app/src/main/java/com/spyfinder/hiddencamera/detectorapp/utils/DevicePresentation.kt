package com.spyfinder.hiddencamera.detectorapp.utils

import com.spyfinder.hiddencamera.detectorapp.R
import com.spyfinder.hiddencamera.detectorapp.model.WifiDevice
import com.spyfinder.hiddencamera.detectorapp.scan.Finding

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
    fun nextStep(device: WifiDevice) = when {
        device.isCurrentPhone -> R.string.ux_step_self
        device.details["identity_query"] == "Authentication required" -> R.string.ux_step_auth
        !device.analysisComplete -> R.string.ux_step_incomplete
        device.finding == Finding.CAMERA_FEATURES -> R.string.ux_step_video
        else -> R.string.ux_step_unknown
    }
}
