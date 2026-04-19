# TV音乐播放器 🎵

一款专为 Android TV 设计的本地音乐播放器，完整支持遥控器 D-Pad 导航。

## 功能特性

- 📂 **自动扫描** 内置存储和外部存储（U盘/TF卡）中的音频文件
- 🖼️ **封面显示** 歌曲列表和播放页均显示专辑封面（内嵌/文件夹封面）
- 🏷️ **音质标签** 自动识别并显示 FLAC / Hi-Res / WAV / 320K / 128K 等音质
- 📝 **歌词同步** 支持 `.lrc` 歌词文件，播放时自动高亮滚动
- ❤️ **收藏管理** 长按歌曲收藏，收藏页快速访问
- 📋 **播放队列** 完整队列管理，支持随机/循环模式
- 🔍 **实时搜索** 按标题/艺术家/专辑搜索，300ms 防抖
- 👤 **艺术家分类** 按艺术家浏览，显示专辑数和歌曲数
- ↕️ **多种排序** 按标题/艺术家/专辑/日期/时长/文件大小排序
- 🎮 **遥控器优化** 完整 D-Pad 焦点导航，高亮动画流畅

## 支持格式

MP3 · FLAC · AAC · OGG · WAV · M4A · OPUS · APE · WMA

## 系统要求

- Android TV 7.0 (API 24) 或更高
- 存储读取权限

## 下载安装

从 [Releases](../../releases) 页面下载对应架构的 APK：

| 文件 | 架构 | 适用设备 |
|------|------|---------|
| `TVMusic-arm64-v8a.apk` | 64位 ARM | 大多数现代 Android TV |
| `TVMusic-armeabi-v7a.apk` | 32位 ARM | 旧款 Android TV |

```bash
# ADB 安装
adb install TVMusic-arm64-v8a.apk
```

## 构建方法

### 本地构建

```bash
# 1. 克隆项目
git clone https://github.com/yourname/AndroidTVMusic.git
cd AndroidTVMusic

# 2. 生成签名 keystore（已内置，可跳过）
keytool -genkey -v -keystore keystore/tvmusic.jks \
  -alias tvmusic -keyalg RSA -keysize 2048 -validity 10000 \
  -storepass tvmusic123 -keypass tvmusic123 \
  -dname "CN=TVMusic, OU=Dev, O=TVMusic, L=CN, S=CN, C=CN"

# 3. 构建 Release APK（同时输出 arm64 和 arm32）
./gradlew assembleRelease

# 输出路径：
# app/build/outputs/apk/release/TVMusic-1.0.0-arm64-v8a.apk
# app/build/outputs/apk/release/TVMusic-1.0.0-armeabi-v7a.apk
```

### GitHub Actions 自动构建

推送 tag 即可触发自动构建并发布 Release：

```bash
git tag v1.0.0
git push origin v1.0.0
```

## 项目结构

```
app/src/main/
├── java/com/tvmusic/app/
│   ├── data/
│   │   ├── db/          # Room 数据库（Song / Favorite / Queue）
│   │   ├── model/       # 数据模型
│   │   └── repository/  # 数据仓库
│   ├── media/
│   │   ├── scanner/     # MediaStore 音频扫描
│   │   ├── metadata/    # 封面提取 / LRC 歌词解析
│   │   └── player/      # Media3 ExoPlayer 封装
│   └── ui/
│       ├── main/        # MainActivity + 侧边栏导航
│       ├── list/        # 歌曲列表（排序/封面/音质）
│       ├── player/      # 播放页（封面+歌词+控制）
│       ├── artists/     # 艺术家分类
│       ├── favorites/   # 收藏列表
│       ├── queue/       # 播放队列
│       └── search/      # 实时搜索
└── res/
    ├── layout/          # 所有 XML 布局
    ├── drawable/        # 矢量图标 + 焦点 selector
    ├── values/          # 颜色 / 字符串 / 主题
    └── menu/            # 排序菜单
```

## 依赖库

| 库 | 版本 | 用途 |
|----|------|------|
| Media3 ExoPlayer | 1.3.0 | 音频播放引擎 |
| Leanback | 1.2.0-alpha04 | Android TV UI |
| Room | 2.6.1 | 本地数据库 |
| Glide | 4.16.0 | 封面图片加载 |
| Kotlin Coroutines | 1.7.3 | 异步处理 |

## 许可证

MIT License
