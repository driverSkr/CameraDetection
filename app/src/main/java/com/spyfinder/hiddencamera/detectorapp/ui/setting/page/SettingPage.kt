package com.spyfinder.hiddencamera.detectorapp.ui.setting.page

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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.android.play.core.review.ReviewException
import com.google.android.play.core.review.ReviewManagerFactory
import com.spyfinder.hiddencamera.detectorapp.R
import com.spyfinder.hiddencamera.detectorapp.theme.Black
import com.spyfinder.hiddencamera.detectorapp.theme.White
import com.spyfinder.hiddencamera.detectorapp.ui.setting.view.SettingItemView
import com.spyfinder.hiddencamera.detectorapp.ui.subscribe.SubscribeActivity
import com.spyfinder.hiddencamera.detectorapp.utils.LaunchUtils
import com.spyfinder.hiddencamera.detectorapp.utils.ShareUtils
import com.spyfinder.hiddencamera.detectorapp.utils.SubscribeHelper
import com.spyfinder.hiddencamera.detectorapp.utils.findBaseActivityVBind
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Composable
fun SettingPage() {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val isSubscribed = SubscribeHelper.isSubscribedFlow.collectAsState().value
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
        }

        if (!isSubscribed) {
            Spacer(modifier = Modifier.height(28.dp))
            // 未订阅时展示订阅卡片，订阅后自动隐藏。
            Image(
                painter = painterResource(R.mipmap.img_subscribe_card),
                contentScale = ContentScale.Fit,
                contentDescription = null,
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .fillMaxWidth()
                    .clickable{ SubscribeActivity.launch(context) }
            )
        }
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
                            ShareUtils.shareTextWithHighlightedLinks(context, "应用分享", "https://play.google.com/store/apps/details?id=" + context.packageName)
                        }
                        "Privacy Policy" -> {
                            LaunchUtils.launchWeb(context, "https://sites.google.com/view/spycamerafinder-privacy-policy/home", context.getString(R.string.app_name))
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
