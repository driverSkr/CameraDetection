package com.spyfinder.hiddencamera.detectorapp.scan

import com.spyfinder.hiddencamera.detectorapp.model.WifiDevice

/** Published rows are immutable by convention; progress-only ticks reuse the same list instance. */
class DeviceSnapshotCache {
    private val devices = mutableMapOf<String, WifiDevice>()
    private var cached = emptyList<WifiDevice>()
    private var dirty = false
    @Synchronized fun update(device: WifiDevice) {
        if (devices[device.ip] != device) { devices[device.ip] = device; dirty = true }
    }
    @Synchronized fun snapshot(): List<WifiDevice> {
        if (dirty) {
            cached = devices.values.sortedBy { ScanRules.ipv4(it.ip) }
            dirty = false
        }
        return cached
    }
}
