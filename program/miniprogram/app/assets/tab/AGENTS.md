# `assets/tab/` TabBar 图标说明

本目录存放自定义 TabBar 的图标资源（PNG）。由 `custom-tab-bar/index.wxml` 在不同选中态下切换使用。

## 文件一览

- `home.png` / `home-active.png`：首页「每日觉察」。
- `chat.png` / `chat-active.png`：Tab「马上沟通」。
- `me.png` / `me-active.png`：Tab「我的」。
- `test.png` / `test-active.png`：预留/历史（当前 TabBar 中未使用该图标对）。

## 使用方式

- 在 WXML 中通过三元表达式切换：`selected ? '...-active.png' : '....png'`。

