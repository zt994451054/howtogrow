# `towxml/latex/` 数学公式组件（Towxml）

用于渲染 LaTeX 公式。默认依赖 `towxml/config.js` 中配置的远程解析 API。

## 文件说明

- `latex.json`：声明组件。
- `latex.wxml` / `latex.wxss`：公式展示结构与样式。
- `latex.js`：组件逻辑（请求解析服务/渲染等，视上游实现）。

## 注意事项

- 上线前需评估远程解析服务的稳定性与数据合规，建议自建解析服务并替换配置。

