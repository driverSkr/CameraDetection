# 项目问题优化记录

日期：2026-09-15。对应 `PROJECT_REAUDIT_2026-09-15.md`。

## 已完成修改

1. **组播失败可见**：增加发现通道健康状态，区分准备、发送、接收阶段及正常完成、失败、取消。网络错误与权限错误给出不同提示；取消不会误报网络故障。失败提示进入实时界面、最终结果和历史记录，提供中英文。正常无响应仍不等同于失败，100% 继续表示计划工作结束。
2. **防止重复进入**：相机入口在权益查询、订阅跳转、相机打开期间锁定，使用 Activity Result 在返回后恢复入口；磁场开始操作也增加查询与订阅等待保护。
3. **相机重试反馈**：重试立即清除旧错误和控件状态，显示启动提示；启动期间不保留可重复点击的错误卡片。权限从设置恢复时同步清理旧状态。
4. **小屏与大字体**：扫描位置标题和网格分配独立的布局空间，网格可滚动，卡片高度适应文本。磁场页整体支持滚动，仪表、数值、量程和操作区顺序排布；提示取消两行截断，保留原仪表资源与映射。
5. **取消事件去重**：由 ScanViewModel 统一上报一次取消，同时记录进度、原因和来源；区分按钮、后台、替换扫描和 ViewModel 清理。
6. **服务分析并发**：所有设备共享最多 8 个分析连接名额，同一设备的端口可并发分析，尤其减少末尾少量设备的串行等待。发现阶段仍限制为 32 个工作者；不缩短超时、不跳过计划地址、不重新引入总时长截断。新增剩余分析设备数及无设备身份数据的本地耗时诊断。
7. **正式包收缩**：开启 Release 代码与资源收缩，补齐原来引用但缺失的正式渠道日志规则，并保留 ViewBinding 反射依赖的泛型签名、类层级和 inflate 方法，以及 Gson 远程配置数据类。保留 Crashlytics 映射产物；本地验证构建跳过上传映射任务。

## 真机验证中补充发现并修复

**VPN 虚拟网络被选作 Wi-Fi 扫描目标。** 系统将当前 VPN 报告为 `WIFI|VPN`，旧代码只检查 WIFI 且优先默认网络，因此选中 `172.19.0.1/30`，两秒内完成 2 个地址的扫描；真实无线接口为 `192.168.2.51/21`。这次短扫描不作为真实局域网覆盖验证。

新增统一的 Wi-Fi 网络策略，明确排除 VPN。扫描选择、网络变化检查及 Wi-Fi 信息辅助方法均使用该策略，仍通过所选 Wi-Fi 的 Network 创建和绑定 socket。没有修改或关闭手机 VPN。

## 验证说明

- 组播故障测试覆盖不同失败阶段、正常静默结束、取消传播、权限拒绝及历史文案本地化。
- 并发测试验证多个设备共享全局预算、尾部单设备可用多个连接、结果顺序和取消后的名额释放。
- 真机流程测试覆盖 Activity 重建、相机预览/滤镜/镜头、订阅页状态、快速 5 连点只打开一次及返回后再次进入。
- 布局用例在应用内模拟 360×480dp、英文和双倍字号，验证网格不覆盖标题、末行可到达、磁场提示无文本溢出、开始按钮可滚动到达；未修改手机系统字号或分辨率。
- 收缩包以临时调试签名副本覆盖安装验证，未卸载或清空应用数据。冷启动进入主页面成功，磁场取得 19.5 μT 样本，相机出现预览就绪控件，旧的 181 台设备历史可以读取。随后恢复安装调试包。
- 五个各模拟等待 40ms 的端口测试：串行 233ms，共享预算并发 46ms。这是桌面 JVM 模拟调度结果，不代表真实 Wi-Fi 提速比例。
- 最终收缩对比：正式 APK 从 39,552,109 字节降至 24,186,394 字节，约减少 38.8%；同为正式 Release 构建，未拿 Debug 包作为基线。

订阅临时放行常量保持不变。真实购买、退款和所有厂商相机故障场景不在本次通过范围内。

保留规则参考 [Android 官方 R8 完整模式说明](https://developer.android.com/topic/performance/app-optimization/full-mode) 及 [Keep rule 示例](https://developer.android.com/topic/performance/app-optimization/keep-rule-examples)。

## 最终自动化验证

| 检查 | 结果 |
|---|---|
| `:app:testDevVersionDebugUnitTest` | 99 项通过，0 失败、0 错误 |
| `:app:connectedDevVersionDebugAndroidTest` | Android 16 / PLY110：7 项通过，0 失败、0 错误、0 跳过 |
| `:app:lintDevVersionDebug` | 0 错误、114 条警告、1 条信息 |
| `git diff --check` | 通过 |

新增 VPN 用例覆盖同时携带 Wi-Fi 与 VPN 标记的网络不能作为本地扫描目标。

验证材料保存在 `app/build`：`optimization-verified-final.log`、`optimized-real-scan.log`、`release-delivery.log`、JUnit XML、Lint 报告、`release-smoke-*.xml`，以及磁场布局截图 `release-smoke-sensor.png`。

最终调试 APK 的 SHA-256：`53E0481908804B208B89317DBF14FCC942EFA315944AA0C5EC9C3007A68358CF`。

## 真实局域网与最终交付

保持手机 VPN 开启，修复后正确扫描 `192.168.2.51/21`，最终结果：

- 地址检查 **2046/2046**，设备 **189**，剩余分析任务 **0**。
- 耗时 **133,292ms**，最终状态 COMPLETE、进度 100%。
- **5 台设备未完成全部分析、2 个地址因网络错误无法确认**，结果与历史摘要均保留这些提示。
- mDNS、SSDP、ONVIF 三个通道均正常结束。
- 发现探测统计：9286 次超时、155 次拒绝、25 次开放、6 次不可达；这些是端口尝试次数，不是不同设备数量。
- 本次与先前约 134 秒的整网扫描处于同一量级。主要时间仍由无响应地址探测决定，没有据此声称整网提速。端口并发优化主要改善分析末尾等待。

最终收缩包再次以调试签名副本覆盖安装并冷启动，成功读取本次 **189 台设备、2046/2046 地址**的历史，验证进程重启和收缩包间的数据恢复。之后恢复安装最终 Debug 包，保留全部现有应用数据。没有修改手机 VPN、字号或屏幕设置。

最终正式构建使用：`:app:assembleProdVersionRelease -x :app:uploadCrashlyticsMappingFileProdVersionRelease`，结果成功；映射文件保留在本地，没有修改正式构建的默认映射上传配置。

- [调试安装包](/C:/Android/driverSkr/CameraDetection/app/build/outputs/apk/devVersion/debug/app-devVersion-debug.apk)
- [正式 Release 安装包](/C:/Android/driverSkr/CameraDetection/app/build/outputs/apk/prodVersion/release/app-prodVersion-release.apk)
- [正式混淆映射](/C:/Android/driverSkr/CameraDetection/app/build/outputs/mapping/prodVersionRelease/mapping.txt)

最终正式 APK 的 SHA-256：`707EFA34907666E76A04D94189D46F4DE86AF64B9C542C2A9515CA15B4526ED3`。
