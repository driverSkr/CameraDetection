package com.ethan.cameradetection.ui.guide.page

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ethan.cameradetection.R
import com.ethan.cameradetection.theme.Black
import com.ethan.cameradetection.ui.guide.view.GuideBannerView
import com.ethan.cameradetection.ui.main.context.LocalMainContextEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Composable
@Preview
fun GuidePage() {
    val context = LocalContext.current
    val localMain = LocalMainContextEntity.current
    val scope = rememberCoroutineScope()
    val bannerInfo = listOf(
        Triple(R.mipmap.img_guide_1, "Scan for Hidden Cameras", "Find hidden cameras on your Wi-Fi network to protect your privacy."),
        Triple(R.mipmap.img_guide_2, "Infrared Red Dot Detection", "Identify suspicious cameras using infrared scanning, even in the dark."),
        Triple(R.mipmap.img_guide_3, "Identify Suspicious Devices", "Stay protected from hidden cameras and devices where you are")
    )
    val pagerState = rememberPagerState { bannerInfo.size }

    Box(modifier = Modifier.fillMaxSize().background(color = Black)) {
        Image(painter = painterResource(R.mipmap.bg_mask), contentScale = ContentScale.Crop, contentDescription = null)

        Column(modifier = Modifier.fillMaxSize()) {
            GuideBannerView(
                modifier = Modifier.fillMaxHeight(0.75f),
                pagerState = pagerState,
                bannerInfo = bannerInfo
            )
            Spacer(modifier = Modifier.height(48.dp))
            Box(modifier = Modifier
                .padding(horizontal = 24.dp)
                .fillMaxWidth()
                .height(56.dp)
                .background(color = Color(0xFF00C46F), shape = RoundedCornerShape(999.dp))
                .clickable{
                    if (pagerState.currentPage < pagerState.pageCount - 1) {
                        scope.launch(Dispatchers.Main) {
                            pagerState.animateScrollToPage(pagerState.currentPage + 1)
                        }
                    } else {
                        localMain.isOpenMainPage = true
                    }
                }
            ) {
                Text(
                    text = "Continue",
                    color = Color(0xFFFFFFFF),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.W500,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
        }
    }
}