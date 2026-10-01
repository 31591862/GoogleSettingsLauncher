# Google 服务入口

这是一个极简 Android App，启动后直接拉起：

com.google.android.gms/com.google.android.gms.app.settings.GoogleSettingsIALink

适用于设备上存在并允许启动该 GMS Activity 的情况。

## 构建

本工程没有带 Gradle Wrapper（仓库里没有 `gradlew`），所以用本机已缓存的 Gradle 发行版直接构建。
当前配置：**AGP 9.3.2 + Gradle 9.5.0 + JDK 25（Android Studio 自带 JBR）+ compileSdk 35**。

在工程目录下执行（Windows PowerShell）：

    $env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
    & "$env:USERPROFILE\.gradle\wrapper\dists\gradle-9.5.0-bin\<哈希目录>\gradle-9.5.0\bin\gradle.bat" --offline assembleDebug

依赖已全部在本机 Gradle 缓存里，`--offline` 可离线构建；首次联网构建去掉该参数即可。

产物：

    app/build/outputs/apk/debug/app-debug.apk   （约 880 KB，debug 签名）

`local.properties` 记录本机 SDK 路径（`sdk.dir`），换机器时需改成自己的 SDK 位置，或用 `ANDROID_HOME` 环境变量。

## 应用图标

图标源图在 `vibe_images/`（AI 生成的四色齿轮 + 蓝色 G，1024px 整块不透明瓦片）。
资源不手工维护，由工具从源图派生：

    # 1) 生成 mipmap/drawable 各密度位图与 adaptive-icon 描述文件
    java tools/MakeIcons.java vibe_images/icon-variant-b-four-color-gear_*.png app/src/main/res
    # 2) 本地按启动器遮罩规则渲染，检查齿轮齿是否被裁
    java tools/Preview.java app/src/main/res vibe_images/icon-preview-masks.png

两条关键约定（都在 `MakeIcons.java` 顶部可调）：

- 源图的黑角与白底之间有一圈抗锯齿灰边，按"非黑非白"抠图会把灰边留下，
  所以抠图判据用的是**色度**（`max-min`）：中性灰/白/黑一律透明，只保留彩色像素。
- 自适应图标的 108dp 画布只有**中间 72dp** 会被显示（四周各 18dp 被遮罩裁掉），启动器是按
  1.5 倍放大画布来裁的。所以画布占比要按可见区反推：齿轮取画布 46%（=50dp）时约占可见区 69%，
  和相邻图标一致；曾经取 60%（=65dp）等于占满可见区 90%，桌面上明显比别的图标大一号。
  `Preview.java` 已按 1.5x 模型渲染，预览即真机比例。旧版位图（API 23/24/25）另存一份，
  它没有 1.5x 画布，直接用 68% 占比。

## 不在多任务列表留痕

`MainActivity` 同时用了两个手段，缺一不可：

- 清单 `android:excludeFromRecents="true"` —— 让启动器查询最近任务时跳过本任务的**新任务**；
- 代码 `finishAndRemoveTask()`（不是 `finish()`）—— 本 App 只是转发一下就退出，
  `finish()` 之后任务壳子（`Activities=[]`）仍会挂在 `dumpsys activity recents` 里，
  必须把任务本身移除。

验证方式（无需在手机上翻界面）：

    adb shell am start -n com.example.googlesettingslauncher/.MainActivity
    adb shell dumpsys activity recents | grep com.example.googlesettingslauncher

改之前会打出一条 `Recent #N: Task{... A=<uid>:com.example.googlesettingslauncher}`，
改之后 grep 无输出，而 `topResumedActivity` 仍是
`com.google.android.gms/.googlesettings.ui.GoogleSettingsActivity`（转发功能未受影响）。

## 已验证结果（2026-09-22，Redmi 2407FRK8EC / Android 16）

    adb install -r app/build/outputs/apk/debug/app-debug.apk
    adb shell am start -n com.example.googlesettingslauncher/.MainActivity

启动后前台 Activity 为 `com.google.android.gms/.googlesettings.ui.GoogleSettingsActivity`，
即"Google 服务"设置页正常打开，App 自身立即 `finish()`。

图标侧：`aapt2 dump badging` 确认 `icon='res/mipmap-anydpi-v26/ic_launcher.xml'`，
HyperOS 桌面（`com.miui.home`）在装包后确实重新生成了该应用图标。
注意 **HyperOS 的「应用信息」页顶栏图标来自 MIUI 自己的图标库/缓存，不做实时渲染**——
实测换成纯 PNG 图标后那一栏仍是系统默认的格子机器人，所以那一页不能用来判断图标是否生效，
要看桌面或应用抽屉（HyperOS 屏蔽 `adb shell input`，翻页需手动）。

## ADB 等价命令

adb shell am start -n com.google.android.gms/com.google.android.gms.app.settings.GoogleSettingsIALink

## 注意

不同版本的 Google Play 服务可能会修改、移除或限制这个内部 Activity。
如果该组件在某个版本不可导出或不存在，App 会提示无法打开。
