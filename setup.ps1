<#
  iMove 开发环境一键安装脚本 (Windows / PowerShell)

  作用：在一台全新的电脑上自动准备好命令行打包所需的全部环境：
    1. JDK 17        —— 清华 Adoptium 镜像
    2. Android SDK   —— 腾讯云镜像（命令行工具 + platform-35 + build-tools 35 + platform-tools）
    3. 写好 local.properties，让 Gradle 找到 SDK
    4. 设置用户级永久环境变量 JAVA_HOME / ANDROID_HOME / PATH

  Gradle 本体与 Maven 依赖的镜像源已经写在 gradle-wrapper.properties 和
  settings.gradle.kts 里（腾讯 / 阿里），无需本脚本处理。

  用法（在项目根目录打开 PowerShell）：
      ./setup.ps1                 # 默认装到 D:\dev
      ./setup.ps1 -InstallRoot E:\dev
      ./setup.ps1 -Build          # 装完顺手打一个 debug 包验证

  无需管理员权限，全部装在 InstallRoot 下，不污染系统。
#>

param(
    [string]$InstallRoot = "D:\dev",
    [switch]$Build
)

$ErrorActionPreference = "Stop"
$ProgressPreference   = "SilentlyContinue"   # 让 Invoke-WebRequest / 进度条不拖慢
$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path

# 版本 / 镜像源（与首次手动安装时一致）
$JdkMirrorDir   = "https://mirrors.tuna.tsinghua.edu.cn/Adoptium/17/jdk/x64/windows/"
$CmdlineToolsUrl = "https://mirrors.cloud.tencent.com/AndroidSDK/commandlinetools-win-11076708_latest.zip"
$AndroidPackages = @("platform-tools", "platforms;android-35", "build-tools;35.0.0")

$JdkHome   = Join-Path $InstallRoot "jdk-17"
$SdkRoot   = Join-Path $InstallRoot "android-sdk"

function Write-Step($msg) { Write-Host "`n==> $msg" -ForegroundColor Cyan }

New-Item -ItemType Directory -Force $InstallRoot | Out-Null

# ---------------------------------------------------------------------------
# 1) JDK 17 (清华 Adoptium 镜像)
# ---------------------------------------------------------------------------
if (Test-Path (Join-Path $JdkHome "bin\java.exe")) {
    Write-Step "JDK 17 已存在，跳过：$JdkHome"
} else {
    Write-Step "解析清华镜像上的最新 JDK 17 文件名..."
    $html = (Invoke-WebRequest -Uri $JdkMirrorDir -UseBasicParsing).Content
    $jdkFile = ([regex]::Matches($html, 'OpenJDK17U-jdk_x64_windows_hotspot_[0-9._]+\.zip') |
                ForEach-Object { $_.Value } | Sort-Object -Unique | Select-Object -Last 1)
    if (-not $jdkFile) { throw "未能在清华镜像找到 JDK 17 安装包" }
    $jdkUrl = $JdkMirrorDir + $jdkFile
    $jdkZip = Join-Path $InstallRoot "jdk17.zip"

    Write-Step "下载 JDK：$jdkFile"
    curl.exe -L --fail --retry 3 -o $jdkZip $jdkUrl
    if ($LASTEXITCODE -ne 0) { throw "JDK 下载失败" }

    Write-Step "解压 JDK..."
    $tmp = Join-Path $InstallRoot "_jdktmp"
    Remove-Item $tmp -Recurse -Force -ErrorAction SilentlyContinue
    Expand-Archive -Path $jdkZip -DestinationPath $tmp -Force
    $inner = Get-ChildItem $tmp -Directory | Select-Object -First 1
    Remove-Item $JdkHome -Recurse -Force -ErrorAction SilentlyContinue
    Move-Item $inner.FullName $JdkHome
    Remove-Item $tmp -Recurse -Force -ErrorAction SilentlyContinue
    Remove-Item $jdkZip -Force -ErrorAction SilentlyContinue
    Write-Host "JDK 17 -> $JdkHome" -ForegroundColor Green
}

$env:JAVA_HOME = $JdkHome
$env:Path = (Join-Path $JdkHome "bin") + ";" + $env:Path

# ---------------------------------------------------------------------------
# 2) Android 命令行工具 (腾讯云镜像)
# ---------------------------------------------------------------------------
$sdkManager = Join-Path $SdkRoot "cmdline-tools\latest\bin\sdkmanager.bat"
if (Test-Path $sdkManager) {
    Write-Step "Android 命令行工具已存在，跳过"
} else {
    Write-Step "下载 Android 命令行工具..."
    $ctZip = Join-Path $InstallRoot "cmdline-tools.zip"
    curl.exe -L --fail --retry 3 -o $ctZip $CmdlineToolsUrl
    if ($LASTEXITCODE -ne 0) { throw "命令行工具下载失败" }

    Write-Step "解压并摆放到 cmdline-tools\latest ..."
    $tmp = Join-Path $InstallRoot "_cmdtmp"
    Remove-Item $tmp -Recurse -Force -ErrorAction SilentlyContinue
    Expand-Archive -Path $ctZip -DestinationPath $tmp -Force
    New-Item -ItemType Directory -Force (Join-Path $SdkRoot "cmdline-tools") | Out-Null
    Remove-Item (Join-Path $SdkRoot "cmdline-tools\latest") -Recurse -Force -ErrorAction SilentlyContinue
    Move-Item (Join-Path $tmp "cmdline-tools") (Join-Path $SdkRoot "cmdline-tools\latest")
    Remove-Item $tmp -Recurse -Force -ErrorAction SilentlyContinue
    Remove-Item $ctZip -Force -ErrorAction SilentlyContinue
    Write-Host "命令行工具就绪" -ForegroundColor Green
}

# ---------------------------------------------------------------------------
# 3) 预先写入官方许可哈希（避免 sdkmanager 交互式询问）
# ---------------------------------------------------------------------------
Write-Step "写入 Android SDK 许可..."
$licDir = Join-Path $SdkRoot "licenses"
New-Item -ItemType Directory -Force $licDir | Out-Null
Set-Content -Path (Join-Path $licDir "android-sdk-license") `
    -Value "`n24333f8a63b6825ea9c5514f83c2829b004d1fee`n8933bad161af4178b1185d1a37fbf41ea5269c55`nd56f5187479451eabf01fb78af6dfcb131a6481e" `
    -NoNewline -Encoding ascii
Set-Content -Path (Join-Path $licDir "android-sdk-preview-license") `
    -Value "`n84831b9409646a918e30573bab4c9c91346d8abd" `
    -NoNewline -Encoding ascii

# ---------------------------------------------------------------------------
# 4) 安装 SDK 组件
# ---------------------------------------------------------------------------
Write-Step "安装 SDK 组件：$($AndroidPackages -join ', ')"
& $sdkManager --sdk_root="$SdkRoot" @AndroidPackages
if ($LASTEXITCODE -ne 0) { throw "SDK 组件安装失败" }
Write-Host "SDK 组件安装完成" -ForegroundColor Green

# ---------------------------------------------------------------------------
# 5) local.properties
# ---------------------------------------------------------------------------
Write-Step "写入 local.properties"
$sdkEsc = $SdkRoot -replace '\\', '\\' -replace ':', '\:'
@"
## Auto-generated by setup.ps1 - do NOT commit (already in .gitignore)
sdk.dir=$sdkEsc
"@ | Set-Content -Path (Join-Path $ScriptDir "local.properties") -Encoding ascii

# ---------------------------------------------------------------------------
# 6) 永久环境变量（用户级）
# ---------------------------------------------------------------------------
Write-Step "设置用户级永久环境变量 JAVA_HOME / ANDROID_HOME / PATH"
[Environment]::SetEnvironmentVariable("JAVA_HOME", $JdkHome, "User")
[Environment]::SetEnvironmentVariable("ANDROID_HOME", $SdkRoot, "User")
[Environment]::SetEnvironmentVariable("ANDROID_SDK_ROOT", $SdkRoot, "User")
$userPath = [Environment]::GetEnvironmentVariable("Path", "User")
$add = @((Join-Path $JdkHome "bin"), (Join-Path $SdkRoot "platform-tools"), (Join-Path $SdkRoot "cmdline-tools\latest\bin"))
foreach ($p in $add) {
    if (($userPath -split ';') -notcontains $p) { $userPath = ($userPath.TrimEnd(';') + ";" + $p) }
}
[Environment]::SetEnvironmentVariable("Path", $userPath, "User")

# ---------------------------------------------------------------------------
# 完成 / 可选构建
# ---------------------------------------------------------------------------
Write-Host "`n环境安装完成 ✅" -ForegroundColor Green
Write-Host "  JDK 17      : $JdkHome"
Write-Host "  Android SDK : $SdkRoot"
Write-Host "  注意：永久环境变量需【重开终端】后才在新窗口生效。" -ForegroundColor Yellow

if ($Build) {
    Write-Step "执行 gradlew.bat assembleDebug 验证打包..."
    Set-Location $ScriptDir
    & "$ScriptDir\gradlew.bat" assembleDebug
    if ($LASTEXITCODE -eq 0) {
        Write-Host "`n打包成功 ✅  APK 位置：app\build\outputs\apk\debug\app-debug.apk" -ForegroundColor Green
    } else {
        throw "打包失败"
    }
} else {
    Write-Host "`n下一步：重开终端后运行  .\gradlew.bat assembleDebug" -ForegroundColor Cyan
}
