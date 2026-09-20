package com.spyfinder.hiddencamera.detectorapp

import android.content.res.Configuration
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.spyfinder.hiddencamera.detectorapp.dialog.view.WifiInfoDetailsView
import com.spyfinder.hiddencamera.detectorapp.model.WifiDevice
import com.spyfinder.hiddencamera.detectorapp.theme.ComposeProjectTheme
import com.spyfinder.hiddencamera.detectorapp.ui.main.MainActivity
import java.util.Locale
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Regression checks for the issues confirmed during the audit. */
@RunWith(AndroidJUnit4::class)
class ReauditEvidenceTest {
    @get:Rule val compose = createEmptyComposeRule()
    @Test fun tipsTitleExpandsItsOwnContentAndExposesState() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            lateinit var title: String
            lateinit var body: String
            lateinit var expanded: String
            lateinit var collapsed: String
            scenario.onActivity { activity ->
                title = activity.getString(R.string.tips_public_title)
                body = activity.getString(R.string.tips_result_1)
                expanded = activity.getString(R.string.tips_expanded)
                collapsed = activity.getString(R.string.tips_collapsed)
                activity.findViewById<androidx.compose.ui.platform.ComposeView>(R.id.composeView).setContent {
                    ComposeProjectTheme { com.spyfinder.hiddencamera.detectorapp.ui.tips.page.TipsPage() }
                }
            }
            compose.onNodeWithText(title).assert(SemanticsMatcher.expectValue(androidx.compose.ui.semantics.SemanticsProperties.StateDescription, collapsed)).performClick()
            compose.onNodeWithText(body).performScrollTo().assertIsDisplayed()
            compose.onNodeWithText(title).performScrollTo().assert(SemanticsMatcher.expectValue(androidx.compose.ui.semantics.SemanticsProperties.StateDescription, expanded)).performClick()
            compose.onNodeWithText(body).assertDoesNotExist()
        }
    }
    @Test fun resultListLeadsWithCameraCluesAndHidesSearch() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            lateinit var needsLook: String
            lateinit var otherDevices: String
            lateinit var printerLabel: String
            lateinit var searchLabel: String
            lateinit var allList: String
            lateinit var incompleteCount: String
            scenario.onActivity { activity ->
                val state = com.spyfinder.hiddencamera.detectorapp.ui.main.context.MainContextEntity(null)
                state.trustedDevices.add(WifiDevice("Printer", "Printer", "192.168.1.20", 0, 0, 0,
                    analysisComplete = true, details = mapOf("mdns_device_type" to "Printer")))
                state.suspiciousDevices.add(WifiDevice("Recorder", "Unknown", "192.168.1.21", 0, 0, 0,
                    finding = com.spyfinder.hiddencamera.detectorapp.scan.Finding.CAMERA_FEATURES,
                    analysisComplete = false, details = mapOf("identity_model" to "NVR")))
                needsLook = activity.getString(R.string.camera_clues)
                otherDevices = activity.getString(R.string.other_devices)
                printerLabel = activity.getString(R.string.identity_printer)
                searchLabel = activity.getString(R.string.ux_search)
                allList = activity.getString(R.string.all_detection_list)
                incompleteCount = activity.resources.getQuantityString(R.plurals.ux_incomplete_count, 1, 1)
                activity.findViewById<androidx.compose.ui.platform.ComposeView>(R.id.composeView).setContent {
                    CompositionLocalProvider(com.spyfinder.hiddencamera.detectorapp.ui.main.context.LocalMainContextEntity provides state,
                        androidx.activity.compose.LocalActivityResultRegistryOwner provides activity) {
                        ComposeProjectTheme { com.spyfinder.hiddencamera.detectorapp.ui.main.view.DetectResultView() }
                    }
                }
            }
            compose.onNodeWithText("$allList (2)").assertIsDisplayed()
            compose.onNodeWithText("$needsLook (1)").assertIsDisplayed()
            compose.onNodeWithText("$otherDevices (1)").assertIsDisplayed()
            compose.onNodeWithText("NVR").assertIsDisplayed()
            compose.onNodeWithText(printerLabel).performScrollTo().assertIsDisplayed()
            compose.onNodeWithText(searchLabel).assertDoesNotExist()
            compose.onNodeWithText(incompleteCount).assertDoesNotExist()
            compose.onNodeWithText("192.168.1.20").assertDoesNotExist()
            compose.onNodeWithText("192.168.1.21").assertDoesNotExist()
            val nvr = compose.onNodeWithText("NVR").fetchSemanticsNode().boundsInRoot
            val printer = compose.onNodeWithText(printerLabel).fetchSemanticsNode().boundsInRoot
            assertTrue("Camera clues should sort above other devices", nvr.top < printer.top)
            compose.onNodeWithText("$printerLabel (1)").performClick()
            compose.onNodeWithText("NVR").assertDoesNotExist()
            compose.onNodeWithText(printerLabel).assertIsDisplayed()
            compose.onNodeWithText("$allList (2)").performClick()
            compose.onNodeWithText("NVR").assertIsDisplayed()
            compose.onNodeWithText("$otherDevices (1)").performClick()
            compose.onNodeWithText(printerLabel).assertDoesNotExist()
            compose.onNodeWithText("NVR").assertIsDisplayed()
        }
    }

    @Test fun successfulPriceRetryClearsLoadingError() {
        var attempts = 0
        val vm = com.spyfinder.hiddencamera.detectorapp.ui.subscribe.viewmodel.SubscribeViewModel {
            if (++attempts == 1) error("Simulated store outage")
            listOf(com.spyfinder.hiddencamera.detectorapp.model.SubModel())
        }
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { vm.load(it.applicationContext) }
            compose.waitUntil(5000) { !vm.loading && vm.productMessageRes != 0 }
            scenario.onActivity { vm.load(it.applicationContext, true) }
            compose.waitUntil(5000) { !vm.loading && vm.products.isNotEmpty() }
            assertTrue(vm.productMessageRes == 0)
        }
    }

    @Test fun unreadableHistoryReportsFailureAndRetryRecovers() {
        val base = androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().targetContext
        val directory = java.nio.file.Files.createTempDirectory(base.cacheDir.toPath(), "reaudit-history-").toFile()
        val file = java.io.File(directory, "scan_history_v3.json")
        val isolated = object : android.content.ContextWrapper(base) {
            override fun getApplicationContext(): android.content.Context = this
            override fun getFilesDir(): java.io.File = directory
            override fun getSharedPreferences(name: String, mode: Int): android.content.SharedPreferences =
                base.getSharedPreferences(directory.name + name, mode)
        }
        try {
            file.writeText("{\"version\":999}")
            val archive = kotlinx.coroutines.runBlocking {
                com.spyfinder.hiddencamera.detectorapp.utils.ScanHistoryStore.load(isolated)
            }
            assertTrue(archive.recent == null && archive.complete == null)
                        assertTrue(com.spyfinder.hiddencamera.detectorapp.utils.ScanHistoryStore.readFailed.value)
            file.writeText("{\"version\":3,\"records\":{}}")
            kotlinx.coroutines.runBlocking { com.spyfinder.hiddencamera.detectorapp.utils.ScanHistoryStore.load(isolated) }
            assertTrue(!com.spyfinder.hiddencamera.detectorapp.utils.ScanHistoryStore.readFailed.value)
        } finally { file.delete(); directory.delete() }
    }

    @Test fun ipLabelAndAddressDoNotOverlapAtLargeFont() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                val english = activity.createConfigurationContext(Configuration(activity.resources.configuration).apply { setLocale(Locale.ENGLISH) })
                val dialog = BottomSheetDialog(activity)
                val device = WifiDevice("Unknown", "Unknown", "192.168.255.254", 0, 0, 0,
                    details = mapOf("port_8554" to "RTSP", "server_8554" to "Example streaming server"))
                activity.findViewById<androidx.compose.ui.platform.ComposeView>(R.id.composeView).setContent {
                    CompositionLocalProvider(LocalContext provides english,
                        LocalDensity provides Density(activity.resources.displayMetrics.density, 2f)) {
                        ComposeProjectTheme {
                            Box(Modifier.requiredSize(360.dp, 480.dp)) {
                                WifiInfoDetailsView(dialog, device) {}
                            }
                        }
                    }
                }
            }
            compose.onNodeWithText("192.168.255.254").performScrollTo()
            val label = compose.onNodeWithText("IP Address").fetchSemanticsNode().boundsInRoot
            val value = compose.onNodeWithText("192.168.255.254").fetchSemanticsNode().boundsInRoot
            android.util.Log.i("ReauditEvidence", "IP label=$label value=$value")
            assertTrue("IP label overlaps address", label.bottom <= value.top)
            compose.onNodeWithText("Show technical details").performScrollTo().performClick()
            compose.onNodeWithText("8554/RTSP").performScrollTo().assertIsDisplayed()
            compose.onNodeWithText("Example streaming server").performScrollTo().assertIsDisplayed()
        }
    }
}
