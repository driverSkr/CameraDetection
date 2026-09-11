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
        QuietTopBar(context.getString(R.string.safety_tips)) { context.findActivity()?.finish() }
        QuietHeading(context.getString(R.string.know_how), context.getString(R.string.tips_title), context.getString(R.string.tips_description))
        QuietPanel(tinted = true) { QuietIcon(R.drawable.svg_icon_tips); Text(context.getString(R.string.tips_hero), style = MaterialTheme.typography.headlineSmall) }
        listOf(
            context.getString(R.string.tips_hotel) to context.getString(R.string.tips_hotel_body),
            context.getString(R.string.tips_public) to context.getString(R.string.tips_public_body),
            context.getString(R.string.tips_personal) to context.getString(R.string.tips_personal_body)
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
