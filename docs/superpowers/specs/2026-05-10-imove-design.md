# iMove - 相机照片传输 Android App 设计文档

**日期**: 2026-05-10
**版本**: 1.0
**状态**: 已确认

---

## 1. 技术栈

| 类别 | 选择 | 说明 |
|------|------|------|
| 语言 | Kotlin | Android官方推荐语言 |
| UI框架 | Jetpack Compose | 现代声明式UI |
| 设计规范 | Material Design 3 | 最新Material Design |
| 架构模式 | MVVM | ViewModel + StateFlow + Repository |
| 依赖注入 | Hilt | Google官方推荐 |
| 数据库 | Room | 存储设备配置和传输记录 |
| 偏好存储 | DataStore | 用户设置 |
| 图片加载 | Coil | Kotlin原生，Compose支持好 |
| 导航 | Jetpack Navigation Compose | 官方导航方案 |
| 异步 | Kotlin Coroutines + Flow | 结构化并发 |
| 最低API | Android 10 (API 29) | 覆盖大部分设备 |
| 包名 | io.github.imove | - |

---

## 2. 项目结构

```
io.github.imove/
├── ui/                          # Compose UI层
│   ├── home/                    # 首页
│   ├── transfer/                # 自定义传输页
│   ├── preview/                 # 全屏预览页
│   ├── settings/                # 设置页
│   ├── components/              # 可复用组件
│   └── theme/                   # Material 3主题
├── viewmodel/                   # 状态管理层
│   ├── HomeViewModel.kt
│   ├── TransferViewModel.kt
│   ├── PreviewViewModel.kt
│   └── SettingsViewModel.kt
├── domain/                      # 业务逻辑层
│   ├── model/                   # 领域模型
│   ├── repository/              # Repository接口
│   └── usecase/                 # 用例
├── data/                        # 数据访问层
│   ├── local/                   # 本地存储
│   │   ├── database/            # Room数据库
│   │   └── datastore/           # Preferences
│   ├── usb/                     # USB设备访问
│   └── repository/              # Repository实现
├── service/                     # 后台服务
│   └── TransferService.kt
├── di/                          # 依赖注入
│   ├── AppModule.kt
│   ├── DatabaseModule.kt
│   └── RepositoryModule.kt
└── util/                        # 工具类
    ├── ExifUtils.kt
    ├── FileUtils.kt
    └── DateUtils.kt
```

---

## 3. 数据层设计

### 3.1 核心数据模型

#### StorageDevice (存储设备)

```kotlin
data class StorageDevice(
    val id: String,           // UUID
    val volumeUuid: String,   // 卷UUID
    val volumeLabel: String,  // 卷标
    val sourcePath: String,   // 配置的源目录
    val lastConnected: Long   // 最后连接时间
)
```

#### MediaFile (媒体文件)

```kotlin
data class MediaFile(
    val id: String,
    val name: String,
    val path: String,         // 设备上的路径
    val size: Long,
    val mimeType: String,
    val dateTaken: Long,      // EXIF拍摄日期
    val dateModified: Long,   // 文件修改日期
    val isVideo: Boolean
)
```

#### TransferItem (传输项)

```kotlin
data class TransferItem(
    val id: String,
    val file: MediaFile,
    val status: TransferStatus,
    val addedAt: Long,
    val completedAt: Long? = null
)
```

#### TransferStatus (传输状态)

```kotlin
enum class TransferStatus {
    QUEUED,        // 排队中
    TRANSFERRING,  // 传输中
    COMPLETED,     // 已完成
    SKIPPED,       // 跳过（文件已存在）
    FAILED,        // 失败
    CANCELLED      // 已取消
}
```

### 3.2 数据库设计 (Room)

#### devices_table

| 列名 | 类型 | 说明 |
|------|------|------|
| id | TEXT (PK) | 设备ID |
| volume_uuid | TEXT (UNIQUE) | 卷UUID |
| volume_label | TEXT | 卷标 |
| source_path | TEXT | 源目录路径 |
| last_connected | INTEGER | 最后连接时间 |

#### transferred_files_table

| 列名 | 类型 | 说明 |
|------|------|------|
| id | TEXT (PK) | 记录ID |
| file_name | TEXT | 文件名 |
| source_device_id | TEXT (FK) | 来源设备ID |
| transferred_at | INTEGER | 传输时间 |
| destination_path | TEXT | 目标路径 |

### 3.3 DataStore 偏好设置

```kotlin
data class UserPreferences(
    val targetDirectory: String = "Pictures/iMove/",
    val gridColumns: Int = 3,        // 1-4
    val language: String = "system", // zh/en/system
    val darkMode: Boolean = false
)
```

### 3.4 Repository接口

#### DeviceRepository

```kotlin
interface DeviceRepository {
    fun getConnectedDevice(): Flow<StorageDevice?>
    suspend fun getDeviceByVolume(volumeUuid: String): StorageDevice?
    suspend fun saveDevice(device: StorageDevice)
    suspend fun updateSourcePath(deviceId: String, path: String)
}
```

#### MediaRepository

```kotlin
interface MediaRepository {
    fun getFilesFromDevice(device: StorageDevice): Flow<List<MediaFile>>
    suspend fun getFilesByDateRange(device: StorageDevice, startDate: Long, endDate: Long): List<MediaFile>
    suspend fun isFileTransferred(fileName: String, deviceId: String): Boolean
    suspend fun markAsTransferred(file: MediaFile, deviceId: String)
}
```

#### TransferRepository

```kotlin
interface TransferRepository {
    fun addToQueue(files: List<MediaFile>)
    fun getQueue(): Flow<List<TransferItem>>
    fun removeFromQueue(itemId: String)
    fun clearQueue()
    fun cancelTransfer()
}
```

---

## 4. UI层设计

### 4.1 导航结构

```
HomeScreen → TransferScreen → PreviewScreen
HomeScreen → SettingsScreen
```

### 4.2 页面设计

#### HomeScreen (首页)

**无设备状态：**
- 显示连接图标和提示文案："请连接存储设备"

**已连接状态：**
- 卡片布局展示4种传输模式：
  1. 传输今日
  2. 传输近三日
  3. 自定义传输
  4. 传输全部（放在最后，降低误触风险）
- 右上角或菜单中提供设置入口

#### TransferScreen (自定义传输页面)

**单选模式（默认）：**
- 左侧：返回按钮
- 中部：文件总数统计
- 右侧：多选模式切换按钮（☑）
- 点击缩略图：加入传输队列
- 长按缩略图：全屏预览

**多选模式：**
- 左侧：关闭按钮（退出多选模式）
- 右侧："Move (已选数量)" 批量传输按钮
- 点击缩略图：选中/取消选中
- 拖动选择：批量选中
- 长按缩略图：全屏预览

**网格布局：**
- 默认每行3张
- 用户可选择1/2/3/4列
- 选择记忆在本地

**状态标记：**
- ✓ 已传输完成（右下角）
- ⏳ 在队列中等待（右下角）
- 视频时长显示（右下角）

**队列面板：**
- 底部按钮显示队列数量
- 点击展开Bottom Sheet
- 列出队列中的文件
- 可单独移除（正在传输中的不可移除）

#### PreviewScreen (全屏预览页面)

- 全屏显示图片/视频
- 支持左右滑动切换
- 图片：直接显示
- 视频：显示播放按钮
- 工具栏："Move"按钮
- 顶部：返回按钮

#### SettingsScreen (设置页面)

- 目标目录配置
- 重新选择当前设备的源目录
- 语言切换（中文/English/跟随系统）
- 深色模式切换（浅色/深色/跟随系统）
- 网格列数设置（1/2/3/4列）

---

## 5. 设备连接与识别

### 5.1 支持的设备类型

- **相机**（MTP协议连接）
- **读卡器**（USB大容量存储，内含相机SD卡）

### 5.2 设备识别机制

- 使用 **Volume UUID + Volume Label** 组合标识设备
- 已配置设备自动使用记忆的源目录
- 新设备弹出目录选择器
- 已配置设备的源目录不存在 → 重新选择

### 5.3 技术实现

**推荐方案：Storage Access Framework (SAF)**

- 使用 `ACTION_OPEN_DOCUMENT_TREE` 让用户选择目录
- 使用 `takePersistableUriPermission` 保持访问权限
- 注册 `ACTION_USB_DEVICE_ATTACHED` 广播
- 通过 `StorageManager` 获取卷信息

### 5.4 权限需求

```xml
<uses-permission android:name="android.permission.USB_PERMISSION" />
<uses-permission android:name="android.permission.MANAGE_EXTERNAL_STORAGE" />
<uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
```

---

## 6. 传输系统

### 6.1 传输队列

- 文件被加入队列后，按先进先出顺序逐个传输（单线程）
- 用户快速连续点击多个文件时，文件依次入队
- 传输过程中，用户可以继续浏览列表并添加更多文件到队列
- 用户可在通知栏中取消整个传输队列
- 队列仅存在于内存中，App被杀掉后队列丢失，不持久化

### 6.2 传输状态

```
QUEUED → TRANSFERRING → COMPLETED
                     → SKIPPED (文件已存在)
                     → FAILED (传输异常)
                     → CANCELLED (用户取消)
```

### 6.3 前台服务

**TransferService 职责：**
- 维护传输队列
- 执行文件复制操作
- 更新传输进度
- 发送系统通知
- 处理取消请求

**生命周期：**
- `startForegroundService()` 启动
- 显示持久通知（进度条）
- 队列为空时自动停止
- 用户可从通知栏取消

**线程模型：**
- 主线程：服务生命周期
- IO线程：文件复制操作
- 单线程队列：避免并发问题
- 协程：异步任务管理

### 6.4 通知设计

**传输中通知：**
- 标题：正在传输照片
- 内容：已传输 X/Y，队列等待 N 个
- 进度条：显示当前进度
- 操作：取消传输

**传输完成通知：**
- 标题：传输完成
- 内容：成功 X 个，跳过 Y 个，失败 Z 个
- 操作：查看文件

**传输中断通知：**
- 标题：传输中断
- 内容：中断原因（如存储空间不足）
- 操作：重试

### 6.5 文件传输逻辑

- **重名处理**：目标目录存在同名文件 → 跳过，不覆盖
- **文件名保持**：保持原始文件名不变
- **平铺存放**：所有文件直接存放在目标目录，不保留原始目录结构
- **EXIF优先**：日期判断优先读取EXIF拍摄日期，回退到文件修改日期

### 6.6 性能优化

- **缓冲复制**：使用缓冲区进行文件复制，减少IO次数
- **进度更新频率**：每传输1MB或完成一个文件更新一次进度
- **内存管理**：及时释放大文件引用，避免OOM
- **电池优化**：使用WakeLock确保后台不中断

---

## 7. 国际化与主题

### 7.1 国际化方案

- 使用Android资源系统（strings.xml）
- 支持中文（默认）和英文
- 语言切换立即生效，无需重启

**语言选项：**
- 跟随系统（默认）
- 中文（简体）
- English

**实现方式：**
- 使用 `AppCompatDelegate.setApplicationLocales()`
- 保存到DataStore
- 保持与Android 13+系统语言设置兼容

### 7.2 Material Design 3主题

**浅色主题：**
- 主色调：#2196F3 (Blue)
- 背景色：#FAFAFA
- 表面色：#FFFFFF
- 文字色：#1C1B1F
- 圆角：12dp（卡片）

**深色主题：**
- 主色调：#90CAF9 (Light Blue)
- 背景色：#1C1B1F
- 表面色：#2B2930
- 文字色：#E6E1E5
- 圆角：12dp（卡片）

**切换选项：**
- 跟随系统（默认）
- 浅色模式
- 深色模式

**Compose实现：**
- 使用 `MaterialTheme` 组件
- 动态颜色方案（Dynamic Color）
- 支持Android 12+动态取色
- 旧版本使用预定义颜色

### 7.3 组件样式

- **卡片**：圆角12dp，轻量阴影，表面色背景
- **按钮**：圆角20dp，主色调填充
- **AppBar**：表面色背景，标题居中
- **底部导航**：表面色背景，主色调选中
- **对话框**：圆角28dp，表面色背景

### 7.4 字体与间距

- **字体**：使用系统默认字体（Roboto / Noto Sans CJK）
- **标题**：18sp，粗体
- **正文**：14sp，常规
- **说明文字**：12sp，次要颜色
- **间距**：8dp基础单位，16dp标准间距

---

## 8. 性能要求

| 项目 | 要求 |
|------|------|
| 性能 | 图片列表滚动流畅，缩略图懒加载，大图预览无卡顿 |
| 权限 | USB设备访问权限、存储读写权限（SAF） |
| 兼容性 | 最低Android 10（API 29），适配主流机型 |
| 稳定性 | 传输过程中App切后台不中断，系统不杀进程 |
| 国际化 | 支持中文和英文，跟随系统语言或手动切换 |

---

## 9. 测试策略

### 9.1 单元测试

- ViewModel层：状态管理、业务逻辑
- Repository层：数据访问、转换逻辑
- UseCase层：业务规则验证

### 9.2 集成测试

- 数据库操作
- 文件系统操作
- USB设备连接模拟

### 9.3 UI测试

- 关键用户流程
- 页面导航
- 状态变化

---

## 10. 待确认事项

无

---

## 11. 变更记录

| 日期 | 版本 | 变更内容 |
|------|------|----------|
| 2026-05-10 | 1.0 | 初始设计文档 |
