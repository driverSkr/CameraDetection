# 项目复查报告 · 2026-09-14

审查版本：`0730845`。覆盖 Wi-Fi 扫描、证据判断、扫描状态与历史、磁场检测、相机控制、订阅查询、分享和多语言入口。本轮未修改应用源代码、UI 或订阅开关。

结论：建议优先修复 3 个 P1 问题；另有 8 个 P2 缺陷或流程优化项。P1 表示影响核心检测结果可信度，应优先处理；P2 表示特定场景下的漏检、流程中断或体验问题。以下区分已复现、代码确认和待设备验证的风险，不将静态分析等同于全流程实测。

## P1：优先修复

### P1-01 磁场显示值与 μT 单位不匹配

- 位置：[SensorPage.kt:113](/C:/Android/driverSkr/CameraDetection/app/src/main/java/com/spyfinder/hiddencamera/detectorapp/ui/main/page/SensorPage.kt:113)、[显示位置:259](/C:/Android/driverSkr/CameraDetection/app/src/main/java/com/spyfinder/hiddencamera/detectorapp/ui/main/page/SensorPage.kt:259)。
- 现象：传感器向量先计算真实磁场强度，然后归一化到 0–100；页面把归一化结果标为 μT。按当前公式，50 μT 显示 3 μT，100 μT 显示 8 μT，1000 μT 显示 100 μT。
- 影响：核心仪表读数错误，用户无法据此比较实际磁场变化。
- 修复方向：分别保存 `magnitudeMicroTesla` 与仪表比例；数值显示真实 μT，指针使用归一化比例。保留原仪表、布局和动画。增加有效值、极值以及读数与指针分离的测试。
- 验证：代码数据流确认，公式复算已运行；本轮未注入真机传感器事件。

### P1-02 把所有 ConnectException 当作设备在线和端口拒绝

- 位置：[NetworkScanner.kt:95](/C:/Android/driverSkr/CameraDetection/app/src/main/java/com/spyfinder/hiddencamera/detectorapp/scan/NetworkScanner.kt:95)、[分析阶段:137](/C:/Android/driverSkr/CameraDetection/app/src/main/java/com/spyfinder/hiddencamera/detectorapp/scan/NetworkScanner.kt:137)。
- 现象：发现阶段捕获 `ConnectException` 直接返回 true，分析阶段统一作为已完成的阴性探测。Android 的带超时连接路径会把多种非超时错误包装成该异常，不能只凭异常类型认定目标拒绝了端口。
- 影响：网络不可达等情况可能生成虚假的在线设备，同时掩盖未完成的端口分析。
- 修复方向：解析异常原因链中的 errno，只把成功连接或明确的 `ECONNREFUSED` 作为当前探测的响应依据；其他错误区分超时、网络不可达、取消和未知。发现阶段与分析阶段共用一套分类规则，补充故障注入测试。
- 验证：代码与 [AOSP IoBridge.isConnected 实现](https://android.googlesource.com/platform/libcore/+/master/luni/src/main/java/libcore/io/IoBridge.java#323) 交叉确认；本轮未在手机上制造网络故障。

### P1-03 RTSP 响应只读取一次，合法分段响应会漏判

- 位置：[NetworkScanner.kt:130](/C:/Android/driverSkr/CameraDetection/app/src/main/java/com/spyfinder/hiddencamera/detectorapp/scan/NetworkScanner.kt:130)。
- 现象：只执行一次 `read(bytes)`，随即判断是否 RTSP。首段若仅为 `RTSP/1.`，后续才到达 `0 401 Unauthorized...`，当前逻辑会结束此次识别。
- 影响：确实返回视频协议的服务丢失摄像头线索；若同时有 HTTP 等普通证据，还可能最终显示未发现摄像头特征。
- 修复方向：在总期限和字节上限内循环读取完整状态行，再判断协议；区分完整阴性响应、截断响应与超时。增加分段 200/401、提前 EOF、超时与取消测试。
- 验证：本地回环 TCP 服务分两段发送响应，调用当前编译产物中的 `DiscoveryProtocols.isRtsp`：单次读取返回 false，收齐响应后返回 true。符合 [InputStream.read 的返回长度约定](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/io/InputStream.html#read(byte%5B%5D))。这是读取模式与解析器的复现，并非完整 Android 扫描器端到端测试。

## P2：扫描覆盖、流程与体验

### P2-01 发现端口与分析端口不一致，8554 服务可能进不了分析

位置：[NetworkScanner.kt:61](/C:/Android/driverSkr/CameraDetection/app/src/main/java/com/spyfinder/hiddencamera/detectorapp/scan/NetworkScanner.kt:61)。发现阶段只试 80、443、554，分析阶段才试 8554、5000。若设备无组播回应，前三个端口静默丢包而 8554 可达，该设备不会进入分析。建议统一探测能力配置，将关键视频端口纳入发现，并结合时间预算安排顺序。验证方式：配置仅开放 8554、其他探测端口丢包的模拟设备。本轮为代码路径确认。

### P2-02 大网段的发现阶段可能耗尽 45 秒预算

位置：[NetworkScanner.kt:59](/C:/Android/driverSkr/CameraDetection/app/src/main/java/com/spyfinder/hiddencamera/detectorapp/scan/NetworkScanner.kt:59)、[ScanViewModel.kt:75](/C:/Android/driverSkr/CameraDetection/app/src/main/java/com/spyfinder/hiddencamera/detectorapp/scan/ScanViewModel.kt:75)。当前必须完成整段发现，才开始服务分析。1024 个目标、32 并发、每个目标最多 3 次 450ms 连接超时，发现阶段耗时估算接近 43.2 秒，加上组播约 2 秒，已接近总上限。即使较早发现了目标，也可能来不及确认服务。建议采用发现与分析流水线，优先分析有视频线索的目标，并给分析阶段保留预算。此为超时路径估算，实际耗时取决于网络响应，尚未进行真机性能测量。

### P2-03 部分扫描没有持久记录，大网段可能始终无法形成新历史

位置：[ScanViewModel.kt:85](/C:/Android/driverSkr/CameraDetection/app/src/main/java/com/spyfinder/hiddencamera/detectorapp/scan/ScanViewModel.kt:85)、[ScanRules.kt:32](/C:/Android/driverSkr/CameraDetection/app/src/main/java/com/spyfinder/hiddencamera/detectorapp/scan/ScanRules.kt:32)。只有完整扫描才保存历史。/21 网段的 2046 个地址最多选 1024 个，结果必定为部分完成；普通网段只要一个设备分析不完整也会走此路径。退出并重建进程后，本次发现和标记均无法恢复。

保留“部分结果不能冒充完整结果、不能覆盖最后完整记录”的既有原则，建议另存最近一次扫描，带时间、网段、覆盖率和完成状态，同时保留最后完整记录。本地已验证 /21 目标选择为 `1024/2046, limited=true`；持久化影响由调用路径确认。

### P2-04 新扫描开始后，旧历史入口在当前会话内消失

位置：[DetectCheckView.kt:183](/C:/Android/driverSkr/CameraDetection/app/src/main/java/com/spyfinder/hiddencamera/detectorapp/ui/main/view/DetectCheckView.kt:183)、[历史按钮:254](/C:/Android/driverSkr/CameraDetection/app/src/main/java/com/spyfinder/hiddencamera/detectorapp/ui/main/view/DetectCheckView.kt:254)。历史按钮仅在 `isStartDetect=false` 的分支展示，该状态开始扫描后只被设为 true，未在结束或取消后复位。已有旧历史时再扫描并取消，界面只剩重查和本次结果，旧历史仍存在但没有入口。

建议保留原按钮样式，在非扫描状态提供稳定的历史入口，或在原结果返回流程恢复对应入口；避免直接复位导致本次结果丢失。补充“有历史→新扫描→失败/取消→查看旧历史”的 UI 测试。代码路径已确认。

### P2-05 快速拖动缩放条可能产生持续的假错误提示

位置：[CameraScannerPage.kt:199](/C:/Android/driverSkr/CameraDetection/app/src/main/java/com/spyfinder/hiddencamera/detectorapp/ui/camera/page/CameraScannerPage.kt:199)、[错误面板:120](/C:/Android/driverSkr/CameraDetection/app/src/main/java/com/spyfinder/hiddencamera/detectorapp/ui/camera/page/CameraScannerPage.kt:120)。每次拖动均提交缩放请求，任意失败都写入全局 error；后续缩放成功不清除此错误。新请求替代旧请求时，CameraX 可以正常取消旧请求，因此快速拖动可能弹出故障面板并遮挡画面。

建议忽略被新请求替代的取消、校验相机实例及请求序号，仅对当前有效请求的真实失败提示，并在成功后清除对应控制错误。保持现有 Slider 和页面。依据：[CameraControl.setZoomRatio 官方约定](https://developer.android.com/reference/androidx/camera/core/CameraControl#setZoomRatio(float))；具体设备上的触发频率待真机验证。

### P2-06 磁场监听缺少前后台生命周期控制

位置：[SensorPage.kt:131](/C:/Android/driverSkr/CameraDetection/app/src/main/java/com/spyfinder/hiddencamera/detectorapp/ui/main/page/SensorPage.kt:131)。监听注册只依赖 `isListening` 和组合销毁；页面停留在组合中但 Activity 退后台时没有显式注销，恢复前台后仍显示检测中。还忽略 `registerListener` 的 Boolean 返回值，注册失败也会显示正在检测。

建议只在页面前台且用户选择检测时注册，离开前台立即注销；明确返回后的恢复策略；注册失败反馈到原提示区域，并移除每次读数的 error 日志。代码层面的生命周期缺口已确认，系统是否暂停实际采样以及耗电影响依机型和 Android 版本而定，本轮不作耗电量结论。

### P2-07 订阅状态刷新重复排队，弱网时拖慢恢复和权限检查

位置：[BaseActivityVBind.kt:35](/C:/Android/driverSkr/CameraDetection/app/src/main/java/com/spyfinder/hiddencamera/detectorapp/base/BaseActivityVBind.kt:35)、[MainActivity.kt:51](/C:/Android/driverSkr/CameraDetection/app/src/main/java/com/spyfinder/hiddencamera/detectorapp/ui/main/MainActivity.kt:51)、[SubscribeHelper.kt:36](/C:/Android/driverSkr/CameraDetection/app/src/main/java/com/spyfinder/hiddencamera/detectorapp/utils/SubscribeHelper.kt:36)。主页面一次 onResume 会从基类和子类各发起一次 force 刷新；订阅页也存在首次加载与生命周期刷新入口。Mutex 只把查询串行化，不合并重复查询，force 又跳过缓存。弱网时后来的恢复操作或权限查询会等待前面查询结束。

建议统一前台刷新入口，并让同一轮并发调用共享在途查询；真正的购买/恢复操作按明确的新鲜度策略触发刷新。当前临时放行会绕过功能权限等待，但真实恢复购买仍使用该链路。代码确认，未进行真实 Google Play 订单测试。

### P2-08 分享内容缺少应用下载链接

位置：[SettingPage.kt:98](/C:/Android/driverSkr/CameraDetection/app/src/main/java/com/spyfinder/hiddencamera/detectorapp/ui/setting/page/SettingPage.kt:98)、[ShareUtils.kt:18](/C:/Android/driverSkr/CameraDetection/app/src/main/java/com/spyfinder/hiddencamera/detectorapp/utils/ShareUtils.kt:18)。调用传入“分享应用/Share App”与独立 URL；纯文本直接发送前者，HTML 逻辑只高亮原文已有 URL，没有追加链接。因此两种分享正文都没有下载地址。

建议先生成“本地化文案 + 换行 + Play 链接”的完整正文，再构建分享 Intent。验证中英文 `EXTRA_TEXT` 均包含包名对应链接。代码路径确认；本轮未向他人发送分享消息。

## 可后续安排的优化与验证

- 扫描更新：每分析一个设备都会构建、排序全量快照，并清空重填 Compose 列表。设备较多时增加分配和重组压力；建议按短时间窗口合并更新、保留稳定列表键并增量更新。当前为代码复杂度判断，不能据此声称已出现卡顿或 ANR。
- 多语言：目前英文兜底及中文分支有测试覆盖；扫描文案仍通过英文字符串匹配翻译。后续改成结构化状态码和参数，可减少修改英文提示后中文翻译失配的维护风险。系统权限/商店界面的语言由外部系统控制。
- 小屏、大字体、TalkBack：部分图标按钮缺少描述、页面存在固定尺寸。建议针对原 UI 做适配验证，再局部修补；本轮没有视觉证据，不将所有固定尺寸直接列为已确认溢出。
- 测试覆盖：补充分段/慢速协议响应、受限端口、网络错误分类、部分历史、取消后历史入口、相机请求竞态和传感器注册失败；Google Play 待支付、断网恢复、购买回调丢失仍需要专门的订单测试环境。

订阅临时放行保持 `true`，TODO 仍在，属于用户指定的测试配置，不列为误改缺陷。恢复正式发布行为时再关闭该开关。

## 本轮验证与边界

- 执行 `:app:testDevVersionDebugUnitTest` 与 `:app:lintDevVersionDebug`，构建任务成功。
- 现有 35 个单元测试全部通过，0 失败、0 错误。Lint 为 0 错误、112 警告、1 信息；警告数量不等于实际业务缺陷数。
- 本地辅助程序调用当前编译的协议解析器与网段规则，复现 RTSP 分段问题并核算 /21 覆盖、磁场显示公式。
- [验证日志](/C:/Android/driverSkr/CameraDetection/app/build/audit-verification.log)、[辅助程序](/C:/Android/driverSkr/CameraDetection/app/build/audit/ScanAudit.java)、[复现输出](/C:/Android/driverSkr/CameraDetection/app/build/audit/reproduction.log)。build 目录内容可能被 clean 删除。
- 未运行 connected instrumentation tests，未安装/卸载应用、清理数据、更改手机设置或执行真实付款。扫描硬件、真实路由环境和商店端到端结果仍需后续专门验证。

所有修复方向均以保留原 UI、颜色、圆角、图标与布局风格为约束，通过逻辑修正及必要的局部反馈完成。
