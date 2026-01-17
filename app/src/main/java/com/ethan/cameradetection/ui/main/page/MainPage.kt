package com.ethan.cameradetection.ui.main.page

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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ethan.cameradetection.R
import com.ethan.cameradetection.ui.main.context.LocalMainContextEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Composable
fun MainPage() {
    val localMain = LocalMainContextEntity.current
    val pagerState = rememberPagerState { 4 }

    Box(modifier = Modifier.fillMaxSize().background(color = Color(0xFF000000))) {
        Image(painter = painterResource(R.mipmap.bg_mask), contentScale = ContentScale.Crop, contentDescription = null)

        Column(modifier = Modifier.fillMaxSize()) {
            HorizontalPager(
                state = pagerState,
                userScrollEnabled = false,
                modifier = Modifier.fillMaxWidth().weight(1f)
            ) { page ->
                when(page) {
                    0 -> DetectPage()
                    1 -> SensorPage()
                    2 -> ScannerPage()
                    3 -> FeaturePage()
                }
            }

            if (!localMain.isShowResult.value || pagerState.currentPage != 0) {
                NavigationBarView(
                    modifier = Modifier.navigationBarsPadding().fillMaxWidth().height(64.dp),
                    pagerState = pagerState
                )
            }
        }
    }
}

@Composable
fun NavigationBarView(modifier: Modifier = Modifier, pagerState: PagerState) {
    val scope = rememberCoroutineScope()

    Row(modifier = modifier) {
        Column(
            modifier = Modifier.fillMaxHeight().weight(1f).then(
                if (pagerState.currentPage == 0) Modifier.alpha(1f) else Modifier.alpha(0.5f)
            ).clickable {
                scope.launch(Dispatchers.Main) {
                    pagerState.animateScrollToPage(0)
                }
            },
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Image(painter = painterResource(R.drawable.svg_icon_detect), contentDescription = null)
            Spacer(modifier = Modifier.height(4.dp))
            Text("Detect", color = Color(0xFFFFFFFF), fontSize = 10.sp, fontWeight = FontWeight.W500)
        }
        Column(
            modifier = Modifier.fillMaxHeight().weight(1f).then(
                if (pagerState.currentPage == 1) Modifier.alpha(1f) else Modifier.alpha(0.5f)
            ).clickable {
                scope.launch(Dispatchers.Main) {
                    pagerState.animateScrollToPage(1)
                }
            },
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Image(painter = painterResource(R.drawable.svg_icon_sensor), contentDescription = null)
            Spacer(modifier = Modifier.height(4.dp))
            Text("Sensor", color = Color(0xFFFFFFFF), fontSize = 10.sp, fontWeight = FontWeight.W500)
        }
        Column(
            modifier = Modifier.fillMaxHeight().weight(1f).then(
                if (pagerState.currentPage == 2) Modifier.alpha(1f) else Modifier.alpha(0.5f)
            ).clickable {
                scope.launch(Dispatchers.Main) {
                    pagerState.animateScrollToPage(2)
                }
            },
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Image(painter = painterResource(R.drawable.svg_icon_scanner), contentDescription = null)
            Spacer(modifier = Modifier.height(4.dp))
            Text("Scanner", color = Color(0xFFFFFFFF), fontSize = 10.sp, fontWeight = FontWeight.W500)
        }
        Column(
            modifier = Modifier.fillMaxHeight().weight(1f).then(
                if (pagerState.currentPage == 3) Modifier.alpha(1f) else Modifier.alpha(0.5f)
            ).clickable {
                scope.launch(Dispatchers.Main) {
                    pagerState.animateScrollToPage(3)
                }
            },
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Image(painter = painterResource(R.drawable.svg_icon_feature), contentDescription = null)
            Spacer(modifier = Modifier.height(4.dp))
            Text("Feature", color = Color(0xFFFFFFFF), fontSize = 10.sp, fontWeight = FontWeight.W500)
        }
    }
}