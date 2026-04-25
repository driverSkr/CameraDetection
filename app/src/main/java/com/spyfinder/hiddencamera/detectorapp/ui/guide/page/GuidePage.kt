package com.spyfinder.hiddencamera.detectorapp.ui.guide.page

import android.util.Log
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
import androidx.compose.runtime.LaunchedEffect
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
import com.google.android.play.core.review.ReviewException
import com.google.android.play.core.review.ReviewManagerFactory
import com.spyfinder.hiddencamera.detectorapp.R
import com.spyfinder.hiddencamera.detectorapp.theme.Black
import com.spyfinder.hiddencamera.detectorapp.ui.guide.view.GuideBannerView
import com.spyfinder.hiddencamera.detectorapp.utils.findBaseActivityVBind
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Composable
@Preview
fun GuidePage(onComplete: (() -> Unit)? = null) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val bannerInfo = listOf(
        Triple(R.mipmap.img_guide_1, "Scan for Hidden Cameras", "Find hidden cameras on your Wi-Fi network to protect your privacy."),
        Triple(R.mipmap.img_guide_2, "Infrared Red Dot Detection", "Identify suspicious cameras using infrared scanning, even in the dark."),
        Triple(R.mipmap.img_guide_3, "Identify Suspicious Devices", "Stay protected from hidden cameras and devices where you are")
    )
    val pagerState = rememberPagerState { bannerInfo.size }

    LaunchedEffect(pagerState.currentPage) {
        /**
         * TODO Google Play 内评（In-App Review）有严格限制，满足下面任意一条，就绝对不会显示：
         ** 1.调试 / 测试包（debug 包、非 Google Play 安装的包、本地直接运行的 APK、模拟器）
         ** 2.配额用光了（Google 限制：每个 app 每用户 最多弹 3 次 / 年，弹过一次后，冷却几小时 / 几天才会再弹）
         ** 3.用户已经评价过（一旦你点过【提交】，永远不会再弹给你这个账号）
         ** 4.用户已经评价过该 app 的其他版本（一旦你评价过某个版本，那么这个版本下的所有用户都不再会弹）
         ** 5.设备没有安装最新版 Google Play 服务
         ** 6.应用不是从 Google Play 安装的
         */
        if (pagerState.currentPage == pagerState.pageCount - 1) {
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
                        // Activity 场景直接回调跳转主页
                        onComplete?.invoke()
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
