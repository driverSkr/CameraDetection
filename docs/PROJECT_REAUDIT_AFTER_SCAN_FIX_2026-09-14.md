# 扫描修复后的项目复查

日期：2026-09-14。代码基线：`1a39761`（wifi和磁场扫描优化）。

本轮检查 Wi-Fi 发现与服务分析、进度和生命周期、历史记录、磁场、相机权限与恢复、功能入口、订阅、语言和设置流程。结论：建议处理 **1 项 P1、6 项 P2**；另有 **2 项需要压测的性能优化点**。没有发现足以列为 P0 的问题，不代表所有设备和异常场景已经验证。

本轮只新增复查报告与构建目录中的验证材料，未修改业务代码或 UI，也没有恢复订阅拦截。

## 问题清单

### R1 · P1：自动灭屏仍会中断长时间 Wi-Fi 扫描

**触发条件**：用户开始扫描后不操作，系统灭屏时间短于扫描时间；或用户主动锁屏/切到其他应用。

**代码依据**：[ScanViewModel.kt:39](/C:/Android/driverSkr/CameraDetection/app/src/main/java/com/spyfinder/hiddencamera/detectorapp/scan/ScanViewModel.kt:39) 在进程进入后台时直接取消当前扫描。应用源码未设置扫描期间的 `keepScreenOn` / `FLAG_KEEP_SCREEN_ON`，也没有后台扫描服务。

**影响**：45 秒截断已经修复，但普通用户仍可能在 30 秒或 1 分钟自动灭屏后得到取消结果。当前两轮真实扫描均约 134 秒，生命周期处理必须与这一时长匹配。

**验证程度**：代码路径确认；本轮只读查询手机设置，灭屏超时为 `600000ms`（10 分钟），插电保持唤醒为 `15`。这解释了此前接线测试为何未覆盖普通灭屏条件；本轮没有修改这些系统设置，也未声称已复现自动灭屏取消。

**建议**：扫描 RUNNING 时保持前台屏幕常亮，完成、失败、取消和退出时释放；明确主动离开应用的中断行为。若产品需要锁屏后继续扫描，再设计前台服务及通知。前台亮屏方案符合 [Android 官方保持亮屏指南](https://developer.android.com/develop/background-work/background-tasks/awake/screen-on)。

### R2 · P2：mDNS 服务线索可能归属到错误设备，公告端口未参与分析

**触发条件**：网络通过 mDNS 代理/网关代答，或设备公布的服务端口不在当前固定探测端口中。

**代码依据**：[DiscoveryProtocols.kt:53](/C:/Android/driverSkr/CameraDetection/app/src/main/java/com/spyfinder/hiddencamera/detectorapp/scan/DiscoveryProtocols.kt:53) 只提取 PTR 服务名称，忽略 SRV、A/AAAA；[NetworkScanner.kt:227](/C:/Android/driverSkr/CameraDetection/app/src/main/java/com/spyfinder/hiddencamera/detectorapp/scan/NetworkScanner.kt:227) 将所得证据全部归到 UDP 报文发送方 IP。

**实际验证**：使用当前编译出的解析器，输入含 PTR、SRV、A 的合法构造报文：代理发送方为 `192.168.2.1`，公告目标为 `192.168.2.99:9554`。解析器输出只有服务名称和摄像头相关标记，没有目标 IP、端口；按当前调用链会归到代理地址。这是本地构造数据验证，不是发现现场路由器确实有此误报。

**影响**：可能把代理/路由器标为有视频线索，并且没有主动验证公告的实际服务端点。普通设备自行应答时不一定触发地址归属错误。

**建议**：按实例关联 PTR→SRV→A/AAAA，保留服务端口；仅对当前允许网段内的端点进行有界验证。无法解析端点时明确保留“未解析服务公告”，不要自动归属发送方。SRV 提供真实目标主机和端口，见 [RFC 6763 §5](https://datatracker.ietf.org/doc/html/rfc6763#section-5)。

### R3 · P2：完成说明能换行，但小屏和大字体下仍可能与雷达重叠

**触发条件**：完成说明较长，配合较小可用高度、较大系统字体或较长英文文本。

**代码依据**：[DetectCheckView.kt:125](/C:/Android/driverSkr/CameraDetection/app/src/main/java/com/spyfinder/hiddencamera/detectorapp/ui/main/view/DetectCheckView.kt:125) 的头部说明自动增高；[同文件:129](/C:/Android/driverSkr/CameraDetection/app/src/main/java/com/spyfinder/hiddencamera/detectorapp/ui/main/view/DetectCheckView.kt:129) 的雷达始终固定 313dp、独立居中。两块布局没有相互避让或滚动约束。

**验证程度**：这是由布局约束确认的边界风险。当前连接手机、当前中文和字体下已再次只读检查，说明完整显示且无重叠；本轮未切换系统字体或屏幕尺寸。

**建议**：保留原雷达、配色、字体与换行行为，让头部和雷达共享可用高度约束；空间不足时对说明区域提供可访问的滚动或展开方式。验收至少覆盖 360×640dp、英文和 1.5/2.0 字体比例。

### R4 · P2：相机绑定后的异步失败缺少错误恢复

**触发条件**：CameraX 绑定返回后，硬件打开或会话配置发生错误，或使用中相机出现严重错误。

**代码依据**：[CameraPreview.kt:47](/C:/Android/driverSkr/CameraDetection/app/src/main/java/com/spyfinder/hiddencamera/detectorapp/ui/camera/view/CameraPreview.kt:47) 在 `bindToLifecycle` 返回后立即调用 ready，错误处理只捕获初始化和绑定调用抛出的异常；没有观察 `cameraInfo.cameraState` 的异步错误，也没有根据预览流状态判断是否真正开始显示。

**影响**：部分黑屏场景可能仍显示可操作控件，页面没有相应故障提示与重试入口。既有缩放/闪光灯错误处理不能覆盖预览本身的失败。

**验证程度**：代码缺口确认，故障注入尚未执行。CameraX 官方区分可自动恢复与需要用户/开发者介入的错误，见 [CameraState](https://developer.android.com/reference/androidx/camera/core/CameraState)。

**建议**：按生命周期观察相机状态，区分正在打开、预览中、可恢复错误和严重错误；接入现有局部提示及重试区域，并在切镜头/离页时移除旧观察者。

### R5 · P2：磁场读数未反映传感器精度和数据失效状态

**触发条件**：系统报告磁场精度低或不可靠；或者已注册的传感器长时间不再产生有效样本。

**代码依据**：[SensorPage.kt:112](/C:/Android/driverSkr/CameraDetection/app/src/main/java/com/spyfinder/hiddencamera/detectorapp/ui/main/page/SensorPage.kt:112) 接受有限数值即更新读数，`onAccuracyChanged` 为空；当前未跟踪最后有效样本时间。失去有效样本后可以一直保留旧值及“停止检测”状态。

**影响**：指针比例虽然已校准，用户仍无法区分可信的实时读数、低精度读数和停留的旧值。显示映射正确不等于硬件测量可靠。

**验证程度**：代码缺口确认；此前真机正常采样和停止已验证。本轮没有制造磁干扰或硬件失效。[Android 传感器指南](https://developer.android.com/develop/sensors-and-location/sensors/sensors_overview) 定义了精度回调及不可靠、低、中、高精度状态。

**建议**：接收事件精度和精度变化回调；低精度时显示简短校准提示；对长时间无有效样本设置失效状态和重试，不改变原仪表及 μT 线性映射。

### R6 · P2：从“功能”重新进入 Wi-Fi 会重启正在进行的扫描

**触发路径**：开始 Wi-Fi 扫描 → 切到“功能”Tab → 点击 Wi-Fi 功能卡片。

**代码依据**：[FeaturePage.kt:83](/C:/Android/driverSkr/CameraDetection/app/src/main/java/com/spyfinder/hiddencamera/detectorapp/ui/main/page/FeaturePage.kt:83) 无条件设置自动扫描标记；[DetectCheckView.kt:94](/C:/Android/driverSkr/CameraDetection/app/src/main/java/com/spyfinder/hiddencamera/detectorapp/ui/main/view/DetectCheckView.kt:94) 收到标记就调用 `vm.start()`；`start()` 首先取消已有扫描。切换应用内 Tab 本身并不会使进程后台化。

**影响**：用户原本只是回到扫描界面，却丢失正在推进的进度，从 0 开始。长扫描下代价更明显。

**验证程度**：连续代码路径确认，本轮未启动额外扫描来覆盖用户当前记录。

**建议**：RUNNING 时仅导航到已有扫描；空闲时才根据入口意图自动开始。将“重新扫描”保留为明确操作。

### R7 · P2：“评分”按钮存在点击后无反馈的正常分支

**触发条件**：Google Play 内评额度不允许弹窗，或请求失败。

**代码依据**：[SettingPage.kt:111](/C:/Android/driverSkr/CameraDetection/app/src/main/java/com/spyfinder/hiddencamera/detectorapp/ui/setting/page/SettingPage.kt:111) 的评分按钮只调用内评 API；失败仅输出日志，没有商店入口。注释中的固定次数/永久不能再弹等断言不应作为产品逻辑依据。

**影响**：用户明确点击评分却看不到操作结果。API 的完成回调也不能证明用户看到了弹窗或已评分。

**验证程度**：代码路径及官方行为确认，本轮未调用真实评分流程。Google 官方明确建议不要使用显式按钮触发受额度限制的内评，见 [In-App Reviews 指南](https://developer.android.com/guide/playcore/in-app-review)。

**建议**：设置里的评分按钮打开商店详情页，并提供网页回退；内评保留在适当的自然完成节点。

## 需要压测后定优先级的性能优化

1. **完整设备快照刷新过于频繁。** [NetworkScanner.kt:47](/C:/Android/driverSkr/CameraDetection/app/src/main/java/com/spyfinder/hiddencamera/detectorapp/scan/NetworkScanner.kt:47) 在锁内重建、合并证据并排序全部设备；约每 200ms 执行一次。[ScanViewModel.kt:125](/C:/Android/driverSkr/CameraDetection/app/src/main/java/com/spyfinder/hiddencamera/detectorapp/scan/ScanViewModel.kt:125) 再在主线程重建信任映射、复制和划分列表。建议将简单进度与设备变更分离，有设备变化再生成快照，测 200/1000/4096 台设备下的主线程耗时及分配量。本轮未获得 CPU/帧率实测，不能称已确认卡顿或 ANR。

2. **历史检查点重复编码与解码整份档案。** [ScanHistoryStore.kt:29](/C:/Android/driverSkr/CameraDetection/app/src/main/java/com/spyfinder/hiddencamera/detectorapp/utils/ScanHistoryStore.kt:29) 每次保存先完整 encode，再 decode 校验，最后将整个 JSON 写入 SharedPreferences；[同文件:58](/C:/Android/driverSkr/CameraDetection/app/src/main/java/com/spyfinder/hiddencamera/detectorapp/utils/ScanHistoryStore.kt:58) 会分别编码 recent/complete，即使它们指向同一条完整记录。IO 已在后台，仍有大档案的分配和写入成本。建议按记录 ID 去重、仅持久化变更、控制存储规模，压测后决定是否改为原子文件或数据库。保留历史恢复、写入失败提示和校验保障。本轮未测得 OOM 或写入丢失。

## 已复核、无需重复列为缺陷的内容

- 45 秒总截断与 33 秒发现预算已移除；正在运行的扫描以实际队列完成为终点。
- /21 网络已能规划 2046 个地址；此前两轮真机分别约 134.159/134.335 秒，均达到 100% 并保存 COMPLETE。本轮只读页面仍显示后一轮结果。
- 100% 表示计划工作执行完成；个别设备分析不完整及网络错误仍单独提示。该语义是当前设计，不再把“不确定证据”混同于“扫描没结束”。
- 磁场数字与指针共用读数；原刻度映射、0–100 μT 量程、超量程提示均保留。
- 双历史记录、原始证据与用户信任标记分离、连接错误分类、状态行有界读取等已有修复仍在。
- 中文系统使用中文、其他语种使用英文的策略及资源测试仍通过。
- 订阅临时放行和恢复 TODO 是用户指定的测试配置，不列为本轮缺陷；真实购买、退款及恢复仍须在正式 Play 测试条件下验收。

## 本轮验证与边界

- 执行 `:app:testDevVersionDebugUnitTest :app:lintDevVersionDebug`，构建任务成功。69 项单测通过，0 失败、0 错误、0 跳过；Lint 0 错误、114 条警告。
- 构造报文直接调用当前编译出的 `DiscoveryProtocols`，验证公告目标和端口被丢弃。材料：[DiscoveryAudit.java](/C:/Android/driverSkr/CameraDetection/app/build/DiscoveryAudit.java)、[验证输出](/C:/Android/driverSkr/CameraDetection/app/build/reaudit-mdns.txt)。
- 只读获取当前手机的前台页面、扫描结果及亮屏设置；未安装新包、未卸载或清空数据，未改系统设置，未向他人发送任何信息。
- 未执行相机故障注入、传感器不可靠/断流注入、多种尺寸和字体真机测试，以及 4096 设备性能压测。这些明确列为后续验收项。

建议先处理 R1，再处理 R2/R6 的扫描正确性与流程问题，然后完成 R3/R4/R5/R7；性能优化按压测数据安排。所有修改应继续在原 UI 上修补，并保持中英文与页面风格一致。
