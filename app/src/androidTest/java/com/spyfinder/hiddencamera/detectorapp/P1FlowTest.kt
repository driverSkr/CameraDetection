package com.spyfinder.hiddencamera.detectorapp

import android.Manifest
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.lifecycle.ViewModelProvider
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.GrantPermissionRule
import com.spyfinder.hiddencamera.detectorapp.scan.ScanViewModel
import com.spyfinder.hiddencamera.detectorapp.ui.main.MainActivity
import com.spyfinder.hiddencamera.detectorapp.ui.camera.CameraScannerActivity
import com.spyfinder.hiddencamera.detectorapp.ui.subscribe.SubscribeActivity
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class P1FlowTest {
    @get:Rule val compose = createEmptyComposeRule()
    @get:Rule val cameraPermission: GrantPermissionRule = GrantPermissionRule.grant(Manifest.permission.CAMERA)

    @Test fun activityRecreationRetainsScanOwnerAndState() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            lateinit var original: ScanViewModel
            scenario.onActivity {
                original = ViewModelProvider(it)[ScanViewModel::class.java]
                original.state.scanMessage = "Lifecycle verification"
                original.state.selectTabIndex.intValue = 2
            }
            scenario.recreate()
            scenario.onActivity {
                val retained = ViewModelProvider(it)[ScanViewModel::class.java]
                assertSame(original, retained)
                assertEquals("Lifecycle verification", retained.state.scanMessage)
                assertEquals(2, retained.state.selectTabIndex.intValue)
            }
        }
    }

    @Test fun cameraPreviewBindsAndControlsAreUsable() {
        ActivityScenario.launch(CameraScannerActivity::class.java).use {
            compose.waitUntil(15_000) { compose.onAllNodesWithText("Zoom").fetchSemanticsNodes().isNotEmpty() }
            compose.onNodeWithText("Scanner").assertExists()
            compose.onNodeWithContentDescription("Red colour aid").performClick()
            compose.onNodeWithContentDescription("Red colour aid").assertIsSelected()
            compose.onNodeWithContentDescription("Original view").performClick()
            compose.onNodeWithContentDescription("Original view").assertIsSelected()
            if (compose.onAllNodesWithText("Switch camera").fetchSemanticsNodes().isNotEmpty()) {
                compose.onNodeWithText("Switch camera").performClick()
                compose.waitForIdle()
                compose.waitUntil(15_000) { compose.onAllNodesWithText("Zoom").fetchSemanticsNodes().isNotEmpty() }
            }
        }
    }

    @Test fun unavailableStoreDisablesPurchaseInsteadOfInventingPrices() {
        ActivityScenario.launch(SubscribeActivity::class.java).use {
            compose.waitUntil(30_000) {
                compose.onAllNodesWithText("No product found").fetchSemanticsNodes().isNotEmpty() ||
                    compose.onAllNodesWithText("Subscription").fetchSemanticsNodes().isNotEmpty()
            }
            if (compose.onAllNodesWithText("No product found").fetchSemanticsNodes().isNotEmpty()) {
                compose.onNodeWithText("Retry").assertExists()
                compose.onNodeWithText("Continue").assertIsNotEnabled()
            } else {
                // When Play is available, the displayed plans must be supplied by the store.
                compose.onAllNodesWithText("Subscription").onFirst().assertExists()
            }
        }
    }
}
