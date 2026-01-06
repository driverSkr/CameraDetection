package com.ethan.cameradetection.ui.guide.view

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ethan.cameradetection.R

@Composable
fun GuideBannerView(modifier: Modifier = Modifier, pagerState: PagerState, bannerInfo: List<Triple<Int, String, String>>) {

    Column(modifier.fillMaxWidth()) {
        HorizontalPager(
            state = pagerState,
            modifier = modifier.weight(1f)
        ) { page ->
            Column(modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Box(modifier = Modifier.weight(1f)) {
                    BannerPhoto(
                        modifier = Modifier.align(Alignment.BottomCenter),
                        page = page,
                        photo = bannerInfo[page].first
                    )
                    Box(modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .height(100.dp)
                        .background(brush = Brush.verticalGradient(colorStops = arrayOf(0f to Color(0x00000000), 1f to Color(0xFF000000))))
                    )
                    if (page == 2) {
                        Image(painter = painterResource(R.mipmap.img_banner_3_card), modifier = Modifier.align(Alignment.Center).offset(y = 127.dp), contentDescription = null)
                    }
                }
                Spacer(modifier = Modifier.height(21.dp))
                Text(bannerInfo[page].second, color = Color(0xFF00C46F), fontSize = 24.sp, fontWeight = FontWeight.W700, textAlign = TextAlign.Center)
                Spacer(modifier = Modifier.height(8.dp))
                Text(bannerInfo[page].third, color = Color(0xFF96939E), fontSize = 14.sp, fontWeight = FontWeight.W400, textAlign = TextAlign.Center)
            }
        }
        Spacer(modifier = Modifier.height(27.dp))
        BannerIndicator(modifier = Modifier.align(Alignment.CenterHorizontally), pagerState = pagerState, num = bannerInfo.size)
    }
}

@Composable
fun BannerPhoto(modifier: Modifier = Modifier, page: Int, photo: Int) {

    Box(modifier = modifier) {
        Image(painter = painterResource(photo), contentDescription = null)
        when(page) {
            0 -> {
                Image(
                    painter = painterResource(R.mipmap.img_radar),
                    modifier = Modifier.align(Alignment.Center).offset(y = 20.dp),
                    contentDescription = null
                )
                Image(
                    painter = painterResource(R.mipmap.img_position),
                    modifier = Modifier.size(64.dp).align(Alignment.CenterEnd).offset(x = (-10).dp, y = (-90).dp),
                    contentDescription = null
                )
                Image(
                    painter = painterResource(R.mipmap.img_position),
                    modifier = Modifier.size(52.dp).align(Alignment.CenterStart).offset(x = (-24).dp, y = 65.dp),
                    contentDescription = null
                )
                Image(
                    painter = painterResource(R.mipmap.img_position),
                    modifier = Modifier.size(48.dp).align(Alignment.CenterEnd).offset(x = 24.dp, y = 120.dp),
                    contentDescription = null
                )
            }
            1 -> {
                Image(
                    painter = painterResource(R.mipmap.img_camera_left),
                    modifier = Modifier.size(88.dp).align(Alignment.CenterEnd).offset(x = 20.dp, y = (-40).dp),
                    contentDescription = null
                )
                Image(
                    painter = painterResource(R.mipmap.img_camera_right),
                    modifier = Modifier.size(61.dp).align(Alignment.CenterStart).offset(x = (-15).dp, y = 30.dp),
                    contentDescription = null
                )

                Row(modifier = Modifier.align(Alignment.BottomCenter).offset(y = (-40).dp), horizontalArrangement = Arrangement.spacedBy(17.5.dp)) {
                    val colorList = listOf(Color(0xFFDD1313), Color(0xFF00C424), Color(0xFF1C73FF))
                    repeat(3) { index ->
                        Box(modifier = Modifier
                            .border(width = 1.5.dp, color = if (index == 0) Color(0xFFFFFFFF) else Color(0x00FFFFFF), shape = RoundedCornerShape(999.dp))
                            .padding(4.dp)
                            .size(43.dp)
                            .background(
                                color = colorList[index],
                                shape = RoundedCornerShape(999.dp)
                            )
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun BannerIndicator(modifier: Modifier = Modifier, pagerState: PagerState, num: Int) {
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        repeat(num) { index ->
            val isSelected = index == pagerState.currentPage % num

            Box(modifier = Modifier
                .size(10.dp)
                .background(
                    color = if (isSelected) Color(0xFFFFFFFF) else Color(0xFF5B5B5E),
                    shape = RoundedCornerShape(999.dp)
                )
            )
        }
    }
}