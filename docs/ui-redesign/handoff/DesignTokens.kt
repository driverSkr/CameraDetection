package com.spyfinder.hiddencamera.detectorapp.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * SpyFinder 新 UI Design Tokens v3（方向 C：浅色信任壳 + 深色检测沉浸）
 * 与设计画布《SpyFinder UI 交付包（产品版 + 开发版）》· 开发版设计规范一一对应。
 */

// ---------- 品牌 / 强调 ----------
val AccentCta = Color(0xFF0066CC)      // 主 CTA、选中态
val RadarSweep = Color(0xFF2997FF)     // 扫描进度条 / 雷达进度
val RadarSweepLine = Color(0xFF4DA6FF) // 雷达扫描引导线 / 端点光点
val RadarGrid = Color(0xFF2E5580)      // 雷达网格基准色

// ---------- 语义色：红 = 风险 / 绿 = 安全 ----------
val RiskRed = Color(0xFFE5484D)        // 数字 / 图标 / 雷达可疑点
val RiskDeep = Color(0xFFC7362C)       // 胶囊文字
val RiskBg = Color(0xFFFDE7E9)         // 胶囊 / 图标底
val SafeGreen = Color(0xFF34C759)      // 数字
val SafeDeep = Color(0xFF1D9A4F)       // 胶囊文字
val SafeBg = Color(0xFFDFF5E7)         // 胶囊 / 图标底

// ---------- 浅色（信任壳） ----------
val LightBg = Color(0xFFFFFFFF)
val LightSurface = Color(0xFFF5F5F7)
val LightTextPrimary = Color(0xFF1D1D1F)
val LightTextSecondary = Color(0xFF7A7A7A)
val LightBorder = Color(0xFFE0E0E3)

// ---------- 深色（检测沉浸） ----------
val DarkBg = Color(0xFF000000)
val DarkSurface = Color(0xFF1D1D1F)
val DarkSurface2 = Color(0xFF2A2A2C)
val DarkTextPrimary = Color(0xFFFFFFFF)
val DarkTextSecondary = Color(0xFFCCCCCC)
val DarkProgressTrack = Color(0xFF272729)

// ---------- 雷达圈层（由外向内） ----------
val RadarRingOuter = Color(0xFF2E5580) // w 2.5
val RadarRing2 = Color(0xFF3A6899)     // w 1.75（刻度环同色）
val RadarRing3 = Color(0xFF4A7DB4)     // w 1.5
val RadarRingInner = Color(0xFF5C95CE) // w 1.5
val RadarAxis = Color(0xFF22456B)      // 十字轴 w 1
val RadarWedgeWide = Color(0x8C16395F) // 宽扇 45°，alpha 55%
val RadarWedgeNarrow = Color(0x662C6BB0) // 窄扇 15°，alpha 40%
val RadarBlip = Color(0xFFE5484D)      // 可疑设备点
val RadarBlipHalo = Color(0xFF4A1A1E)  // 可疑点光环
