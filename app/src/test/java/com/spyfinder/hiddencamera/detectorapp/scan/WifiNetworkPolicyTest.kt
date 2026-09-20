package com.spyfinder.hiddencamera.detectorapp.scan

import com.spyfinder.hiddencamera.detectorapp.R
import org.junit.Assert.*
import org.junit.Test

class WifiNetworkPolicyTest {
    @Test fun singleLocalWifiIsReady() {
        assertNull(scanPreflightMessage(airplane = true, localWifiCount = 1, hasWifi = true, hasVpn = true))
    }

    @Test fun airplaneWithoutWifiIsCalledOut() {
        assertEquals(R.string.scan_airplane, scanPreflightMessage(true, 0, hasWifi = false, hasVpn = false))
    }

    @Test fun wifiPlusVpnAsksToDisconnectVpn() {
        assertEquals(R.string.scan_disconnect_vpn, scanPreflightMessage(false, 0, hasWifi = true, hasVpn = true))
    }

    @Test fun noWifiUsesConnectMessage() {
        assertEquals(R.string.scan_connect_wifi, scanPreflightMessage(false, 0, hasWifi = false, hasVpn = false))
        assertEquals(R.string.scan_connect_wifi, scanPreflightMessage(false, 0, hasWifi = false, hasVpn = true))
    }

    @Test fun multipleLocalWifiIsAmbiguous() {
        assertEquals(R.string.scan_multiple_networks, scanPreflightMessage(false, 2, hasWifi = true, hasVpn = false))
    }
}
