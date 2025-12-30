Component({
  data: {
    selected: 0,
    list: [
      { pagePath: "/pages/home/index", text: "首页", icon: "⌂" },
      { pagePath: "/pages/test/index", text: "每日自测", icon: "✓" },
      { pagePath: "/pages/chat/index", text: "马上沟通", icon: "💬" },
      { pagePath: "/pages/me/index", text: "我的", icon: "👤" },
    ],
  },
  methods: {
    onTap(e) {
      const { index, path } = e.currentTarget.dataset;
      wx.switchTab({ url: path });
      this.setData({ selected: Number(index) });
    },
  },
});

