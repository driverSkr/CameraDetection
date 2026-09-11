# Quiet Check UI 实施记录

已在 `feature-new_ui` 分支将原型落实到 Android Compose 页面。APK 位于 `app/build/outputs/apk/devVersion/debug/app-devVersion-debug.apk`。

## 已替换

- 全局浅色 / 深色配色，恢复系统原生 density 和字号缩放；统一状态栏前景。
- 四栏主导航：Wi-Fi / Magnetic / Scanner / Tools。
- Wi-Fi 首页、真实扫描数量、完成摘要、最近结果、锁定结果、设备列表与详情。
- 磁场待机 / 测量 / 设备不支持页面，原始 μT 数值与生命周期注销。
- 全部 12 个检查位置、相机说明与权限恢复、实际 CameraX 预览、三色滤镜和重置。
- 工具、技巧折叠列表、设置、三页引导。
- 订阅套餐、加载、商品失败、购买失败 / 取消 / 断线、恢复购买反馈。
- 可复用的页面容器、标题、按钮、卡片、图标容器、状态标签、环形主视觉、统计块和说明文本。

Wi-Fi 与磁场的主要操作固定在底部，内容区域可滚动，避免小屏和大字号时主操作被挤出首屏。12 个位置在窄屏上可纵向滚动查看完整内容。

## 与原型一致的业务边界

- 原有 Wi-Fi 结果、磁场和相机订阅门槛保留，购买成功后继续原操作。
- 首次引导进入订阅、非订阅用户冷启动展示订阅的路由保留。
- 最近记录仍仅保存最新一次结果，未添加多次历史、云同步或虚构时间。
- 设备列表显示待核实 / 本机与已确认，保留原有手动确认能力。
- 相机使用真实预览与颜色滤镜，不展示原型占位、模拟结果或自动识别结论。
- 正式 UI 不包含原型的页面选择器、演示数据或示例报价。

## 为正确呈现状态所做的配套修复

- 修正磁场归一化数值被标为 μT 的问题。
- 新的 Wi-Fi 页面展示真实发现数量，不沿用定时增长百分比或随机红点。
- 扫描启动失败走独立错误分支，不保存成成功空记录；页面等待超过 60 秒显示失败。
- 离开 Wi-Fi 页面时取消当前界面扫描会话，旧回调不会更新新会话；最近完成的结果保留。
- 网络权限按用户操作请求，新增说明、拒绝恢复和系统设置返回处理；补充粗略定位声明和请求。
- 相机初始化异常显示重试，离开预览释放当前 CameraX use case。
- 商品列表只展示商店成功返回价格的套餐；加载失败时购买按钮禁用。
- 支付成功 / 已拥有同步完成状态，使页面正确关闭并续接原流程。
- 系统返回由 Compose BackHandler 处理订阅关闭路由；移除旧的 Activity 返回覆盖。

## 验证情况

### 已完成

- 开发版 APK 构建。
- 最终 `assembleDevVersionDebug` 和 `lintDevVersionDebug` 均成功；Lint 为 0 errors / 127 warnings（包括项目已有的依赖、资源及兼容性提示）。
- Compose 自动化测试 APK 构建。
- 已在当前连接的 PLY110 手机覆盖安装开发版，未清除原应用数据。
- 使用真实 App 检查 Wi-Fi → Magnetic → Scanner → Tools → Tips → Settings → Wi-Fi 的导航与返回。
- 检查订阅页商品查询失败界面及购买禁用状态。
- 检查 Wi-Fi 与磁场固定底部按钮在设备上的实际可见性。
- 保存以下实机截图，均来自实际运行的 App。
- `git diff --check` 通过。构建与测试尝试日志保存于 `app/build/reports/ui-redesign/`。

| 页面 | 实机截图 |
| --- | --- |
| Wi-Fi | [首页](screenshots/device-home.png) |
| Magnetic | [磁场](screenshots/device-magnetic.png) |
| Scanner | [检查位置](screenshots/device-scanner.png) |
| Tools | [工具](screenshots/device-tools.png) |
| Tips | [技巧](screenshots/device-tips.png) |
| Settings | [设置](screenshots/device-settings.png) |
| Subscription | [订阅加载失败状态](screenshots/device-subscribe.png) |

### 验证限制

手机的系统安装防护阻止安装独立的 `com.spyfinder.hiddencamera.detectorapp.test` 测试 APK，因此没有运行 JUnit instrumentation 测试，不将其计为通过。没有修改设备防护设置。新增测试源码覆盖引导前后跳转、导航与 12 个位置、未订阅结果不暴露设备名、深色与 1.5 倍字号，后续可在允许测试 APK 的设备上运行。

尚未实测：实际购买 / 恢复成功、真实磁场连续测量、付费相机滤镜完整流程、各种权限拒绝组合、异常网络扫描和深色大字号设备表现。本次实机检查使用现有未订阅状态，未绕过付费门槛或发起交易。

当前扫描层依然沿用原有子网与端口识别方法。UI 会话取消会忽略旧回调，但第三方库已发出的底层网络请求没有新增强制中止能力。本轮未重写设备分类算法。

页面语言沿用原有英文；新界面文案尚未完整抽取为多语言资源，后续本地化应单独完成。
