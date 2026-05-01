# 重新编译说明：HTTP 修正版

这版修复了安卓 App 无法访问 http://127.0.0.1:8000/chat 的常见问题。

## 修复内容

1. AndroidManifest.xml 增加：

```xml
android:usesCleartextTraffic="true"
android:networkSecurityConfig="@xml/network_security_config"
```

2. 新增：

```text
app/src/main/res/xml/network_security_config.xml
```

允许 App 访问 HTTP 明文地址。

3. 本机桥接服务改为监听：

```text
0.0.0.0:8000
```

但 App 里仍优先填写：

```text
http://127.0.0.1:8000/chat
```

## 你现在需要做

1. 把新版 zip 传到电脑：

```text
/sdcard/Download/AAA-hermes-httpfix.zip
```

2. 解压。
3. 上传解压后的 hermes-desktop-card 里面的内容到 GitHub 仓库。
4. GitHub Actions 里运行 Build Android APK。
5. 下载新的 app-debug.apk。
6. 手机上卸载旧的 Hermes 桌面卡片。
7. 安装新的 app-debug.apk。
8. App 里填写：

```text
API 地址：http://127.0.0.1:8000/chat
Token：留空
自定义指令：请只回复：收到
```

9. 点保存设置，再发送自定义指令。
