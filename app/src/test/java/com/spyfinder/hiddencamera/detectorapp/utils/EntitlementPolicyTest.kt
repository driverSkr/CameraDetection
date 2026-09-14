package com.spyfinder.hiddencamera.detectorapp.utils

import org.junit.Assert.assertEquals
import org.junit.Test

class EntitlementPolicyTest {
    @Test fun failedQueryIsNotAnUnpaidCustomer() { assertEquals(AccessStatus.UNKNOWN, EntitlementPolicy.resolve(null, false)) }
    @Test fun lifetimeAccessSurvivesSubscriptionQueryFailure() { assertEquals(AccessStatus.ACTIVE, EntitlementPolicy.resolve(null, true)) }
    @Test fun subscriptionAccessSurvivesLifetimeQueryFailure() { assertEquals(AccessStatus.ACTIVE, EntitlementPolicy.resolve(true, null)) }
    @Test fun onlyTwoSuccessfulEmptyQueriesCanDenyAccess() { assertEquals(AccessStatus.INACTIVE, EntitlementPolicy.resolve(false, false)) }
    @Test fun coldStartIsUnknown() { assertEquals(AccessStatus.UNKNOWN, EntitlementPolicy.resolve(null, null)) }
}
