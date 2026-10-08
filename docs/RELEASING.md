# 发布 0.1.0

1. 执行 prepare-dependencies.ps1，固定依赖和模型并核对 SHA-256。
2. 执行 build.ps1 -Tasks test,remapJar、package.ps1、check-release.py，检查必须零退出码。
3. 用最终 JAR 执行 ParaformerOpusIntegration，检查真实识别、双向翻译、重启、缓存和模型删除恢复。
4. 在普通 Forge 与 Lunar 下验收麦克风、侧键、语音发送、代理连接的双行聊天和模型管理。
5. GitHub 提交源码、许可及文档，不提交 work、run、模型或完整 JAR；Release 放完整 JAR、SHA256SUMS.txt、安装及许可说明。不要发布历史 TTS 包。
6. 发布说明标明 Windows x64、Forge 1.8.9、CPU 推理、包大小、首次模型释放、已知术语误译和实际测试范围。

当前运行库采用官方无 TTS C API 版，完整版权与源码获取说明随 JAR 保留。
版本仍为 0.1.0；源码仓库为 https://github.com/binbin0227/easy_chat。
源码公开不等于发行文件已发布；JAR 上传到 GitHub Release 或 CurseForge 前仍须
执行上述检查。新增依赖或模型时重新审查。
