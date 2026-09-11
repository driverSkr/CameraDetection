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
            QuietBadge("SPYFINDER PRO")
            IconButton(onClick = close) { Icon(painterResource(R.drawable.svg_icon_close), "Close subscription", Modifier.size(24.dp)) }
        }
        QuietHeading("A closer look", "More tools.\nClearer details.", "Unlock the full inspection toolkit.")
        QuietPanel(tinted = true) {
            QuietIcon(R.drawable.svg_icon_scanner)
            Text("One plan. All three checks.", style = MaterialTheme.typography.titleMedium)
            QuietBody("Device details · Magnetic sensor\nCamera filters · No ads")
        }
        when {
            loading -> QuietPanel {
                Text("Loading plans…")
                LinearProgressIndicator(Modifier.fillMaxWidth())
                QuietBody("Connecting to Google Play.")
            }
            products.isEmpty() -> QuietPanel {
                Text("Plans couldn’t be loaded")
                QuietBody("Check your connection and try again.")
                QuietButton("Retry") { retry++ }
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
                            Column(Modifier.weight(1f)) { Text(SubscribeHelper.getProductType(model.id)); QuietBody("Auto-renewing plan", true) }
                            Text("${model.currency.orEmpty()}${model.price.orEmpty()}", style = MaterialTheme.typography.titleMedium)
                        }
                    }
                }
            }
        }
        if (purchaseState in 2..4) QuietNote(when (purchaseState) {
            4 -> "Purchase canceled. You haven’t activated a new plan."
            3 -> "The store disconnected. Check your connection and try again."
            else -> "Purchase was not completed. Please try again."
        })
        error?.let { QuietNote(it) }
        QuietButton(if (purchaseState == 5) "Waiting for Google Play…" else selected?.let {
            "Continue · ${it.currency.orEmpty()}${it.price.orEmpty()} / ${when (SubscribeHelper.getProductType(it.id)) { "Weekly" -> "week"; "Monthly" -> "month"; else -> "year" }}"
        } ?: "Continue", enabled = !loading && selected != null && purchaseState != 5) {
            context.findBaseActivityVBind()?.let { activity ->
                error = null
                vm.isBuySuccess.value = 5
                Event.event(context, Event.SUBSCRIBE_CONTINUE_CLICK, Event.PARAM_PLAN_ID to selected?.id)
                vm.buySubscribe(selected, activity, dialog)
            }
        }
        QuietBody("Auto-renews until canceled in Google Play. Your selected plan’s price is shown above.", true)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            TextButton(onClick = { LaunchUtils.launchWeb(context, "https://sites.google.com/view/spycamerafinder-privacy-policy/home", "Privacy policy") }) { Text("Privacy") }
            TextButton(onClick = { LaunchUtils.launchWeb(context, "https://sites.google.com/view/spycamerafinder-terms-of-use/home", "Terms of use") }) { Text("Terms") }
        }
        RestorePurchases()
    }
}
