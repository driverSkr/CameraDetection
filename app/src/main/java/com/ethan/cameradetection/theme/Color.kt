package com.ethan.cameradetection.theme

import androidx.compose.ui.graphics.Color
import androidx.core.graphics.toColorInt

fun parseColor(colorHex: String): Color {
    return Color(colorHex.toColorInt())
}

val Transparent = Color(0x00000000)

val Purple80 = Color(0xFFD0BCFF)
val PurpleGrey80 = Color(0xFFCCC2DC)
val Pink80 = Color(0xFFEFB8C8)

val Purple40 = Color(0xFF6650a4)
val PurpleGrey40 = Color(0xFF625b71)
val Pink40 = Color(0xFF7D5260)
val PurpleCEA9FF_10 = Color(0x1ACEA9FF)
val Purple9B64FF = Color(0xFF9B64FF)
val PurpleBC5DFF = Color(0xFFBC5DFF)
val Purple790AC9 = Color(0xFF790AC9)
val Purple9258FA = Color(0xFF9258FA)
val PurpleBC97FF = Color(0xFFBC97FF)
val PurpleBE6DFF = Color(0xFFBE6DFF)
val Purple712FE7 = Color(0xFF712FE7)
val Purple291E3D = Color(0xFF291E3D)
val Purple1F1B26 = Color(0xFF1F1B26)
val ColorFF7762_20 = Color(0x33FF7762)
val Color412D5C_20 = Color(0x33412D5C)
val Color0XFF37255B = Color(0XFF37255B)
val Purple8A49FF = Color(0xFF8A49FF)
val Purple8A49FF_20 = Color(0x338A49FF)

val Black = Color(0xFF000000)
val Black90 = Color(0xE6000000)
val Black80 = Color(0xCC000000)
val Black70 = Color(0xB3000000)
val Black60 = Color(0x99000000)
val Black50 = Color(0x80000000)
val Black40 = Color(0x66000000)
val Black30 = Color(0x4D000000)
val Black24 = Color(0x3D000000)
val Black20 = Color(0x33000000)
val Black10 = Color(0x1A000000)
val Black8 = Color(0x14000000)
val Black_24252C = Color(0xFF24252C)
val Black151515 = Color(0xFF151515)
val Black242427 = Color(0xFF242427)
val Black2E1000 = Color(0xFF2E1000)
val Black0C0C0F = Color(0xFF0C0C0F)
val Black0C0C0F60 = Color(0x990C0C0F)
val Black0C0C0F_40 = Color(0x660C0C0F)
val Black0C0C0F_30 = Color(0x4D0C0C0F)
val Black0C0C0F_20 = Color(0x330C0C0F)

val Gray = Color(0xFF808080) //灰色
val Grey20 = Color(0x33D9D9D9)
val DarkGray = Color(0xFFA9A9A9) //深灰色
val LightGray = Color(0xFFD3D3D3) //浅灰色

val White = Color(0xFFFFFFFF) //纯白
val White90 = Color(0xE6FFFFFF)
val White80 = Color(0xCCFFFFFF)
val White70 = Color(0xB3FFFFFF)
val White60 = Color(0x99FFFFFF)
val White50 = Color(0x80FFFFFF)
val White40 = Color(0x66FFFFFF)
val White32 = Color(0x52FFFFFF)
val White30 = Color(0x4DFFFFFF)
val White24 = Color(0x3DFFFFFF)
val White20 = Color(0x33FFFFFF)
val White16 = Color(0x29FFFFFF)
val White12 = Color(0x1FFFFFFF)
val White10 = Color(0x1AFFFFFF)
val White8 = Color(0x14FFFFFF)
val White4 = Color(0x0AFFFFFF)
val White444447 = Color(0xFF444447)

val Brown = Color(0xFFA52A2A) //棕色

val Red = Color(0xFFFF0000) //纯红
val RedFF0048 = Color(0xFFFF0048)
val DarkRed = Color(0xFF8B0000) //深红色
val RedFF5F2D = Color(0xFFFF5F2D)
val RedFF5762 = Color(0xFFFF5762)

val Orange = Color(0xFFFFA500) //橙色
val DarkOrange = Color(0xFFFF8C00) //深橙色

val Gold = Color(0xFFFFD700) //金

val Yellow = Color(0xFFFFFF00) //纯黄
val LightYellow = Color(0xFFFFFFE0) //浅黄色

val Green = Color(0xFF008000) //纯绿
val DarkGreen = Color(0xFF006400) //深绿色
val LightGreen = Color(0xFF90EE90) //淡绿色

val Cyan = Color(0xFF00FFFF) //青色
val DarkCyan = Color(0xFF008B8B) //深青色
val LightCyan = Color(0xFFE1FFFF) //淡青色

val Blue = Color(0xFF0000FF) //纯蓝
val LightBLue = Color(0xFFADD8E6) //淡蓝
val DarkBlue = Color(0xFF00008B) //深蓝色

val Purple = Color(0xFF800080) //紫色

val Pink = Color(0xFFFFC0CB) //粉红
val LightPink = Color(0xFFFFB6C1) //浅粉红

val color1 = Color(0xFF7DB1C2)
val color2 = Color(0xFFBC9F94)
val color3 = Color(0xFFA28DBB)
val color4 = Color(0xFF7AA6CB)
val color5 = Color(0xFF5B967D)
val color6 = Color(0xFFBDA877)
val color7 = Color(0xFFAFAA97)
val color8 = Color(0xFF87A7C5)
val color9 = Color(0xFF679CAC)

val colorList = listOf("#8D7D90", "#D39F81", "#9CAEC9", "#959D88", "#757E6D", "#BCA163", "#D4B7A8", "#675F54", "#336279", "#5E3F2C")

val brushColor = arrayOf(0f to Black, 0.1f to Black90, 0.2f to Black80, 0.3f to Black70, 0.4f to Black60, 0.5f to Black50, 0.6f to Black40, 0.7f to Black30, 0.8f to Black20, 0.9f to Black10, 1f to Transparent)
val brushColorReverse = arrayOf(0f to Transparent, 0.1f to Black10, 0.2f to Black20, 0.3f to Black30, 0.4f to Black40, 0.5f to Black50, 0.6f to Black60, 0.7f to Black70, 0.8f to Black80, 0.9f to Black90, 1f to Black)