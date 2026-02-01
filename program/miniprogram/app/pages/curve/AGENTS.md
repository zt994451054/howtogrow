# `pages/curve/` 家长曲线

本目录实现“成长曲线”与“困扰分布详情”。

## 文件说明

### `index.*`（家长曲线）

- `index.js`
  - 孩子选择：`fetchChildren()` + 缓存 `STORAGE_KEYS.navCurveSelectedChildId`；
  - 日期范围：默认近 90 天，支持 ActionSheet（7/30/90/自定义），自定义范围最大 180 天（超出自动裁剪）；
  - 趋势图：`fetchGrowthReport(childId, from, to)` → ECharts 折线图（5 维度：亲子关系/规则/学习/情绪/沟通）；
  - 维度开关：可隐藏/显示曲线（若全部关闭则回退全显示）；
  - 状态分布 + 困扰 top3：按月份并发拉 `getMonthlyAwareness(childId, month)`，聚合后计算；
  - 坚持天数：`fetchAwarenessPersistence(childId)`。
- `index.wxml`：自绘 header（头像问候 + 孩子切换）+ ECharts 卡片 + 维度 pills + 分布卡片。
- `index.wxss`：整体视觉、图表卡片、日期范围 sheet 等。
- `index.json`：依赖 `ui-icon` 与 `ec-canvas`。

### `troubles.*`（困扰分布详情）

- `troubles.js`
  - 入参：`childId`、`from`、`to`（均为 `YYYY-MM-DD`）
  - 拉取：按月份并发 `getMonthlyAwareness`，聚合 `troubleScenes` 并统计次数；
  - 输出：按次数降序，生成条形百分比（相对最大值）。
- `troubles.wxml/.wxss`：排行列表样式（Top1/2/3 高亮）。
- `troubles.json`：依赖 `ui-header`。

## 使用方式（页面跳转）

- 困扰详情：`/pages/curve/troubles?childId=<id>&from=YYYY-MM-DD&to=YYYY-MM-DD`

## 技术要点

- 曲线页使用内容区 `scroll-view` 实现“顶部固定 + 内容滚动”；ECharts 仍在滚动容器内，若出现抖动需重点回归。
