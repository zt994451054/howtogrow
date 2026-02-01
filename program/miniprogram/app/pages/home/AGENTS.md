# `pages/home/` 首页与每日觉察

本目录实现「每日觉察」主入口与“某天详情”。

## 文件说明

### `index.*`（首页：按月卡片）

- `index.js`
  - 登录：`ensureLoggedIn()` + `getCachedMe()` 渲染头像；
  - 孩子：`fetchChildren()` 拉取并缓存选择到 `STORAGE_KEYS.navHomeSelectedChildId`；
  - Banner：`listBanners()` 渲染 swiper；
  - 月份：picker `fields="month"`，最早支持 `MIN_MONTH=2025-06`；
  - 日历卡片：`getMonthlyAwareness(childId, month)` → `dayList`（按日期倒序）；
  - 自测预览：对 `assessmentId` 批量调用 `getDailyRecordDetail(..., { toast:false })`，从题目/选项提取标题与建议，填充卡片 footer（并发池 `promisePool`，并用 `_assessmentPreviewReqId` 防止过期写入）。
- `index.wxml`：自绘 header（头像问候 + 右侧菜单）；孩子切换弹层；Banner swiper；按日卡片网格。
- `index.wxss`：卡片网格、mood 角标、菜单弹层、空态等。
- `index.json`：`navigationStyle: custom`，依赖 `ui-icon`。

### `detail.*`（某天详情：时间线 + Sheet）

- `detail.js`
  - 入参：必须带 `childId`、`date(YYYY-MM-DD)`，可选 `open=status`（直接打开育儿状态 sheet）。
  - 数据：从 `getMonthlyAwareness(childId, month)` 找到当天记录，构造时间线 items：
    - 育儿状态（`services/parenting-status`）
    - 烦恼存档（`services/daily-troubles` + `services/trouble-scenes`）
    - 行为镜子（每日自测：`pages/test/*`，或历史结果）
    - 育儿日记（`services/parenting-diary` + `services/uploads`）
    - 继续深度咨询（跳转 `pages/chat/index`）
  - 三个全屏 sheet：
    - 状态：环绕选择 + `upsertDailyParentingStatus`
    - 烦恼：场景多选 + `upsertDailyTroubleRecord`
    - 日记：文本 + 图片上传 + `upsertDailyDiary`
- `detail.wxml`：时间线列表 + 3 个 overlay（全屏）sheet。
- `detail.wxss`：时间线视觉、overlay 全屏交互、各 sheet 内部样式。
- `detail.json`：`navigationStyle: custom`，依赖 `ui-header`。

## 使用方式（页面跳转）

- 从首页进入某天：`/pages/home/detail?childId=<id>&date=YYYY-MM-DD`
- 从 TabBar 中间按钮进入“状态 sheet”：`/pages/home/detail?childId=<id>&date=YYYY-MM-DD&open=status`

## 技术要点

- 当天自测限制：只有 `date === today` 才允许开始答题；否则提示“仅支持当天完成”。
- 三个 sheet 使用全屏 overlay，顶部通过 `overlaySafeTopPx` 规避胶囊按钮遮挡。

