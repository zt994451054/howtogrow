# Howtotalk 小程序（家长端）— 全局说明

本目录（`program/miniprogram/app`）是可在微信开发者工具直接打开的原生小程序工程。本文档用于让大模型/协作者快速理解工程结构、关键业务流程与技术约定；各子目录下的 `AGENTS.md` 会进一步说明该目录内每个文件的职责与使用方式。

## 如何运行

1. 微信开发者工具 → 导入项目
2. 目录选择：`program/miniprogram/app`
3. AppID：使用你的小程序 AppID（没有也可用测试号/无 AppID 方式导入）

关键配置：
- API Base：`services/config.js` → `API_BASE_URL`（后端地址）
- H5 Base：`services/config.js` → `H5_BASE_URL`（Banner 详情 WebView 地址）

## 项目结构（概要）

- `app.js`：小程序启动入口；`onLaunch()` 调用 `services/auth.ensureLoggedIn()` 做自动登录/续期。
- `app.json`：页面路由、全局窗口（`navigationStyle: custom`）、自定义 `tabBar` 配置。
- `custom-tab-bar/`：自定义 TabBar（4 个 tab + 中间快捷入口按钮）。
- `pages/`：页面层（每个页面由 `.js/.wxml/.wxss/.json` 组成）。
- `components/`：通用组件（自定义 Header/Icon、资料完善弹窗、ECharts Canvas 适配层）。
- `services/`：所有后端接口封装（统一走 `services/request.apiRequest`）。
- `utils/`：纯工具方法（日期/系统尺寸）。
- `styles/`：全局设计变量与通用布局类。
- `assets/`：静态资源（tab 图标等）。
- `towxml/`：第三方库（Markdown/HTML → WXML 渲染），用于 AI 回复富文本展示。

## 后端交互约定（非常重要）

统一通过 `services/request.js` 的 `apiRequest(method, path, data, options)` 访问后端，默认行为：
- 自动拼接：`API_BASE_URL + API_PREFIX + path`
- 自动注入：`Authorization: Bearer <token>`（从 `STORAGE_KEYS.token` 获取）
- 统一错误：非 `code === "OK"` 视为失败，默认 toast 提示
- 自动重登：`code === "UNAUTHORIZED"` 时清理 token/me，并尝试调用 `/miniprogram/auth/wechat-login` 续期后重试一次

因此：页面/组件不要直接 `wx.request` 调业务接口；新增接口请只在 `services/` 增加封装函数。

## 关键业务流程（高层）

- 冷启动登录：`app.js` → `ensureLoggedIn()`（微信 `wx.login` → 后端换 token → 缓存 token/me）
- 首页「每日觉察」：
  - 加载孩子列表 → 选择 childId 并写入 `STORAGE_KEYS.navHomeSelectedChildId`
  - 调 `getMonthlyAwareness(childId, month)` 渲染按天卡片
  - 对存在 `assessmentId` 的日期并发拉取 `getDailyRecordDetail`，抽取“题干 + 首条建议/AI 总结”作为卡片底部预览
- 详情「每日觉察」：
  - 从月度聚合中读取当天状态/烦恼/日记/assessmentId
  - 三个全屏 sheet：育儿状态 / 烦恼存档 / 育儿日记（分别调用 `services/*` 的 upsert）
  - 行为镜子（每日自测）：当天可进入；历史记录可回看
- 「马上沟通」AI 对话：
  - `sendChatMessage` 写入用户消息 → `streamChat` 以 SSE 流式拉取 assistant 输出
  - UI 使用“打字机效果”逐步渲染增量；对部分设备增加 fallback polling（轮询拉取最新 assistant 消息）
  - AI 回复 Markdown 通过 `towxml` 渲染为 WXML 节点树
- 订阅支付：
  - `pages/me/subscription` → `createOrder` 获取 `payParams` → `wx.requestPayment`
  - 如接口返回 `SUBSCRIPTION_REQUIRED`，页面以 Modal 引导去订阅

## 开发约定（给后续协作者/大模型）

- 新增页面：必须在 `app.json` 注册；若需要 tab 项，需同步更新 `custom-tab-bar/index.wxml` 与 `app.json.tabBar.list`。
- 数据缓存：统一使用 `services/storage.js` 与 `services/config.js` 的 `STORAGE_KEYS`；避免散落自定义 key。
- UI：全局颜色/圆角/阴影优先使用 `styles/tokens.wxss` 的 CSS 变量；页面左右统一用 `--page-padding`，顶部安全区用 `--page-nav-height`（状态栏 + 系统按钮）并在其下加 `--page-padding` 间隔；板块上下间距使用 `--section-gap`；正文黑/灰使用 `--text-ink` / `--text-ink-muted`；布局类优先用 `styles/util.wxss`。
- 第三方库：
  - `towxml/`、`components/ec-canvas/echarts.js` 属于 vendored 代码；不要“局部修修补补”，升级时建议整体替换并做回归。

## 快速自检

- SSE 流式错误处理：`node services/__tests__/chat.test.js`
