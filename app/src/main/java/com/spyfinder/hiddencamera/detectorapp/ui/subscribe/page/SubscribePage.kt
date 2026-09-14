package com.spyfinder.hiddencamera.detectorapp.ui.subscribe.page

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.spyfinder.hiddencamera.detectorapp.R
import com.spyfinder.hiddencamera.detectorapp.event.Event
import com.spyfinder.hiddencamera.detectorapp.theme.Black
import com.spyfinder.hiddencamera.detectorapp.theme.Transparent
import com.spyfinder.hiddencamera.detectorapp.theme.White
import com.spyfinder.hiddencamera.detectorapp.theme.White10
import com.spyfinder.hiddencamera.detectorapp.ui.subscribe.view.SubProductView
import com.spyfinder.hiddencamera.detectorapp.ui.subscribe.viewmodel.SubscribeViewModel
import com.spyfinder.hiddencamera.detectorapp.utils.LaunchUtils
import com.spyfinder.hiddencamera.detectorapp.utils.findBaseActivityVBind

import android.app.Activity
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.runtime.DisposableEffect
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.spyfinder.hiddencamera.detectorapp.ui.subscribe.viewmodel.PurchaseUiState


@Composable
fun SubscribePage(onDismiss: (() -> Unit)? = null) {
    val context = LocalContext.current
    val activity = context.findBaseActivityVBind() ?: return
    val vm: SubscribeViewModel = viewModel(activity)
    val owner = LocalLifecycleOwner.current
    LaunchedEffect(Unit) { vm.load(context); vm.refreshOnResume() }
    DisposableEffect(owner, vm) {
        val observer = LifecycleEventObserver { _, event -> if (event == Lifecycle.Event.ON_RESUME) vm.refreshOnResume() }
        owner.lifecycle.addObserver(observer)
        onDispose { owner.lifecycle.removeObserver(observer) }
    }
    LaunchedEffect(vm.purchaseState) {
        if (vm.purchaseState == PurchaseUiState.SUCCESS) {
            activity.setResult(Activity.RESULT_OK)
            if (onDismiss != null) onDismiss() else activity.finish()
        }
    }
    val canBuy = vm.selected != null && !vm.loading && vm.purchaseState !in listOf(PurchaseUiState.LAUNCHING, PurchaseUiState.PENDING, PurchaseUiState.SUCCESS)
    val fontScale = LocalDensity.current.fontScale.coerceAtLeast(1f)
    BoxWithConstraints(Modifier.fillMaxSize().background(Black).navigationBarsPadding()) {
        val pageHeight = maxOf(maxHeight, 800.dp * fontScale)
        Box(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            Box(Modifier.fillMaxWidth().height(pageHeight)) {
                Box(modifier = Modifier.fillMaxWidth()) {
                    Image(painter = painterResource(R.mipmap.img_subscribe_demo_1), modifier = Modifier.fillMaxWidth(), contentScale = ContentScale.FillWidth, contentDescription = null)
                    Box(modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .height(200.dp)
                        .background(brush = Brush.verticalGradient(colorStops = arrayOf(0f to Transparent, 1f to Black)))
                    )

                    Box(modifier = Modifier.align(Alignment.BottomCenter).offset(x = (-20).dp, y = (-78).dp)) {
                        Image(painter = painterResource(R.mipmap.img_light_cone_big), contentDescription = null)
                        Image(painter = painterResource(R.drawable.svg_icon_red_dot), contentDescription = null, modifier = Modifier.align(Alignment.TopEnd).offset(x = 4.dp, y = (-2).dp))
                        Image(painter = painterResource(R.mipmap.img_sub_camera), contentDescription = null, modifier = Modifier.align(Alignment.BottomStart).offset(x = (-20).dp))
                    }

                    Box(modifier = Modifier.align(Alignment.BottomEnd).offset(x = (-30).dp, y = (-100).dp)) {
                        Image(painter = painterResource(R.mipmap.img_light_cone_small), contentDescription = null)
                        Image(painter = painterResource(R.drawable.svg_icon_red_dot), contentDescription = null, modifier = Modifier.align(Alignment.BottomStart).offset(x = (-2).dp, y = 4.dp))
                        Image(painter = painterResource(R.mipmap.img_sub_notebook), contentDescription = null, modifier = Modifier.align(Alignment.TopEnd).offset(y = (-20).dp))
                    }
                    // 弃用，可能后边会开启
        //            Image(
        //                painter = painterResource(R.mipmap.img_position),
        //                modifier = Modifier
        //                    .align(Alignment.BottomStart)
        //                    .padding(start = 74.dp, bottom = 44.dp)
        //                    .size(32.dp),
        //                contentDescription = null
        //            )
        //            Image(
        //                painter = painterResource(R.mipmap.img_position),
        //                modifier = Modifier
        //                    .align(Alignment.BottomStart)
        //                    .padding(start = 23.dp, bottom = 199.dp)
        //                    .size(32.dp),
        //                contentDescription = null
        //            )
        //            Image(
        //                painter = painterResource(R.mipmap.img_position),
        //                modifier = Modifier
        //                    .align(Alignment.BottomEnd)
        //                    .padding(end = 41.dp, bottom = 144.dp)
        //                    .size(32.dp),
        //                contentDescription = null
        //            )
                }

                Image(
                    painter = painterResource(R.drawable.svg_icon_close_30),
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .statusBarsPadding()
                        .padding(top = 15.dp, end = 16.dp)
                        .clickable{
                            Event.event(context, Event.FEATURE_CLICK, Event.PARAM_FEATURE to "subscribe_close")
                            if (onDismiss != null) {
                                onDismiss.invoke()
                            } else {
                                context.findBaseActivityVBind()?.finish()
                            }
                        },
                    contentDescription = null
                )

                Column(modifier = Modifier.fillMaxWidth().padding(top = 110.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Advanced",
                        style = TextStyle(
                            brush = Brush.horizontalGradient(colorStops = arrayOf(0f to Color(0xFF01C587), 1f to Color(0xFFBCF085))),
                            fontWeight = FontWeight.W700,
                            fontSize = 32.sp
                        ),
                        softWrap = false,
                        maxLines = 1
                    )
                    Text(
                        text = "Hidden Camera Finder",
                        style = TextStyle(
                            brush = Brush.horizontalGradient(colorStops = arrayOf(0f to Color(0xFF01C587), 1f to Color(0xFFBCF085))),
                            fontWeight = FontWeight.W700,
                            fontSize = 32.sp
                        ),
                        softWrap = false,
                        maxLines = 1
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Column {
                        Row(modifier = Modifier) {
                            Image(painter = painterResource(R.drawable.svg_icon_correct), modifier = Modifier.padding(end = 4.dp), contentDescription = null)
                            Text("Unlock All Pro Features", color = White, fontSize = 16.sp, fontWeight = FontWeight.W500)
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(modifier = Modifier) {
                            Image(painter = painterResource(R.drawable.svg_icon_correct), modifier = Modifier.padding(end = 4.dp), contentDescription = null)
                            Text("View Devices‘ Information", color = White, fontSize = 16.sp, fontWeight = FontWeight.W500)
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(modifier = Modifier) {
                            Image(painter = painterResource(R.drawable.svg_icon_correct), modifier = Modifier.padding(end = 4.dp), contentDescription = null)
                            Text("No Ads Experience", color = White, fontSize = 16.sp, fontWeight = FontWeight.W500)
                        }
                    }
                }

                Column(
                    modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(bottom = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(modifier = Modifier.fillMaxWidth().height(148.dp * fontScale)) {
                        if (vm.loading) {
                            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center).size(36.dp), color = White, trackColor = White10, strokeCap = StrokeCap.Round)
                        } else if (vm.products.isEmpty()) {
                            EmptyView { vm.load(context, true) }
                        } else {
                            Row(Modifier.fillMaxSize().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Bottom) {
                                vm.products.forEach { model ->
                                    SubProductView(Modifier.weight(1f), vm.selected?.id == model.id, model) { vm.select(model) }
                                }
                            }
                        }
                    }
                    if (vm.message.isNotBlank()) {
                        Spacer(Modifier.height(8.dp))
                        Text(vm.message, modifier = Modifier.padding(horizontal = 24.dp), color = Color(0xFF96939E), fontSize = 12.sp, textAlign = TextAlign.Center)
                    }
                    Spacer(modifier = Modifier.height(18.dp))
                    Text(vm.selected?.let { "${it.formattedPrice} / ${periodLabel(it.billingPeriod)}. Auto-renews. Cancel anytime." } ?: "Auto-Renewable. Cancel anytime.", modifier = Modifier.padding(horizontal = 24.dp), textAlign = TextAlign.Center, color = Color(0xFF96939E), fontSize = 14.sp, fontWeight = FontWeight.W400)
                    Spacer(modifier = Modifier.height(20.dp))
                    Box(modifier = Modifier
                        .padding(horizontal = 24.dp)
                        .fillMaxWidth()
                        .height(56.dp)
                        .background(color = Color(0xFF00C46F), shape = RoundedCornerShape(999.dp))
                        .border(width = 1.dp, shape = RoundedCornerShape(999.dp), brush = Brush.verticalGradient(colorStops = arrayOf(0f to White10, 0.5f to Transparent, 1f to White10)))
                        .alpha(if (canBuy) 1f else 0.5f)
                        .clickable(enabled = canBuy) { vm.buy(activity) }
                    ) {
                        Text(when (vm.purchaseState) { PurchaseUiState.LAUNCHING -> "Opening Google Play…"; PurchaseUiState.PENDING -> "Payment pending"; else -> "Continue" }, color = White, fontSize = 16.sp, fontWeight = FontWeight.W500, modifier = Modifier.align(Alignment.Center))
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Restore purchases", color = Color(0xFF00C46F), fontSize = 12.sp, modifier = Modifier.clickable(enabled = vm.purchaseState != PurchaseUiState.LAUNCHING) { vm.restore(context) }.padding(4.dp))
                    Spacer(modifier = Modifier.height(8.dp))
                    Row {
                        Text("Privacy Policy", color = Color(0xFF00C46F), fontSize = 12.sp, fontWeight = FontWeight.W400, modifier = Modifier.clickable {
                            LaunchUtils.launchWeb(context, "https://sites.google.com/view/spycamerafinder-privacy-policy/home", "Privacy Policy")
                        })
                        Text(" and ", color = White, fontSize = 12.sp, fontWeight = FontWeight.W400)
                        Text("Terms of Use", color = Color(0xFF00C46F), fontSize = 12.sp, fontWeight = FontWeight.W400, modifier = Modifier.clickable {
                            LaunchUtils.launchWeb(context, "https://sites.google.com/view/spycamerafinder-terms-of-use/home", "Terms of Use")
                        })
                    }
                }
            }
        }
    }
}

@Composable
fun EmptyView(modifier: Modifier = Modifier, onRetryClick: () -> Unit) {
    Column(modifier = modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Text("No product found", color = Color.Red, fontSize = 16.sp, fontWeight = FontWeight.W400)
        Box(modifier = Modifier
            .padding(top = 20.dp)
            .border(width = 1.dp, color = Color.White, shape = RoundedCornerShape(44.dp))
            .padding(vertical = 10.dp, horizontal = 30.dp)
            .clickable { onRetryClick.invoke() }) {
            Text("Retry", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.W400)
        }
    }
}
fun periodLabel(period: String) = when (period) { "P1W" -> "week"; "P1M" -> "month"; "P1Y" -> "year"; else -> period }
