package com.spyfinder.hiddencamera.detectorapp.scan

import com.spyfinder.hiddencamera.detectorapp.model.WifiDevice
import org.junit.Assert.*
import org.junit.Test
import kotlin.system.measureNanoTime

class DeviceSnapshotCacheTest {
    private fun device(ip: String) = WifiDevice("Unknown", "Unknown", ip, 0, 0, 0,
        evidence = List(8) { "TCP: Port ${5000 + it} is open (service not confirmed)" })
    @Test fun onlyChangedEvidenceReplacesTheSnapshotAndKeepsAddressOrder() {
        val cache = DeviceSnapshotCache()
        val later = device("192.168.1.10"); val earlier = device("192.168.1.2")
        cache.update(later); cache.update(earlier)
        val first = cache.snapshot()
        assertEquals(listOf(earlier, later), first)
        cache.update(later.copy())
        assertSame(first, cache.snapshot())
        cache.update(later.copy(analysisComplete = false))
        assertNotSame(first, cache.snapshot())
        assertTrue(first.last().analysisComplete)
        assertFalse(cache.snapshot().last().analysisComplete)
    }
    @Test fun measuresUnchangedPollingWithoutRebuildingRows() {
        for (count in listOf(200, 1000, 4096)) {
            val devices = (1..count).map { device(ScanRules.address(0xc0a80000L + it)) }
            val cache = DeviceSnapshotCache().also { state -> devices.forEach(state::update) }
            val snapshot = cache.snapshot()
            fun oldSnapshot() = devices.map { it.copy(evidence = it.evidence.distinct().map { line -> "$line" }) }.sortedBy { ScanRules.ipv4(it.ip) }
            repeat(3) { oldSnapshot() }
            val before = measureNanoTime { repeat(20) { assertEquals(count, oldSnapshot().size) } } / 20_000_000.0
            val after = measureNanoTime { repeat(20) { assertSame(snapshot, cache.snapshot()) } } / 20_000_000.0
            println("SNAPSHOT count=$count previousUnchangedPollMs=$before cachedUnchangedPollMs=$after")
        }
    }
}
