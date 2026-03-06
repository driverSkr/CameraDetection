package com.spyfinder.hiddencamera.detectorapp.ui.main.context

import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.spyfinder.hiddencamera.detectorapp.model.WifiDevice

class MainContextEntity {
    var isOpenMainPage by mutableStateOf(false)
    var isStartDetect = mutableStateOf(false)
    var isAnimating = mutableStateOf(false)
    var isShowResult = mutableStateOf(false)

    // DetectPage
    val suspiciousDevices = mutableStateListOf<WifiDevice>()
    val trustedDevices = mutableStateListOf<WifiDevice>()

    val selectTabIndex = mutableIntStateOf(0)
}

val LocalMainContextEntity = compositionLocalOf { MainContextEntity() }