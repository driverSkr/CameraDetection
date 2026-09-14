# 中英文适配说明

## 语言规则

- 系统首选语言为中文（包括 zh-CN、zh-TW、zh-HK、zh-Hans、zh-Hant）：应用显示中文，本次中文文案统一为简体。
- 系统首选语言为英文或其他语言：应用显示英文。
- 不读取旧的手动语言偏好，不新增语言选择页面。系统切换语言后，应用在页面重建或重新回到前台时使用对应语言。
- 仅使用系统语言列表的第一项；例如首选法语、次选中文时，应用仍显示英文。

## 覆盖内容

已提供 184 条中英文资源，覆盖引导、底部导航、Wi-Fi 扫描、扫描状态和历史说明、结果列表、设备详情、磁场检测、相机提示及控制、功能页、安全提示、设置、分享、订阅套餐周期和购买异常。

原有页面布局、配色、圆角、图标、雷达及设备详情弹窗保持原样。订阅临时放行开关和 TODO 保留。

扫描记录继续保留原始数据，在展示时转换应用生成的说明；语言切换不会改写检测依据或设备分类。IP、MAC、协议标识、外部设备名称及商店提供的价格保持原值。外部网页、系统权限界面和 Google Play 界面由相应提供方控制语言。

## 主要文件

- `app/src/main/res/values/strings.xml`：默认英文。
- `app/src/main/res/values-zh/strings.xml`：中文。
- `utils/LanguagePolicy.kt`、`utils/AppLanguage.kt`：系统首选语言选择与上下文配置。
- `utils/ScanStrings.kt`：扫描状态、检测依据及已有历史记录的显示转换。
- `app/src/main/res/mipmap-zh-xxhdpi/img_subscribe_card.png`：设置页中文订阅卡片；英文继续使用原图。

## 检查

- 最终构建、单元测试、设备测试源码编译和 Lint 命令通过。Lint 为 0 错误、112 条警告、1 条信息，已有警告未全部清理。
- 35 项 JVM 单元测试覆盖原有回归逻辑，以及中文变体、其他语言英文回退、资源完整性、格式占位符、历史摘要与检测依据转换。
- 设备回归测试源码改为按资源定位文案，避免只匹配英文；本次编译检查，不运行 connectedAndroidTest。
- 已在系统语言为 zh-Hans-CN 的连接手机上确认自动中文，检查扫描首页、取消状态、结果列表、设备详情、设置页和订阅异常页。
- 其他系统语言的选择规则通过单元测试验证，未修改用户手机系统语言。
- 手机更新采用 `adb install -r`，未卸载应用或清空应用数据。
- 构建及最终检查日志：`app/build/localization/final-verification.log`。

## 中文卡片素材

使用内置 image_gen 工具，以原订阅卡片为参照进行文字本地化；保留绿色渐变、金色皇冠和白色胶囊按钮，中文图片在原卡片位置显示并沿用圆角裁剪。首次请求连接失败，重试生成成功，没有使用 CLI/API 回退。

最终项目内素材路径：`C:/Android/driverSkr/CameraDetection/app/src/main/res/mipmap-zh-xxhdpi/img_subscribe_card.png`。

生成提示词保存在 `docs/localization-banner-prompt.txt`。
