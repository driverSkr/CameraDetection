package com.spyfinder.hiddencamera.detectorapp.ui.tips.page

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.spyfinder.hiddencamera.detectorapp.R
import com.spyfinder.hiddencamera.detectorapp.ui.components.*
import com.spyfinder.hiddencamera.detectorapp.utils.findActivity

@Composable
fun TipsPage() {
    val context = LocalContext.current
    QuietPage(navigationPadding = true) {
        QuietTopBar("Safety tips") { context.findActivity()?.finish() }
        QuietHeading("A little know-how", "Look closer.\nStay aware.", "Practical checks, at your own pace.")
        QuietPanel(tinted = true) { QuietIcon(R.drawable.svg_icon_tips); Text("Start with what faces the room.", style = MaterialTheme.typography.headlineSmall) }
        listOf(
            "Hotel & rental rooms" to "Look over objects facing the bed or changing area. Check unfamiliar holes or unusual placements without opening electrical equipment.",
            "Public spaces" to "Pay attention to unfamiliar objects near changing areas. If something concerns you, ask staff for help.",
            "Personal precautions" to "Combine network review with a visual check. If you remain concerned, leave the area and contact the property operator."
        ).forEachIndexed { index, (title, description) ->
            var expanded by rememberSaveable { mutableStateOf(index == 0) }
            QuietPanel {
                Row(Modifier.fillMaxWidth().clickable { expanded = !expanded }.padding(vertical = 12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(title, Modifier.weight(1f)); Text(if (expanded) "−" else "+")
                }
                AnimatedVisibility(expanded) { QuietBody(description) }
            }
        }
    }
}
