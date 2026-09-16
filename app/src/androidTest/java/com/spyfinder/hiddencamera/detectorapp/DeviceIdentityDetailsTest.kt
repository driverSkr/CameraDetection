package com.spyfinder.hiddencamera.detectorapp

import android.content.Context
import android.content.res.Configuration
import android.graphics.Bitmap
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ActivityScenario
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.spyfinder.hiddencamera.detectorapp.dialog.view.WifiInfoDetailsView
import com.spyfinder.hiddencamera.detectorapp.model.WifiDevice
import com.spyfinder.hiddencamera.detectorapp.theme.ComposeProjectTheme
import com.spyfinder.hiddencamera.detectorapp.ui.main.MainActivity
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.io.File
import java.util.Locale

class DeviceIdentityDetailsTest {
    @get:Rule val compose = createEmptyComposeRule()

    @Test fun chineseDetailsShowKnownModelAndReasonBeforeTechnicalExpansion() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            lateinit var context: Context
            lateinit var screenshot: File
            scenario.onActivity { activity ->
                context = activity.createConfigurationContext(Configuration(activity.resources.configuration).apply { setLocale(Locale.SIMPLIFIED_CHINESE) })
                screenshot = File(activity.getExternalFilesDir(null), "identity-details-zh.png")
                val dialog = BottomSheetDialog(activity)
                val device = WifiDevice("", "", "192.168.1.20", 0, 0, 0, details = mapOf(
                    "upnp_name" to "客厅设备", "identity_manufacturer" to "Example", "identity_model" to "XYZ-123",
                    "ssdp_st" to "urn:schemas-upnp-org:device:MediaRenderer:1", "port_80" to "HTTP"))
                activity.findViewById<androidx.compose.ui.platform.ComposeView>(R.id.composeView).setContent {
                    CompositionLocalProvider(LocalContext provides context) {
                        ComposeProjectTheme { WifiInfoDetailsView(dialog, device) {} }
                    }
                }
            }
            compose.onNodeWithText(context.getString(R.string.identity_status_model)).assertIsDisplayed()
            compose.onNodeWithText("XYZ-123").assertIsDisplayed()
            compose.onNodeWithText("Example").assertIsDisplayed()
            compose.onNodeWithText(context.getString(R.string.ux_service_playback)).assertIsDisplayed()
            compose.onNodeWithText("80/HTTP").assertDoesNotExist()
            compose.onRoot().captureToImage().asAndroidBitmap().let { bitmap ->
                screenshot.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
            }
            compose.onNodeWithText(context.getString(R.string.ux_step_model)).performScrollTo().assertIsDisplayed()
            compose.onNodeWithText(context.getString(R.string.ux_show_technical)).performScrollTo().performClick()
            compose.onNodeWithText("80/HTTP").performScrollTo().assertIsDisplayed()
        }
    }

    @Test fun largeTextUnknownDetailsRemainScrollableAndTrustActionReachable() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            lateinit var context: Context
            var marked = false
            scenario.onActivity { activity ->
                context = activity.createConfigurationContext(Configuration(activity.resources.configuration).apply { setLocale(Locale.ENGLISH) })
                val dialog = BottomSheetDialog(activity)
                val device = WifiDevice("", "", "192.168.1.21", 0, 0, 0)
                activity.findViewById<androidx.compose.ui.platform.ComposeView>(R.id.composeView).setContent {
                    CompositionLocalProvider(LocalContext provides context,
                        LocalDensity provides Density(activity.resources.displayMetrics.density, 2f)) {
                        ComposeProjectTheme {
                            Box(Modifier.requiredSize(360.dp, 480.dp)) {
                                WifiInfoDetailsView(dialog, device) { marked = true }
                            }
                        }
                    }
                }
            }
            compose.onNodeWithText(context.getString(R.string.identity_status_unknown)).performScrollTo().assertIsDisplayed()
            compose.onNodeWithText(context.getString(R.string.mark_trusted)).performScrollTo().performClick()
            compose.runOnIdle { assertTrue(marked) }
        }
    }
}
