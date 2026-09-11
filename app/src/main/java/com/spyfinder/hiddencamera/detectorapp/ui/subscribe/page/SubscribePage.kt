package com.spyfinder.hiddencamera.detectorapp.ui.subscribe.page

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.spyfinder.hiddencamera.detectorapp.R
import com.spyfinder.hiddencamera.detectorapp.event.Event
import com.spyfinder.hiddencamera.detectorapp.model.SubModel
import com.spyfinder.hiddencamera.detectorapp.ui.components.*
import com.spyfinder.hiddencamera.detectorapp.ui.subscribe.viewmodel.SubscribeViewModel
import com.spyfinder.hiddencamera.detectorapp.utils.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout

@Composable
fun SubscribePage(onDismiss: (() -> Unit)? = null) {
    val context = LocalContext.current
    val vm: SubscribeViewModel = viewModel()
    val dialog = remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(true) }
    var products by remember { mutableStateOf<List<SubModel>>(emptyList()) }
    var selected by remember { mutableStateOf<SubModel?>(null) }
    var retry by remember { mutableIntStateOf(0) }
    var error by remember { mutableStateOf<String?>(null) }
    val subscribed by SubscribeHelper.isSubscribedFlow.collectAsState()
    val purchaseState = vm.isBuySuccess.value
    val close = { if (onDismiss != null) onDismiss() else context.findActivity()?.finish(); Unit }
    BackHandler(onBack = close)
    LaunchedEffect(subscribed, purchaseState) { if (subscribed || purchaseState == 1) close() }
    LaunchedEffect(retry) {
        loading = true; error = null; selected = null
        products = try { withTimeout(15_000) { vm.querySubProduct(context) } }
        catch (exception: TimeoutCancellationException) { emptyList() }
        catch (exception: CancellationException) { throw exception }
        catch (exception: Exception) { emptyList() }
        selected = products.firstOrNull { SubscribeHelper.getProductType(it.id) == "Weekly" } ?: products.firstOrNull()
        loading = false
        Event.event(context, Event.SUBSCRIBE_PRODUCT_QUERY, Event.PARAM_PRODUCT_COUNT to products.size)
    }
    LaunchedEffect(Unit) { Event.event(context, Event.PAGE_VIEW, Event.PARAM_PAGE to "subscribe") }
    QuietPage(navigationPadding = true) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            QuietBadge(context.getString(R.string.pro_badge))
            IconButton(onClick = close) { Icon(painterResource(R.drawable.svg_icon_close), context.getString(R.string.close_subscription), Modifier.size(24.dp)) }
        }
        QuietHeading(context.getString(R.string.closer_look), context.getString(R.string.more_tools), context.getString(R.string.unlock_toolkit))
        QuietPanel(tinted = true) {
            QuietIcon(R.drawable.svg_icon_scanner)
            Text(context.getString(R.string.one_plan), style = MaterialTheme.typography.titleMedium)
            QuietBody(context.getString(R.string.pro_features))
        }
        when {
            loading -> QuietPanel {
                Text(context.getString(R.string.loading_plans))
                LinearProgressIndicator(Modifier.fillMaxWidth())
                QuietBody(context.getString(R.string.connecting_play))
            }
            products.isEmpty() -> QuietPanel {
                Text(context.getString(R.string.plans_error))
                QuietBody(context.getString(R.string.check_connection))
                QuietButton(context.getString(R.string.retry)) { retry++ }
            }
            else -> {
                products.sortedBy { when (SubscribeHelper.getProductType(it.id)) { "Weekly" -> 0; "Monthly" -> 1; else -> 2 } }.forEach { model ->
                    val chosen = selected?.id == model.id
                    Surface(onClick = {
                        if (purchaseState != 5) {
                            selected = model
                            Event.event(context, Event.SUBSCRIBE_PRODUCT_SELECT, Event.PARAM_PLAN_ID to model.id, Event.PARAM_PRICE to model.price)
                        }
                    }, shape = RoundedCornerShape(18.dp), border = BorderStroke(if (chosen) 2.dp else 1.dp,
                        if (chosen) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant),
                        color = if (chosen) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface) {
                        Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Column(Modifier.weight(1f)) { Text(context.planLabel(model.id)); QuietBody(context.getString(R.string.auto_plan), true) }
                            Text("${model.currency.orEmpty()}${model.price.orEmpty()}", style = MaterialTheme.typography.titleMedium)
                        }
                    }
                }
            }
        }
        if (purchaseState in 2..4) QuietNote(when (purchaseState) {
            4 -> context.getString(R.string.purchase_cancelled_note)
            3 -> context.getString(R.string.store_disconnected_note)
            else -> context.getString(R.string.purchase_failed_note)
        })
        error?.let { QuietNote(it) }
        QuietButton(if (purchaseState == 5) context.getString(R.string.waiting_play) else selected?.let {
            context.getString(R.string.continue_price, "${it.currency.orEmpty()}${it.price.orEmpty()}", context.planLabel(it.id, period = true))
        } ?: context.getString(R.string.action_continue), enabled = !loading && selected != null && purchaseState != 5) {
            context.findBaseActivityVBind()?.let { activity ->
                error = null
                vm.isBuySuccess.value = 5
                Event.event(context, Event.SUBSCRIBE_CONTINUE_CLICK, Event.PARAM_PLAN_ID to selected?.id)
                vm.buySubscribe(selected, activity, dialog)
            }
        }
        QuietBody(context.getString(R.string.auto_renew_note), true)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            TextButton(onClick = { LaunchUtils.launchWeb(context, "https://sites.google.com/view/spycamerafinder-privacy-policy/home", context.getString(R.string.privacy_policy)) }) { Text(context.getString(R.string.privacy)) }
            TextButton(onClick = { LaunchUtils.launchWeb(context, "https://sites.google.com/view/spycamerafinder-terms-of-use/home", context.getString(R.string.terms_of_use)) }) { Text(context.getString(R.string.terms)) }
        }
        RestorePurchases()
    }
}
