# `towxml/parse/highlight/` 代码高亮（Towxml）

Towxml 通过 highlight.js 实现代码高亮。是否启用由 `towxml/config.js` 的 `highlight` 数组决定。

## 文件说明

- `highlight.js`：highlight.js 的构建产物（上游打包文件）。
- `index.js`：读取 `config.highlight`，按语言列表注册语法（`require(...).default`）。

## 注意事项（本工程现状）

- 当前 `towxml/config.js` 将 `highlight` 置为空数组，并注明 Towxml 3.x 部分语言文件为 ESM，小程序环境无法直接 `require`。
- 如确需高亮：建议按 Towxml 官方方式“按需构建”得到兼容版本后整体替换本目录。

