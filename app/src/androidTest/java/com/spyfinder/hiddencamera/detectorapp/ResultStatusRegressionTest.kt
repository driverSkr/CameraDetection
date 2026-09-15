package com.spyfinder.hiddencamera.detectorapp

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.spyfinder.hiddencamera.detectorapp.ui.main.MainActivity
import com.spyfinder.hiddencamera.detectorapp.ui.main.context.*
import com.spyfinder.hiddencamera.detectorapp.ui.main.view.DetectResultView
import com.spyfinder.hiddencamera.detectorapp.scan.ScanStatus
import com.spyfinder.hiddencamera.detectorapp.theme.ComposeProjectTheme
import com.spyfinder.hiddencamera.detectorapp.utils.ScanStrings
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ResultStatusRegressionTest {
    @get:Rule val compose = createEmptyComposeRule()

    @Test fun failureReasonIsVisibleWithoutExpansionAndReturnOpensScan() {
        checkStatus(ScanStatus.FAILED, R.string.ux_scan_failed_empty, true)
    }

    @Test fun cancelledScanDoesNotClaimFiltersHidDevices() {
        checkStatus(ScanStatus.CANCELLED, R.string.ux_scan_incomplete_empty, true)
    }

    @Test fun completedEmptyScanHasItsOwnExplanation() {
        checkStatus(ScanStatus.COMPLETE, R.string.ux_scan_empty, false)
    }

    private fun checkStatus(status: ScanStatus, emptyResource: Int, attention: Boolean) {
        val state = MainContextEntity(null)
        lateinit var reason: String
        lateinit var empty: String
        lateinit var noMatches: String
        lateinit var returnLabel: String
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                state.currentRecordId = "status-regression"
                state.scanStatus = status
                state.scanMessage = "Connect to Wi-Fi before scanning."
                state.isShowResult.value = true
                reason = ScanStrings.text(activity, state.scanMessage)
                empty = activity.getString(emptyResource)
                noMatches = activity.getString(R.string.ux_no_matches)
                returnLabel = activity.getString(R.string.ux_return_to_scan)
                activity.findViewById<androidx.compose.ui.platform.ComposeView>(R.id.composeView).setContent {
                    CompositionLocalProvider(LocalMainContextEntity provides state,
                        androidx.activity.compose.LocalActivityResultRegistryOwner provides activity) {
                        ComposeProjectTheme { DetectResultView() }
                    }
                }
            }
            if (attention) compose.onNodeWithText(reason).performScrollTo().assertIsDisplayed()
            compose.onNodeWithText(empty).performScrollTo().assertIsDisplayed()
            compose.onNodeWithText(noMatches).assertDoesNotExist()
            if (attention) {
                compose.onNodeWithText(returnLabel).performScrollTo().performClick()
                compose.runOnIdle {
                    assertFalse(state.isShowResult.value)
                    assertEquals(0, state.selectTabIndex.intValue)
                    assertFalse(state.pendingWifiAutoScan.value)
                }
            }
        }
    }
}
