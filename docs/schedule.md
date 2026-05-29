# 项目排期与阶段记录

## 任务拆分

项目按功能递进拆为 10 个阶段：

| 阶段 | 任务 | 主要产出 |
| --- | --- | --- |
| 阶段 0 | 项目结构、依赖、基础导航 | Compose 项目、底部导航、路由框架 |
| 阶段 1 | 数据库和 mock 数据 | Room 表结构、DAO、FakeCloudApi、Repository 初始化 |
| 阶段 2 | 文件列表和文件夹导航 | 文件列表、文件夹进入、返回上一级、面包屑 |
| 阶段 3 | 上传本地文件 | 系统文件选择器、私有目录复制、最近转存 |
| 阶段 4 | 文件删除、移动、重命名 | 更多菜单、AlertDialog、递归删除、移动校验 |
| 阶段 5 | TXT 文档阅读器 | TXT 读取、UTF-8/GBK 解码、分页、左右滑动 |
| 修复阶段 | mock TXT 和上传选择器修复 | mock TXT 写真实文件、上传 MIME 调整 |
| 阶段 6 | 网盘首页 | 用户信息、最近转存、最近浏览 |
| 阶段 7 | 文件分享和 DeepLink | 分享记录、分享链接、DeepLink 拉起、分享页 |
| 阶段 8 | 视频系统播放器 | FileProvider、content Uri、系统播放器、最近浏览 |
| 阶段 9 | 交付文档和测试说明 | README、实现说明、图表、测试用例、排期 |

## 实际排期表

| 日期 | 阶段 | 工作内容 | 状态 |
| --- | --- | --- | --- |
| 第 1 天 | 阶段 0 | 创建 Android Studio 项目，接入 Compose、Navigation、基础目录结构 | 已完成 |
| 第 1 天 | 阶段 1 | 创建 Room Entity/DAO/Database，添加 mock JSON 和 FakeCloudApi | 已完成 |
| 第 2 天 | 阶段 2 | 实现文件列表、文件夹导航、加载/错误/空状态 | 已完成 |
| 第 2 天 | 阶段 3 | 实现本地文件上传，复制到私有目录并写入最近转存 | 已完成 |
| 第 3 天 | 阶段 4 | 实现分享占位、重命名、移动、删除和递归删除 | 已完成 |
| 第 3 天 | 阶段 5 | 实现 TXT 阅读器、分页算法、最近浏览 | 已完成 |
| 第 4 天 | 修复阶段 | 修复 mock TXT 无真实文件、优化上传 MIME 类型 | 已完成 |
| 第 4 天 | 阶段 6 | 实现网盘首页和最近记录展示 | 已完成 |
| 第 5 天 | 阶段 7 | 实现分享链接、DeepLink、分享文件列表 | 已完成 |
| 第 5 天 | 阶段 8 | 实现视频文件系统播放器打开 | 已完成 |
| 第 5 天 | 阶段 9 | 完成交付文档和测试说明 | 已完成 |

## 每阶段完成内容

### 阶段 0：项目结构、依赖、基础导航

- 创建 Android App 工程。
- 使用 Kotlin 和 Jetpack Compose。
- 接入 Material 3。
- 接入 Navigation Compose。
- 创建底部导航：网盘、文件。
- 建立 `core`、`data`、`domain`、`feature`、`navigation` 分层目录。

### 阶段 1：Room 数据库、mock 数据、Repository 初始化

- 新增 `CloudFileEntity`、`RecentBrowseEntity`、`RecentTransferEntity`、`ShareRecordEntity`。
- 新增对应 DAO。
- 新增 `AppDatabase`。
- 新增 `mock_files.json`。
- 新增 `FakeCloudApi` 从 assets 读取 mock JSON。
- 新增 `FileRepository` 和 `FileRepositoryImpl`。
- 首次启动时将 mock 数据写入数据库。

### 阶段 2：文件列表和文件夹导航

- 文件 tab 展示当前目录文件。
- 支持根目录和子文件夹切换。
- 支持返回上一级。
- 支持面包屑展示。
- TXT 点击进入 Reader 路由。
- VIDEO 初期使用 Snackbar 占位。

### 阶段 3：上传本地文件

- 使用 `ActivityResultContracts.OpenDocument()` 打开系统文件选择器。
- 支持 `text/plain`、`application/octet-stream`、`video/*`、`*/*`。
- 上传文件复制到 `filesDir/cloud/`。
- 数据库 path 保存真实绝对路径。
- 上传成功写入 `recent_transfer`。
- 文件列表通过 Room Flow 自动刷新。

### 阶段 4：文件删除、移动、重命名

- 文件项右侧新增更多菜单。
- 菜单包含分享、重命名、移动、删除。
- 重命名使用 AlertDialog，不修改物理路径。
- 删除使用确认框。
- 删除文件夹时递归删除子文件和子文件夹。
- 删除时清理最近浏览和最近转存记录。
- 移动时选择目标文件夹。
- 禁止移动到自己或自己的子孙目录。

### 阶段 5：TXT 文档阅读器

- 新增 `ReaderViewModel`。
- Repository 新增 `readTextFile(fileId)`。
- TXT 文件读取优先 UTF-8，失败后 GBK。
- 文件不存在时显示错误页。
- 新增 `TxtPaginator`。
- 阅读器显示标题、正文、页码。
- 支持左滑下一页、右滑上一页。
- 打开 TXT 时写入最近浏览。

### 修复阶段：mock TXT 和上传选择器

- mock TXT 初始化时写入真实文件到 `filesDir/cloud/`。
- 数据库 path 保存真实绝对路径。
- 已初始化旧数据会在启动时自动修复 mock TXT path。
- 上传 MIME 增加 `application/octet-stream`，提高普通文件选择兼容性。

### 阶段 6：网盘首页

- 新增 `HomeViewModel`。
- 首页展示用户信息卡片。
- 展示文件总数和已使用容量。
- 最近转存通过 `recent_transfer` join `cloud_file` 查询。
- 最近浏览通过 `recent_browse` join `cloud_file` 查询。
- 最多显示 10 条。
- 空状态分别展示“暂无转存记录”和“暂无浏览记录”。

### 阶段 7：文件分享和 DeepLink 拉起

- 分享生成随机 `shareId`。
- 分享记录写入 `share_record`。
- 分享链接格式为 `simplecloud://share?shareId=...`。
- 链接不包含文件名、路径、大小等明文信息。
- 支持复制链接和系统分享。
- Manifest 注册 `simplecloud://share`。
- `MainActivity` 支持冷启动和 `onNewIntent` DeepLink 解析。
- 分享页根据 `shareId` 展示文件列表。
- 无效 `shareId` 显示错误。

### 阶段 8：视频文件使用系统播放器打开

- 新增 FileProvider 配置。
- 新增 `res/xml/file_paths.xml`。
- 新增 `VideoPlayerUtil`。
- 视频点击使用 `content://` Uri。
- Intent 使用 `ACTION_VIEW` 和 `video/*`。
- 添加 `FLAG_GRANT_READ_URI_PERMISSION`。
- 打开视频时写入最近浏览。
- 文件不存在或无播放器时显示 Snackbar。

### 阶段 9：交付文档和测试说明

- 新增 README。
- 新增实现说明文档。
- 新增 Mermaid 图表文档。
- 新增最小功能验证用例集。
- 新增阶段排期和完成记录。
