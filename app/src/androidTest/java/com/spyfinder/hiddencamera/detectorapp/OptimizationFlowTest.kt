package com.spyfinder.hiddencamera.detectorapp

import android.content.Context
import android.content.res.Configuration
import androidx.activity.compose.LocalActivityResultRegistryOwner
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.lifecycle.ViewModelProvider
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.spyfinder.hiddencamera.detectorapp.scan.ScanViewModel
import com.spyfinder.hiddencamera.detectorapp.theme.ComposeProjectTheme
import com.spyfinder.hiddencamera.detectorapp.ui.main.MainActivity
import com.spyfinder.hiddencamera.detectorapp.ui.main.context.LocalMainContextEntity
import com.spyfinder.hiddencamera.detectorapp.ui.main.page.ScannerPage
import com.spyfinder.hiddencamera.detectorapp.ui.main.page.SensorPage
import com.spyfinder.hiddencamera.detectorapp.ui.camera.CameraScannerActivity
import java.util.Locale
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class OptimizationFlowTest {
    @get:Rule val compose = createEmptyComposeRule()
    @get:Rule val cameraPermission: androidx.test.rule.GrantPermissionRule =
        androidx.test.rule.GrantPermissionRule.grant(android.Manifest.permission.CAMERA)

    @androidx.test.filters.SdkSuppress(minSdkVersion = 28)
    @Test fun wifiBackedVpnIsNotALocalScanTarget() {
        val wifi = android.net.NetworkRequest.Builder().addTransportType(android.net.NetworkCapabilities.TRANSPORT_WIFI).build()
        val vpn = android.net.NetworkRequest.Builder().addTransportType(android.net.NetworkCapabilities.TRANSPORT_WIFI)
            .addTransportType(android.net.NetworkCapabilities.TRANSPORT_VPN).build()
        assertTrue(com.spyfinder.hiddencamera.detectorapp.scan.isLocalWifi(wifi::hasTransport))
        assertFalse(com.spyfinder.hiddencamera.detectorapp.scan.isLocalWifi(vpn::hasTransport))
        assertFalse(com.spyfinder.hiddencamera.detectorapp.scan.isLocalWifi(null))
    }

    private fun setPage(activity: MainActivity, sensor: Boolean): Context {
        val config = Configuration(activity.resources.configuration).apply { setLocale(Locale.ENGLISH) }
        val english = activity.createConfigurationContext(config)
        val state = ViewModelProvider(activity)[ScanViewModel::class.java].state
        state.selectTabIndex.intValue = if (sensor) 1 else 2
        activity.findViewById<androidx.compose.ui.platform.ComposeView>(R.id.composeView).setContent {
            CompositionLocalProvider(LocalContext provides english, LocalMainContextEntity provides state,
                LocalActivityResultRegistryOwner provides activity,
                LocalDensity provides Density(activity.resources.displayMetrics.density, 2f)) {
                ComposeProjectTheme {
                    Box(Modifier.requiredSize(360.dp, 480.dp).testTag("smallViewport")) {
                        if (sensor) SensorPage() else ScannerPage()
                    }
                }
            }
        }
        return english
    }

    @Test fun smallScannerKeepsHeaderOutsideGridAndLastRowReachable() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            lateinit var english: Context
            scenario.onActivity { english = setPage(it, false) }
            val header = compose.onNodeWithText(english.getString(R.string.scanner_description)).fetchSemanticsNode().boundsInRoot
            val first = compose.onNodeWithText(english.getString(R.string.object_tv)).fetchSemanticsNode().boundsInRoot
            assertTrue("Grid overlaps the header", first.top >= header.bottom)
            compose.onNode(hasScrollToIndexAction()).performScrollToIndex(11)
            compose.onNodeWithText(english.getString(R.string.object_router)).assertIsDisplayed()
        }
    }

    @Test fun smallMagneticPageShowsUntruncatedHelpAndReachableButton() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            lateinit var english: Context
            scenario.onActivity { english = setPage(it, true) }
            val help = compose.onNodeWithText(english.getString(R.string.magnetic_help)).performScrollTo()
            val layouts = mutableListOf<TextLayoutResult>()
            help.performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(layouts) }
            assertTrue(layouts.isNotEmpty())
            assertFalse("Help text is clipped", layouts.single().hasVisualOverflow)
            compose.onNodeWithText(english.getString(R.string.start_detection)).performScrollTo().assertIsDisplayed()
        }
    }

    @Test fun rapidScannerClicksLaunchOnceAndReturningUnlocksEntry() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val monitor = instrumentation.addMonitor(CameraScannerActivity::class.java.name, null, false)
        try {
            ActivityScenario.launch(MainActivity::class.java).use { scenario ->
                lateinit var label: String
                scenario.onActivity {
                    ViewModelProvider(it)[ScanViewModel::class.java].state.selectTabIndex.intValue = 2
                    label = it.getString(R.string.object_tv)
                }
                val action = compose.onNodeWithText(label).fetchSemanticsNode().config[SemanticsActions.OnClick].action!!
                // Deliver a burst on the UI thread before navigation can cover the originating page.
                compose.runOnUiThread { repeat(5) { action() } }
                compose.waitForIdle()
                val opened = monitor.waitForActivityWithTimeout(10_000)
                assertNotNull(opened)
                instrumentation.waitForIdleSync()
                assertEquals(1, monitor.hits)
                instrumentation.runOnMainSync { opened.finish() }
                compose.waitUntil(10_000) { scenario.state == androidx.lifecycle.Lifecycle.State.RESUMED }
                compose.waitForIdle()
                val entry = compose.onNodeWithText(label).assertIsEnabled()
                val again = entry.fetchSemanticsNode().config[SemanticsActions.OnClick].action!!
                compose.runOnUiThread { again() }
                compose.waitForIdle()
                val second = monitor.waitForActivityWithTimeout(10_000)
                assertNotNull(second)
                instrumentation.waitForIdleSync()
                assertEquals(2, monitor.hits)
                instrumentation.runOnMainSync { second.finish() }
            }
        } finally { instrumentation.removeMonitor(monitor) }
    }
}
