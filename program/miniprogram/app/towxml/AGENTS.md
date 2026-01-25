# `towxml/`（vendored）Markdown/HTML 渲染库

本目录为第三方库 Towxml（`towxml/package.json` 版本 `3.0.6`），用于将 Markdown/HTML 转成小程序可渲染的 WXML 节点树。

当前工程主要使用场景：
- `pages/chat/index.js`：将 AI 回复（Markdown）转换为 nodes；
- `pages/chat/index.wxml`：通过 `<towxml nodes="{{item.nodes}}"></towxml>` 渲染富文本。

## 文件说明

- `index.js`：库的 JS 入口，导出 `(str, type, option) => nodes`。
  - `type: "markdown"`：先走 `parse/markdown`（markdown-it）再走 `parse`（HTML → nodes）
  - `type: "html"`：直接走 `parse`
- `towxml.js/.wxml/.wxss/.json`：小程序组件 `towxml`，负责根据 nodes 渲染。
- `decode.*`：Towxml 内部使用的 decode 组件（由 `towxml/towxml.json` 引用）。
- `config.js`：Towxml 渲染配置（启用的 markdown 插件、wxml 原生标签白名单、自定义组件列表、事件绑定方式等）。
- `style/`：正文样式与主题。
- `parse/`：解析核心（HTML 解析、Markdown 解析、实体/Tokenize 等）。
- `audio-player/`、`img/`、`table/`、`latex/`、`todogroup/`、`yuml/`：Towxml 自带的自定义组件实现。
- `README.md`：Towxml 官方说明（上游）。

## 技术要点（与本工程相关）

- 代码高亮：`towxml/config.js` 中 `highlight` 默认空数组；注释说明了 Towxml 3.x 部分语言文件是 ESM，小程序环境无法直接 `require`，如需高亮请按官方“按需构建”流程替换代码。
- LaTeX/YUML：`towxml/config.js` 配置了远程解析 API（上游默认）；若要上线使用需评估稳定性/合规，并建议自建服务。

## 维护约定

- 本目录属于 vendored 代码：不要在其中做业务逻辑或局部修补。
- 若需升级 Towxml：建议整体替换 `towxml/`，并回归验证聊天页渲染、样式、点击事件等。

