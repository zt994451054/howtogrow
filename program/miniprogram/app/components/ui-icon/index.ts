Component({
  properties: {
    name: { type: String, value: "" },
    size: { type: Number, value: 22 },
  },
  observers: {
    name() {
      this.setData({ glyph: this.getGlyph(this.properties.name) });
    },
  },
  data: {
    glyph: "",
  },
  lifetimes: {
    attached() {
      this.setData({ glyph: this.getGlyph(this.properties.name) });
    },
  },
  methods: {
    getGlyph(name: string) {
      const map: Record<string, string> = {
        back: "‹",
        close: "×",
        menu: "≡",
        edit: "✎",
        bookmark: "🔖",
        chevronRight: "›",
      };
      return map[name] ?? "";
    },
  },
});

