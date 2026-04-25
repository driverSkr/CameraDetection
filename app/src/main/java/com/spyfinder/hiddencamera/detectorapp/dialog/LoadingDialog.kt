package com.spyfinder.hiddencamera.detectorapp.dialog

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.material.Text
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.spyfinder.hiddencamera.detectorapp.theme.White
import com.spyfinder.hiddencamera.detectorapp.theme.White10

@Composable
@Preview
fun rememberLoadingDialog(): MutableState<Boolean> {
    val loadingDialogState = remember { mutableStateOf(false) }
    if (loadingDialogState.value) {
        Dialog(onDismissRequest = { loadingDialogState.value = false }, properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false)) {
            Box {
                CircularProgressIndicator(modifier = Modifier
                    .align(Alignment.Center)
                    .size(36.dp), color = White, trackColor = White10, strokeCap = StrokeCap.Round)

            }
        }
    }
    return loadingDialogState
}

@Composable
@Preview
fun rememberLoading(): MutableState<Boolean> {
    val loadingDialogState = remember { mutableStateOf(false) }
    if (loadingDialogState.value) {
        Dialog(onDismissRequest = { loadingDialogState.value = false }, properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false)) {}
    }
    return loadingDialogState
}


@Composable
fun rememberLoadingDialogByText(string: String): MutableState<Boolean> {
    val loadingDialogState = remember { mutableStateOf(false) }
    if (loadingDialogState.value) {
        Dialog(onDismissRequest = { loadingDialogState.value = false }, properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false)) {
            Box {
                Column (modifier = Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally){
                    CircularProgressIndicator(modifier = Modifier
                        .size(36.dp), color = White, trackColor = White10, strokeCap = StrokeCap.Round)
                    Text(text = string, fontSize = 14.sp, color = White)
                }


            }
        }
    }
    return loadingDialogState
}