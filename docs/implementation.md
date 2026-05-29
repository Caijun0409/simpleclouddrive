# 实现说明

## 架构设计

项目采用轻量分层结构：

- `feature`：Compose 页面和 ViewModel。页面只负责展示和用户交互，ViewModel 负责 UI 状态和事件分发。
- `domain`：业务模型和 Repository 接口。UI 层只依赖 `FileRepository`，不直接访问 Room。
- `data`：Room、本地文件存储、mock API、Repository 实现。
- `core`：通用 UI 组件和工具类，如 DeepLink、视频播放器打开工具。
- `navigation`：统一管理 Compose 导航路由。

依赖创建集中在 `AppContainer`，避免在页面中直接创建数据库或 DAO。异步数据使用 Flow 暴露，耗时操作统一通过 Repository 切到 `Dispatchers.IO`。

## 数据库设计

Room 数据库 `AppDatabase` 当前包含四张表：

### cloud_file

对应 `CloudFileEntity`，保存网盘文件元数据。

| 字段 | 说明 |
| --- | --- |
| `fileId` | 主键，文件唯一 ID |
| `name` | 文件名 |
| `size` | 文件大小，文件夹为 0 |
| `path` | 文件路径。上传文件和 mock TXT 保存真实绝对路径 |
| `parentId` | 父文件夹 ID，根目录为 null |
| `type` | `FOLDER`、`TXT`、`VIDEO`、`OTHER` |
| `timestamp` | 创建时间 |
| `modifiedAt` | 修改时间 |

### recent_transfer

对应 `RecentTransferEntity`，保存最近上传/转存记录。

| 字段 | 说明 |
| --- | --- |
| `fileId` | 主键，关联 `cloud_file.fileId` |
| `transferTime` | 转存时间 |

### recent_browse

对应 `RecentBrowseEntity`，保存最近浏览记录。

| 字段 | 说明 |
| --- | --- |
| `fileId` | 主键，关联 `cloud_file.fileId` |
| `browseTime` | 浏览时间 |

### share_record

对应 `ShareRecordEntity`，保存分享记录。

| 字段 | 说明 |
| --- | --- |
| `shareId` | 主键，分享链接唯一 ID |
| `fileIdsJson` | 被分享文件 ID 列表的 JSON 字符串 |
| `createdAt` | 创建时间 |

## mock 网络流程

流程为：

```text
mock_files.json -> FakeCloudApi -> FileDto -> CloudFileEntity -> Room -> CloudFile -> UI
```

1. `FakeCloudApi.fetchFiles()` 从 assets 读取 `mock_files.json`。
2. JSON 通过 Kotlinx Serialization 解析为 `FileDto`。
3. `FileRepositoryImpl.initializeIfNeeded()` 将 DTO 转为 `CloudFileEntity`。
4. TXT mock 文件在初始化时写入 `filesDir/cloud/`，并把真实绝对路径保存到 `path`。
5. DAO 通过 Flow 观察文件列表。
6. Repository 把 Entity 映射为 domain model `CloudFile`。
7. Compose 页面订阅 ViewModel 状态并展示。

如果数据库已经初始化过，Repository 仍会检查 mock TXT 的 path 是否存在；如果不存在，会重新创建真实文本文件并更新数据库，解决旧数据指向假路径的问题。

## TXT 分页算法

分页逻辑位于 `TxtPaginator`：

1. 统一换行符：将 `\r\n`、`\r` 转为 `\n`。
2. 根据屏幕宽度、字体大小估算每行字符数。
3. 根据屏幕高度、行高估算每页行数。
4. `每页字符数 = 每行字符数 * 每页行数`。
5. 按字符切页。
6. 在分页边界附近优先寻找换行符，避免把段落切得太碎。
7. 返回 `List<String>`，Reader 根据当前页索引展示。

分页在 `ReaderViewModel` 中通过 `withContext(Dispatchers.IO)` 执行，避免大文本分页阻塞 UI。

## DeepLink 分享实现

分享入口在文件列表更多菜单中：

1. 用户点击“分享”。
2. `FileListViewModel.shareFile()` 调用 `FileRepository.createShareLink(fileId)`。
3. Repository 生成随机 `shareId`。
4. 将 `shareId` 和 `[fileId]` 写入 `share_record`。
5. `DeepLinkUtil.buildShareLink()` 生成 `simplecloud://share?shareId=...`。
6. UI 弹出 Dialog，提供复制和系统分享。

链接只包含 `shareId`，不包含文件名、路径、大小等明文信息。

Manifest 注册：

```xml
<data
    android:host="share"
    android:scheme="simplecloud" />
```

`MainActivity` 在冷启动和 `onNewIntent` 中调用 `DeepLinkUtil.parseShareId(uri)`。解析成功后把 `shareId` 传给 `SimpleCloudApp`，再导航到 `share/{shareId}`。

分享页流程：

1. `ShareViewModel` 根据 `shareId` 查询 `ShareRecordEntity`。
2. 解析 `fileIdsJson`。
3. 批量查询对应 `CloudFileEntity`。
4. 映射为 `CloudFile` 展示。
5. 无效 `shareId` 显示错误页。

## 文件上传实现

上传入口使用 `ActivityResultContracts.OpenDocument()`。MIME 类型包含：

- `text/plain`
- `application/octet-stream`
- `video/*`
- `*/*`

上传流程：

1. 系统文件选择器返回 Uri。
2. Repository 读取显示名、大小和 MIME。
3. 根据 MIME 和扩展名判断 `FileType`。
4. `FileStorageManager.copyUriToPrivateStorage()` 将文件复制到 `filesDir/cloud/`。
5. `cloud_file.path` 保存复制后的真实绝对路径。
6. 插入 `cloud_file`。
7. 写入 `recent_transfer`。
8. Room Flow 自动刷新文件列表和首页最近转存。

## 文件删除、移动、重命名实现

### 重命名

重命名只更新数据库：

- `name`
- `modifiedAt`

不会修改真实物理文件路径，避免移动或重命名私有文件时出现路径失效。

### 删除

删除逻辑放在 Repository：

1. 查询目标文件。
2. 如果是文件夹，递归查询全部子文件和子文件夹。
3. 对 App 私有目录中的真实文件执行物理删除。
4. 删除 `recent_browse` 和 `recent_transfer` 关联记录。
5. 删除 `cloud_file` 记录。

递归删除保证删除文件夹时不会留下孤儿节点。

### 移动

移动只更新 `parentId` 和 `modifiedAt`。

限制：

- 不能移动到自己。
- 文件夹不能移动到自己的子孙目录。
- 目标必须是文件夹或根目录。

合法性判断在 Repository 中完成，UI 只展示可选目标并发起操作。

## 视频打开实现

视频文件使用系统播放器打开：

1. 点击视频后先写入最近浏览。
2. 校验数据库 path 指向的真实文件是否存在。
3. 使用 FileProvider 生成 `content://` Uri。
4. 使用 `Intent.ACTION_VIEW` 打开。
5. MIME 类型为 `video/*`。
6. 添加 `FLAG_GRANT_READ_URI_PERMISSION`。

如果没有播放器，显示“未找到可打开该视频的应用”。如果文件不存在，显示明确错误。

## 异常处理

- 文件不存在：Reader 和视频打开都会显示明确错误，不崩溃。
- TXT 解码失败：先 UTF-8，失败后 GBK。
- 分享 ID 无效：分享页显示“分享链接无效”。
- 上传读取失败：Snackbar 显示失败原因。
- 移动非法目标：Repository 抛出业务错误，ViewModel 转为 Snackbar。
- 删除私有文件：只删除确认属于 `filesDir/cloud/` 的文件，避免误删外部路径。

## 难点与解决方案

### mock TXT 没有真实文件

早期 mock 数据只有元数据，`readme.txt` 的 path 指向 `/readme.txt`，Reader 无法读取。解决方案是在初始化时为 mock TXT 写入真实文本文件，并把绝对路径保存到数据库。

### App 私有文件给外部播放器读取

私有目录不能直接用 `file://` 暴露。解决方案是配置 FileProvider，只开放 App `files/` 目录，并通过 `content://` Uri 授权播放器临时读取。

### 文件夹移动可能形成环

如果允许文件夹移动到自己的子目录，会导致目录树成环。解决方案是在 Repository 中递归判断目标是否为当前文件夹的子孙节点。

### TXT 分页无法精确测量所有字符

Compose 文本真实排版受字体、语言和设备影响。当前采用估算算法：根据宽度、字体大小、行高计算字符容量，并优先在换行处断页。这样实现简单稳定，适合训练营阶段。

### DeepLink 需要兼容冷启动和热启动

冷启动在 `onCreate` 解析 `intent.data`，热启动通过 `launchMode="singleTop"` 和 `onNewIntent` 解析新 Intent。解析结果通过 Compose 状态传入应用导航层。
