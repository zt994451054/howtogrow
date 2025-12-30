Component({
  data: {
    selected: 0,
    list: [
      {
        pagePath: "/pages/home/index",
        text: "首页",
        iconPath: "/assets/tab/home.png",
        selectedIconPath: "/assets/tab/home-active.png",
      },
      {
        pagePath: "/pages/test/index",
        text: "每日自测",
        iconPath: "/assets/tab/test.png",
        selectedIconPath: "/assets/tab/test-active.png",
      },
      {
        pagePath: "/pages/chat/index",
        text: "马上沟通",
        iconPath: "/assets/tab/chat.png",
        selectedIconPath: "/assets/tab/chat-active.png",
      },
      {
        pagePath: "/pages/me/index",
        text: "我的",
        iconPath: "/assets/tab/me.png",
        selectedIconPath: "/assets/tab/me-active.png",
      },
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
