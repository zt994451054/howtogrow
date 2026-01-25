# `components/ui-icon/` 轻量图标组件

通过 `name` 映射到一个字符（glyph）渲染，减少引入 iconfont 的成本。适合 Header/按钮等小图标场景。

## 文件说明

- `index.json`：声明组件。
- `index.wxml`：渲染 `<text>`，通过 `size` 控制字号。
- `index.wxss`：基础行高样式。
- `index.js`：
  - `properties`：`name`、`size`。
  - `getGlyph()`：维护 icon 名称到字符的映射表。
  - `observers.name`：动态更新 glyph。

## 使用方式

```wxml
<ui-icon name="back" size="{{24}}" />
```

## 维护约定

- 如需新增图标：只在 `getGlyph()` 的 `map` 中扩展，并保持语义化命名。
- 本组件不处理颜色，颜色由父级 `color` 样式继承控制。

