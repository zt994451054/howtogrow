# `components/ui-header/` 自定义导航栏

用于 `navigationStyle: "custom"` 的页面顶部导航栏，负责处理状态栏高度/胶囊按钮遮挡问题，并提供左右按钮事件。

## 文件说明

- `index.json`：声明组件并依赖 `ui-icon`。
- `index.wxml`：布局（左按钮 / 中标题或 Logo / 右按钮）。
- `index.wxss`：sticky、毛玻璃背景、按钮样式等。
- `index.js`：
  - 计算 `statusBarHeight`、`navContentHeight`、`safeTopPaddingPx`。
  - `avoidMenu=true` 时，内容会下移到胶囊按钮下方，避免遮挡。
  - 事件：`left`、`right`（由页面处理导航/行为）。

## 使用方式

页面 `.json`：
- `"ui-header": "/components/ui-header/index"`

页面 WXML：
- `<ui-header title="标题" left="back" right="edit" bind:left="onBack" bind:right="onEdit" avoidMenu="{{true}}" />`

## 技术要点

- `getMenuButtonBoundingClientRect()` 在部分基础库/环境下可能返回异常值，组件已做兜底。
- 组件不直接调用 `wx.navigateBack()`，避免页面路由策略分散。

