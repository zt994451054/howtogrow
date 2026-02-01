# `towxml/parse/parse2/` HTML 解析器（Towxml）

Towxml 内部的 HTML 解析实现（类 htmlparser2），用于将 HTML 字符串解析为节点树，供 `towxml/parse/index.js` 转换成 WXML nodes。

## 文件说明

- `index.js`：解析入口（组装 Tokenizer/Parser）。
- `Tokenizer.js`：将输入流拆分为 token。
- `Parser.js`：消费 token 并构建节点结构。
- `domhandler/`：DOM handler（节点结构与遍历）。
- `entities/`：HTML entity 编解码与映射表。

