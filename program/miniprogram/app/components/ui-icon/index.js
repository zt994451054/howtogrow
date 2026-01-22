Component({
  properties: {
    name: { type: String, value: "" },
    size: { type: Number, value: 22 },
  },
  data: {
    glyph: "",
  },
  lifetimes: {
    attached() {
      this.setData({ glyph: this.getGlyph(this.data.name) });
    },
  },
  observers: {
    name(v) {
      this.setData({ glyph: this.getGlyph(v) });
    },
  },
  methods: {
    getGlyph(name) {
      const map = {
        back: "‹",
        close: "×",
        menu: "≡",
        edit: "✎",
        copy: "📋",
        bookmark: "🔖",
        chevronRight: "›",
        chevronDown: "⌄",
        calendar: "📅",
      };
      return map[name] || "";
    },
  },
});
