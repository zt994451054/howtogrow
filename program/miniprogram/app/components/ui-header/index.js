function getSystemMetrics() {
  const sys = wx.getWindowInfo ? wx.getWindowInfo() : wx.getSystemInfoSync();
  const menu = wx.getMenuButtonBoundingClientRect ? wx.getMenuButtonBoundingClientRect() : null;
  const statusBarHeight = sys.statusBarHeight || 20;
  const navBarHeight = menu ? menu.bottom + (menu.top - statusBarHeight) : statusBarHeight + 44;
  return { statusBarHeight, navBarHeight, menu };
}

Component({
  properties: {
    title: { type: String, value: "" },
    logo: { type: String, value: "" },
    left: { type: String, value: "" },
    right: { type: String, value: "" },
    // When true, render the header content below the WeChat menu capsule to avoid overlap.
    avoidMenu: { type: Boolean, value: false },
  },
  data: {
    statusBarHeight: 0,
    navContentHeight: 44,
    safeTopPaddingPx: 0,
  },
  lifetimes: {
    attached() {
      const { statusBarHeight, navBarHeight, menu } = getSystemMetrics();
      const avoidMenu = Boolean(this.properties.avoidMenu);

      if (avoidMenu && menu && Number(menu.bottom) > 0) {
        this.setData({
          statusBarHeight,
          safeTopPaddingPx: Math.max(0, Number(menu.bottom) + 8),
          navContentHeight: 44,
        });
        return;
      }

      this.setData({
        statusBarHeight,
        safeTopPaddingPx: Math.max(0, Number(statusBarHeight) || 0),
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
