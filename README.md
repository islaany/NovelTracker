# NovelTracker · 小说阅读记录

截图 → OCR 自动识别书名 → 联网查简介/主角/高光 → 带标签记录，点名字无缝打开详情。

## 技术栈
- Kotlin + Jetpack Compose (Material 3)
- MVVM + Navigation
- Room（本地存储）
- ML Kit 中文 OCR（端侧离线）
- 小说简介搜索（接口已抽象，当前为 Mock，待接真实 API）

## 页面
- **Home（书架）**：小说卡片列表 + 标签筛选，顶部显示总数
- **Add（添加流程）**：截图 → OCR → 提取书名 → 搜索 → 保存 三步向导（带进度条）
- **Detail（详情）**：封面 + 书名/作者/标签 + 想再看/想推荐开关 + 简介/主角/高光
- **Tags（标签管理）**：自建标签 + 颜色

## 当前状态
框架版本：UI 全部可点通，OCR 与本地存储已接真实实现，小说简介搜索为 Mock 占位。
首次启动会自动写入 4 本示例小说 + 标签（方便直接看界面），可在 App 内删除。
想清空重来：手机「设置 → 应用 → NovelTracker → 清除数据」，下次启动会重新写入示例。

## 在手机上运行 / 安装
1. 用 **Android Studio** 打开本目录（`File → Open`，首次会自动 Sync Gradle 并下载 wrapper）。
2. 手机开启「开发者选项 → USB 调试」，连上电脑。
3. 点 Android Studio 的 **Run ▶**（或 `Shift+F10`），选你的设备即可安装并运行。
   - 想生成可单独安装的 APK：`Build → Build Bundle(s) / APK(s) → Build APK(s)`，产物在 `app/build/outputs/apk/debug/`，传到手机安装即可。
4. 注意：minSdk = 24（Android 7.0+），请确认手机系统 ≥ 7.0。

## 接真实小说 API
改动只在 `di/AppContainer`：把 `MockNovelSearchService()` 换成基于以下任一接口的实现：
- `zhuishushenqi`（追书神器）：/book/{id} 返回简介/标签/评分
- `owllook_api`：/v1/novels/{name} 返回主角信息
- `Amibk/novel-api`：/search/{keyword} 返回小说信息/目录

## 目录结构
```
app/src/main/java/com/huqi/noveltracker/
├── data/        # model / local(Room) / repository(接口+实现)
├── di/          # AppContainer（手动依赖装配，换实现只改这里）
├── ui/          # theme / navigation / component / screen(home,add,detail,tags)
└── MainActivity / NovelTrackerApplication
```
