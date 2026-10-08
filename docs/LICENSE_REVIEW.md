# 开源许可审计（2026-10-08）

## 结论

自有 Java 源码可按现有 MIT 许可上传 GitHub。当前完整 JAR **未通过公开二进制发布审核**：内置的官方 sherpa JNI 包含 GPLv3 eSpeak-NG 代码，但当前分发材料没有完整覆盖对应源码等义务。禁止把它描述为纯 MIT/Apache 包。这里记录的是具体的许可取证和未完成事项，不是已取得法律许可的保证。

## 已核对的直接组件

| 组件 | 许可 | 说明 |
|---|---|---|
| Easy Chat 自有源码 | MIT | 根目录 LICENSE |
| sherpa-onnx 1.13.8 项目代码 | Apache-2.0 | 官方 LICENSE，JVM/Windows JAR SHA-256 已固定 |
| Paraformer 模型 | Apache-2.0 | 原 ModelScope 型号 v1.1.9 模型卡明确标注；模型卡和独立 Apache 文本随包保留 |
| FunASR 项目代码 | MIT | 此 MIT 文本不是 Paraformer 权重的许可；项目本身不打包 Python FunASR |
| OPUS-MT 两个方向 | Apache-2.0 | Helsinki-NLP 原模型卡声明；Xenova 转换源、修订和哈希保留 |
| ONNX Runtime 1.17.3 | MIT及附带第三方条款 | 保留 Microsoft 版权、LICENSE 和 ThirdPartyNotices |
| Gradle Wrapper | Apache-2.0 | 构建工具 LICENSE/NOTICE 在 licenses/；不作为游戏运行库打包 |

Whisper、llama/Qwen 运行库、历史许可证、旧 UI 测试及截图已移入本地 work/archive/pre-release-20261008，源码资源和新发行包不再附带它们。千问权重继续保留在 work/text-upgrade，不提交 Git。Minecraft/Forge/LWJGL/Gson 由游戏环境提供，不复制进发行包。

## GPLv3 发现与证据

本地原生包固定 SHA-256：`33fbdbd5410e9ba9bdda94aa164ec8f7825bb49246420d8ce9bdd88219d97039`。

该包只有 onnxruntime.dll 和 sherpa-onnx-jni.dll；后者含 eSpeak 初始化、文本转音素等符号，以及 eSpeak 自身错误信息。这不是仅从依赖名称猜测。摘要及符号取证见 [native-license-audit.json](native-license-audit.json)。官方 v1.13.8 CMake 的 SHERPA_ONNX_ENABLE_TTS 默认 ON，Windows JNI 构建未关闭它；BUILD_ESPEAK_NG_EXE=OFF 只关闭独立可执行文件，并未关闭链接库。该 CMake 固定 eSpeak-NG 修订为 ed530aa113046142eb5115cf2fc9157854d0ffe1，其 COPYING 明确是 GPLv3。

来源：
- [官方 Windows JNI 构建](https://github.com/k2-fsa/sherpa-onnx/blob/v1.13.8/.github/workflows/windows-x64-jni.yaml)
- [TTS 默认构建选项](https://github.com/k2-fsa/sherpa-onnx/blob/v1.13.8/CMakeLists.txt)
- [固定 eSpeak-NG 依赖](https://github.com/k2-fsa/sherpa-onnx/blob/v1.13.8/cmake/espeak-ng-for-piper.cmake)
- [该修订 GPLv3 文本](https://github.com/csukuangfj/espeak-ng/blob/ed530aa113046142eb5115cf2fc9157854d0ffe1/COPYING)

已补许可证文本和明确告知，但**加许可证文件不等于完成 GPL 对应源码义务**。独立 Java 进程隔离也不能免除所分发 native 代码自身的义务。ASR 包中静态依赖还包括 kaldi-native-fbank、kaldi-decoder、simple-sentencepiece 等；替换后的实际构建仍需核对完整版本、许可和 NOTICE。ASR worker 所带 ONNX DLL 的实际版本及专属第三方通知也需在最终运行库审计中锁定，不能只沿用主进程 1.17.3 的通知就宣称覆盖全部。

## 推荐处理

优先重建只含识别功能的 JNI，明确 SHERPA_ONNX_ENABLE_TTS=OFF、SHERPA_ONNX_ENABLE_SPEAKER_DIARIZATION=OFF、SHERPA_ONNX_ENABLE_PORTAUDIO=OFF、SHERPA_ONNX_ENABLE_WEBSOCKET=OFF、SHERPA_ONNX_ENABLE_BINARY=OFF，保留 JNI 和 Paraformer 所需代码；验明 eSpeak/piper 已移除，补齐剩余静态依赖通知并回归后再发布。不得未经验证就将同名 ASR-only DLL 当成官方二进制覆盖并继续使用旧哈希。

另一条路是按 GPL 分发包含该代码的运行组件，提供完整对应源码、许可证和重建材料，并审查与其他代码组合的许可条件。本轮没有擅自将整个项目改为 GPL，也没有声明这条路已完成。

完整发行包检查脚本默认对此失败；--allow-license-blockers 只允许内部结构检查，不允许将结果解释为发布许可。
