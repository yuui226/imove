# Android 命令行开发环境搭建指南（Windows）

> 不安装 Android Studio，仅用命令行工具完成编译、安装、调试。

---

## 一、下载所需工具

| 工具 | 用途 | 下载地址 |
|------|------|---------|
| JDK 17 | Java 编译环境 | [Microsoft OpenJDK 17](https://aka.ms/download-jdk/microsoft-jdk-17.0.13-windows-x64.msi) |
| Android SDK cmdline-tools | SDK 管理器（sdkmanager） | [Command line tools only](https://developer.android.com/studio#command-line-tools-only) |
| platform-tools | adb 调试工具 | [Platform Tools](https://developer.android.com/tools/releases/platform-tools) |

> JDK 也可用 winget 一键安装：`winget install Microsoft.OpenJDK.17`

---

## 二、目录结构

所有 Android SDK 文件统一放在一个目录下，推荐 `C:\Users\<你的用户名>\cmdline-tools\`：

```
C:\Users\HYS\cmdline-tools\
├── latest\                  ← cmdline-tools 解压后必须放在 latest 子目录下
│   ├── bin\
│   │   ├── sdkmanager.bat
│   │   └── ...
│   └── lib\
├── platform-tools\          ← platform-tools 解压后直接放这里
│   ├── adb.exe
│   └── ...
├── platforms\               ← sdkmanager 自动下载创建
│   └── android-35\
├── build-tools\             ← sdkmanager 自动下载创建
│   └── 34.0.0\
└── licenses\                ← 手动创建或 sdkmanager --licenses 自动生成
```

### 整理目录

如果 cmdline-tools 解压后直接是 `bin/` 和 `lib/`（没有 `latest` 层），需要手动整理：

```bash
mkdir -p C:\Users\HYS\cmdline-tools\latest
mv C:\Users\HYS\cmdline-tools\bin C:\Users\HYS\cmdline-tools\latest\
mv C:\Users\HYS\cmdline-tools\lib C:\Users\HYS\cmdline-tools\latest\
mv C:\Users\HYS\cmdline-tools\NOTICE.txt C:\Users\HYS\cmdline-tools\latest\
mv C:\Users\HYS\cmdline-tools\source.properties C:\Users\HYS\cmdline-tools\latest\
```

platform-tools 解压后直接放到 `C:\Users\HYS\cmdline-tools\platform-tools\` 即可。

---

## 三、设置环境变量

以管理员身份打开 CMD，执行：

```cmd
setx JAVA_HOME "C:\Program Files\Microsoft\jdk-17.0.13.11-hotspot"
setx ANDROID_HOME "C:\Users\HYS\cmdline-tools"
setx PATH "%PATH%;C:\Program Files\Microsoft\jdk-17.0.13.11-hotspot\bin;C:\Users\HYS\cmdline-tools\latest\bin;C:\Users\HYS\cmdline-tools\platform-tools"
```

> `setx PATH` 有 1024 字符截断风险。如果 PATH 很长，建议手动添加：
> 1. `Win + R` → 输入 `sysdm.cpl` → 回车
> 2. **高级** → **环境变量**
> 3. 用户变量中新建 `JAVA_HOME` 和 `ANDROID_HOME`
> 4. 编辑 `Path`，添加三条路径：
>    - `C:\Program Files\Microsoft\jdk-17.0.13.11-hotspot\bin`
>    - `C:\Users\HYS\cmdline-tools\latest\bin`
>    - `C:\Users\HYS\cmdline-tools\platform-tools`

**设置完必须重开终端才能生效。**

验证：

```bash
java -version
adb version
sdkmanager --version
```

---

## 四、接受 SDK 许可证 + 安装组件

```bash
# 接受许可证（一路按 y）
sdkmanager --licenses

# 安装本项目需要的组件
sdkmanager "platforms;android-35" "build-tools;34.0.0" "platform-tools"
```

---

## 五、手机无线调试配对（首次）

1. 手机进入 **设置** → **关于手机** → 连续点击 **版本号** 7 次，开启开发者选项
2. 进入 **设置** → **系统** → **开发者选项**
3. 开启 **无线调试**，点进去
4. 点 **使用配对码配对设备**，记下 **配对码**、**IP地址** 和 **端口号**
5. 电脑上执行配对：

```bash
adb pair <手机IP>:<配对端口> <配对码>
# 示例：adb pair 192.168.5.119:38981 930050
```

6. 配对成功后，连接设备（注意：连接端口 ≠ 配对端口，看无线调试页面上方显示的端口）：

```bash
adb connect <手机IP>:<连接端口>
```

7. 验证连接：

```bash
adb devices
```

看到设备列表中有你的手机即可。

> 以后同网络下只需 `adb connect`，不用再配对。

---

## 六、日常开发命令

```bash
# 编译并安装到手机（无线调试时可能超时）
./gradlew installDebug

# 推荐：分步执行，更稳定
./gradlew assembleDebug
adb install app/build/outputs/apk/debug/app-debug.apk

# 清缓存重新编译
./gradlew clean installDebug

# 只编译不安装
./gradlew assembleDebug

# 查看手机日志
adb logcat

# 卸载 app
adb uninstall io.github.imove

# 断开设备
adb disconnect
```

---

## 七、卸载清理

### 删除 SDK

```cmd
rmdir /s /q C:\Users\HYS\cmdline-tools
```

### 删除 JDK

```cmd
rmdir /s /q "C:\Program Files\Microsoft\jdk-17.0.13.11-hotspot"
```

### 清理环境变量

1. `Win + R` → `sysdm.cpl` → **高级** → **环境变量**
2. 删除 `JAVA_HOME`、`ANDROID_HOME`
3. 从 `Path` 中移除相关条目

---

## 八、常见问题

### Q: `adb` 命令没反应
检查 PATH 中 platform-tools 路径是否正确，重开终端。

### Q: `JAVA_HOME is not set`
确认 JDK 安装路径正确，JAVA_HOME 环境变量已设置，重开终端。

### Q: `License not accepted`
运行 `sdkmanager --licenses` 一路按 `y`。

### Q: `Could not determine SDK root`
cmdline-tools 必须放在 `<sdk_root>/cmdline-tools/latest/` 目录下。

### Q: 安装 APK 时手机上弹确认框
进入 **开发者选项** → 开启 **USB安装**（或"通过USB安装应用"）。

### Q: 无线安装超时
先重连再装：
```bash
adb disconnect
adb connect <手机IP>:<端口>
adb install app/build/outputs/apk/debug/app-debug.apk
```
