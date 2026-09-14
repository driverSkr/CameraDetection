package com.spyfinder.hiddencamera.detectorapp.utils

import com.spyfinder.hiddencamera.detectorapp.model.WifiDevice
import com.spyfinder.hiddencamera.detectorapp.scan.ScanCoverage
import com.spyfinder.hiddencamera.detectorapp.scan.ScanStatus

data class ScanRecord(
    val id: String, val startedAt: Long, val endedAt: Long?, val network: String,
    val status: ScanStatus?, val coverage: ScanCoverage, val summary: String,
    val devices: List<WifiDevice>
) {
    fun interrupted() = if (status == ScanStatus.RUNNING) copy(status = ScanStatus.CANCELLED,
        summary = "The previous scan was interrupted. Start a new scan.") else this
    fun trust(ip: String, trusted: Boolean) = copy(devices = devices.map {
        if (it.ip == ip && !it.isCurrentPhone) it.copy(userTrusted = trusted) else it
    })
}

data class ScanArchive(val recent: ScanRecord? = null, val complete: ScanRecord? = null) {
    fun record(value: ScanRecord): ScanArchive {
        if (value.coverage.checked == 0 && value.devices.none { !it.isCurrentPhone }) return this
        return copy(recent = value, complete = if (value.status == ScanStatus.COMPLETE) value else complete)
    }
    fun trust(recordId: String, ip: String, trusted: Boolean) = copy(
        recent = recent?.let { if (it.id == recordId) it.trust(ip, trusted) else it },
        complete = complete?.let { if (it.id == recordId) it.trust(ip, trusted) else it })
    fun interrupted() = copy(recent = recent?.interrupted())
}
