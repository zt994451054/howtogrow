# `assets/` 静态资源说明

本目录仅存放静态图片资源，不包含运行时代码。

## 文件一览

- `logo-placeholder.png`：Logo 占位图（用于缺省/开发阶段展示）。
- `tab/`：自定义 TabBar 的图标资源（激活/未激活成对提供）。

## 使用方式

- 资源通过绝对路径引用，例如：`/assets/tab/home.png`。
- TabBar 图标由 `custom-tab-bar/index.wxml` 统一引用与切换。

## 维护约定

- 图标文件建议成对命名：`xxx.png` + `xxx-active.png`。
- 尽量保持尺寸一致，避免 TabBar 布局跳动。

