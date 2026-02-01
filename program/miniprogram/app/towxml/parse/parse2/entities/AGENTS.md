# `towxml/parse/parse2/entities/` HTML Entities（Towxml）

处理 HTML entity 的编码/解码（例如 `&amp;`、`&#x1F600;` 等）。

## 文件说明

- `index.js`：入口与导出。
- `decode.js` / `decode_codepoint.js`：解码实现（含 codepoint 处理）。
- `encode.js`：编码实现。
- `maps/`：实体映射表（legacy/xml/entities/decode）。

