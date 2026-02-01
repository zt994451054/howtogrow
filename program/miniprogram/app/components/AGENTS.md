# `components/` 通用组件说明

本目录是页面复用组件集合。页面通过 `usingComponents` 引入（见各页面的 `.json`）。

## 子目录与职责

- `auth-modal/`：资料完善弹窗（头像 + 昵称），用于进入关键功能前的资料补全。
- `home-header/`：首页样式的顶部 Header（头像问候 + 菜单），供多个页面复用。
- `ui-header/`：自定义导航栏（适配安全区/胶囊按钮），替代系统导航栏。
- `ui-icon/`：轻量图标组件（字符 glyph 映射），用于 Header/按钮等。
- `ec-canvas/`：ECharts 适配层（vendored），用于曲线页图表绘制。

## 维护约定

- 组件对外只暴露最小必要的 `properties` 与事件（`triggerEvent`）。
- 页面如需调用后端，请通过 `services/*`，组件内也同样遵守该约定。
