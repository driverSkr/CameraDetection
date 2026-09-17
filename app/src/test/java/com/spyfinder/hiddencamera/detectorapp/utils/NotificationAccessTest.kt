package com.spyfinder.hiddencamera.detectorapp.utils

import org.junit.Assert.*
import org.junit.Test

class NotificationAccessTest {
    @Test fun belowAndroid13DoesNotAsk() {
        assertEquals(NotificationAccess.Step.NOT_REQUIRED, NotificationAccess.step(32, false, false, false))
    }

    @Test fun grantedPermissionStartsImmediately() {
        assertEquals(NotificationAccess.Step.GRANTED, NotificationAccess.step(33, true, false, false))
        assertEquals(NotificationAccess.Step.GRANTED, NotificationAccess.step(33, true, true, false))
    }

    @Test fun firstAskAndRationaleUseAnExplanation() {
        assertEquals(NotificationAccess.Step.EXPLAIN, NotificationAccess.step(33, false, false, false))
        assertEquals(NotificationAccess.Step.EXPLAIN, NotificationAccess.step(33, false, true, true))
    }

    @Test fun permanentlyDeniedOpensSettings() {
        assertEquals(NotificationAccess.Step.SETTINGS, NotificationAccess.step(33, false, true, false))
    }
}
