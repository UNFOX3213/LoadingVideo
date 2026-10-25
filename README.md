# LoadingVideo

[中文](#中文说明) · [English](#english)

## 中文说明

在 Minecraft 1.12.2 模组加载期间播放带声音的本地视频。视频播完后，如果模组仍在加载，则恢复原加载界面和进度条；如果加载已经完成，则进入主菜单。

### 下载与版本选择

从 [Releases](https://github.com/UNFOX3213/LoadingVideo/releases) 下载适合加载器的 JAR，**两端仅安装一个**。

| 文件 | 加载器 | 运行环境 | 显卡要求 |
| --- | --- | --- | --- |
| `loadingvideo-forge-1.12.2-0.2.0.jar` | Forge 1.12.2，已检查 14.23.5.2847 / 14.23.5.2864 的注入点 | Java 8、LWJGL 2 | OpenGL 3.2+ |
| `loadingvideo-cleanroom-1.12.2-0.2.0.jar` | Cleanroom 0.6.13-alpha | Java 25、LWJGL 3 | OpenGL 3.2+ |

这是实验性客户端核心模组，不支持服务器或 Minecraft 1.20 / 1.21。

### 安装

1. 关闭游戏，备份实例的 `config/splash.properties`。
2. 将对应 JAR 放入该实例的 `mods/`。
3. 将 `config/splash.properties` 中的 `enabled` 设为 `true`。
4. 单独安装 FFmpeg，按[配置示例](config-example/video.properties)创建 `config/cleanroom-video/video.properties`，填写 FFmpeg 可执行文件路径。Windows 路径使用正斜杠。
5. 将视频放到 `config/cleanroom-video/video.mp4`，然后启动游戏。

```properties
ffmpeg=C:/tools/ffmpeg/bin/ffmpeg.exe
video=video.mp4
```

`video` 的相对路径以 `config/cleanroom-video/` 为基准。为方便迁移，两端均沿用此配置目录名。仓库和发行包不包含视频或 FFmpeg 程序。

### 行为与限制

- 视频与模组加载并行；加载先完成时等待视频，视频先结束时恢复加载界面。
- FFmpeg 流式输出视频帧，不会将整部视频解码到内存。
- 当前以 640×360、30 FPS 解码，按比例加黑边，再缩放到游戏窗口。
- 音轨由独立 FFmpeg 进程解码，通过 Java Sound 以 48 kHz 双声道播放。目前为近似同步，长视频可能发生音画漂移。
- 尚未提供跳过键、音量设置或音频主时钟校正。
- 在加载器原有渲染线程绘制，不额外创建游戏窗口。
- 视频缺失时使用原加载界面；解码或绘制失败会记录错误并回退。
- 加载完成后等待视频的时间上限为十分钟。
- 替换加载线程的其他加载画面模组可能冲突。无法识别的注入点会被拒绝，避免只完成部分修改。

排查时，在游戏日志中搜索 `First decoded RGB frame ready`、`First frame drawn`、`Video ended; restored original loading screen`，并检查 `config/cleanroom-video/video-error.log` 和 `audio-error.log`。

### 构建

需要 PowerShell 和 JDK 25，确保 `javac`、`jar` 在 PATH 中。Forge 产物使用 `--release 8` 编译。

```powershell
# 独立构建 Forge 版，自动下载固定版本的编译依赖。
./build.ps1 -Target forge

# 使用本地 Minecraft 库，并在 Java 8 下运行检查。
./build.ps1 -Target forge -MinecraftLibraries 'D:/Minecraft/libraries' -Java8 'C:/Java8/bin/java.exe'

# 额外运行 LWJGL 2 渲染测试，需要 Windows 和显卡环境。
./build.ps1 -Target forge -MinecraftLibraries 'D:/Minecraft/libraries' -Java8 'C:/Java8/bin/java.exe' -RenderTest

# 构建两端；Cleanroom 依赖需要从本地提供。
./build.ps1 -Target all -MinecraftLibraries 'D:/Minecraft/libraries'
```

产物位于 `dist/`，下载依赖缓存于 `.deps/`。JAR 不打包 Minecraft、Forge、LWJGL 或 ASM。

Cleanroom 构建使用已验证的 PCL 库目录布局：`-MinecraftLibraries` 下的 `cleanroom-pcl2/net/minecraftforge/forge/1.12.2-14.23.5.2847/forge-1.12.2-14.23.5.2847.jar` 必须是实际的 Cleanroom 0.6.13-alpha 加载器。其他布局可将对应加载器 JAR 放到 `.deps/cleanroom-0.6.13-alpha.jar`，其余固定版本 LWJGL / ASM 依赖仍需在库目录中提供。

### 验证范围与恢复

- **Forge：**Java 8 编译目标及运行检查，2847 / 2864 实际加载器注入点检查，视频缺失回退，LWJGL 2 核心模式像素回读、状态恢复和播完返回加载界面测试。独立 Forge 14.23.5.2864 实例启动日志确认了测试视频解码、绘制、恢复加载界面和模组加载完成。
- **Cleanroom：**注入点和视频缺失检查；已在真实 Cleanroom 0.6.13-alpha 整合包中观察到视频播放并正常进入主菜单，用户已确认播完返回加载界面可用。
- 音频听感、长视频同步、第三方加载画面兼容性，以及崩溃或强制关闭窗口时的清理，尚未全面测试。

卸载时关闭游戏，移除 LoadingVideo JAR，恢复备份的 `splash.properties`。之后可移除 `config/cleanroom-video/`。本模组不修改世界存档。

## English

Play a local video with audio while Minecraft 1.12.2 loads mods. When playback ends, the normal loading screen and progress bars return if loading is still in progress. If loading finishes first, the main menu waits for the video.

### Downloads and compatibility

Download **one** matching JAR from [Releases](https://github.com/UNFOX3213/LoadingVideo/releases).

| File | Loader | Runtime | Graphics |
| --- | --- | --- | --- |
| `loadingvideo-forge-1.12.2-0.2.0.jar` | Forge 1.12.2; hooks checked on 14.23.5.2847 / 14.23.5.2864 | Java 8, LWJGL 2 | OpenGL 3.2+ |
| `loadingvideo-cleanroom-1.12.2-0.2.0.jar` | Cleanroom 0.6.13-alpha | Java 25, LWJGL 3 | OpenGL 3.2+ |

These are experimental client coremods. Servers and Minecraft 1.20 / 1.21 are unsupported. Do not install both builds together.

### Installation

1. Close Minecraft and back up the instance's `config/splash.properties`.
2. Place the matching JAR in that instance's `mods/` directory.
3. Set `enabled=true` in `config/splash.properties`.
4. Install FFmpeg separately. Create `config/cleanroom-video/video.properties` using [the example](config-example/video.properties), and set `ffmpeg` to its executable path. Use forward slashes in Windows paths.
5. Copy your video to `config/cleanroom-video/video.mp4`, then launch the game.

```properties
ffmpeg=C:/tools/ffmpeg/bin/ffmpeg.exe
video=video.mp4
```

Relative video paths resolve against `config/cleanroom-video/`. Both builds retain this folder name for migration compatibility. No video or FFmpeg executable is distributed with the repository or releases.

### Behavior and limitations

- Loading and playback run in parallel. The normal splash returns after playback; loading completion waits for an unfinished video.
- FFmpeg streams frames without decoding the entire movie into memory.
- Output currently uses a fixed 640×360 canvas at 30 FPS, with aspect-ratio padding, then scales to the game window.
- A separate FFmpeg process decodes audio for Java Sound playback at 48 kHz stereo. Synchronization is approximate and may drift during long videos.
- No skip key, volume setting, or audio-clock correction is available yet.
- Rendering runs in the loader's existing thread without creating another game window.
- A missing video keeps the original splash. Decoder or renderer failures log an error and fall back to it.
- Waiting for playback after loading completes is capped at ten minutes.
- Other splash replacements may conflict. Unknown hook layouts are rejected rather than patched partially.

For troubleshooting, search the game log for `First decoded RGB frame ready`, `First frame drawn`, and `Video ended; restored original loading screen`. Also inspect `config/cleanroom-video/video-error.log` and `audio-error.log`.

### Building

Use PowerShell and JDK 25, with `javac` and `jar` on PATH. Forge compiles with `--release 8`.

```powershell
# Standalone Forge build; downloads pinned compilation dependencies.
./build.ps1 -Target forge

# Use local Minecraft libraries and run checks on Java 8.
./build.ps1 -Target forge -MinecraftLibraries 'D:/Minecraft/libraries' -Java8 'C:/Java8/bin/java.exe'

# Also run the LWJGL 2 rendering test; requires Windows and a GPU.
./build.ps1 -Target forge -MinecraftLibraries 'D:/Minecraft/libraries' -Java8 'C:/Java8/bin/java.exe' -RenderTest

# Build both targets; supply Cleanroom dependencies locally.
./build.ps1 -Target all -MinecraftLibraries 'D:/Minecraft/libraries'
```

Artifacts appear in `dist/`; dependency downloads are cached in `.deps/`. Neither JAR bundles Minecraft, Forge, LWJGL, or ASM.

The Cleanroom build expects the tested PCL layout: `cleanroom-pcl2/net/minecraftforge/forge/1.12.2-14.23.5.2847/forge-1.12.2-14.23.5.2847.jar` under `-MinecraftLibraries` must contain the actual Cleanroom 0.6.13-alpha loader. For other layouts, copy that loader to `.deps/cleanroom-0.6.13-alpha.jar`; the remaining pinned LWJGL / ASM dependencies must still be supplied in the libraries directory.

### Validation and removal

- **Forge:** Java 8 compilation target and runtime checks; hooks checked on actual 2847 / 2864 loader JARs; missing-video fallback; LWJGL 2 core-profile pixel readback, state restoration, and return-to-loading tests. An isolated Forge 14.23.5.2864 startup logged successful sample-video decoding, drawing, return to the splash, and completed mod loading.
- **Cleanroom:** hook and missing-video checks; visible playback and transition to the main menu in a real Cleanroom 0.6.13-alpha modpack. The user confirmed return to the loading screen after playback.
- Listening quality, long-video synchronization, third-party splash compatibility, and crash or forced-window-close cleanup are not comprehensively tested.

To remove, close Minecraft, remove the LoadingVideo JAR, and restore your backed-up `splash.properties`. You may then remove `config/cleanroom-video/`. The mod does not modify saved worlds.
