# `towxml/parse/` 解析核心（Towxml）

该目录负责将 Markdown/HTML 转为 Towxml 的 nodes 结构，再由组件渲染。

## 文件说明

- `index.js`：HTML → nodes 的主入口
  - 使用 `parse2`（类 htmlparser2）把 HTML 转为 DOM-like 结构
  - 根据 `towxml/config.js` 的 `wxml/components` 做标签映射（例如 `a → navigator`、`audio → audio-player`）
  - 处理 class 注入（`h2w__${tag}`）与资源相对路径（`option.base`）
  - 处理事件绑定（写入 `global._events`）
- `markdown/`：Markdown 解析（markdown-it + plugins）
- `highlight/`：代码高亮（highlight.js 适配）
- `parse2/`：HTML tokenizer/parser + entities + domhandler

## 与本工程的关系

聊天页仅使用 Markdown 渲染，依赖链为：`towxml/index.js` → `parse/markdown` → `parse/index.js`。

