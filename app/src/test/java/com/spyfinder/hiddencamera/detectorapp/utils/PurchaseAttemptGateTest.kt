package com.spyfinder.hiddencamera.detectorapp.utils

import org.junit.Assert.*
import org.junit.Test

class PurchaseAttemptGateTest {
    @Test fun purchaseSuccessIsDeliveredOnlyOnce() {
        val gate = PurchaseAttemptGate(); val id = gate.begin()
        assertTrue(gate.complete(id)); assertFalse(gate.complete(id)); assertFalse(gate.accepts(id))
    }
    @Test fun delayedCallbackCannotAffectNewAttempt() {
        val gate = PurchaseAttemptGate(); val old = gate.begin(); val current = gate.begin()
        assertFalse(gate.accepts(old)); assertFalse(gate.complete(old)); assertTrue(gate.complete(current))
    }
    @Test fun pendingAttemptCanStillCompleteLater() {
        val gate = PurchaseAttemptGate(); val id = gate.begin()
        assertTrue(gate.accepts(id)); assertTrue(gate.accepts(id)); assertTrue(gate.complete(id))
    }
}
