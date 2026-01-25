# `services/__tests__/` 说明

该目录包含服务层的轻量级 Node 测试脚本（非 Jest 框架）。

## 文件说明

- `chat.test.js`：
  - 使用 `assert` + 模拟 `global.wx` 来测试 `services/chat.streamChat()`；
  - 覆盖场景：
    - HTTP 401：应 toast “未登录”、abort request、触发 onError；
    - SSE `event:error`（携带 `{"code":"UNAUTHORIZED","message":"未登录"}`）：应清理 token/me、toast、abort。

## 运行方式

在仓库根（或本目录）执行：
- `node services/__tests__/chat.test.js`

