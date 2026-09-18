package com.spyfinder.hiddencamera.detectorapp.ui.main.page
import com.spyfinder.hiddencamera.detectorapp.theme.AppColors
import com.spyfinder.hiddencamera.detectorapp.theme.AppSpacing

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.pager.PagerState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.spyfinder.hiddencamera.detectorapp.R
import com.spyfinder.hiddencamera.detectorapp.event.Event
import com.spyfinder.hiddencamera.detectorapp.ui.main.context.LocalMainContextEntity
import com.spyfinder.hiddencamera.detectorapp.ui.main.view.FeatureItemView
import com.spyfinder.hiddencamera.detectorapp.ui.setting.SettingActivity
import com.spyfinder.hiddencamera.detectorapp.ui.tips.TipsActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * 功能页
 */
@Composable
fun FeaturePage() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val localMain = LocalMainContextEntity.current
    val featureItemList = listOf(
        Triple(context.getString(R.string.feature_wifi), R.drawable.svg_icon_wifi, context.getString(R.string.feature_wifi_description)),
        Triple(context.getString(R.string.feature_magnetic), R.drawable.svg_icon_magnetic, context.getString(R.string.feature_magnetic_description)),
        Triple(context.getString(R.string.tab_scanner), R.drawable.svg_icon_scanner_big, context.getString(R.string.feature_camera_description)),
        Triple(context.getString(R.string.title_tips), R.drawable.svg_icon_tips, context.getString(R.string.feature_tips_description)),
    )

    Column(modifier = Modifier.fillMaxSize().statusBarsPadding().padding(top = AppSpacing.pageTop)) {
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = AppSpacing.screen), verticalAlignment = Alignment.CenterVertically) {
            Column {
                Text(context.getString(R.string.title_features), color = AppColors.textPrimary, fontSize = 28.sp, fontWeight = FontWeight.W700)
                Spacer(modifier = Modifier.height(AppSpacing.compact))
                Text(context.getString(R.string.detection_method), color = AppColors.textPrimary.copy(0.6f), fontSize = 14.sp, fontWeight = FontWeight.W400)
            }
            Spacer(modifier = Modifier.weight(1f))
            Image(painter = painterResource(R.drawable.svg_icon_settings), contentDescription = null, modifier = Modifier.clickable{
                // 设置入口点击埋点，方便观察功能页的工具入口使用情况。
                Event.event(context, Event.FEATURE_CLICK, Event.PARAM_FEATURE to "settings")
                SettingActivity.launch(context)
            })
        }
        Spacer(modifier = Modifier.height(AppSpacing.pageTop))
        LazyColumn(
            contentPadding = PaddingValues(horizontal = AppSpacing.screen),
            verticalArrangement = Arrangement.spacedBy(AppSpacing.section),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(featureItemList.size) { index ->
                FeatureItemView(featureItemList[index]) {
                    val featureName = when (index) {
                        0 -> "wifi"
                        1 -> "magnetic"
                        2 -> "scanner"
                        else -> "tips"
                    }
                    // 功能卡片点击埋点，用于分析用户偏好的检测方式。
                    Event.event(context, Event.FEATURE_CLICK, Event.PARAM_FEATURE to featureName)
                    if (index != 3) {
                        if (index == 0) localMain.openWifiFeature()
                        else {
                            localMain.pendingWifiAutoScan.value = false
                            localMain.selectTabIndex.intValue = index
                        }
                    } else {
                        TipsActivity.launch(context)
                    }
                }
            }
        }
    }
}