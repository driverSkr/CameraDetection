package com.spyfinder.hiddencamera.detectorapp

import android.graphics.Bitmap
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.Density
import androidx.test.platform.app.InstrumentationRegistry
import com.spyfinder.hiddencamera.detectorapp.model.WifiDevice
import com.spyfinder.hiddencamera.detectorapp.theme.ComposeProjectTheme
import com.spyfinder.hiddencamera.detectorapp.ui.guide.page.GuidePage
import com.spyfinder.hiddencamera.detectorapp.ui.main.context.*
import com.spyfinder.hiddencamera.detectorapp.ui.main.page.MainPage
import com.spyfinder.hiddencamera.detectorapp.ui.main.view.DetectResultView
import com.spyfinder.hiddencamera.detectorapp.ui.tips.page.TipsPage
import com.spyfinder.hiddencamera.detectorapp.utils.SubscribeHelper
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.io.File

class QuietUiTest {
    @get:Rule val compose = createComposeRule()

    private fun label(id: Int) = InstrumentationRegistry.getInstrumentation().targetContext.getString(id)

    private fun snapshot(name: String) {
        compose.waitForIdle()
        val dir = File(InstrumentationRegistry.getInstrumentation().targetContext.getExternalFilesDir(null), "ui-redesign").apply { mkdirs() }
        File(dir, "$name.png").outputStream().use { stream ->
            compose.onRoot().captureToImage().asAndroidBitmap().compress(Bitmap.CompressFormat.PNG, 100, stream)
        }
    }

    @Test fun guideContinuesAndReturnsBeforeCompleting() {
        var completed = false
        compose.setContent { ComposeProjectTheme(darkTheme = false) { GuidePage { completed = true } } }
        compose.onNodeWithText(label(R.string.guide_network_title)).assertIsDisplayed()
        snapshot("01-guide-light")
        compose.onNodeWithText(label(R.string.action_continue)).performScrollTo().performClick()
        compose.onNodeWithText(label(R.string.guide_camera_title)).assertExists()
        compose.onNodeWithText(label(R.string.back)).performScrollTo().performClick()
        compose.onNodeWithText(label(R.string.guide_network_title)).assertExists()
        repeat(2) { compose.onNodeWithText(label(R.string.action_continue)).performScrollTo().performClick() }
        compose.onNodeWithText(label(R.string.get_started)).performScrollTo().performClick()
        compose.runOnIdle { assertTrue(completed) }
    }

    @Test fun navigationRetainsAllToolsAndAllTwelveLocations() {
        val main = MainContextEntity(null)
        compose.setContent { ComposeProjectTheme(darkTheme = false) {
            CompositionLocalProvider(LocalMainContextEntity provides main) { MainPage() }
        } }
        compose.onNodeWithText(label(R.string.scan_wifi)).assertExists()
        snapshot("02-wifi-light")
        compose.onNodeWithText(label(R.string.nav_magnetic)).performClick()
        snapshot("03-magnetic-light")
        compose.onNodeWithText(label(R.string.nav_scanner)).performClick()
        compose.onNodeWithText(label(R.string.location_tv)).assertExists()
        compose.onNodeWithText(label(R.string.location_router)).performScrollTo().assertIsDisplayed()
        snapshot("04-scanner-light")
        compose.onNodeWithText(label(R.string.nav_tools)).performClick()
        compose.onNodeWithText(label(R.string.safety_tips)).assertExists()
        snapshot("05-tools-light")
        compose.onNodeWithText(label(R.string.magnetic_check)).performClick()
        compose.runOnIdle { assertEquals(1, main.selectTabIndex.intValue) }
    }

    @Test fun lockedResultsNeverExposeDeviceIdentity() {
        val main = MainContextEntity(null)
        main.suspiciousDevices.add(WifiDevice("private-device-name", "Camera", "192.168.1.20", R.drawable.svg_icon_sensor, 0, 0, riskLevel = 1))
        compose.runOnIdle { SubscribeHelper.updateSubscribeState(false) }
        compose.setContent { ComposeProjectTheme(darkTheme = false) {
            CompositionLocalProvider(LocalMainContextEntity provides main) { DetectResultView() }
        } }
        compose.onNodeWithText(label(R.string.unlock_details)).performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("private-device-name").assertDoesNotExist()
        compose.onNodeWithText(label(R.string.needs_review)).assertExists()
        snapshot("06-locked-results-light")
    }

    @Test fun tipsRemainUsableWithLargeTextAndDarkTheme() {
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, 1.5f)) {
                ComposeProjectTheme(darkTheme = true) { TipsPage() }
            }
        }
        compose.onNodeWithText(label(R.string.tips_personal)).performScrollTo().performClick()
        compose.onNodeWithText(label(R.string.tips_personal_body))
            .performScrollTo().assertIsDisplayed()
        snapshot("07-tips-dark-large-text")
    }
}
