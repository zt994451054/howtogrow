# `pages/chat/` 马上沟通（AI 对话）

本目录实现 AI 咨询对话页（会话列表、历史消息、SSE 流式输出、快捷提问）。

## 文件说明

- `index.js`
  - 会话：`listChatSessions()` 拉取最近会话；抽屉侧边栏选择/新建会话；
  - 消息：`listChatMessages(sessionId, limit, beforeMessageId)` 分页加载，向上滚动触发加载更多；
  - 发送：`sendChatMessage()` 写入用户消息后，`streamChat()` 通过 SSE 拉取 assistant 增量：
    - UI 以“打字机效果”逐步追加 delta；
    - 某些设备 chunk/done 不稳定：启动 fallback polling 轮询拉取最终完整消息；
  - 渲染：AI 消息 Markdown 用 `towxml` 转为 nodes（`<towxml nodes="{{...}}">`）；
  - 权限：若 `isProfileComplete(me)===false`，会弹出 `auth-modal` 先补全资料；
  - 订阅：发送失败且 `code === "SUBSCRIPTION_REQUIRED"` 时，引导去订阅页。
- `index.wxml`：顶部栏 + 抽屉会话列表 + 消息流（含快捷问题 chips）+ 输入框。
- `index.wxss`：消息气泡、抽屉动画、typing 状态、towxml 样式收敛等。
- `index.json`：依赖 `ui-icon`、`auth-modal`、`towxml`。

## 使用方式

该页为 tab 页：通过 TabBar 进入（`/pages/chat/index`）。

## 技术要点

- `typing` 状态下禁止重复发送，避免并发 stream 导致 UI 乱序。
- AI 复制按钮仅对已完成消息显示（避免复制到半截流式文本）。

