# `pages/banner/` Banner 详情页（WebView）

用于展示运营 Banner 的 H5 详情内容（通过 `web-view` 打开）。

## 文件说明

- `detail.js`
  - 入参：`id`（bannerId）
  - URL：`services/config.H5_BASE_URL + /h5/banners/{id}`（见 `buildBannerH5Url`）
  - 顶部：使用 `ui-header` 提供返回按钮。
- `detail.wxml`：`<web-view src="{{src}}" />`。
- `detail.wxss`：flex 布局，保证 webview 占满剩余空间。
- `detail.json`：依赖 `ui-header`。

## 使用方式

- 页面跳转：`/pages/banner/detail?id=<bannerId>`

