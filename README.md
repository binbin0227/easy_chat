# Easy Chat 0.1.0

Minecraft Forge 1.8.9 的本地语音与聊天翻译模组，目前为 Windows x64 测试版。

- 按住说话键：Paraformer 识别中文，OPUS-MT 中译英，松键后发送或填入聊天框。
- 接收玩家消息（支持 Hypixel 聊天格式及代理连接）：英文立即显示，OPUS-MT 英译中完成后在原消息下一行添加中文。仅本地显示，不发送译文，不显示“翻译中”。
- 过滤服务器通知、自己的消息和不符合玩家聊天格式的内容。
- 设置包含麦克风选择、音量测试、鼠标侧键绑定、语音翻译测试、发送方式、提示位置和模型管理。

默认模型和引擎都在完整 JAR 中，无需联网、API Key、Ollama、Python 或另外安装 Java。Paraformer 和 OPUS 使用 CPU。Whisper 和千问已从当前运行链路、模型目录和设置中移除。

## 安装和使用

完整说明见 [docs/INSTALL.md](docs/INSTALL.md)。把 `outputs/easy-chat-forge-1.8.9-0.1.0.jar` 放进使用中的 Forge mods 目录，不要解压。当前本机测试使用 Lunar 的 Forge 1.8.9 profile。

默认 `V` 说话、`P` 打开设置，也可用 `/easychat`。第一次进入时会检查并释放 Paraformer 模型。OPUS 按首次使用释放。设置和缓存位于实际游戏目录的 `easychat/`，更新 JAR 后有效缓存继续复用。

此版本仅保留一个 Paraformer 中文识别型号和 OPUS 的两个翻译方向。模型选择只列可用模型；模型管理负责恢复、导入和删除，不会因为选择未下载模型而导致链路失效。删除内置模型的本地副本后不会自动恢复，需手动点击恢复按钮。

## 从源码构建

需要已有 JDK 21 和 Windows x64；测试任务另使用已有 JDK 8，不会自动安装。只有 JDK 21 时用 `build.ps1 -Tasks remapJar` 可编译发行 JAR，测试须另外具备 JDK 8。生成字节码兼容 Java 8；构建脚本不安装 Java。首次构建需要联网下载 Gradle/Forge 开发依赖；模型及运行库按固定 SHA-256 校验。

```powershell
./scripts/prepare-dependencies.ps1
./scripts/build.ps1 -Tasks test,remapJar
./scripts/package.ps1
python ./scripts/check-release.py
```

`prepare-dependencies.ps1 -CheckOnly` 仅核对已有文件；`-Source Mirror` 可为 Hugging Face 文件选择备用源。无需将模型、运行库或构建缓存提交到 Git。

当前已换用官方无 TTS 的识别运行库，保留第三方许可、版权和对应源码获取说明。发布检查核对实际 DLL 哈希及模型、许可材料，详见 [许可审计](docs/LICENSE_REVIEW.md)。

Paraformer 在一个持久的 Java CPU 后台进程中运行，隔离其 ONNX Runtime DLL 与游戏内 OPUS 的版本。首次启动加载模型，后续复用。进程读取有超时，取消语音会终止该进程；游戏退出时也会清理。

回归测试和实际链路验证见 [docs/VERIFICATION.md](docs/VERIFICATION.md)。开源许可见 [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md) 和 [docs/LICENSE_REVIEW.md](docs/LICENSE_REVIEW.md)。

语音识别改进不代表翻译术语已经解决。OPUS 可能把守床、末影珍珠等词按一般口语理解；后续可在真实麦克风测试后添加游戏语境术语处理。本轮没有修改术语翻译规则。

本地开发归档说明见 [项目目录](docs/PROJECT_LAYOUT.md)。千问权重仍保留在开发工作目录 `work/text-upgrade/Qwen3-1.7B-Q8_0.gguf`，不参与当前构建。此前模型的源码、测试和说明在 `work/archive/experiments-before-release/retired-source/`，供未来恢复方案时参考。旧的游戏目录模型仍由文件登记表追踪，可在旧模型管理页手动删除。
