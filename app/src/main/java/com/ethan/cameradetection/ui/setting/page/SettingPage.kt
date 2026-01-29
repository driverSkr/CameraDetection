package com.ethan.cameradetection.ui.setting.page

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ethan.cameradetection.R
import com.ethan.cameradetection.theme.Black
import com.ethan.cameradetection.theme.White
import com.ethan.cameradetection.ui.setting.view.SettingItemView
import com.ethan.cameradetection.ui.subscribe.SubscribeActivity
import com.ethan.cameradetection.utils.findBaseActivityVBind

@Composable
fun SettingPage() {
    val context = LocalContext.current
    val settingItemList = listOf(
        Pair(R.drawable.svg_icon_share_app, "Share App"),
        Pair(R.drawable.svg_icon_privacy_policy, "Privacy Policy"),
//        Pair(R.drawable.svg_icon_restore, "Restore"),
        Pair(R.drawable.svg_icon_rate_us, "Rate us"),
    )

    Column(modifier = Modifier.fillMaxSize().background(color = Black).statusBarsPadding()) {
        Box(modifier = Modifier.fillMaxWidth().height(54.dp).padding(start = 12.dp, end = 16.dp)) {
            Image(
                painter = painterResource(R.drawable.svg_icon_back),
                contentDescription = null,
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .clickable{ context.findBaseActivityVBind()?.finish() }
            )
            Text("Setting", color = White, fontSize = 18.sp, fontWeight = FontWeight.W500, modifier = Modifier.align(Alignment.Center))
            // todo 第一版先不要订阅页
//            Image(
//                painter = painterResource(R.mipmap.img_crown),
//                contentDescription = null,
//                modifier = Modifier
//                    .size(40.dp)
//                    .align(Alignment.CenterEnd)
//                    .clickable{ SubscribeActivity.launch(context) }
//            )
        }

        // todo 第一版先不要订阅页
//        Spacer(modifier = Modifier.height(28.dp))
//        Image(
//            painter = painterResource(R.mipmap.img_subscribe_card),
//            contentScale = ContentScale.Fit,
//            contentDescription = null,
//            modifier = Modifier
//                .padding(horizontal = 16.dp)
//                .fillMaxWidth()
//                .clickable{ SubscribeActivity.launch(context) }
//        )
        Spacer(modifier = Modifier.height(19.dp))
        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(settingItemList.size) { index ->
                SettingItemView(settingItemList[index]) {

                }
            }
        }
    }
}