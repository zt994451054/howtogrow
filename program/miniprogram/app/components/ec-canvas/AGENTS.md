# `components/ec-canvas/` ECharts 适配层（vendored）

该目录来自 `echarts-for-weixin` 生态的适配实现，用于在微信小程序 Canvas 上运行 ECharts。

## 文件说明

- `ec-canvas.js`：核心组件逻辑
  - 自动判断基础库版本，优先使用 `canvas type="2d"`（>= 2.9.0），否则回退旧 Canvas。
  - 对 ECharts 做预处理：关闭 `series.progressive`（避免 `drawImage` 参数限制导致异常）。
  - 暴露 `init` / `canvasToTempFilePath` 等能力，供页面 `bind:init` 初始化图表实例。
- `ec-canvas.wxml` / `ec-canvas.wxss` / `ec-canvas.json`：组件声明与样式。
- `wx-canvas.js`：对 Canvas API 的封装，桥接触摸事件到 zrender handler。
- `echarts.js`：ECharts 构建产物（体积大、已压缩）。

## 使用方式（页面侧）

页面 `.json`：
- `"ec-canvas": "/components/ec-canvas/ec-canvas"`

页面 WXML：
- `<ec-canvas canvas-id="xxx" ec="{{ec}}" bind:init="onEcInit" />`

页面 JS：
- 在 `onEcInit(e)` 中 `echarts.init(canvas, null, { width, height, devicePixelRatio })` 并 `canvas.setChart(chart)`。

## 维护约定

- 本目录属于第三方/半第三方代码：不要进行“零散修改”。如需升级请整体替换并做回归（曲线页、图表交互、截图导出等）。

