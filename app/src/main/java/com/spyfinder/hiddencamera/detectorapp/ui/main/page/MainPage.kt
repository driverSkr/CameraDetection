package com.spyfinder.hiddencamera.detectorapp.ui.main.page

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.spyfinder.hiddencamera.detectorapp.R
import com.spyfinder.hiddencamera.detectorapp.event.Event
import com.spyfinder.hiddencamera.detectorapp.ui.main.context.LocalMainContextEntity

@Composable
fun MainPage() {
    val localMain = LocalMainContextEntity.current
//    val pagerState = rememberPagerState { 4 }

    Box(modifier = Modifier
        .fillMaxSize()
        .background(color = Color(0xFF000000))) {
        Image(painter = painterResource(R.mipmap.bg_mask), contentScale = ContentScale.Crop, contentDescription = null)

        Column(modifier = Modifier.fillMaxSize()) {
//            HorizontalPager(
//                state = pagerState,
//                userScrollEnabled = false,
//                beyondViewportPageCount = 3,
//                modifier = Modifier.fillMaxWidth().weight(1f)
//            ) { page ->
//                when(page) {
//                    0 -> DetectPage()
//                    1 -> SensorPage()
//                    2 -> ScannerPage()
//                    3 -> FeaturePage(pagerState)
//                }
//            }
            AnimatedContent(localMain.selectTabIndex.intValue, modifier = Modifier.fillMaxWidth().weight(1f)) { index ->
                when(index) {
                    0 -> DetectPage()
                    1 -> SensorPage()
                    2 -> ScannerPage()
                    3 -> FeaturePage()
                }
            }

            if (!localMain.isShowResult.value || localMain.selectTabIndex.intValue != 0) {
                NavigationBarView(
                    modifier = Modifier
                        .navigationBarsPadding()
                        .fillMaxWidth()
                        .height(64.dp)
                )
            }
        }
    }
}

@Composable
fun NavigationBarView(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val localMain = LocalMainContextEntity.current

    fun switchTab(index: Int, tabName: String) {
        // 底部导航点击埋点，用于观察基础功能入口分布。
        Event.event(context, Event.TAB_CLICK, Event.PARAM_TAB to tabName)
        localMain.selectTabIndex.intValue = index
    }

    Row(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .weight(1f)
                .then(
                    if (localMain.selectTabIndex.intValue == 0) Modifier.alpha(1f) else Modifier.alpha(0.5f)
                )
                .clickable {
                    switchTab(0, "detect")
                },
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Image(painter = painterResource(R.drawable.svg_icon_detect), contentDescription = null)
            Spacer(modifier = Modifier.height(4.dp))
            Text("Detect", color = Color(0xFFFFFFFF), fontSize = 12.sp, fontWeight = FontWeight.W500)
        }
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .weight(1f)
                .then(
                    if (localMain.selectTabIndex.intValue == 1) Modifier.alpha(1f) else Modifier.alpha(0.5f)
                )
                .clickable {
                    switchTab(1, "magnetic")
                },
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Image(painter = painterResource(R.drawable.svg_icon_sensor), contentDescription = null)
            Spacer(modifier = Modifier.height(4.dp))
            Text("Sensor", color = Color(0xFFFFFFFF), fontSize = 12.sp, fontWeight = FontWeight.W500)
        }
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .weight(1f)
                .then(
                    if (localMain.selectTabIndex.intValue == 2) Modifier.alpha(1f) else Modifier.alpha(0.5f)
                )
                .clickable {
                    switchTab(2, "scanner")
                },
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Image(painter = painterResource(R.drawable.svg_icon_scanner), contentDescription = null)
            Spacer(modifier = Modifier.height(4.dp))
            Text("Scanner", color = Color(0xFFFFFFFF), fontSize = 12.sp, fontWeight = FontWeight.W500)
        }
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .weight(1f)
                .then(
                    if (localMain.selectTabIndex.intValue == 3) Modifier.alpha(1f) else Modifier.alpha(0.5f)
                )
                .clickable {
                    switchTab(3, "feature")
                },
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Image(painter = painterResource(R.drawable.svg_icon_feature), contentDescription = null)
            Spacer(modifier = Modifier.height(4.dp))
            Text("Feature", color = Color(0xFFFFFFFF), fontSize = 12.sp, fontWeight = FontWeight.W500)
        }
    }
}
