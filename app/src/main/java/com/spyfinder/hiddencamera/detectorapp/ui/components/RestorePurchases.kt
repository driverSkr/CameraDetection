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
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()
    var restoring by remember { mutableStateOf(false) }
    var result by remember { mutableStateOf<String?>(null) }
    QuietRow(R.drawable.svg_icon_restore, if (restoring) context.getString(R.string.restoring) else context.getString(R.string.restore), context.getString(R.string.check_play_account)) {
        if (!restoring) scope.launch {
            restoring = true
            result = try {
                val active = withTimeout(15_000) { SubscribeHelper.queryPurchase().isNotEmpty() }
                SubscribeHelper.updateSubscribeState(active)
                if (active) context.getString(R.string.restore_success) else context.getString(R.string.restore_empty)
            } catch (exception: kotlinx.coroutines.TimeoutCancellationException) {
                context.getString(R.string.store_timeout)
            } catch (exception: CancellationException) { throw exception }
            catch (exception: Exception) { context.getString(R.string.restore_error) }
            finally { restoring = false }
        }
    }
    if (restoring) LinearProgressIndicator()
    result?.let { text ->
        AlertDialog(onDismissRequest = { result = null }, title = { Text(context.getString(R.string.restore)) }, text = { Text(text) },
            confirmButton = { TextButton(onClick = { result = null }) { Text(context.getString(R.string.done)) } })
    }
}
