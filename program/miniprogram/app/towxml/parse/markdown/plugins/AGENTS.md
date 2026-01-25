# `towxml/parse/markdown/plugins/` Markdown 插件（Towxml）

该目录包含 Towxml 的 markdown-it 插件实现，按需启用（由 `towxml/config.js` 控制）。

## 文件说明

- `sub.js`：下标语法支持。
- `sup.js`：上标语法支持。
- `ins.js`：删除线/插入线语法支持（依上游定义）。
- `mark.js`：文本高亮语法支持。
- `emoji.js`：emoji 语法支持。
- `todo.js`：todo list 语法支持（配合 `todogroup` 组件）。
- `latex.js`：LaTeX 语法支持（配合 `latex` 组件与远程解析服务）。
- `yuml.js`：yUML 语法支持（配合 `yuml` 组件与远程解析服务）。

