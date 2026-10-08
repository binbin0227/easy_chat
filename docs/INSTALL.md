# Easy Chat 0.1.0 安装与使用

支持 Windows 64 位、Minecraft Forge 1.8.9。当前为本地测试版，尚未发布。

## 单 JAR 安装

将 `easy-chat-forge-1.8.9-0.1.0.jar` 放进当前启动器实际使用的 Forge mods 目录，不要解压。Lunar 的 Forge 1.8.9 配置通常位于 `%USERPROFILE%/.lunarclient/profiles/1.8/mods/forge-1.8.9/`，以启动器实际目录为准。

**当前完整包尚未通过二进制许可审计，请先阅读 LICENSE_REVIEW.md，不作为公开发布包上传。**

JAR 已包含 Paraformer int8、OPUS 中译英、OPUS 英译中及 Windows 本地引擎，无需额外软件或 API 配置。全部在 CPU 上运行，不使用显卡。Whisper、千问和显卡加速选项已移除。

## 使用

1. 按 `P` 或输入 `/easychat` 打开设置。
2. 在语音发送页选择麦克风，用音量测试确认有输入。
3. 修改说话按键时可按键盘键或点击鼠标侧键，`Esc` 取消绑定。
4. 语音测试会显示 Paraformer 识别的中文和 OPUS 生成的英文，不发给服务器。
5. 在游戏中按住 `V` 说中文，松开后自动发送。也可选择填入聊天框，手动确认发送。

语音链路固定为 **Paraformer 识别中文 → OPUS 中译英**。接收翻译使用 **OPUS 英译中**。中文输出和英文输出分别展示，便于分辨是识别错误还是翻译错误。

接收翻译识别玩家聊天格式，不验证服务器身份或连接 IP，支持代理连接，跳过自己的消息、服务器通知和无法确认是玩家聊天的内容。英文立即显示，完成后在原消息下补中文。翻译只在自己的客户端显示，不会发送到服务器。

## 四个设置页

- 语音发送：麦克风、音量测试、说话键、发送方式、中文识别模型、中译英模型和语音测试。
- 接收翻译：开关与英译中模型。
- 模型管理：识别模型管理、两种 OPUS 模型的详情、清除下载缓存和旧模型管理。
- 通用设置：提示位置、打开设置按键和恢复默认设置。

提示可选左上角、聊天框或隐藏；左上角提示显示约 5 秒后隐藏。

## 模型和缓存

模型释放到**实际 Minecraft 游戏目录**的 `easychat/` 下，本机使用 `%APPDATA%/.minecraft/easychat/`：

```text
easychat/
  settings.properties
  model-files.json
  models/paraformer-zh-int8/model.int8.onnx
  models/paraformer-zh-int8/tokens.txt
  text-model/opus-zh-en/
  text-model/opus-en-zh/
  runtime/paraformer-1.13.8/
```

有效文件经过 SHA-256 校验后复用，目录不带模组版本号，更新模组无需重新下载。

Paraformer 下载页保留并列的官方源／备用源，并支持复制链接、浏览器打开。当前完整包已内置，点击准备会从 JAR 恢复，不需要联网。浏览器下载后可导入支持的 `model.int8.onnx`；文件复制到上述目录，原文件保留，词表由 JAR 提供。导入前校验型号和摘要，不接受任意 ONNX。

两个方向的 OPUS 均可单独删除本地副本或恢复。删除后有持久标记，重启不会自动恢复。清除下载缓存只清理登记的临时文件；旧模型／未识别文件页用于手动管理已移除型号和外部文件，不自动删除未知文件。

旧的 Whisper、千问模型不会因更新自动删除，但已从可用模型列表中移除，文件仍能在旧模型管理页找到。开发工作目录中的千问权重另外保留，不参与当前 JAR。

## 日志与限制

识别引擎错误见 `easychat/paraformer-engine.log`；聊天翻译错误见 `easychat/text-translation.log`。

本轮已完成独立 Java 8 链路验证，实际 Lunar 麦克风、按键、界面和 Hypixel 消息仍需玩家验收。OPUS 的游戏术语翻译可能不准确。本轮没有加入游戏术语纠错。
