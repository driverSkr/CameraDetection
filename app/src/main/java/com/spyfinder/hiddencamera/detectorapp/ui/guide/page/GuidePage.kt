package com.spyfinder.hiddencamera.detectorapp.ui.guide.page

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import com.spyfinder.hiddencamera.detectorapp.R
import com.spyfinder.hiddencamera.detectorapp.ui.components.*

@Composable
fun GuidePage(onComplete: (() -> Unit)? = null) {
    var page by rememberSaveable { mutableIntStateOf(0) }
    val icons = listOf(R.drawable.svg_icon_wifi, R.drawable.svg_icon_scanner, R.drawable.svg_icon_magnetic)
    val titles = listOf("Know what’s\nconnected.", "Take a\ncloser look.", "Notice the\nfield changes.")
    val descriptions = listOf("Review devices connected to your current Wi-Fi network.", "Use your camera and color filters to inspect unfamiliar objects.", "Use the magnetic sensor as an additional check around your space.")
    key(page) {
        QuietPage(navigationPadding = true) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text("SpyFinder"); QuietBadge("GETTING STARTED") }
            QuietOrbit(icons[page])
            QuietHeading(listOf("01 / Network", "02 / Camera", "03 / Magnetic")[page], titles[page], descriptions[page])
            LinearProgressIndicator(progress = { (page + 1) / 3f }, modifier = Modifier.fillMaxWidth())
            QuietBody("${page + 1} of 3", true)
            QuietButton(if (page == 2) "Get started" else "Continue") { if (page < 2) page++ else onComplete?.invoke() }
            if (page > 0) QuietButton("Back", secondary = true) { page-- }
        }
    }
}
