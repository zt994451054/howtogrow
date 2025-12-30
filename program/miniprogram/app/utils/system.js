function getSystemMetrics() {
  const sys = wx.getSystemInfoSync();
  const menu = wx.getMenuButtonBoundingClientRect ? wx.getMenuButtonBoundingClientRect() : null;

  const statusBarHeight = sys.statusBarHeight || 20;
  const navBarHeight = menu ? menu.bottom + (menu.top - statusBarHeight) : statusBarHeight + 44;

  return {
    statusBarHeight,
    navBarHeight,
    windowWidth: sys.windowWidth,
    windowHeight: sys.windowHeight,
    safeArea: sys.safeArea,
  };
}

module.exports = { getSystemMetrics };

