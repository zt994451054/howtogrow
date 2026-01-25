# `towxml/audio-player/` 音频播放器组件（Towxml）

Towxml 提供的音频组件，用于替代/增强小程序原生 audio 在部分机型的兼容问题。

## 文件说明

- `audio-player.json`：声明组件。
- `audio-player.wxml`：播放器 UI（控制条/进度等）。
- `audio-player.wxss`：播放器样式。
- `audio-player.js`：组件逻辑（播放/暂停/进度控制等）。
- `Audio.js`：音频能力封装（Towxml 内部实现）。
- `loading.svg`：加载态资源。

## 使用方式

- Towxml 在解析到 `<audio>` 时会映射到 `audio-player` 组件（见 `towxml/parse/index.js` 的 `correspondTag`）。

## 维护约定

- 业务侧不要直接依赖内部实现细节；如需修改，优先通过升级 Towxml 解决。

