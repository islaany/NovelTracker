# NovelTracker · 小说阅读记录

截图 → OCR 自动识别书名 → 联网查简介/主角/高光 → 带标签记录，点名字无缝打开详情。

## 技术栈
- Kotlin + Jetpack Compose (Material 3)
- MVVM + Navigation
- Room（本地存储）
- ML Kit 中文 OCR（端侧离线）
- 小说简介搜索（接口已抽象，当前为 Mock，待接真实 API）

## 页面
- **Home（书架）**：小说卡片列表 + 标签筛选
- **Add（添加流程）**：截图 → OCR → 提取书名 → 搜索 → 保存 五步向导
- **Detail（详情）**：封面 + 书名/作者/标签 + 想再看/想推荐 + 简介/主角/高光
- **Tags（标签管理）**：自建标签 + 颜色

## 状态
当前为框架版本：UI 全部可点通，OCR 与本地存储已接真实实现，小说简介搜索为 Mock 占位（接真 API 只改 `di/AppContainer` 一处）。

## 运行
用 Android Studio 打开本目录，`File > Sync Project with Gradle Files` 后运行。
