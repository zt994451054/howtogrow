function getSystemMetrics() {
  const sys = wx.getWindowInfo ? wx.getWindowInfo() : wx.getSystemInfoSync();
  const menu = wx.getMenuButtonBoundingClientRect ? wx.getMenuButtonBoundingClientRect() : null;
  const statusBarHeight = sys.statusBarHeight || 20;
  const navBarHeight = menu ? menu.bottom + (menu.top - statusBarHeight) : statusBarHeight + 44;
  return { statusBarHeight, navBarHeight };
}

Component({
  properties: {
    title: { type: String, value: "" },
    logo: { type: String, value: "" },
    left: { type: String, value: "" },
    right: { type: String, value: "" },
  },
  data: {
    statusBarHeight: 0,
    navContentHeight: 44,
  },
  lifetimes: {
    attached() {
      const { statusBarHeight, navBarHeight } = getSystemMetrics();
      this.setData({
        statusBarHeight,
        navContentHeight: Math.max(44, navBarHeight - statusBarHeight),
      });
    },
  },
  methods: {
    onLeft() {
      this.triggerEvent("left");
    },
    onRight() {
      this.triggerEvent("right");
    },
  },
});
