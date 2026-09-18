package com.spyfinder.hiddencamera.detectorapp.ui.setting.page
import com.spyfinder.hiddencamera.detectorapp.theme.AppSpacing
import com.spyfinder.hiddencamera.detectorapp.theme.AppColors

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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.spyfinder.hiddencamera.detectorapp.BuildConfig
import com.spyfinder.hiddencamera.detectorapp.R
import com.spyfinder.hiddencamera.detectorapp.theme.AppColors.background
import com.spyfinder.hiddencamera.detectorapp.theme.AppColors.textPrimary
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
    val isChinese = LocalConfiguration.current.locales[0].language == "zh"
    val isSubscribed = SubscribeHelper.isSubscribedFlow.collectAsState().value
    var showAbout by remember { mutableStateOf(false) }
    val settingItemList = listOf(
        Pair(R.drawable.svg_icon_share_app, R.string.share_app),
        Pair(R.drawable.svg_icon_privacy_policy, R.string.privacy_policy),
        Pair(R.drawable.svg_icon_restore, R.string.restore),
        Pair(R.drawable.svg_icon_rate_us, R.string.rate_us),
        Pair(R.drawable.svg_icon_tips, R.string.about),
    )

    Column(modifier = Modifier.fillMaxSize().background(color = AppColors.background).statusBarsPadding()) {
        Box(modifier = Modifier.fillMaxWidth().height(AppSpacing.topBar).padding(start = AppSpacing.section, end = AppSpacing.screen)) {
            Image(
                painter = painterResource(R.drawable.svg_icon_back),
                contentDescription = context.getString(R.string.a11y_back),
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .clickable{ context.findBaseActivityVBind()?.finish() }
            )
            Text(context.getString(R.string.title_setting), color = AppColors.textPrimary, fontSize = 18.sp, fontWeight = FontWeight.W500, modifier = Modifier.align(Alignment.Center))
        }

        if (!isSubscribed) {
            Spacer(modifier = Modifier.height(28.dp))
            // 未订阅时展示订阅卡片，订阅后自动隐藏。
            Image(
                painter = painterResource(R.mipmap.img_subscribe_card),
                contentScale = ContentScale.Fit,
                contentDescription = null,
                modifier = Modifier
                    .padding(horizontal = AppSpacing.screen)
                    .fillMaxWidth()
                    .then(if (isChinese) Modifier.clip(RoundedCornerShape(percent = 30)) else Modifier)
                    .clickable{ SubscribeActivity.launch(context) }
            )
        }
        Spacer(modifier = Modifier.height(19.dp))
        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = AppSpacing.screen),
            verticalArrangement = Arrangement.spacedBy(AppSpacing.section)
        ) {
            items(settingItemList.size) { index ->
                SettingItemView(settingItemList[index].let {
                    it.first to if (it.second == R.string.about)
                        context.getString(R.string.about_version, BuildConfig.VERSION_NAME, BuildConfig.VERSION_CODE)
                    else context.getString(it.second)
                }) {
                    when(settingItemList[index].second) {
                        R.string.share_app -> {
                            ShareUtils.shareTextWithHighlightedLinks(context, context.getString(R.string.share_body), "https://play.google.com/store/apps/details?id=" + context.packageName)
                        }
                        R.string.privacy_policy -> {
                            LaunchUtils.launchWeb(context, "https://sites.google.com/view/spycamerafinder-privacy-policy/home", context.getString(R.string.app_name))
                        }
                        R.string.restore -> {
                            scope.launch(Dispatchers.Default) {
                                SubscribeHelper.refreshSubscribeStateSuspend(force = true)
                                withContext(Dispatchers.Main) {
                                    Toast.makeText(context, if (SubscribeHelper.lastQueryFailed) context.getString(R.string.restore_query_error) else if (SubscribeHelper.isSubscribed) context.getString(R.string.purchases_restored) else context.getString(R.string.no_active_purchase), Toast.LENGTH_LONG).show()
                                }
                            }
                        }
                        R.string.rate_us -> LaunchUtils.launchPlayStore(context)
                        R.string.about -> showAbout = true
                    }
                }
            }
        }
        if (showAbout) {
            AlertDialog(
                onDismissRequest = { showAbout = false },
                title = { Text(context.getString(R.string.about)) },
                text = { Text(context.getString(R.string.about_version, BuildConfig.VERSION_NAME, BuildConfig.VERSION_CODE)) },
                confirmButton = {
                    TextButton(onClick = { showAbout = false }) {
                        Text(context.getString(R.string.a11y_close))
                    }
                }
            )
        }
    }
}