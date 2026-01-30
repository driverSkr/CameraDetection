package com.ethan.cameradetection.ui.setting.page

import android.util.Log
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ethan.cameradetection.R
import com.ethan.cameradetection.theme.Black
import com.ethan.cameradetection.theme.White
import com.ethan.cameradetection.ui.setting.view.SettingItemView
import com.ethan.cameradetection.utils.LaunchUtils
import com.ethan.cameradetection.utils.ShareUtils
import com.ethan.cameradetection.utils.findBaseActivityVBind
import com.google.android.play.core.review.ReviewException
import com.google.android.play.core.review.ReviewManagerFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Composable
fun SettingPage() {
    val scope = rememberCoroutineScope()
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
                    when(settingItemList[index].second) {
                        "Share App" -> {
                            // todo 分享应用链接
                            ShareUtils.shareTextWithHighlightedLinks(context, "应用分享", "应用链接")
                        }
                        "Privacy Policy" -> {
                            // todo 待提供隐私链接
                            LaunchUtils.launchWeb(context, "https://www.baidu.com", "测试")
                        }
                        "Rate us" -> {
                            scope.launch(Dispatchers.Main) {
                                context.findBaseActivityVBind()?.let { activityVBind ->
                                    val manager = ReviewManagerFactory.create(context)
                                    val request = manager.requestReviewFlow()
                                    request.addOnCompleteListener { task ->
                                        if (task.isSuccessful) {
                                            val reviewInfo = task.result
                                            val flow = manager.launchReviewFlow(activityVBind, reviewInfo)
                                            flow.addOnCompleteListener { result ->
                                                // 用户已经看到评分弹窗，无论他们是否实际评分
                                                // 用户在这里记录日志或执行其他操作
                                                Log.d("ethan", "结果${result.result}")
                                            }
                                        } else {
                                            val reviewErrorCode = (task.exception as ReviewException).errorCode
                                            Log.e("ethan", "Review error:$reviewErrorCode")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}