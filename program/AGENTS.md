# Repository Guidelines（仓库指南）

## 项目结构与模块组织

本仓库以**部署编排 + 技术方案**为主；业务代码目录可能会在后续补齐/初始化（当前 Dockerfile 会在缺少 `backend/pom.xml` 或 `web/package.json` 时直接失败）。

- `backend/`：后端（Spring Boot）镜像构建；数据库基线在 `backend/db/`（`schema.sql`、`schema-关系说明.md`）。
- `web/`：PC Web（Vue + Vite）静态产物镜像 + `nginx.conf`。
- `miniprogram/`：微信小程序制品镜像（当前为占位/制品容器）。
- `deploy/`：`docker-compose.*.yml` 与 `.env*.example`（本地/生产编排与配置模板）。
- `*/ci/`：镜像 build/push 脚本。
- `技术方案-通用.md` 与 `*/技术方案.md`：架构、API、数据模型与约定。

## 构建、测试与开发命令

- 启动本地 MySQL（仅开发）：`docker compose -f deploy/docker-compose.dev.yml up -d`
- 初始化数据库（示例）：`mysql -h 127.0.0.1 -P 3306 -uroot -p123456 howtotalk < backend/db/schema.sql`
- 本地构建镜像：
  - 后端：`docker build -t howtogrow-backend:dev backend`
  - Web：`docker build -t howtogrow-web:dev web`
- 构建并推送镜像（需环境变量）：`REGISTRY=... NAMESPACE=... bash backend/ci/build-and-push-image.sh`（`web/`、`miniprogram/` 同理）。
- 生产编排（类生产）：复制 `deploy/.env.example` 为 `deploy/.env`，再执行 `docker compose --env-file deploy/.env -f deploy/docker-compose.prod.yml up -d`

## 代码风格与命名约定

- 后端：业务规则尽量收敛到 `domain/`，IO/第三方放到 `infrastructure/`（见 `backend/技术方案.md`）；API 前缀统一 `/api/v1/...`，响应建议 `{ code, message, data, traceId }`。
- 数据库：统一 `utf8mb4`；时间字段使用 `*_at`；金额使用 `*_cent`；所有表/字段必须写注释（见 `技术方案-通用.md`）。
- 前端：建议 Vue 3 + TypeScript；API Base 通过 `VITE_API_BASE_URL` 配置注入（见 `web/技术方案.md`）。

## 测试指南

当前仓库快照未提交具体测试框架/脚手架。引入非平凡逻辑或改动行为时：
- 后端：优先为 domain 规则补充单测，并运行 `mvn test`。
- Web：按项目实际选型补充组件/单测（落地后可通过 `npm test`/`npm run test` 运行）。

## 接口注释与文档（强制）

- 所有对外 HTTP 接口的请求/响应 DTO 字段必须添加 `@Schema(description = "...")` 字段说明（便于 OpenAPI 与联调）。
- 修改/新增接口或字段时，必须同步更新 `backend/API.md`；该文档作为 Web/小程序端联调与实现的统一依据。

## 提交与 PR 规范

当前目录不包含 Git 历史信息；建议使用 **Conventional Commits**（例如 `feat: add daily assessment submit guard`）。PR 需包含：变更摘要、配置变更说明（新增/调整 env 变量）、以及 UI 变更截图（如涉及前端）。

## 安全与配置提示

- 禁止提交真实密钥/Token；以 `deploy/.env.example`、`deploy/.env.dev.example` 作为模板，通过环境变量或文件挂载注入（尤其是微信支付私钥、AI API Key），并避免写入日志。
- `deploy/docker-compose.dev.yml` 中的数据库账号/密码仅用于本地开发示例；生产环境请使用强密码与最小权限账号，并优先使用外部托管数据库。
