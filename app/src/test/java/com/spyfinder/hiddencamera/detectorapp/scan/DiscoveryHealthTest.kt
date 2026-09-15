package com.spyfinder.hiddencamera.detectorapp.scan

import java.io.IOException
import java.util.concurrent.CancellationException
import org.junit.Assert.*
import org.junit.Test

class DiscoveryHealthTest {
    @Test fun quietChannelCompletesWithoutWarning() {
        val health = DiscoveryHealth()
        health.run("mDNS", { false }) { stage -> stage(DiscoveryHealth.Stage.RECEIVING) }
        assertEquals(DiscoveryHealth.Outcome.COMPLETE, health.snapshot().getValue("mDNS").outcome)
        assertEquals("", health.warnings())
    }
    @Test fun failuresRetainChannelStageAndSurviveLaterSuccess() {
        val health = DiscoveryHealth()
        DiscoveryHealth.Stage.entries.forEach { failureStage ->
            health.run(failureStage.name, { false }) { stage -> stage(failureStage); throw IOException("private network details") }
        }
        health.run("SSDP", { false }) { }
        assertEquals(3, health.snapshot().values.count { it.outcome == DiscoveryHealth.Outcome.FAILED })
        assertTrue(health.warnings().contains("could not start"))
        assertTrue(health.warnings().contains("could not be sent"))
        assertTrue(health.warnings().contains("could not be received"))
        assertFalse(health.warnings().contains("private network details"))
    }
    @Test fun cancelledSocketIsNotReportedAsNetworkFailure() {
        val health = DiscoveryHealth()
        var closed = false
        try {
            health.run("ONVIF", { closed }) { closed = true; throw IOException("socket closed") }
            fail("Cancellation must propagate")
        } catch (_: CancellationException) { }
        assertEquals(DiscoveryHealth.Outcome.CANCELLED, health.snapshot().getValue("ONVIF").outcome)
        assertEquals("", health.warnings())
    }
    @Test fun permissionFailuresRemainActionable() {
        val health = DiscoveryHealth()
        health.run("SSDP", { false }) { throw SecurityException() }
        assertTrue(health.warnings().contains("permission was denied"))
    }
}
