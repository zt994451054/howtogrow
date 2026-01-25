# `styles/` 全局样式

本目录提供全局设计变量与通用布局类，供页面/组件复用。

## 文件说明

- `tokens.wxss`：设计 token（CSS 变量），包括品牌色、背景色、文本色、圆角、阴影等。
- `util.wxss`：通用布局工具类（flex row/col、居中、两端对齐、单行省略、hairline、tap 等）。

## 使用方式

- 已在 `app.wxss` 中全局 `@import`：
  - `@import "./styles/tokens.wxss";`
  - `@import "./styles/util.wxss";`

## 维护约定

- 新增全局颜色/尺寸优先加入 `tokens.wxss`，避免散落硬编码。
- 工具类尽量保持语义稳定，避免带业务含义的命名。

