# 没有 Android Studio 时如何编译 APK

如果你没有 Android Studio，推荐用 GitHub Actions 在线编译。

## 方法 A：GitHub Actions 在线编译，推荐

1. 在电脑上打开 https://github.com/new
2. 新建一个仓库，例如：

   hermes-desktop-card

3. 把本项目所有文件上传到仓库。

   注意：上传的是解压后的 hermes-desktop-card 文件夹里面的内容，不是 tar.gz 压缩包本身。

4. 仓库里已经包含这个文件：

   .github/workflows/build-apk.yml

   它会自动在线安装 Android SDK 并编译 APK。

5. 上传完成后，打开仓库页面的 Actions 标签。
6. 选择 “Build Android APK”。
7. 点击 “Run workflow”。
8. 等待编译完成。
9. 点进成功的构建记录，在页面底部 Artifacts 下载：

   hermes-desktop-card-debug-apk

10. 解压下载到的 artifact，里面会有：

   app-debug.apk

11. 把 app-debug.apk 传回手机安装。

## 方法 B：只装命令行工具，不装 Android Studio

如果你不想用 GitHub，可以在电脑上安装：

- JDK 17
- Gradle
- Android SDK Command-line Tools

然后在项目目录运行：

```bash
gradle wrapper
./gradlew assembleDebug
```

Windows 上是：

```bat
gradle wrapper
gradlew.bat assembleDebug
```

但这个方法比 GitHub Actions 麻烦，因为要手动配置 ANDROID_HOME 和 SDK。

## 方法 C：把项目发给有 Android Studio 的朋友代编译

让对方打开项目，Build APK，然后把 app-debug.apk 发回给你。
