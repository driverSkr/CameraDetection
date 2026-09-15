# 项目再次复查

日期：2026-09-15；代码基线：`24e82c9`。

结论：本轮未确认新的 P0/P1 问题，建议处理 4 项 P2 和 1 项 P3，另有 2 项优化建议。自动化测试通过不代表异常网络、所有硬件和屏幕组合均已验证。本轮只新增本报告，没有修改业务代码。

## 1. P2：组播探测失败被静默忽略

位置：[NetworkScanner.kt:271](/C:/Android/driverSkr/CameraDetection/app/src/main/java/com/spyfinder/hiddencamera/detectorapp/scan/NetworkScanner.kt:271)，相关汇总在同文件 149 行。

- **触发**：mDNS、SSDP 或 ONVIF 的 socket 绑定、发送或接收发生 IOException。
- **现状**：整个 UDP 通道直接结束，catch 不记录错误；上层 finally 仍标记组播阶段结束。最终摘要只统计 TCP 地址错误、无法解析的公告等，没有组播通道失败信息。
- **影响**：用户无法区分“正常执行但没有设备回复”和“探测请求根本没有成功发出”。其他工作完成时，摘要仍可显示扫描完成，遗漏了重要的覆盖限制。
- **建议**：给每个发现通道记录成功发起、正常结束、失败及原因；在结果和历史显示失败通道及重试提示。保留“100% 表示计划工作结束”的既有语义，不必把所有无响应或证据不确定都改成扫描失败。
- **验证程度**：代码路径确认；本轮未对 Android socket 注入故障。

## 2. P2：相机入口缺少重复点击和重复导航保护

位置：[ScannerPage.kt:82](/C:/Android/driverSkr/CameraDetection/app/src/main/java/com/spyfinder/hiddencamera/detectorapp/ui/main/page/ScannerPage.kt:82)。

- **触发**：在页面切换前快速连续点击扫描位置；恢复订阅拦截后，权益查询等待期间连续点击也会排队。
- **现状**：每次点击启动一个协程，独立调用相机 Activity 或订阅 Activity 的启动方法；没有 checking/opening 状态。相机 Activity 使用默认启动模式，没有去重保障。
- **影响**：可能重复打开页面，返回时出现多层相同页面；同时重复记录打开事件。
- **建议**：从接受首次点击到导航完成持有单次操作标记，期间禁用入口；返回后复位。磁场开始入口的权益查询也应采用同样的保护。
- **验证程度**：调用路径确认；现有真机测试没有覆盖快速连点，未把重复页面描述为本轮实测结果。

## 3. P2：相机重试过程中仍显示旧错误，允许连续重建预览

位置：[CameraScannerPage.kt:151](/C:/Android/driverSkr/CameraDetection/app/src/main/java/com/spyfinder/hiddencamera/detectorapp/ui/camera/page/CameraScannerPage.kt:151)。

- **触发**：相机报错后点击重试。
- **现状**：重试只清理控制请求、置空 camera 并增加 retry；没有清空 error。错误卡片因 error 非空继续显示，“正在启动”因 error 非空不会显示。直到 onReady 成功才清除旧错误。
- **影响**：重试没有明确反馈；用户继续点击会反复销毁并创建预览，可能延长恢复时间。
- **建议**：重试立即切换到加载状态、清除上一轮错误，加载期间禁用重试；超时或新错误后再允许重试。权限从设置恢复时一并处理旧错误状态。
- **验证程度**：状态条件确认；真机正常预览和控件测试通过，故障恢复注入尚未执行。

## 4. P2：扫描位置页和磁场页仍有小屏、大字体布局风险

位置：[ScannerPage.kt:117](/C:/Android/driverSkr/CameraDetection/app/src/main/java/com/spyfinder/hiddencamera/detectorapp/ui/main/page/ScannerPage.kt:117)、[SensorPage.kt:290](/C:/Android/driverSkr/CameraDetection/app/src/main/java/com/spyfinder/hiddencamera/detectorapp/ui/main/page/SensorPage.kt:290)。

- **扫描位置页**：标题位于顶部，网格独立居中，二者没有共享垂直布局约束。12 个项目共 4 行，每行 86dp、行间 20dp，内容约 404dp；可用高度较小或标题换行时，网格会侵入标题区域。
- **磁场页**：仪表固定 280dp 并向上偏移；底部提示固定 60dp，文本最多两行并使用省略号。较长英文校准提示或大字体下，操作说明会被截断，且没有展开入口。
- **建议**：让标题与网格按 Column/weight 分配空间；磁场提示允许自适应高度或展开，空间不足时可滚动，保留当前仪表和视觉风格。
- **验证程度**：布局约束风险；本轮没有修改手机字号、分辨率，也没有完成多尺寸视觉验收。建议覆盖 360×640dp、中文/英文、1.0/1.5/2.0 字体比例。

## 5. P3：一次手动取消上报两次同名事件

位置：[DetectCheckView.kt:210](/C:/Android/driverSkr/CameraDetection/app/src/main/java/com/spyfinder/hiddencamera/detectorapp/ui/main/view/DetectCheckView.kt:210)、[ScanViewModel.kt:152](/C:/Android/driverSkr/CameraDetection/app/src/main/java/com/spyfinder/hiddencamera/detectorapp/scan/ScanViewModel.kt:152)。

- **触发**：点击扫描取消按钮。
- **现状**：按钮先上报 WIFI_SCAN_CANCEL，随后 vm.cancel() 再上报同名事件；参数不同，但事件名相同。
- **影响**：按事件次数统计时，一次操作计为两次取消，影响扫描放弃率和漏斗分析。
- **建议**：在 ViewModel 统一上报取消，将来源和进度作为参数传入；若保留按钮点击事件，应使用不同事件名。
- **验证程度**：两个同步调用点确认；没有查询线上统计数据。

## 优化建议，不作为已实测性能缺陷

### A. 大网段扫描的长尾耗时

位置：[ProbeSupport.kt:9](/C:/Android/driverSkr/CameraDetection/app/src/main/java/com/spyfinder/hiddencamera/detectorapp/scan/ProbeSupport.kt:9)、[ScanPipeline.kt:9](/C:/Android/driverSkr/CameraDetection/app/src/main/java/com/spyfinder/hiddencamera/detectorapp/scan/ScanPipeline.kt:9)、[NetworkScanner.kt:194](/C:/Android/driverSkr/CameraDetection/app/src/main/java/com/spyfinder/hiddencamera/detectorapp/scan/NetworkScanner.kt:194)。

发现阶段每个未响应地址串行探测 5 个端口，每次连接超时 450ms，32 个工作者。假设约 4096 个地址都耗尽超时，仅发现阶段的粗略计算为 `4096 × 5 × 0.45 / 32 ≈ 288 秒`。这是代码参数推算，不是本轮真机耗时。分析阶段有 8 个工作者，每台设备的多个端口也串行执行，另有连接和读取等待。

建议先记录发现/分析各阶段耗时、响应类型分布、剩余任务数量，再考虑共享总并发预算下的端口级调度、会话内探测结果复用，并更清楚地显示剩余分析工作。保留完整队列执行与取消能力，不重新引入以前已经移除的固定总时长截断。

### B. Release 包未启用代码及资源收缩

位置：[app/build.gradle:49](/C:/Android/driverSkr/CameraDetection/app/build.gradle:49)。

minifyEnabled、shrinkResources 均被注释。可评估开启后的包体收益，清理不用的资源与依赖。需先验证反射、支付、Firebase、资源引用及崩溃符号还原；本轮没有生成收缩包，不能给出包体或启动速度收益数字。

## 已复核的上一轮修复

- 扫描运行时保持亮屏，退出/停止释放；主动进入后台仍会取消扫描。
- mDNS 已关联 PTR、SRV、A/AAAA，并使用公告端口，不再直接把公告目标归到发送方。
- 扫描队列有并发上限，取消时关闭 socket，进度和证据确定性保持区分。
- 设备快照缓存、最近/完整历史区分、原子文件写入和失败提示仍在。
- 磁场低精度、样本过期处理以及相机异步错误观察已存在。
- 运行中的 Wi-Fi 扫描从功能页返回时不会主动重启。
- 订阅绕过开关是上一轮明确保留的测试配置。本轮未恢复拦截，也不把它计为新增缺陷；发布时应单独核对。

## 本轮验证结果

| 检查 | 结果 |
|---|---|
| `:app:testProdVersionDebugUnitTest` | 90 项通过，0 失败、0 错误、0 跳过 |
| `:app:lintProdVersionDebug` | 0 错误、114 条警告、1 条信息 |
| `:app:connectedDevVersionDebugAndroidTest` | PLY110 / Android 16 真机：3 项通过，0 失败、0 错误、0 跳过 |

真机用例包括 Activity 重建后的 ViewModel 保留、相机预览和滤镜/可用镜头切换、订阅页可用或不可用状态。执行设备测试构建并部署了 devVersion debug 应用及测试包。未提交真实购买。

本轮没有重新执行完整 Wi-Fi 扫描、组播错误注入、相机占用/断流注入、传感器断流注入，也没有做多字号/屏幕或 4096 设备真机压测。

Lint 警告主要是依赖版本/版本目录建议、矢量路径、未使用资源和弃用 API 等；114 条警告不能等同于 114 个用户可见缺陷。

建议顺序：先补组播失败记录及入口保护，再完善相机重试和布局，随后统一埋点；性能方案依据阶段耗时实测决定。
