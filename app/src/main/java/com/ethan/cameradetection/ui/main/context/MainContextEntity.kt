package com.ethan.cameradetection.ui.main.context

import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

class MainContextEntity {
    var isOpenMainPage by mutableStateOf(false)
    var isShowResult by mutableStateOf(false)
}

val LocalMainContextEntity = compositionLocalOf { MainContextEntity() }