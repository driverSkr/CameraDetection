package com.spyfinder.hiddencamera.detectorapp.ui.components

import androidx.compose.material3.*
import androidx.compose.runtime.*
import com.spyfinder.hiddencamera.detectorapp.R
import com.spyfinder.hiddencamera.detectorapp.utils.SubscribeHelper
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout

@Composable
fun RestorePurchases() {
    val scope = rememberCoroutineScope()
    var restoring by remember { mutableStateOf(false) }
    var result by remember { mutableStateOf<String?>(null) }
    QuietRow(R.drawable.svg_icon_restore, if (restoring) "Restoring purchases…" else "Restore purchases", "Check your Google Play account.") {
        if (!restoring) scope.launch {
            restoring = true
            result = try {
                val active = withTimeout(15_000) { SubscribeHelper.queryPurchase().isNotEmpty() }
                SubscribeHelper.updateSubscribeState(active)
                if (active) "Restore complete. Pro access is active." else "No active purchase was found for this account."
            } catch (exception: kotlinx.coroutines.TimeoutCancellationException) {
                "The store took too long to respond. Please try again."
            } catch (exception: CancellationException) { throw exception }
            catch (exception: Exception) { "We couldn’t restore purchases. Check your connection and try again." }
            finally { restoring = false }
        }
    }
    if (restoring) LinearProgressIndicator()
    result?.let { text ->
        AlertDialog(onDismissRequest = { result = null }, title = { Text("Restore purchases") }, text = { Text(text) },
            confirmButton = { TextButton(onClick = { result = null }) { Text("Done") } })
    }
}
