package com.spyfinder.hiddencamera.detectorapp.scan

import org.junit.Assert.*
import org.junit.Test
import java.util.concurrent.CancellationException

class DiscoveryPolicyTest {
    @Test fun silentAddressUsesFullTimeoutOnEveryPort() {
        val calls = mutableListOf<Pair<Int, Int>>()
        val result = DiscoveryPolicy().discover({ true }) { port, timeout ->
            calls.add(port to timeout)
            ProbeResult.TIMEOUT
        }
        assertTrue(result.checked)
        assertFalse(result.responded)
        assertEquals(ProbeSupport.ports.map { it to 450 }, calls)
    }

    @Test fun slowVideoDeviceIsStillFound() {
        val result = DiscoveryPolicy().discover({ true }) { port, timeout ->
            if (port == 8554 && timeout >= 400) ProbeResult.OPEN else ProbeResult.TIMEOUT
        }
        assertTrue(result.responded)
    }

    @Test fun routeFailuresRemainUnverified() {
        assertTrue(DiscoveryPolicy().discover({ true }) { _, _ -> ProbeResult.UNREACHABLE }.uncertain)
    }

    @Test fun cancellationBetweenPortsCannotCompleteAddress() {
        var calls = 0
        val result = DiscoveryPolicy().discover({ calls < 1 }) { _, _ -> calls++; ProbeResult.TIMEOUT }
        assertFalse(result.checked)
    }

    @Test(expected = CancellationException::class) fun socketCancellationPropagates() {
        DiscoveryPolicy().discover({ true }) { _, _ -> ProbeResult.CANCELLED }
    }

    @Test fun replyStopsProbingAndConcurrencyIsBounded() {
        val policy = DiscoveryPolicy()
        var calls = 0
        assertTrue(policy.discover({ true }) { _, _ -> calls++; ProbeResult.REFUSED }.responded)
        assertEquals(1, calls)
        assertEquals(2, policy.workers(2))
        assertEquals(32, policy.workers(254))
        assertEquals(48, policy.workers(1022))
        assertEquals(64, policy.workers(2046))
    }
}
