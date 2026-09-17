package com.spyfinder.hiddencamera.detectorapp.scan

import org.junit.Assert.*
import org.junit.Test

class WifiNetworkPolicyTest {
    @Test fun singleLocalWifiIsReady() {
        assertNull(scanPreflightMessage(airplane = true, localWifiCount = 1, hasWifi = true, hasVpn = true))
    }

    @Test fun airplaneWithoutWifiIsCalledOut() {
        assertEquals(SCAN_AIRPLANE_MODE, scanPreflightMessage(true, 0, hasWifi = false, hasVpn = false))
    }

    @Test fun wifiPlusVpnAsksToDisconnectVpn() {
        assertEquals(SCAN_DISCONNECT_VPN, scanPreflightMessage(false, 0, hasWifi = true, hasVpn = true))
    }

    @Test fun noWifiUsesConnectMessage() {
        assertEquals(SCAN_CONNECT_WIFI, scanPreflightMessage(false, 0, hasWifi = false, hasVpn = false))
        assertEquals(SCAN_CONNECT_WIFI, scanPreflightMessage(false, 0, hasWifi = false, hasVpn = true))
    }

    @Test fun multipleLocalWifiIsAmbiguous() {
        assertEquals(SCAN_MULTIPLE_NETWORKS, scanPreflightMessage(false, 2, hasWifi = true, hasVpn = false))
    }
}
