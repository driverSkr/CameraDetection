package com.spyfinder.hiddencamera.detectorapp.ui.setting.page

import android.util.Log
import android.widget.Toast
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
import kotlinx.coroutines.withContext

@Composable
fun SettingPage() {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val isSubscribed = SubscribeHelper.isSubscribedFlow.collectAsState().value
    val settingItemList = listOf(
        Pair(R.drawable.svg_icon_share_app, "Share App"),
        Pair(R.drawable.svg_icon_privacy_policy, "Privacy Policy"),
        Pair(R.drawable.svg_icon_restore, "Restore"),
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
                        "Restore" -> {
                            scope.launch(Dispatchers.Default) {
                                SubscribeHelper.refreshSubscribeStateSuspend()
                                withContext(Dispatchers.Main) {
                                    Toast.makeText(context, "订阅状态刷新完成", Toast.LENGTH_LONG).show()
                                }
                            }
                        }
                        "Rate us" -> {
                            /**
                             * TODO Google Play 内评（In-App Review）有严格限制，满足下面任意一条，就绝对不会显示：
                             ** 1.调试 / 测试包（debug 包、非 Google Play 安装的包、本地直接运行的 APK、模拟器）
                             ** 2.配额用光了（Google 限制：每个 app 每用户 最多弹 3 次 / 年，弹过一次后，冷却几小时 / 几天才会再弹）
                             ** 3.用户已经评价过（一旦你点过【提交】，永远不会再弹给你这个账号）
                             ** 4.用户已经评价过该 app 的其他版本（一旦你评价过某个版本，那么这个版本下的所有用户都不再会弹）
                             ** 5.设备没有安装最新版 Google Play 服务
                             ** 6.应用不是从 Google Play 安装的
                             */
                            context.findBaseActivityVBind()?.let { activity ->
                                val manager = ReviewManagerFactory.create(context)
                                val request = manager.requestReviewFlow()
                                request.addOnCompleteListener { task ->
                                    if (task.isSuccessful) {
                                        val reviewInfo = task.result
                                        // 必须用 Activity 才能弹！
                                        manager.launchReviewFlow(activity, reviewInfo).addOnCompleteListener { result ->
                                            // 用户已经看到评分弹窗，无论他们是否实际评分
                                            // 用户在这里记录日志或执行其他操作
                                            Log.d("ethan", "结果${result.result}")
                                        }
                                    } else {
                                        // 弹不出来，这里会打印原因
                                        val errorCode = (task.exception as? ReviewException)?.errorCode
                                        Log.e("ethan", "评分弹窗失败 errorCode: $errorCode")
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
