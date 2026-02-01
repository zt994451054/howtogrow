# `towxml/img/` 图片解析组件（Towxml）

用于 Towxml 渲染图片节点时的专用组件（支持样式、预览等）。

## 文件说明

- `img.json`：声明组件。
- `img.wxml` / `img.wxss`：图片展示结构与样式。
- `img.js`：组件逻辑（点击预览等，视上游实现）。

## 使用方式

- Towxml 解析到图片相关节点时会使用该组件（配置见 `towxml/config.js` 的 `components`）。

