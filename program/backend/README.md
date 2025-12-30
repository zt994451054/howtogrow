# 后端开发指南（MVP）

## 依赖
- Java 17（推荐 21+）
- Maven
- MySQL 8.0（可用 `deploy/docker-compose.dev.yml` 启动）

## 本地启动（示例）

1) 启动 MySQL：
```bash
docker compose -f ../deploy/docker-compose.dev.yml up -d
```

如需“重置并重建表结构”（会清空本地 MySQL volume）：
```bash
bash ../deploy/scripts/db-reset-dev.sh
```

2) 初始化表结构（第一次需要）：
```bash
mysql -h 127.0.0.1 -P 3306 -uroot -p123456 howtotalk < db/schema.sql
```

3) 设置必须的环境变量（示例值请自行替换为强随机值）：
```bash
export JWT_SECRET_MINIPROGRAM='...至少32字符...'
export JWT_SECRET_ADMIN='...至少32字符...'
```

AI（OpenAI 兼容网关）环境变量（不要写入代码/仓库）：
```bash
export OPENAI_API_BASE='https://...'
export OPENAI_API_KEY='...'
export OPENAI_MODEL='...'
```

4) 运行：
```bash
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

## 数据库初始化方式
- 以 `backend/db/schema.sql` 为准（包含默认 `admin/admin` 超级管理员 seed）。
- `deploy/docker-compose.dev.yml`：首次启动 MySQL 会自动执行 `backend/db/schema.sql` 建表与初始化数据。

## 已实现接口（按技术方案）
- 小程序登录：`POST /api/v1/miniprogram/auth/wechat-login`（`dev` 下支持 `code=mock:<openid>`）
- 用户信息：`GET /api/v1/miniprogram/me`
- 孩子管理：`/api/v1/miniprogram/children`
- 每日自测：`/api/v1/miniprogram/assessments/daily/begin|sessions/{sessionId}/replace|sessions/{sessionId}/submit`
- 成长报告：`GET /api/v1/miniprogram/reports/growth?childId=...&from=YYYY-MM-DD&to=YYYY-MM-DD`
- 鸡汤语：`GET /api/v1/miniprogram/quotes/random`
- AI 自测总结：`POST /api/v1/miniprogram/assessments/daily/{id}/ai-summary`（需要订阅；`dev` 默认 mock）
- AI 实时对话：`/api/v1/miniprogram/ai/chat/sessions|sessions/{id}/messages|sessions/{id}/stream`（需要订阅；`dev` 默认 mock）
- 订阅套餐/下单：`GET /api/v1/miniprogram/subscriptions/plans`、`POST /api/v1/miniprogram/subscriptions/orders`
- 微信支付回调（当前 mock 幂等落库/发放骨架）：`POST /api/v1/pay/wechat/notify`

## 运营端接口（MVP）
- 运营登录：`POST /api/v1/admin/auth/login`
- 运营信息：`GET /api/v1/admin/auth/me`
- 年龄段/维度：`/api/v1/admin/age-groups`、`/api/v1/admin/dimensions`
- 题库：`GET /api/v1/admin/questions`、`GET /api/v1/admin/questions/{id}`、`POST /api/v1/admin/questions/import-excel`
- 题库写接口：`POST /api/v1/admin/questions`、`PUT /api/v1/admin/questions/{id}`、`DELETE /api/v1/admin/questions/{id}`
- 用户/订单/自测查询：`/api/v1/admin/users`、`/api/v1/admin/orders`、`/api/v1/admin/assessments`
- 套餐/鸡汤语：`/api/v1/admin/plans`、`/api/v1/admin/quotes`
- RBAC 管理：`/api/v1/admin/rbac/*`（需要 `RBAC:MANAGE` 权限）

## 说明
- 本仓库当前以“技术方案 + 部署编排”为主，业务实现按 `backend/技术方案.md` 逐步补齐。
- 微信支付 v3 的验签/解密已基于 WxJava 实现；生产环境需正确配置商户私钥、证书序列号与 API v3 Key。
- 运营端接口已启用“权限码校验”（见 `backend/db/schema.sql` 中的 `admin_permission` seed），可通过 `admin/admin` 登录后调用 `/api/v1/admin/rbac/*` 管理其他管理员/角色/权限。
- AI 相关接口已启用简单限流：
  - 每分钟对话次数：环境变量 `AI_CHAT_PER_MINUTE`（默认 60）
  - 每日总结次数：环境变量 `AI_SUMMARY_PER_DAY`（默认 10）

## 接口文档
- 统一参考：`backend/API.md`
- 机器可读 OpenAPI：`GET /v3/api-docs`（开发时可访问 `GET /swagger-ui/index.html`）
