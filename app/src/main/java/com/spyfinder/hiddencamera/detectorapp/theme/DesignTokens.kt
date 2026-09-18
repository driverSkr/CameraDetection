package com.spyfinder.hiddencamera.detectorapp.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** 当前深色检测界面共用的视觉设计 Token。 */
object AppColors {
    /** 透明色，用于渐变端点和透明边框。 */
    val transparent = Transparent
    /** 页面背景色。 */
    val background = Black
    /** 普通卡片和容器的背景色。 */
    val surface = Color(0xFF1E2024)
    /** 层级更高的卡片背景色。 */
    val surfaceElevated = Color(0xFF242227)
    /** 弹窗和错误面板的背景色。 */
    val dialogSurface = Color(0xFF161618)
    /** 扫描进行中横幅的背景色。 */
    val scanBanner = Color(0xFF1C3A2E)
    /** 扫描进行中横幅的进度文字颜色。 */
    val scanBannerText = Color(0xFFB7E0C8)
    /** 主要品牌色，用于主按钮和强调状态。 */
    val primary = Color(0xFF00C46F)
    /** 品牌色的深色变体。 */
    val primaryDark = Color(0xFF01C587)
    /** 品牌色的浅色变体，常用于渐变和高亮。 */
    val primarySoft = Color(0xFFBCF085)
    /** 主要文字颜色。 */
    val textPrimary = White
    /** 次要文字颜色。 */
    val textSecondary = White60
    /** 辅助说明文字颜色。 */
    val textMuted = Color(0xFF96939E)
    /** 浅色渐变上的深色文字。 */
    val textOnBright = Color(0xFF010101)
    /** 边框和弱分隔线颜色。 */
    val outline = White10
    /** 半透明白色蒙层。 */
    val overlayLight = Color(0x33FFFFFF)
    /** 分类 tab 未选中胶囊，比列表卡片更透。 */
    val tabSurface = Color(0x0DFFFFFF)
    /** 分类 tab 选中胶囊。 */
    val tabSurfaceSelected = Color(0x3300C46F)
    /** 半透明黑色遮罩。 */
    val scrim = Black60
    /** 错误提示颜色。 */
    val error = Color(0xFFFF5F5F)
    /** 摄像头线索的强调色和淡色背景。 */
    val warning = Color(0xFFFE2D3F)
    val warningSurface = Color(0x33FE2D3F)
    /** 摄像头线索列表卡片浅底，弱于顶部计数卡。 */
    val warningSurfaceMuted = Color(0x1AFE2D3F)
    /** 相机滤镜的红、绿、蓝三种颜色。 */
    val filterRed = Color(0xFFDD1313)
    val filterGreen = Color(0xFF00C424)
    val filterBlue = Color(0xFF1C73FF)
    /** 引导页未选中圆点的颜色。 */
    val indicatorInactive = Color(0xFF5B5B5E)
    /** 结果数量的橙色强调色。 */
    val resultAccent = Orange
    /** 强错误色，用于无法加载商品的状态。 */
    val errorStrong = Red
}

object AppSpacing {
    /** 图标、文字和紧凑元素之间的最小间距。 */
    val micro = 4.dp
    /** 页面内容的常用左右边距。 */
    val screen = 16.dp
    /** 区块或列表项之间的间距。 */
    val section = 12.dp
    /** 紧凑控件之间的间距。 */
    val compact = 8.dp
    /** 页面中使用的大间距。 */
    val large = 24.dp
    /** 页面内容距离顶部的常用间距。 */
    val pageTop = 18.dp
    /** 顶部栏的标准高度。 */
    val topBar = 54.dp
    /** 主要按钮和列表项的标准高度。 */
    val control = 56.dp
    /** 底部导航栏的标准高度。 */
    val navigationBar = 64.dp
    /** 大型横向内容的常用宽度间距。 */
    val wide = 48.dp
}

object AppShapes {
    /** 普通卡片的圆角半径。 */
    val card = 20.dp
    /** 大卡片或重点容器的圆角半径。 */
    val cardLarge = 24.dp
    /** 胶囊按钮、标签和圆形控件使用的圆角半径。 */
    val pill = 999.dp
}

object AppTypography {
    /** 页面主标题字号。 */
    val pageTitleSize = 28.sp
    /** 顶部栏标题字号。 */
    val topBarTitleSize = 18.sp
    /** 正文和页面说明字号。 */
    val bodySize = 14.sp
    /** 标签和辅助信息字号。 */
    val labelSize = 12.sp
    /** 页面主标题字重。 */
    val pageTitleWeight = FontWeight.W700
    /** 正文字重。 */
    val bodyWeight = FontWeight.W400
    /** 标签文字字重。 */
    val labelWeight = FontWeight.W500
}
