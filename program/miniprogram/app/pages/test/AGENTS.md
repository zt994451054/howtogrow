# `pages/test/` 每日自测（行为镜子）

本目录实现“每日自测”完整流程：引导 → 答题 → 结果。

## 文件说明

### `intro.*`（引导页）

- `intro.js`
  - 入参：`childId`、`childName`（URL encode）
  - 开始：`beginDailyAssessment(childId)` 获取 sessionId 与题目 items；
  - 缓存：写入 `services/daily-session.setDailySession()`；
  - 跳转：进入 `/pages/test/question`。

### `question.*`（答题页）

- `question.js`
  - 从 `getDailySession()` 读取题目与已选答案；
  - 单选/多选：依据 `questionType` 更新答案；
  - 进度：进度条 = `(currentIndex+1)/items.length`；
  - 至少 5 题：答满 5 题后出现“继续做题 / 提交”双按钮；
  - 换题：`replaceDailyQuestion(sessionId, { childId, displayOrder })` 替换当前题，并清理旧题答案；
  - 提交：`submitDailyAssessment(sessionId, { childId, answers })` 成功后写入 `assessmentId` 并跳转结果页。

### `result.*`（结果页）

- `result.js`
  - 从 `daily-session` 读取 items+answers，构造回顾列表（含 improvementTip）；
  - `mode=history` 时按钮文案与返回行为不同；
  - 可选：`fetchAiSummary(assessmentId)` 生成 AI 总结：
    - 若 `code === "SUBSCRIPTION_REQUIRED"`，弹窗引导订阅；
    - 成功后写回 `daily-session`，避免重复生成。

## 技术要点

- 跨页状态通过 `services/daily-session`（storage）传递，避免 URL 传输过大数据。
- 结果页会尝试根据页面栈找到入口页/详情页，尽量让“完成自测”回到合理位置。

