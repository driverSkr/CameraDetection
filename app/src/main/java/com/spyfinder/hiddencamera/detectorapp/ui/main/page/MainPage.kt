package com.spyfinder.hiddencamera.detectorapp.ui.main.page

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.spyfinder.hiddencamera.detectorapp.R
import com.spyfinder.hiddencamera.detectorapp.event.Event
import com.spyfinder.hiddencamera.detectorapp.ui.main.context.LocalMainContextEntity

@Composable
fun MainPage() {
    val main = LocalMainContextEntity.current
    Column(Modifier.fillMaxSize()) {
        Box(Modifier.weight(1f)) {
            when (main.selectTabIndex.intValue) {
                0 -> DetectPage()
                1 -> SensorPage()
                2 -> ScannerPage()
                else -> FeaturePage()
            }
        }
        if (!main.isShowResult.value || main.selectTabIndex.intValue != 0) NavigationBarView()
    }
}

@Composable
fun NavigationBarView(modifier: Modifier = Modifier) {
    val main = LocalMainContextEntity.current
    val context = LocalContext.current
    val labels = listOf("Wi-Fi", "Magnetic", "Scanner", "Tools")
    val names = listOf("detect", "magnetic", "scanner", "feature")
    val icons = listOf(R.drawable.svg_icon_detect, R.drawable.svg_icon_sensor, R.drawable.svg_icon_scanner, R.drawable.svg_icon_feature)
    NavigationBar(modifier, containerColor = MaterialTheme.colorScheme.surface, tonalElevation = 0.dp) {
        labels.forEachIndexed { index, label ->
            NavigationBarItem(selected = main.selectTabIndex.intValue == index, onClick = {
                Event.event(context, Event.TAB_CLICK, Event.PARAM_TAB to names[index])
                main.selectTabIndex.intValue = index
            }, icon = { Icon(painterResource(icons[index]), null, Modifier.size(22.dp)) }, label = { Text(label) },
                colors = NavigationBarItemDefaults.colors(indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                    selectedIconColor = MaterialTheme.colorScheme.primary, selectedTextColor = MaterialTheme.colorScheme.primary))
        }
    }
}
