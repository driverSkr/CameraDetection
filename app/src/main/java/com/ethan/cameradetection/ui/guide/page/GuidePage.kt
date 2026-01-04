package com.ethan.cameradetection.ui.guide.page

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import com.ethan.cameradetection.R
import com.ethan.cameradetection.theme.Black

@Composable
@Preview
fun GuidePage() {
    val pagerState = rememberPagerState { 3 }
    Box(modifier = Modifier.fillMaxSize().background(color = Black)) {
        Image(painter = painterResource(R.mipmap.bg_mask), contentScale = ContentScale.Crop, contentDescription = null)

        Column(modifier = Modifier.fillMaxSize()) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxWidth().fillMaxHeight(0.7f)
            ) { page ->

            }
        }
    }
}