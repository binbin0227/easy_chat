# 0.1.0 Paraformer＋OPUS 验证记录

构建标识：`20261007-paraformer-opus`。公开版本保持 `0.1.0`。

## 已验证

- Gradle 编译主模组和独立 Paraformer Java worker，字节码目标 Java 8。
- 完整单元测试：设置迁移、模型方向选择、缓存管理、路径边界、旧模型登记、Hypixel 消息解析、聊天行更新、按键偏好和音频相关检查。
- 从完整 JAR，在实际 Java 8 下执行 `ParaformerOpusIntegration`，不依赖 Python、Ollama 或网络。
- Paraformer 识别后交给 OPUS 中译英，英文正常返回。
- OPUS 英译中独立工作，返回中文。
- 终止并重新启动 ASR 后台进程后，协议与 DLL 加载仍正常。
- 有效权重不会在再次准备时重写；删除包含权重和词表，删除标记阻止自动恢复，显式恢复重新释放并校验。
- 已核对工作目录中千问权重仍存在，SHA-256 为 `061b54daade076b5d3362dac252678d17da8c68f07560be70818cace6590cb1a`。

## 实际链路示例

以下是合成录音经 Java worker＋OPUS 的输出，不代表真人麦克风效果：

| Paraformer 中文识别 | OPUS 英文输出 |
|---|---|
| 谁来防守 | Who's going to defend us? |
| 帮我拿两颗绿宝石 | Get me two emeralds. |
| 你守床我去中间 | You stay in bed and I'll go in the middle. |
| 我有末影珍珠可以过去 | I've got the last pearls to pass. |

后两条暴露 OPUS 对游戏语境的翻译限制；本轮只替换识别和翻译链路，没有声称已修复术语。

原始执行日志：`work/paraformer-opus-integration.log`。更早的同音频模型初筛报告：`outputs/asr-comparison-20261007/识别效果对比.md`。

## 待人工验收

- Lunar 中实际启动与麦克风、鼠标侧键、设置页布局。
- Hypixel 真实消息中的中英双行显示、消息顺序、通知过滤。
- 游戏运行时的资源和帧率影响。
- 真人中文口语和中英混说的识别、最终英文可理解性。

本次未自动启动或登录游戏，没有对外发布。


## 2026-10-07 聊天接收兼容修复（20261007-chat-history）

- 改为在客户端 tick 读取已经显示的完整聊天历史，避免依赖可能被其他客户端模组取消的 Forge 聊天接收事件。仍只识别玩家消息，并排除自己的消息、服务器通知和含中文的消息。
- 按原始 ChatLine 实例定位消息，兼容普通消息共用零 ID；译文完成后原位追加下一行，保留消息年龄与聊天历史。已清除或滚出历史的消息不会重新出现。
- 补充换行兼容和有大小上限的 text-translation.log：记录识别、入队、显示或失败阶段；拒绝格式仅记录前缀码点，不记录正文。
- 35 项单元测试通过。完整发行 JAR 实测截图短句及 defend the bed、I need blocks、hello 都返回非空中文；这验证识别与本地模型运行，不代表术语准确性。
- 尚未在实际 Lunar 服务器中复验显示效果。旧日志只有原文、没有翻译错误，无法据此确认旧版遗漏发生在哪个环节。
- Paraformer → OPUS 语音链路和已有模型文件保持不变。


## 2026-10-08 代理连接下聊天不翻译（20261008-proxy-chat）

实际诊断日志显示，截图消息被读取时 `hypixel=false`；旧版在匹配玩家格式之前直接返回。用户确认通过第三方代理节点连接，连接域名不是 hypixel.net。用旧发行包重放 `[20✫] [黄队] __5H4D0W__: drop iron i buy def` 且域名判断为 false，复现结果为 DROPPED。

修复：彻底删除服务器域名识别方法、解析器中的服务器判断参数和调用端限制。仅根据完整玩家消息格式及发送者信息识别，代理地址不参与判断。保留通知、自己的消息、中文正文等过滤规则。回归用例使用诊断日志中的真实前缀码点，并同时检查截图附近的服务器通知仍被排除。

本次验证包括单元测试、完整发行包校验、截图正文的实际 OPUS 推理；实际 Lunar 进服显示仍需用户复验。


## 2026-10-08 初次许可审计（历史记录；下节记录解决情况）

- 已初始化 main 分支 Git；旧引擎、旧截图、实验目录与临时日志保留在 work/archive，不提交模型、缓存、游戏配置和完整 JAR。
- prepare-dependencies.ps1 -CheckOnly 验证全部 10 个依赖文件摘要；脚本不安装 Java。
- 干净构建及 35 项测试通过（已有 JDK 21 编译、JDK 8 运行测试）。仅 JDK 21 的测试执行遇到 Windows 中文目录 Gradle worker 启动问题，本轮保留已验证的 JDK 8 测试配置；没有新增安装 Java。
- 最终完整包权重、许可证、引擎目录和重复条目检查通过；默认公开发布检查因 GPLv3 native 依赖正确返回非零。这是未解决的许可阻碍，不是已通过公开发布审核。
- 详细日志位于本地 work/reports/release-audit-*.log；源码公开范围检查位于 work/reports/repository-publish-check.json。本次未发布、未创建远端仓库，也未改变当前 Lunar 已安装包。


## 2026-10-08 无 TTS 运行库替换（20261008-asr-only）

- 换用固定哈希的官方 no-tts C API DLL，通过 JNA 调用；不再分发旧 Java JNI/eSpeak/Piper。
- 35 项单元测试通过。最终运行库实际报告 sherpa 1.13.8、ONNX Runtime 1.28.2。
- 完整包实际 Paraformer → OPUS 测试 5 条语音：谁来防守、守床、绿宝石、蓝队、珍珠；3 条聊天英译中均返回中文。
- Java 8 独立进程重启、旧 JNI 缓存清理、原生库和模型有效缓存复用、模型持久删除与显式恢复通过。
- 保留完整版权、BSD/zlib/MIT/Apache 及 ORT 的第三方通知，提供 Eigen MPL 源码获取说明。
- 发布检查核对最终 DLL/归档/模型摘要、嵌套 worker 许可和重复条目，不再豁免旧 GPL 阻碍。
- 测试是合成录音和本地链路验证；守床、末影珍珠和 blocks 的 OPUS 术语误译仍存在，未在本次修改。
- 未启动游戏或发布；真实麦克风及 Lunar 服务器画面仍需手动验收。日志在 work/reports/asr-only-*.log。
