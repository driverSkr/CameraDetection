package com.spyfinder.hiddencamera.detectorapp.scan

import org.junit.Assert.*
import org.junit.Test

class MacLookupTest {
    @Test fun formatsSixByteAddressAndDropsDummy() {
        assertEquals("aa:bb:cc:dd:ee:ff", MacLookup.format(byteArrayOf(0xaa.toByte(), 0xbb.toByte(), 0xcc.toByte(), 0xdd.toByte(), 0xee.toByte(), 0xff.toByte())))
        assertNull(MacLookup.format(ByteArray(6)))
        assertNull(MacLookup.format(byteArrayOf(2, 0, 0, 0, 0, 0)))
    }

    @Test fun readsCompleteArpRowsOnly() {
        val table = """
            IP address       HW type     Flags       HW address            Mask     Device
            192.168.1.1      0x1         0x2         aa:bb:cc:dd:ee:01     *        wlan0
            192.168.1.8      0x1         0x0         00:00:00:00:00:00     *        wlan0
        """.trimIndent()
        assertEquals("aa:bb:cc:dd:ee:01", MacLookup.parseArpMac(table, "192.168.1.1"))
        assertNull(MacLookup.parseArpMac(table, "192.168.1.8"))
        assertNull(MacLookup.parseArpMac(table, "10.0.0.1"))
    }
}
