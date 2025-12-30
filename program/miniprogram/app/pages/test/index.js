const { getCachedChildren, fetchChildren } = require("../../services/children");
const { calcAge } = require("../../utils/date");

Page({
  data: {
    loading: false,
    children: [],
  },
  onShow() {
    const tab = this.getTabBar && this.getTabBar();
    tab && tab.setData && tab.setData({ selected: 1 });

    const cached = getCachedChildren();
    if (cached) this.setData({ children: cached.map((c) => ({ ...c, age: calcAge(c.birthDate) })) });
    this.loadChildren();
  },
  loadChildren() {
    this.setData({ loading: true });
    fetchChildren()
      .then((list) => this.setData({ children: list.map((c) => ({ ...c, age: calcAge(c.birthDate) })) }))
      .catch(() => {
        if (!this.data.children || this.data.children.length === 0) this.setData({ children: [] });
      })
      .finally(() => this.setData({ loading: false }));
  },
  onAddChild() {
    wx.switchTab({ url: "/pages/me/index" });
  },
  onSelectChild(e) {
    const { id, name } = e.currentTarget.dataset;
    wx.navigateTo({ url: `/pages/test/intro?childId=${Number(id)}&childName=${encodeURIComponent(name)}` });
  },
});
