# `pages/me/` 我的（个人中心）

本目录包含“我的”tab 下的入口页及相关功能页：资料编辑、孩子管理、历史记录、订阅购买、协议。

## 文件说明

### `index.*`（我的首页）

- `index.js`
  - 展示：昵称/头像/订阅状态（到期日、是否过期、是否使用过试用等）；
  - 跳转：孩子管理、历史记录、订阅、协议、资料编辑；
  - 资料校验：进入“孩子管理”前若 `isProfileComplete(me)` 为 false，弹出 `auth-modal`。
- `index.wxml/.wxss/.json`：页面布局与样式（自绘 header + 卡片列表）。

### `profile.*`（个人资料）

- `profile.js`
  - 拉取：`ensureLoggedIn()` → `fetchMe()`；
  - 编辑：昵称/出生日期；
  - 头像：支持 `wx.chooseImage` 与 `chooseAvatar` 两种方式，统一走 `uploadAvatar()`；
  - 保存：`updateProfile()` 成功后返回上一页。

### `children.*`（孩子管理）

- `children.js`
  - 列表：`fetchChildren()`；
  - 新增/编辑：同页表单模式（`formMode=add|edit`）；
  - 保存：`createChild()` / `updateChild()`；
  - 进入要求：若资料未完善，弹 `auth-modal`。

### `history.*`（自测历史）

- `history.js`
  - 列表：`listDailyRecords(limit, offset)`；
  - 查看：`getDailyRecordDetail(assessmentId)` 后写入 `daily-session`，跳转到 `/pages/test/result?mode=history`。

### `history-detail.*`（占位）

- 目前保留模板代码，未承载业务；如后续需要“历史详情页”，建议复用 `pages/test/result` 的渲染逻辑或将其抽为组件。

### `subscription.*`（订阅购买）

- `subscription.js`
  - 套餐：`fetchPlans()`，失败时使用内置兜底套餐；
  - 下单：`createOrder({planId})` → `wx.requestPayment(payParams)`；
  - 成功后刷新 `fetchMe()` 并返回。

### `agreement.*`（协议/隐私）

- `agreement.js`
  - 静态配置：协议 meta 与分段内容（数组驱动渲染）；
  - 支持一键复制联系邮箱。

## 技术要点

- “历史查看”统一走 `services/daily-session` 做跨页临时状态传递，避免 URL 传大 payload。
- 订阅页的套餐价格使用 `*_cent`（分）存储，显示层统一格式化为元。

