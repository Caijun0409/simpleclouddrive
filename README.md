# SimpleCloudDrive

SimpleCloudDrive 是一个基于 Kotlin 和 Jetpack Compose 实现的 Android 网盘练习项目。项目使用本地 Room 数据库模拟云盘数据状态，使用 assets 中的 mock JSON 模拟云端接口，并支持文件列表、上传、阅读、视频打开、最近记录、文件管理、分享链接和 DeepLink 拉起。

## 技术栈

- Kotlin
- Jetpack Compose
- Material 3
- Navigation Compose
- Room
- Coroutines
- Flow
- Kotlinx Serialization
- Android Activity Result API
- FileProvider

## 功能列表

- 首次启动从 `mock_files.json` 初始化网盘文件数据。
- 首页展示用户信息、文件总数、已使用容量、最近转存、最近浏览。
- 文件列表展示根目录和文件夹内容。
- 支持进入文件夹、返回上一级、面包屑展示。
- 支持上传本地文件，复制到 App 私有目录 `filesDir/cloud/`。
- 支持 TXT 阅读器，包含 UTF-8/GBK 解码、分页、左右滑动翻页。
- 支持视频文件通过系统播放器打开，使用 FileProvider 提供 `content://` Uri。
- 支持重命名、移动、删除文件。
- 支持递归删除文件夹。
- 支持分享文件，生成 `simplecloud://share?shareId=...` 链接。
- 支持 DeepLink 拉起 App 并进入分享文件页。

## 运行环境

- Android Studio：建议使用支持 Android Gradle Plugin 9.x 的版本。
- JDK：17。
- Android SDK：项目 `compileSdk` 为 36。
- 最低系统版本：Android 8.0，`minSdk = 26`。
- 推荐使用 Android 模拟器或真机运行 Debug 包。

## 启动方式

1. 使用 Android Studio 打开项目根目录。
2. 等待 Gradle Sync 完成。
3. 选择 `app` 运行配置。
4. 选择模拟器或真机，点击 Run。

也可以在命令行编译：

```bash
./gradlew :app:assembleDebug
```

Windows PowerShell：

```powershell
.\gradlew.bat :app:assembleDebug
```

## DeepLink 测试方式

先在文件列表中点击某个文件右侧更多菜单，选择“分享”，复制生成的链接。链接格式只包含 `shareId`：

```text
simplecloud://share?shareId={shareId}
```

使用 adb 测试：

```bash
adb shell am start -W -a android.intent.action.VIEW -d "simplecloud://share?shareId=xxx" com.example.simpleclouddrive
```

预期结果：

- 有效 `shareId`：App 拉起并进入分享文件列表页。
- 无效 `shareId`：分享页显示“分享链接无效”。

## 项目结构说明

```text
app/src/main/java/com/example/simpleclouddrive
├── AppContainer.kt                 # 简单依赖容器，创建 Room、FakeCloudApi、Repository
├── MainActivity.kt                 # Compose 入口，处理 DeepLink 冷启动和 onNewIntent
├── SimpleCloudApp.kt               # 应用壳、底部导航、全局 DeepLink 导航
├── core
│   ├── ui                          # 主题和通用 Loading/Error/Empty 组件
│   └── util                        # DeepLinkUtil、VideoPlayerUtil
├── data
│   ├── local                       # Room 数据库、DAO、Entity、FileStorageManager
│   ├── mapper                      # Entity/DTO 到 domain model 映射
│   ├── remote                      # FakeCloudApi 和 DTO
│   └── repository                  # FileRepositoryImpl
├── domain
│   ├── model                       # CloudFile、FileType
│   └── repository                  # FileRepository 接口
├── feature
│   ├── file                        # 文件列表、上传、管理菜单
│   ├── home                        # 网盘首页
│   ├── reader                      # TXT 阅读器和分页
│   └── share                       # 分享文件页
└── navigation                      # AppNavGraph 和底部导航配置
```

## 核心实现说明

- 初始化数据：`FakeCloudApi` 读取 `assets/mock_files.json`，`FileRepositoryImpl.initializeIfNeeded()` 写入 Room。mock TXT 会额外写入 App 私有目录，数据库保存真实绝对路径。
- 文件上传：`ActivityResultContracts.OpenDocument` 获取 Uri，`FileStorageManager.copyUriToPrivateStorage()` 复制到 `filesDir/cloud/`，Repository 写入 `cloud_file` 和 `recent_transfer`。
- TXT 阅读：`readTextFile()` 从数据库 path 读取真实文件，先按 UTF-8 解码，失败后按 GBK 解码。`TxtPaginator` 根据屏幕尺寸、字体大小和行高估算分页。
- 文件管理：重命名只更新 `name` 和 `modifiedAt`；移动更新 `parentId` 并禁止移到自己或子孙目录；删除文件夹时递归删除子节点和最近记录。
- 视频播放：使用 `FileProvider` 把私有文件转换为 `content://` Uri，通过 `Intent.ACTION_VIEW` 和 `video/*` 打开系统播放器。
- 分享 DeepLink：分享记录只保存本地 `shareId -> fileIdsJson`，链接中只包含 `shareId`。Manifest 注册 `simplecloud://share`，MainActivity 解析后导航到 `share/{shareId}`。
