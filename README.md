# 轻时笺 (QingShiJian)

一款轻量级的 Android 个人管理应用，集成账本、待办、笔记、生日提醒四大功能。

## 功能

- **账本** — 支持项目账单和时段账单，记录日常开销
- **待办** — 创建待办事项，设置优先级和截止日期，到期提醒
- **笔记** — 快速记录文字笔记，支持拼音排序
- **生日** — 管理生日提醒，支持农历/阳历，自动计算倒计时

## 技术栈

| 类别 | 库 | 版本 | 用途 |
|------|-----|------|------|
| 语言 | Kotlin | 2.2.10 | 主要开发语言 |
| UI 框架 | Jetpack Compose | BOM 2024.09 | 声明式 UI 构建 |
| 设计规范 | Material 3 | - | 统一的设计语言和主题系统 |
| 图标扩展 | Material Icons Extended | - | 提供完整的 Material 图标集 |
| 生命周期 | Lifecycle Runtime KTX | 2.6.1 | 管理 Activity/Compose 生命周期 |
| Activity | Activity Compose | 1.8.0 | Compose 与 Activity 的集成桥接 |
| 核心 | Core KTX | 1.10.1 | Android KTX 扩展，简化 API 调用 |
| 序列化 | kotlinx-serialization-json | 1.6.0 | JSON 序列化/反序列化，用于数据模型持久化 |
| 数据存储 | DataStore Preferences | 1.0.0 | 键值对形式的轻量本地存储，保存用户设置和排序偏好 |
| 后台任务 | WorkManager | 2.9.0 | 可靠的后台任务调度，用于定时检查通知提醒 |
| JSON 解析 | Gson | 2.10.1 | JSON 解析库，用于数据备份/恢复的序列化处理 |
| 构建工具 | AGP | 9.1.1 | Android Gradle Plugin |

## 环境要求

- Android Studio
- minSdk 26 / targetSdk 36
- Kotlin + Compose

## 构建与运行

1. 克隆仓库
2. 用 Android Studio 打开项目
3. 同步 Gradle
4. 运行到设备或模拟器

## 许可证

MIT License
