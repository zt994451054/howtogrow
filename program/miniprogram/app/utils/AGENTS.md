# `utils/` 工具方法

本目录放置与业务无关的纯工具函数（可被 services/pages/components 复用）。

## 文件说明

- `date.js`
  - `formatDateYmd(date)`：`Date` → `YYYY-MM-DD`
  - `calcAge(birthDateYmd, now)`：根据生日计算周岁（用于孩子列表展示）
- `system.js`
  - `getSystemMetrics()`：统一读取窗口/安全区/导航栏高度（适配 `wx.getWindowInfo` 与旧 `wx.getSystemInfoSync`）

## 维护约定

- 工具函数应保持无副作用（不读写 storage、不发请求）。
- 若涉及微信 API，需提供兼容旧基础库的兜底实现。

