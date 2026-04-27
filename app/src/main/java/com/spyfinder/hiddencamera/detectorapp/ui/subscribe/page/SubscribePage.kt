package com.spyfinder.hiddencamera.detectorapp.ui.subscribe.page

import android.app.AlertDialog
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.widget.Toast
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.viewmodel.compose.viewModel
import com.spyfinder.hiddencamera.detectorapp.R
import com.spyfinder.hiddencamera.detectorapp.dialog.rememberLoadingDialog
import com.spyfinder.hiddencamera.detectorapp.event.Event
import com.spyfinder.hiddencamera.detectorapp.model.SubModel
import com.spyfinder.hiddencamera.detectorapp.theme.Black
import com.spyfinder.hiddencamera.detectorapp.theme.Transparent
import com.spyfinder.hiddencamera.detectorapp.theme.White
import com.spyfinder.hiddencamera.detectorapp.theme.White10
import com.spyfinder.hiddencamera.detectorapp.ui.subscribe.view.SubProductView
import com.spyfinder.hiddencamera.detectorapp.ui.subscribe.viewmodel.SubscribeViewModel
import com.spyfinder.hiddencamera.detectorapp.utils.findActivity
import com.spyfinder.hiddencamera.detectorapp.utils.findBaseActivityVBind
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Composable
fun SubscribePage(onDismiss: (() -> Unit)? = null) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val dialog = rememberLoadingDialog()
    var isLoading by remember { mutableStateOf(true) }
    var subModelList by remember { mutableStateOf<MutableList<SubModel>?>(null) }
    var selectedSubProduct by remember { mutableStateOf<SubModel?>(null) }
    val subscribeViewModel = context.findBaseActivityVBind()?.let { viewModel<SubscribeViewModel>(it) }

    /**
     * 查询订阅商品
     */
    LaunchedEffect(Unit) {
        // 订阅页曝光埋点，记录所有付费页展示。
        Event.event(context, Event.PAGE_VIEW, Event.PARAM_PAGE to "subscribe")
        isLoading = true
        val queryResult = subscribeViewModel?.querySubProduct(context)
        if (queryResult != null) {
            subModelList = queryResult
            selectedSubProduct = queryResult.getOrNull(1)
            Event.event(context, Event.SUBSCRIBE_PRODUCT_QUERY, Event.PARAM_PRODUCT_COUNT to queryResult.size)
        }
        isLoading = false
    }

    // 监听 订阅状态
    LaunchedEffect(subscribeViewModel?.isBuySuccess?.value) {
        if (subscribeViewModel?.isBuySuccess?.value == 1) {
            if (onDismiss != null) {
                onDismiss.invoke()
            } else {
                context.findBaseActivityVBind()?.finish()
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(color = Black).navigationBarsPadding()) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Image(painter = painterResource(R.mipmap.img_subscribe_demo_1), modifier = Modifier.fillMaxWidth(), contentDescription = null)
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
            Box(modifier = Modifier.fillMaxWidth().height(148.dp)) {
                if (isLoading) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        CircularProgressIndicator(
                            modifier = Modifier.align(Alignment.Center).size(36.dp),
                            color = Color.White,
                            trackColor = White10,
                            strokeCap = StrokeCap.Round
                        )
                    }
                } else {
                    subModelList?.let { list ->
                        Row(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Bottom) {
                            list.forEach { model ->
                                SubProductView(modifier = Modifier.weight(1f),  selectedSubProduct?.id == model.id, model) {
                                    selectedSubProduct = model
                                    // 订阅商品选择埋点，记录用户偏好的付费周期。
                                    Event.event(
                                        context,
                                        Event.SUBSCRIBE_PRODUCT_SELECT,
                                        Event.PARAM_PLAN_ID to model.id,
                                        Event.PARAM_GOODS_ID to model.goods,
                                        Event.PARAM_SKU to model.sku,
                                        Event.PARAM_PRICE to model.price,
                                        Event.PARAM_CURRENCY to model.currency
                                    )
                                }
                            }
                        }
                    } ?: EmptyView {
                        Event.event(context, Event.SUBSCRIBE_PRODUCT_QUERY, Event.PARAM_REASON to "retry")
                        scope.launch(Dispatchers.Default) {
                            isLoading = true
                            val queryResult = subscribeViewModel?.querySubProduct(context)
                            if (queryResult != null) {
                                subModelList = queryResult
                                selectedSubProduct = queryResult[0]
                                Event.event(context, Event.SUBSCRIBE_PRODUCT_QUERY, Event.PARAM_PRODUCT_COUNT to queryResult.size)
                            }
                            isLoading = false
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(18.dp))
            Text("Auto-Renewable. Cancel anytime.", color = Color(0xFF96939E), fontSize = 14.sp, fontWeight = FontWeight.W400)
            Spacer(modifier = Modifier.height(20.dp))
            Box(modifier = Modifier
                .padding(horizontal = 24.dp)
                .fillMaxWidth()
                .height(56.dp)
                .background(color = Color(0xFF00C46F), shape = RoundedCornerShape(999.dp))
                .border(width = 1.dp, shape = RoundedCornerShape(999.dp), brush = Brush.verticalGradient(colorStops = arrayOf(0f to White10, 0.5f to Transparent, 1f to White10)))
                .clickable {
                    Event.event(
                        context,
                        Event.SUBSCRIBE_CONTINUE_CLICK,
                        Event.PARAM_PLAN_ID to selectedSubProduct?.id,
                        Event.PARAM_PRICE to selectedSubProduct?.price,
                        Event.PARAM_CURRENCY to selectedSubProduct?.currency
                    )
                    if (!context.isPurchaseAvailable()) {
                        Event.event(context, Event.PURCHASE_FAILED, Event.PARAM_REASON to "network_unavailable")
                        showPurchaseUnavailableDialog(context)
                        return@clickable
                    }

                    val activity = context.findActivity() as? FragmentActivity
                    if (selectedSubProduct != null && activity != null) {
                        dialog.value = true
                        subscribeViewModel?.buySubscribe(selectedSubProduct, activity, dialog)
                    } else {
                        dialog.value = false
                        Event.event(context, Event.PURCHASE_FAILED, Event.PARAM_REASON to "no_product")
                        Toast.makeText(context, "no product", Toast.LENGTH_SHORT).show()
                    }
                }
            ) {
                Text("Continue", color = White, fontSize = 16.sp, fontWeight = FontWeight.W500, modifier = Modifier.align(Alignment.Center))
            }
            Spacer(modifier = Modifier.height(16.dp))
            Row {
                Text("Terms of Use", color = Color(0xFF00C46F), fontSize = 12.sp, fontWeight = FontWeight.W400)
                Text(" and ", color = White, fontSize = 12.sp, fontWeight = FontWeight.W400)
                Text("Terms of Use", color = Color(0xFF00C46F), fontSize = 12.sp, fontWeight = FontWeight.W400)
            }
        }
    }
}

private fun Context.isPurchaseAvailable(): Boolean {
    val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return false
    val network = connectivityManager.activeNetwork ?: return false
    val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
    return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
        capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
}

private fun showPurchaseUnavailableDialog(context: Context) {
    val activity = context.findActivity() ?: return
    AlertDialog.Builder(activity)
        .setTitle("Unable to Purchase")
        .setMessage("You're offline right now, so purchases are unavailable. Please check your internet connection and try again.")
        .setPositiveButton(android.R.string.ok, null)
        .show()
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
