package com.spyfinder.hiddencamera.detectorapp.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.spyfinder.hiddencamera.detectorapp.R

@Composable
fun QuietPage(navigationPadding: Boolean = false, footer: (@Composable () -> Unit)? = null, content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).statusBarsPadding()
        .then(if (navigationPadding) Modifier.navigationBarsPadding() else Modifier)) {
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 22.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp), content = content)
        if (footer != null) Column(Modifier.fillMaxWidth().padding(horizontal = 22.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)) { footer() }
    }
}

@Composable
fun QuietHeading(eyebrow: String, title: String, description: String = "") {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(eyebrow.uppercase(), fontSize = 11.sp, letterSpacing = 1.7.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
        Text(title, fontSize = 32.sp, lineHeight = 36.sp, letterSpacing = (-1).sp, fontWeight = FontWeight.SemiBold)
        if (description.isNotEmpty()) QuietBody(description)
    }
}

@Composable
fun QuietBody(text: String, small: Boolean = false) {
    Text(text, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = if (small) 12.sp else 14.sp, lineHeight = if (small) 18.sp else 22.sp)
}

@Composable
fun QuietButton(text: String, secondary: Boolean = false, enabled: Boolean = true, onClick: () -> Unit) {
    val modifier = Modifier.fillMaxWidth().heightIn(min = 54.dp)
    if (secondary) OutlinedButton(onClick, modifier, enabled = enabled, shape = RoundedCornerShape(16.dp)) {
        Text(text, textAlign = TextAlign.Center, modifier = Modifier.padding(vertical = 5.dp))
    } else Button(onClick, modifier, enabled = enabled, shape = RoundedCornerShape(16.dp)) {
        Text(text, textAlign = TextAlign.Center, modifier = Modifier.padding(vertical = 5.dp))
    }
}

@Composable
fun QuietPanel(tinted: Boolean = false, content: @Composable ColumnScope.() -> Unit) {
    Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp),
        color = if (tinted) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
        border = if (tinted) null else androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp), content = content)
    }
}

@Composable
fun QuietIcon(@DrawableRes icon: Int, modifier: Modifier = Modifier) {
    Box(modifier.size(46.dp).clip(RoundedCornerShape(15.dp)).background(MaterialTheme.colorScheme.primaryContainer), contentAlignment = Alignment.Center) {
        Icon(painterResource(icon), null, Modifier.size(24.dp), tint = MaterialTheme.colorScheme.primary)
    }
}

@Composable
fun QuietTopBar(title: String, onBack: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onBack) { Icon(painterResource(R.drawable.svg_icon_back), androidx.compose.ui.res.stringResource(R.string.back), Modifier.size(24.dp)) }
        Text(title, Modifier.weight(1f), fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
        Spacer(Modifier.width(48.dp))
    }
}

@Composable
fun QuietBadge(text: String, warning: Boolean = false) {
    Surface(shape = CircleShape, color = if (warning) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.primaryContainer) {
        Text(text, Modifier.padding(horizontal = 10.dp, vertical = 6.dp), fontSize = 11.sp, fontWeight = FontWeight.SemiBold,
            color = if (warning) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.primary)
    }
}

@Composable
fun QuietRow(@DrawableRes icon: Int, title: String, description: String, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).clickable(onClick = onClick).padding(vertical = 12.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        QuietIcon(icon)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            QuietBody(description, small = true)
        }
        Icon(painterResource(R.drawable.svg_icon_next), null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun QuietOrbit(@DrawableRes icon: Int = R.drawable.svg_icon_detect, value: String? = null, label: String = "", diameter: androidx.compose.ui.unit.Dp = 224.dp) {
    val colors = MaterialTheme.colorScheme
    Box(Modifier.fillMaxWidth().padding(vertical = 12.dp), contentAlignment = Alignment.Center) {
        Box(Modifier.size(diameter), contentAlignment = Alignment.Center) {
            Canvas(Modifier.fillMaxSize()) {
                val center = Offset(size.width / 2, size.height / 2)
                drawCircle(colors.outlineVariant, size.width / 2 - 1.dp.toPx(), center, style = Stroke(1.dp.toPx()))
                drawCircle(colors.outlineVariant, size.width * .39f, center, style = Stroke(1.dp.toPx()))
                drawCircle(colors.primaryContainer, size.width * .32f, center)
                drawCircle(colors.primary, 5.dp.toPx(), Offset(size.width * .24f, size.height * .075f))
            }
            if (value == null) QuietIcon(icon, Modifier.size(72.dp))
            else Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(value, fontSize = 46.sp, lineHeight = 50.sp, letterSpacing = (-2).sp, fontWeight = FontWeight.Medium)
                Text(label, fontSize = 12.sp, color = colors.onSurfaceVariant, textAlign = TextAlign.Center)
            }
        }
    }
}

@Composable
fun QuietNote(text: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(Modifier.width(2.dp).height(40.dp).background(MaterialTheme.colorScheme.primary))
        QuietBody(text, small = true)
    }
}

@Composable
fun QuietStats(review: Int, confirmed: Int) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        listOf(review to androidx.compose.ui.res.stringResource(R.string.needs_review), confirmed to androidx.compose.ui.res.stringResource(R.string.phone_confirmed)).forEachIndexed { index, (count, label) ->
            Surface(Modifier.weight(1f), shape = RoundedCornerShape(22.dp),
                color = if (index == 0) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.surface) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(count.toString(), fontSize = 38.sp, fontWeight = FontWeight.Medium,
                        color = if (index == 0) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSurface)
                    Text(label, fontSize = 12.sp)
                }
            }
        }
    }
}
