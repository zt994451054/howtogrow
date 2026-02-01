# `pages/records/` 记录类页面（辅助/预留）

该目录包含独立的记录页面，用于“单功能录入/查看”的场景；目前主要是 `parenting-status`。

## 文件说明

- `parenting-status.js`
  - 入参：`childId`、`recordDate(YYYY-MM-DD)`、`childName`（可选）
  - 流程：登录 → 拉孩子 → 选择日期 → 拉已有记录 → 选择状态 → 保存
  - 接口：`getDailyParentingStatus` / `upsertDailyParentingStatus`
- `parenting-status.wxml/.wxss`：孩子 chips、日期 picker、10 选 1 状态网格。
- `parenting-status.json`：依赖 `ui-header`。

## 备注

- 当前主流程已在 `pages/home/detail` 中提供更完整的“育儿状态”体验；该页可作为调试/快速录入入口保留。

