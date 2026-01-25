# `custom-tab-bar/` 自定义 TabBar

小程序在 `app.json.tabBar.custom=true` 时，会渲染该组件作为底部 TabBar。

## 文件说明

- `index.json`：声明组件。
- `index.wxml`：4 个 tab 项 + 中间悬浮按钮（快捷入口）。
- `index.wxss`：固定底部、居中按钮、选中态样式。
- `index.js`：
  - `selected`：当前 tab 索引（由各 tab 页在 `onShow` 中设置）。
  - `onTap`：`wx.switchTab` 切换 tab，并更新 `selected`。
  - `onMoodTap`（中间按钮）：
    - 优先从 `STORAGE_KEYS.navHomeSelectedChildId/navCurveSelectedChildId` 获取 childId；
    - 没有 childId 时提示并回到首页；
    - 有 childId 时跳到首页再 `navigateTo` 打开 `/pages/home/detail?open=status`（当天育儿状态 sheet）。

## 使用方式

- `app.json` 已配置：`"usingComponents": { "custom-tab-bar": "custom-tab-bar/index" }`
- tab 页在 `onShow` 中调用：
  - `const tab = this.getTabBar && this.getTabBar(); tab && tab.setData({ selected: <index> })`

## 技术要点

- 先 `switchTab` 再 `navigateTo`：避免在 tab 页堆叠栈上直接 push 导致返回栈异常。

