# HowToGrow 运营后台（Web）

## 开发

1. 启动后端（默认 `http://127.0.0.1:8080`）
2. 安装依赖：`npm install`
3. 启动：`npm run dev`

本项目使用运行时 `/config.js` 注入 API base：
- 本地开发：`program/web/public/config.js`
- Docker/Nginx：容器启动时会生成 `/usr/share/nginx/html/config.js`（见 `program/web/docker-entrypoint.d/10-gen-config.sh`）

## 常见问题

### 启动报错：esbuild 平台不匹配
现象：提示类似 “installed esbuild on another platform… needs esbuild-darwin-arm64 but esbuild-darwin-64 is present”（或反过来）。

原因：当前 Node 的架构（`arm64`/`x64`）与 `node_modules` 里安装的 esbuild 二进制不一致（常见于 Intel/Apple Silicon 切换、Rosetta、或复制了 node_modules）。

修复（推荐）：在 `program/web` 下执行：
- `npm run reinstall`

说明：`npm run dev` / `npm run build` 已内置预检脚本，检测到不匹配会自动执行上述重装。
