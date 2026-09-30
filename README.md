# 遥控器

一个用手机自带红外发射器（IR Blaster）控制格力空调的 Android App。用 Jetpack Compose 写的单页遥控界面，按下按键即刻发射红外码，并支持按键音与状态记忆。

![Uploading PixPin_2026-10-01_01-35-05.png…]()


## 功能

- 开关机、温度调节（16 ℃ ~ 30 ℃）
- 模式切换：自动 / 制冷 / 制热 / 除湿 / 送风
- 风速档位循环：自动 / 低 / 中 / 高
- 风向设置：自动扫风、固定上 / 中 / 下
- 按键音选择（可选多种提示音）
- 空调状态与设置自动保存，下次打开恢复上次状态

## 界面

单页布局，从上到下依次为：大字号目标温度、模式 / 风速 / 风向状态栏、开关与扫风按钮、温度加减条、模式选择圆钮，底部为当前状态的文字描述。

## 技术栈

| 项目 | 版本 / 说明 |
| --- | --- |
| 语言 | Kotlin |
| UI | Jetpack Compose + Material 3 |
| 架构 | 单 Activity + AndroidViewModel + Compose State |
| 最低版本 | minSdk 30（Android 11） |
| 编译版本 | compileSdk / targetSdk 37 |
| 构建 | Gradle（Kotlin DSL）+ AGP 9.4.1 + Kotlin 2.2.10 |
| 红外 | `android.hardware.ConsumerIrManager`，38 kHz 载波 |

## 环境要求

- Android Studio（支持 AGP 9.x 的版本）
- JDK 11 及以上
- 一台**带红外发射器**的手机（如小米、华为、荣耀等部分机型）。模拟器和没有红外灯的机型只能看界面，无法真正控制空调。

## 编译运行

```bash
# 克隆后直接用 Android Studio 打开项目根目录即可
./gradlew assembleDebug          # 编译 Debug APK
./gradlew installDebug           # 安装到已连接的设备
./gradlew test                   # 运行单元测试
```

生成的 APK 位于 `app/build/outputs/apk/debug/`。

> `local.properties` 中的 SDK 路径由 Android Studio 自动生成，每个人的机器不同，不需要提交到仓库。

## 项目结构

```
app/src/main/java/com/example/myapplication/
├── MainActivity.kt                  # 入口 Activity
├── GreeRemoteViewModel.kt           # 空调状态管理、持久化、发射调度
├── ir/
│   ├── IrBlaster.kt                 # 红外发射封装（ConsumerIrManager）
│   ├── GreeCodec.kt                 # 格力 8 字节协议编码
│   └── YapofCodec.kt                # 格力新款 16 字节（Kelvinator）协议编码
└── ui/
    ├── GreeRemoteScreen.kt          # 遥控器主界面
    ├── RemoteFeedback.kt            # 按键音反馈
    └── theme/                       # 主题配色与字体
```

## 红外协议说明

项目里实现了两套格力红外编码：

- **GreeCodec**：常见的 8 字节状态帧（YB1F2 / YAA 系列遥控），带 9000/4500 µs 引导码、按位 LSB 发送、帧尾校验和（校验和初值为 10）。
- **YapofCodec**：格力新款遥控（YAPOF3 / YAP0F8 / YAP2F）使用的 16 字节 Kelvinator 协议，分两个数据块发送，不是网上常见的 8 字节短码。

当前 `IrBlaster` 实际发送的是 `YapofCodec` 的 16 字节码。如果你的空调是较老的 8 字节机型，把 `IrBlaster.transmit()` 里的 `YapofCodec.toPulsePattern(state)` 换成 `GreeCodec.toPulsePattern(state)`，并按需调整 `GreeRemoteModel` 即可。

## 已知限制

- 仅适配格力壁挂机，其他品牌空调无法使用。
- 需要硬件红外发射器，App 会在没有红外灯时给出提示而不是静默失败。
- 温度、模式等状态由 App 本地维护，无法读取空调真实状态（红外是单向协议）。

## 许可

本项目仅供学习与个人使用。红外编码参考了 IRremoteESP8266 与 Kelvinator 协议的公开资料，使用时请遵守对应开源协议。
