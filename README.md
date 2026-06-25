# iMove

**相机照片秒传手机，告别繁琐导出。**

每次拍完照，还要开电脑导照片？iMove 让你用一根 OTG 线，把相机存储卡里的照片和视频直接传到手机——插上就识别，选好就传输，全程无需电脑。

## 为什么用 iMove

- 📷 **即插即用** — USB 存储设备插入手机，自动识别，无需手动查找文件夹
- 🕐 **按日期筛选** — 按"近一日 / 近三日 / 近十日"快速选取，不用在几百张图里翻
- ⚡ **高速传输** — 4 路并发复制，智能预取缩略图，浏览流畅不卡顿
- 🖼️ **缩略图预览** — 方格布局浏览所有照片，长按大图预览，支持缩放滑动
- 🎯 **队列管理** — 点击加入队列，按日期组批量添加，传输进度一目了然
- 🌐 **中英双语** — 跟随系统语言自动切换
- 🌙 **深色模式** — 支持浅色 / 深色 / 跟随系统

## 工作流程

```
插入 USB 设备 → 选择照片目录 → 设置保存位置 → 选择时间范围 → 点击传输 → 完成
```

## 支持的设备

- 读卡器（插入相机 SD 卡后连接手机）
- U 盘、移动硬盘等 USB 大容量存储设备

## 支持的格式

| 图片 | 视频 |
|------|------|
| JPG, PNG, GIF, HEIC | MP4, MOV, AVI, MKV |
| RAW: CR2, NEF, ARW | 3GP, WEBM |

## 环境要求

- Android 10+（API 29）
- 支持 OTG 的 Android 设备
- 一根 OTG 数据线或转接头

## 技术栈

Kotlin · Jetpack Compose · Material 3 · Hilt · Room · Coil · MVVM

## 开发与构建

### 首次环境搭建（Windows）

全新电脑上，在项目根目录跑一次安装脚本即可自动准备好打包环境：

```powershell
.\setup.ps1            # 加 -Build 可装完顺手打一个 debug 包验证
```

脚本会自动完成（全部装在 `D:\dev` 下，无需管理员权限）：

- 下载安装 **JDK 17**（清华 Adoptium 镜像）
- 下载安装 **Android SDK**：命令行工具 + Platform 35 + Build-Tools 35 + Platform-Tools（腾讯云镜像）
- 生成 `local.properties`，设置 `JAVA_HOME` / `ANDROID_HOME` 等永久环境变量

> Gradle 本体与 Maven 依赖的镜像源已分别配在 `gradle-wrapper.properties`、`settings.gradle.kts`（腾讯 / 阿里）。
> 脚本默认装到 `D:\dev`，可用 `.\setup.ps1 -InstallRoot E:\dev` 自定义位置。
> 安装的永久环境变量需**重开终端**后才在新窗口生效。

### 打包

```powershell
.\gradlew.bat assembleDebug      # Windows
./gradlew assembleDebug          # macOS / Linux
```

产出路径：`app/build/outputs/apk/debug/app-debug.apk`
