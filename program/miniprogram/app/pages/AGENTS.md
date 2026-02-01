# `pages/` 页面目录说明

本目录包含所有页面（Page）。每个页面通常由四个文件组成：
- `.js`：页面逻辑（数据、生命周期、事件处理）
- `.wxml`：页面结构（模板）
- `.wxss`：页面样式
- `.json`：页面配置（如 `navigationStyle`、组件依赖）

## 页面路由一览（与 `app.json.pages` 对应）

- `pages/home/index`：首页「每日觉察」（按月卡片列表、孩子切换、Banner）。
- `pages/home/detail`：某天详情「每日觉察」（时间线 + 3 个全屏 sheet + 自测/咨询入口）。
- `pages/curve/index`：家长曲线（ECharts 趋势图 + 状态/困扰分布）。
- `pages/curve/troubles`：困扰分布详情（按日期范围统计）。
- `pages/chat/index`：马上沟通（AI 对话、历史会话、快捷提问）。
- `pages/me/index`：我的（入口汇总、订阅状态）。
- `pages/me/profile`：个人资料编辑。
- `pages/me/children`：孩子管理（新增/编辑）。
- `pages/me/history`：自测历史记录列表（跳转查看结果）。
- `pages/me/history-detail`：占位页（当前未承载业务逻辑）。
- `pages/me/subscription`：订阅购买（套餐选择、微信支付）。
- `pages/me/agreement`：协议与隐私政策展示（静态内容）。
- `pages/banner/detail`：Banner 详情（WebView 打开 H5）。
- `pages/test/intro`：每日自测引导页。
- `pages/test/question`：每日自测答题页（支持换题、至少 5 题提交）。
- `pages/test/result`：每日自测结果页（建议回顾、可生成 AI 总结）。
- `pages/records/parenting-status`：独立的育儿状态记录页（辅助/预留）。

## 统一约定

- 多数页面采用 `navigationStyle: "custom"`，顶部使用 `components/ui-header` 或自绘 header。
- 后端访问统一走 `services/`；页面不直接 `wx.request`。

