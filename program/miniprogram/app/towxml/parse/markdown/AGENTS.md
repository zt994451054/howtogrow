# `towxml/parse/markdown/` Markdown 解析（Towxml）

Towxml 使用 markdown-it 作为 Markdown 解析引擎，并根据 `towxml/config.js` 启用插件。

## 文件说明

- `markdown.js`：markdown-it 的构建产物（上游打包后的大文件）。
- `index.js`：
  - 配置 markdown-it（开启 html、breaks、typographer 等）
  - 如启用高亮，则注册 highlight 回调（本工程默认未启用）
  - 按 `towxml/config.js` 的 `markdown/components` 加载插件（从 `plugins/` 引入）
  - 定义 emoji 的渲染规则（输出 `<g-emoji>` 标签）

## 维护约定

- 如需增加 Markdown 语法扩展：优先通过新增/调整 `towxml/config.js` 的插件列表与 `plugins/` 实现。

