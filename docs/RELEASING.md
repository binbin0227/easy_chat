# 发布 0.1.0

当前状态：源码可推送，完整 JAR 的二进制发布被许可问题阻止，详情见 LICENSE_REVIEW.md。

1. 先完成不含 TTS/eSpeak-NG 的识别 JNI 构建、替换和实际识别回归；或提供满足 GPL 的许可、对应源码及重建材料。不要只补一个 GPL 文本就声称完成。
2. 核对该新运行库全部依赖的版本、版权和许可证，再更新依赖摘要、THIRD_PARTY_NOTICES 和发布检查。
3. 用 JDK 21 执行 prepare-dependencies.ps1、build.ps1 -Tasks test,remapJar、package.ps1；执行 check-release.py，必须零退出码。
4. 在普通 Forge 与 Lunar 配置下验收麦克风、侧键、语音发送、经代理连接的双行聊天翻译和模型删除/恢复。
5. GitHub 仓库提交源码和文档，不提交 work、run、模型或完整 JAR。Release 资产放完整 JAR、SHA256SUMS.txt、许可说明；保留二进制依赖要求的对应源码资产（如果适用）。
6. 发布说明标明 Windows x64、Forge 1.8.9、CPU 推理、约 362 MiB、首次模型释放、已知术语误译和测试范围。当前没有发布到任何外部平台，也没有创建 GitHub 仓库。
