package com.spyfinder.hiddencamera.detectorapp.utils

import android.content.Context
import com.spyfinder.hiddencamera.detectorapp.R

// Translate generated categories only; preserve actual network and device names.
fun Context.deviceLabel(value: String): String {
    if (value.isBlank()) return getString(R.string.unknown)
    val resource = when (value) {
    "Camera" -> R.string.device_camera
    "WiFi Router" -> R.string.device_router
    "Router" -> R.string.device_router
    "Phone" -> R.string.device_phone
    "PC" -> R.string.device_computer
    "Computer" -> R.string.device_computer
    "Windows PC" -> R.string.device_computer
    "Storage Device" -> R.string.device_storage
    "Printer" -> R.string.device_printer
    "Apple" -> R.string.device_apple
    "Smart Device" -> R.string.device_smart
    "Web Device" -> R.string.device_web
    "IoT Device" -> R.string.device_iot
    "DNS Server" -> R.string.device_dns
    "Mail Server" -> R.string.device_mail
    "Unknown Device" -> R.string.device_unknown
    "Unknown" -> R.string.unknown
    "TV" -> R.string.location_tv
        else -> return value
    }
    return getString(resource)
}

fun Context.scannerLocationLabel(value: String): String {
 return getString(when (value) {
    "TV" -> R.string.location_tv
    "Socket" -> R.string.location_socket
    "Lampshade" -> R.string.location_lampshade
    "Bedside table" -> R.string.location_bedside
    "TV Cabinet" -> R.string.location_tv_cabinet
    "Wardrobe" -> R.string.location_wardrobe
    "Sofa" -> R.string.location_sofa
    "Smoke Sensor" -> R.string.location_smoke_sensor
    "Shower Head" -> R.string.location_shower
    "Vase" -> R.string.location_vase
    "Air Conditioner" -> R.string.location_air_conditioner
    "Router" -> R.string.location_router
    else -> return value
})
}

fun Context.planLabel(id: String?, period: Boolean = false): String = getString(
    when (SubscribeHelper.getProductType(id)) {
        "Weekly" -> if (period) R.string.period_week else R.string.plan_weekly
        "Monthly" -> if (period) R.string.period_month else R.string.plan_monthly
        else -> if (period) R.string.period_year else R.string.plan_yearly
    }
)
