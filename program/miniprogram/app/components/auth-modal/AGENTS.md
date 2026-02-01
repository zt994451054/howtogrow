# `components/auth-modal/` 资料完善弹窗

该组件用于“必须完成头像/昵称后才能继续”的场景（例如进入孩子管理、发送 AI 咨询）。页面通过 `show` 属性控制显示，并监听组件事件。

## 文件说明

- `index.json`：声明为组件。
- `index.wxml`：弹窗结构（遮罩 + 底部面板 + 头像选择 + 昵称输入）。
- `index.wxss`：弹窗样式与动效（`show` class 控制可见/位移）。
- `index.js`：
  - 头像：使用 `open-type="chooseAvatar"` 获取临时头像地址。
  - 上传：调用 `services/uploads.uploadAvatar()` 上传到后端，得到可持久化 URL。
  - 保存：调用 `services/auth.updateProfile()` 保存昵称/头像。
  - 事件：`close`（点击遮罩关闭）、`success`（保存成功后通知页面）。

## 使用方式

页面引入：
- 在页面 `.json` 配置：`"auth-modal": "/components/auth-modal/index"`
- 在页面 WXML：`<auth-modal show="{{showAuthModal}}" bind:success="onAuthSuccess" bind:close="onAuthClose" />`

## 技术要点

- 组件内部会展示 `wx.showLoading`，并在保存结束后 `wx.hideLoading`。
- 保存成功后不负责跳转，由页面决定下一步（通过 `success` 事件衔接）。

