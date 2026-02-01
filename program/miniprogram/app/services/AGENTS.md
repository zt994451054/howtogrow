# `services/` 后端接口封装层

本目录封装所有与后端交互的逻辑，页面/组件只能调用这里的函数，不应直接 `wx.request`。

## 统一约定

- 后端响应期望形如：`{ code, message, data, traceId }`，其中 `code === "OK"` 表示成功。
- 授权：token 存储在 `STORAGE_KEYS.token`，请求时自动带 `Authorization: Bearer <token>`。
- 自动重登：`UNAUTHORIZED` 时会清理 token/me，并尝试重新登录后重试一次。

## 文件说明（逐个）

- `config.js`：环境常量（`API_BASE_URL`、`API_PREFIX`、`H5_BASE_URL`、`STORAGE_KEYS`）。
- `storage.js`：对 `wx.*StorageSync` 的轻量封装（读写/删除，带 try/catch）。
- `request.js`：统一请求层
  - `apiRequest(method, path, data, options)`
  - `ApiError`（带 `code/traceId`）
  - `startRelogin()`（并发去重）
- `auth.js`：登录与个人资料
  - `ensureLoggedIn()`（无 token / 拉取 me 失败时自动重新登录）
  - `fetchMe()`、`updateProfile()`、`isProfileComplete()`
- `children.js`：孩子管理（GET/POST/PUT `/miniprogram/children`）。
- `assessments.js`：每日自测
  - `beginDailyAssessment()`、`replaceDailyQuestion()`、`submitDailyAssessment()`
  - `fetchAiSummary()`（可能返回 `SUBSCRIPTION_REQUIRED`）
  - `listDailyRecords()`、`getDailyRecordDetail()`
- `awareness.js`：月度觉察聚合（`getMonthlyAwareness(childId, month)`）。
- `reports.js`：成长报告
  - `fetchGrowthReport(childId, from, to)`
  - `fetchAwarenessPersistence(childId)`
- `parenting-status.js`：育儿状态（`getDailyParentingStatus` / `upsertDailyParentingStatus`）。
- `trouble-scenes.js`：烦恼场景列表（`listTroubleScenes()`）。
- `daily-troubles.js`：烦恼存档（日记录）读写（`getDailyTroubleRecord` / `upsertDailyTroubleRecord`）。
- `parenting-diary.js`：育儿日记（日记录）读写（`getDailyDiary` / `upsertDailyDiary`）。
- `quotes.js`：随机文案（按 childId + scene）。
- `chat.js`：AI 对话
  - 会话：`createChatSession()`、`listChatSessions()`
  - 消息：`listChatMessages()`、`sendChatMessage()`
  - 流式：`streamChat()`（SSE 解析 + utf8 流式解码 + 错误兜底）
- `banners.js`：Banner 列表与详情（`listBanners()`、`getBannerDetail()`）。
- `subscriptions.js`：订阅（`fetchPlans()`、`createOrder()`）。
- `uploads.js`：文件上传（头像/日记图片），支持远程 URL 先下载到临时文件再上传。

## 测试

- `__tests__/chat.test.js`：验证 `streamChat()` 对 HTTP 401 与 SSE `event:error` 的处理逻辑。

