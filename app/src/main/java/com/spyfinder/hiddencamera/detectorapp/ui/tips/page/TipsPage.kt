package com.spyfinder.hiddencamera.detectorapp.ui.tips.page

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.spyfinder.hiddencamera.detectorapp.R
import com.spyfinder.hiddencamera.detectorapp.theme.Black
import com.spyfinder.hiddencamera.detectorapp.theme.White
import com.spyfinder.hiddencamera.detectorapp.theme.White10
import com.spyfinder.hiddencamera.detectorapp.theme.White60
import com.spyfinder.hiddencamera.detectorapp.utils.findBaseActivityVBind

@Composable
fun TipsPage() {
    val context = LocalContext.current
    val sections = listOf(
        R.string.tips_home_title to listOf(R.string.tips_body_1, R.string.tips_body_2, R.string.tips_body_3),
        R.string.tips_public_title to listOf(R.string.tips_result_1, R.string.tips_result_2, R.string.tips_result_3),
        R.string.tips_strategy_title to listOf(R.string.tips_action_1, R.string.tips_action_2, R.string.tips_action_3)
    )
    Column(Modifier.fillMaxSize().background(Black).statusBarsPadding().navigationBarsPadding()) {
        Box(Modifier.fillMaxWidth().heightIn(min = 54.dp).padding(horizontal = 12.dp)) {
            Image(painterResource(R.drawable.svg_icon_back), contentDescription = context.getString(R.string.a11y_back),
                modifier = Modifier.align(Alignment.CenterStart).size(48.dp).clickable { context.findBaseActivityVBind()?.finish() }.padding(12.dp))
            Text(context.getString(R.string.title_tips), color = White, fontSize = 18.sp,
                fontWeight = FontWeight.W500, modifier = Modifier.align(Alignment.Center).padding(horizontal = 48.dp, vertical = 12.dp))
        }
        LazyColumn(Modifier.weight(1f).fillMaxWidth(), contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            sections.forEach { (title, bodies) ->
                item(key = title) { TipSection(title, bodies) }
            }
        }
    }
}

@Composable
private fun TipSection(title: Int, bodies: List<Int>) {
    val context = LocalContext.current
    var expanded by rememberSaveable(title) { mutableStateOf(false) }
    Column(Modifier.fillMaxWidth().animateContentSize().background(White10, RoundedCornerShape(20.dp))) {
        Row(Modifier.fillMaxWidth().heightIn(min = 48.dp)
            .semantics(mergeDescendants = true) {
                stateDescription = context.getString(if (expanded) R.string.tips_expanded else R.string.tips_collapsed)
            }
            .clickable(role = Role.Button, onClickLabel = context.getString(if (expanded) R.string.tips_collapse else R.string.tips_expand)) { expanded = !expanded }
            .padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(context.getString(title), color = White, fontSize = 14.sp, fontWeight = FontWeight.W600,
                modifier = Modifier.weight(1f))
            Spacer(Modifier.width(12.dp))
            Image(painterResource(if (expanded) R.drawable.svg_icon_down else R.drawable.svg_icon_next), contentDescription = null)
        }
        if (expanded) {
            Column(Modifier.padding(start = 12.dp, end = 12.dp, bottom = 12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                bodies.forEachIndexed { index, body ->
                    Row(Modifier.fillMaxWidth()) {
                        Text("${index + 1}. ", color = White60, fontSize = 12.sp)
                        Text(context.getString(body), color = White60, fontSize = 12.sp, modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}