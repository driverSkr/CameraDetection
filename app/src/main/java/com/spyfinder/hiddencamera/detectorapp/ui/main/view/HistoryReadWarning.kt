package com.spyfinder.hiddencamera.detectorapp.ui.main.view
import com.spyfinder.hiddencamera.detectorapp.theme.AppSpacing
import com.spyfinder.hiddencamera.detectorapp.theme.AppColors

import androidx.compose.runtime.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.spyfinder.hiddencamera.detectorapp.R
import com.spyfinder.hiddencamera.detectorapp.scan.ScanStatus
import com.spyfinder.hiddencamera.detectorapp.theme.AppColors.textSecondary
import com.spyfinder.hiddencamera.detectorapp.ui.main.context.LocalMainContextEntity
import com.spyfinder.hiddencamera.detectorapp.utils.ScanHistoryStore
import kotlinx.coroutines.launch

@Composable fun HistoryReadWarning() {
    val failed by ScanHistoryStore.readFailed.collectAsState()
    val context = LocalContext.current
    val state = LocalMainContextEntity.current
    val scope = rememberCoroutineScope()
    var retrying by remember { mutableStateOf(false) }
    if (failed) {
        Text(context.getString(R.string.history_read_failed), color = AppColors.textSecondary, fontSize = 12.sp)
        Text(context.getString(if (retrying) R.string.action_checking else R.string.history_retry_read),
            color = AppColors.textSecondary, fontSize = 12.sp,
            modifier = Modifier.clickable(enabled = !retrying && state.scanStatus != ScanStatus.RUNNING) {
                retrying = true
                scope.launch { try { state.retryHistoryLoad() } finally { retrying = false } }
            }.padding(vertical = AppSpacing.section))
    }
}