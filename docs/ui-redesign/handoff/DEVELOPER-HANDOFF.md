# SpyFinder 新 UI · 开发交付说明（v3）

> 配套：设计画布《SpyFinder UI 交付包（产品版 + 开发版）》开发版板块；
> 代码级 Token：同目录 `DesignTokens.kt`；整页参考图：`new-ui-concepts/00~05`。

## 1. 全局约定

- **双模式**：浅色「信任壳」（首页/结果等日常页）+ 深色「检测沉浸」（扫描/相机/订阅）。
  现有 `theme/Theme.kt` 需重建：删除默认紫色 Material 主题与 dynamicColor，按本 Token 表建立
  `SpyFinderLightColorScheme` / `SpyFinderDarkColorScheme`，按路由切换。
- **全局 density 覆盖要移除**：main 分支的 393dp 缩放 hack 会干扰新布局，落地时去掉，改用标准 dp/sp。
- **字体**：数字/英文 Inter（SemiBold/Bold），中文 Noto Sans SC（Regular/Medium/SemiBold）。
  工程内统一走 `Typography` 扩展，禁止页面内散落 fontSize。

## 2. 色彩 Token

见 `DesignTokens.kt`（与开发版规范板色值一一对应）。语义色规则：

| 语义 | 数字/图标 | 胶囊文字 | 胶囊/图标底 |
|---|---|---|---|
| 风险（待核实） | `RiskRed #E5484D` | `RiskDeep #C7362C` | `RiskBg #FDE7E9` |
| 安全（已确认/本机） | `SafeGreen #34C759` | `SafeDeep #1D9A4F` | `SafeBg #DFF5E7` |
| 品牌/CTA | `AccentCta #0066CC`（按钮/选中） | — | — |

## 3. 核心组件规格

| 组件 | 规格 |
|---|---|
| PrimaryButton | 高 56 · 全宽 · r999 · bg `#0066CC` · 文字 17 Medium 白 |
| SecondaryButton（深色页） | 高 52/56 · r999 · bg `DarkSurface #1D1D1F` |
| StatusChip | 高 26 · r999 · 文字 12 Medium · risk/safe 见上表 |
| StatCard | r18 · padding 16 · gap 4 · bg `#F5F5F7`/`#1D1D1F` · 数字 32 Bold |
| 设备行 | r18 · padding 14 · 图标容器 44 r11 bg `#F5F5F7` |
| PillTabBar | 高 62 · 外胶囊 r36 · padding 4 gap 4 · 选中填充 `#0066CC` |
| 圆角体系 | 卡片 18 / 列表内图标 11 / 胶囊 999 |

## 4. 雷达（扫描页）分层与动画

与设计稿图层同名对应，建议 Compose 单 Canvas 实现：

| 层 | 内容 | 动画 |
|---|---|---|
| L1 网格圈层 | 4 环（#2E5580 w2.5 → #3A6899 → #4A7DB4 → #5C95CE w1.5）+ 十字轴 #22456B | 静态 |
| L2 刻度环 | 外圈 12 × 30° 刻度（r132→142），#3A6899 w2 圆头 | 静态 |
| L3 扫描扇面 | 宽扇 45° `#16395F` α55% + 窄扇 15° `#2C6BB0` α40% + 引导线 `#4DA6FF` 3px + 端点光点 | rotate 360°/3s Linear 无限 |
| L4 设备点 | 可疑点 `#E5484D` r5 + 光环 `#4A1A1E` r9→14 / α1→0 | 脉冲 1.2s 循环，位置=扫描结果 |
| L5 进度层 | 大数字 Inter SemiBold 52（压雷达中心）+ 进度条 h6 r999（底 `#272729`/填充 `#2997FF`） | 数据驱动 |

要点：扇面渐变余晖用两层不同 alpha 的 `drawArc` 叠加；红点只在真实扫描命中时出现。

## 5. 页面清单（对应设计稿编号）

1. `01 首页` Wi-Fi 状态胶囊 + 双 StatCard + 主视觉 + 唯一 CTA
2. `02 扫描中` 深色沉浸 + 雷达五层 + 取消按钮
3. `03 检测结果` 双统计卡 + 设备列表（类型推断副标题 + 状态胶囊）+ 重新检测
4. `04 相机检查` 全屏取景 + 四角框 + 滤镜胶囊（红/绿/蓝/重置）
5. `05 订阅` 深色 + 主视觉 + 图标化权益 + 周/年套餐（年卡「最超值」描边卡）

## 6. 落地顺序建议

Theme/Token → 首页 + 扫描页（雷达）→ 结果页 → 订阅页 → 相机页 → 磁场/工具/引导页（待补设计稿）。
