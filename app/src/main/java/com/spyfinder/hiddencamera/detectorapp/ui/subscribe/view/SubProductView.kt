package com.spyfinder.hiddencamera.detectorapp.ui.subscribe.view
import com.spyfinder.hiddencamera.detectorapp.theme.AppSpacing
import com.spyfinder.hiddencamera.detectorapp.theme.AppColors

import androidx.compose.ui.platform.LocalContext

import com.spyfinder.hiddencamera.detectorapp.R

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ethan.pay.utils.SubHelper
import com.spyfinder.hiddencamera.detectorapp.model.SubModel
import com.spyfinder.hiddencamera.detectorapp.theme.Transparent
import com.spyfinder.hiddencamera.detectorapp.theme.AppColors.textPrimary
import com.spyfinder.hiddencamera.detectorapp.theme.AppColors.outline
import com.spyfinder.hiddencamera.detectorapp.utils.SubscribeHelper

@Composable
fun SubProductView(modifier: Modifier = Modifier, isSelected: Boolean, model: SubModel, onClick: () -> Unit) {
    val context = LocalContext.current

    Column(modifier = modifier.fillMaxWidth()) {
        if (isSelected && model.id == SubHelper.getWeekPlanId()) {
            Box(modifier = Modifier
                .fillMaxWidth()
                .height(28.dp)
                .background(brush = Brush.horizontalGradient(colorStops = arrayOf(0f to AppColors.primaryDark, 1f to AppColors.primarySoft)), shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
            ) {
                Text(context.getString(R.string.best_choice), color = AppColors.textOnBright, fontSize = 12.sp, fontWeight = FontWeight.W600, modifier = Modifier.align(Alignment.Center))
            }
        }
        Box(modifier = Modifier
            .fillMaxWidth()
            .height(120.dp)
            .background(
                brush = Brush.verticalGradient(colorStops = arrayOf(0f to AppColors.surface, 1f to AppColors.surfaceElevated)),
                shape = if (isSelected) {
                    if (model.id == SubHelper.getWeekPlanId()) {
                        RoundedCornerShape(bottomStart = AppSpacing.large, bottomEnd = AppSpacing.large)
                    } else {
                        RoundedCornerShape(AppSpacing.large)
                    }
                } else RoundedCornerShape(AppSpacing.large)
            )
            .then(
                if (isSelected) {
                    if (model.id == SubHelper.getWeekPlanId()) {
                        Modifier.border(width = 2.dp, shape = RoundedCornerShape(bottomStart = AppSpacing.large, bottomEnd = AppSpacing.large), brush = Brush.horizontalGradient(colorStops = arrayOf(0f to AppColors.primaryDark, 1f to AppColors.primarySoft)))
                    } else {
                        Modifier.border(width = 2.dp, shape = RoundedCornerShape(AppSpacing.large), brush = Brush.horizontalGradient(colorStops = arrayOf(0f to AppColors.primaryDark, 1f to AppColors.primarySoft)))
                    }
                } else {
                    Modifier.border(width = 1.dp, shape = RoundedCornerShape(AppSpacing.large), brush = Brush.verticalGradient(colorStops = arrayOf(0f to AppColors.outline, 0.5f to Transparent, 1f to AppColors.outline)))
                }
            )
            .clickable{ onClick.invoke() }
        ) {
            Column(modifier = Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                // todo 乱写的，得沟通清楚
                Text(context.getString(R.string.subscription), color = AppColors.textMuted, fontSize = 12.sp, fontWeight = FontWeight.W400)
                Spacer(modifier = Modifier.height(AppSpacing.section))
                Text(model.formattedPrice, color = AppColors.textPrimary, fontSize = 20.sp, fontWeight = FontWeight.W700)
                Spacer(modifier = Modifier.height(AppSpacing.section))
                Text(SubscribeHelper.getProductType(context, model.id), modifier = Modifier.align(Alignment.CenterHorizontally), color = AppColors.textMuted, fontSize = 12.sp, fontWeight = FontWeight.W400)
            }
        }
    }
}