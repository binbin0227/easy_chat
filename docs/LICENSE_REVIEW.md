# 开源许可审计（2026-10-08）

旧包的 TTS/GPL 依赖问题已通过替换运行库处理。现在采用官方无 TTS 的
sherpa-onnx 1.13.8 Windows x64 C API，通过 JNA 调用，不再分发旧 Java JNI。
这是固定哈希的官方预编译库，并非本地重编译。

自有源码仍为 MIT。SherpaAsr.java 是由官方 C API 映射的 ABI 声明，保留
Apache-2.0、Xiaomi 版权和修改说明；第三方文件继续适用原许可。
JNA 在上游双许可中选择 Apache-2.0，另保留 libffi MIT 许可。
Paraformer 和 OPUS 权重保持不变，保留 Apache-2.0、模型卡和转换来源。
FunASR 代码的 MIT 文本不代替 Paraformer 模型许可。

ASR 的 ONNX Runtime 1.28.2 和 OPUS 的 1.17.3 各自保留 MIT 及对应版本
ThirdPartyNotices。同时保留 kaldi-native-fbank、kaldi-decoder、OpenFst、
simple-sentencepiece 的 Apache-2.0，kissfft 的 BSD-3-Clause，hclust-cpp 的
BSD-2-Clause，nlohmann/json 的 MIT。simple-sentencepiece 内含 darts-clone
BSD-2-Clause 和 ThreadPool zlib 的完整声明也保留。ORT 的第三方通知覆盖附带组件；
为 MPL-2.0 Eigen 提供固定修订的可获取源码链接。
Gradle Wrapper 的 Apache-2.0 LICENSE/NOTICE 留在仓库，不打包进游戏运行库。

版本、版权及源码获取说明随 JAR 保存在 licenses/asr-runtime-NOTICES.txt。
二进制哈希和取证见 [native-license-audit.json](native-license-audit.json)。
发布检查验证实际 DLL、排除旧 JNI，并检查许可材料；不能只靠搜索不到字符串
判断许可合规，还结合官方 TTS=OFF 构建流程和固定发行资产。

旧 GPL 文本、JNI 包和旧审计仅留在不提交、不发布的 work/archive 中。
Whisper、千问和历史权重不参与发行；用户已有模型不会删除。

核对来源：

- [官方发行资产](https://github.com/k2-fsa/sherpa-onnx/releases/tag/v1.13.8)
- [官方构建及 no-tts 命名](https://github.com/k2-fsa/sherpa-onnx/blob/v1.13.8/.github/workflows/windows-x64.yaml)
- [JNA 许可选择](https://github.com/java-native-access/jna/blob/5.17.0/LICENSE)
- [ORT 第三方通知](https://github.com/microsoft/onnxruntime/blob/v1.28.2/ThirdPartyNotices.txt)

这是一份具体组件和分发义务的审计记录，不提供无条件法律保证。
发布前执行检查和游戏内验收；新增模型、依赖或版本时重新审核。
