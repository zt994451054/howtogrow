# `towxml/yuml/` 流程图组件（Towxml）

用于渲染 yUML 图。默认依赖 `towxml/config.js` 中配置的远程解析 API。

## 文件说明

- `yuml.json`：声明组件。
- `yuml.wxml` / `yuml.wxss`：图形展示结构与样式。
- `yuml.js`：组件逻辑（请求解析服务/渲染等，视上游实现）。

## 注意事项

- 上线前建议自建解析服务并替换配置，避免依赖公共服务。

