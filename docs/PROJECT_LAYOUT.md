# 项目目录与 Git 范围

| 目录 | 用途 | 提交 Git |
|---|---|---|
| src/main | Forge 模组代码、界面、模型元数据与许可证 | 是 |
| src/asrWorker | 独立 Java 识别进程源码 | 是 |
| src/test | 自动测试与显式集成测试入口 | 是 |
| scripts | 构建、校验和固定摘要的依赖准备 | 是 |
| docs | 安装、验证、许可和发布说明 | 是 |
| gradle、gradlew* | 固定版本 Gradle Wrapper | 是 |
| licenses | Gradle 构建工具许可 | 是 |
| work/libs、work/models | 本地运行库及 Paraformer 模型 | 否 |
| work/text-translation* | 本地 OPUS 模型 | 否 |
| work/text-upgrade | 保留的千问 GGUF，后续可复用 | 否 |
| work/archive/pre-release-20261008 | 已停用引擎、脚本、许可和旧截图 | 否 |
| work/backups、work/reports | 本地备份与本次检查记录 | 否 |
| work/archive/experiments-before-release | 旧实验、日志和原生审计源码取证 | 否 |
| run、build、.gradle | 游戏测试目录与构建缓存 | 否 |
| outputs | 发行资产和摘要，许可清除后上传 Releases | 否 |

旧文件归档未删除，千问权重保留。当前引擎不再引用旧资源。首次克隆使用 prepare-dependencies.ps1 重建依赖，摘要不一致立即失败。用户游戏目录 easychat/ 不由 Git 管理，整理工程不会清理玩家模型。
