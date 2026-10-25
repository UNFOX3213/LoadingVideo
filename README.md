# LoadingVideo

Play a local video with audio during Minecraft 1.12.2 mod loading. The video ends once, then returns to the normal loading screen if loading is still in progress. If loading finishes first, the main menu waits for the video.

## Builds

| Build | Loader | Runtime | Graphics |
| --- | --- | --- | --- |
| `loadingvideo-forge-1.12.2-0.2.0.jar` | Forge 1.12.2, bytecode checked on 14.23.5.2847 and 14.23.5.2864 | Java 8, LWJGL 2 | OpenGL 3.2+ |
| `loadingvideo-cleanroom-1.12.2-0.2.0.jar` | Cleanroom 0.6.13-alpha | Java 25, LWJGL 3 | OpenGL 3.2+ |

Install **one** build for your loader. These are experimental client coremods. They do not support Minecraft 1.20/1.21 or servers.

## Install / 安装

1. Close Minecraft and back up `config/splash.properties`.
2. Put the matching JAR in the instance's `mods/` folder.
3. Set `enabled=true` in `config/splash.properties`.
4. Install FFmpeg separately. Create `config/cleanroom-video/video.properties` using [the example](config-example/video.properties), and set `ffmpeg` to its executable path. Use forward slashes in Windows paths.
5. Copy your video to `config/cleanroom-video/video.mp4`.
6. Launch the game. Search the log for `First decoded RGB frame ready`, `First frame drawn`, and `Video ended; restored original loading screen`.

The existing configuration folder is named `cleanroom-video` on both builds for migration compatibility. No video or FFmpeg binary is distributed with this repository.

关闭游戏后，将对应加载器的 JAR 放入实例 `mods`，启用 `config/splash.properties` 中的 `enabled=true`，再配置 FFmpeg 路径并放入本地视频。播放结束后恢复原加载界面及进度条；如果加载已完成，则进入主菜单。请勿同时安装两端 JAR。

## Behavior and limits

- FFmpeg streams RGB frames through a pipe; frames are not decoded into an entire in-memory movie.
- Video output currently uses a fixed 640×360 canvas at 30 FPS, with aspect-ratio padding, then scales to the game window.
- Audio uses a separate FFmpeg process and Java Sound at 48 kHz stereo. Synchronization is approximate; there is no audio-clock correction, skip key, or volume setting yet.
- Video is drawn in the loader's existing rendering thread; the mod does not create another native game window.
- No video means the original splash remains. Decoder/render failure falls back to the splash and logs the error.
- Waiting for playback after loading completes is capped at ten minutes.
- Other mods that replace the splash thread may conflict. Unknown hook layouts are rejected rather than patched partially.
- For unsupported video/audio, inspect `config/cleanroom-video/video-error.log` and `audio-error.log`.

## Build

Use PowerShell and a JDK 25 compiler (`javac` and `jar` on PATH):

```powershell
# Standalone Forge build: downloads pinned compilation dependencies.
./build.ps1 -Target forge

# Use an existing Minecraft libraries directory; run Forge tests on Java 8.
./build.ps1 -Target forge -MinecraftLibraries 'D:/Minecraft/libraries' -Java8 'C:/Java8/bin/java.exe'

# Also run a small native LWJGL 2 render test (requires Windows and a GPU).
./build.ps1 -Target forge -MinecraftLibraries 'D:/Minecraft/libraries' -Java8 'C:/Java8/bin/java.exe' -RenderTest

# Build both. Cleanroom dependencies must be available locally.
./build.ps1 -Target all -MinecraftLibraries 'D:/Minecraft/libraries'
```

Artifacts appear in `dist/`. Forge compiles with `--release 8`. Neither JAR bundles Minecraft, Forge, LWJGL, or ASM. Dependency downloads are cached in `.deps/` and excluded from Git.

The Cleanroom offline build expects the actual 0.6.13-alpha loader JAR at `cleanroom-pcl2/net/minecraftforge/forge/1.12.2-14.23.5.2847/forge-1.12.2-14.23.5.2847.jar` under `-MinecraftLibraries`. This is the tested PCL layout. For another installation, copy the matching loader JAR to `.deps/cleanroom-0.6.13-alpha.jar`; other pinned LWJGL/ASM dependencies still need to be supplied in the libraries directory.

## Validation

- Forge: isolated Minecraft 1.12.2 / Forge 14.23.5.2864 startup with a four-second sample logged successful frame decode, drawing, return to the loading screen, and completed mod loading; compilation targeting Java 8; hook checks on the actual 2847/2864 loader JARs; missing-file fallback on Java 8; native LWJGL 2 OpenGL 3.2 core-profile pixel readback, state restoration, and return-to-loading checks on Java 8.
- Cleanroom: hook and missing-file checks; original build tested in a real Cleanroom 0.6.13-alpha modpack, including visible playback and transition to the main menu. Return to loading after EOF was confirmed by the user.
- Audio listening quality, long-video synchronization, third-party splash replacement compatibility, and crash/forced-window-close cleanup are not comprehensively tested.

## Restore

Close Minecraft, remove the LoadingVideo JAR, and restore your backed-up `splash.properties`. The `config/cleanroom-video/` folder may then be removed. No saved worlds are changed by this mod.
