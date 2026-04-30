# Hermes 桌面卡片 Android Widget

这是一个最小可用的安卓桌面卡片项目，可以把常用指令从手机桌面发送到 Hermes Agent 的 HTTP/API 服务。

项目位置：
`/data/data/com.termux/files/home/hermes-desktop-card`

## 功能

- 安卓桌面 Widget：4x2 卡片
- 两个快捷按钮：
  - “状态”：询问 Hermes 当前状态和可用工具
  - “总结”：总结最近会话和待办事项
- 设置页面：
  - 配置 API 地址
  - 配置 Bearer Token
  - 发送自定义指令
- Widget 会显示最近一次请求结果或错误

## 重要：API 地址

默认地址是：

```text
http://127.0.0.1:8000/chat
```

如果安卓手机要控制桌面电脑上的 Hermes，不能用 127.0.0.1，因为那代表手机自己。请改成电脑的局域网 IP，例如：

```text
http://192.168.1.23:8000/chat
```

## Hermes 侧需要提供 HTTP 接口

这个 Widget 发送 POST 请求：

```http
POST /chat
Content-Type: application/json
Authorization: Bearer <可选token>

{"message":"你的指令"}
```

你可以用 Hermes 的 gateway API server，或自己包一层小服务，把请求转发给：

```bash
hermes chat -q "收到的 message"
```

如果使用 Hermes Gateway/API Server，请在 Hermes 中运行：

```bash
hermes gateway setup
hermes gateway run
```

然后按你的 API Server 配置填写地址和 token。

## 构建

在有 Android SDK 的机器上运行：

```bash
cd /data/data/com.termux/files/home/hermes-desktop-card
./gradlew assembleDebug
```

如果本目录还没有 gradle wrapper，可以在安装了 Gradle 的环境中运行：

```bash
gradle wrapper
./gradlew assembleDebug
```

生成 APK 一般在：

```text
app/build/outputs/apk/debug/app-debug.apk
```

## 安装和使用

1. 安装 APK 到手机。
2. 打开 “Hermes 桌面卡片” 应用。
3. 把 API 地址改成桌面电脑的 Hermes API 地址。
4. 长按安卓桌面空白处，选择“小组件/Widgets”。
5. 添加 “Hermes 桌面卡片”。
6. 点击卡片按钮即可发送指令。

## 下一步可扩展

- 把两个快捷按钮改成用户自定义指令
- 增加语音输入
- 增加会话列表
- 增加 HTTPS 和局域网自动发现
