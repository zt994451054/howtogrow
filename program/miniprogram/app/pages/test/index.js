const { STORAGE_KEYS } = require("../../services/config");
const { getCachedChildren, fetchChildren } = require("../../services/children");
const { setStorage } = require("../../services/storage");
const { getCachedMe, isProfileComplete } = require("../../services/auth");
const { calcAge } = require("../../utils/date");

Page({
  data: {
    loading: false,
    children: [],
    showAuthModal: false,
    pendingAction: "",
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
    const me = getCachedMe();
    if (!isProfileComplete(me)) {
      this.setData({ showAuthModal: true, pendingAction: "addChild" });
      return;
    }
    this.goAddChild();
  },
  goAddChild() {
    setStorage(STORAGE_KEYS.navMe, { view: "children", action: "add" });
    wx.switchTab({ url: "/pages/me/index" });
  },
  onSelectChild(e) {
    const { id, name } = e.currentTarget.dataset;
    wx.navigateTo({ url: `/pages/test/intro?childId=${Number(id)}&childName=${encodeURIComponent(name)}` });
  },
  onAuthSuccess() {
    const action = this.data.pendingAction;
    this.setData({ showAuthModal: false, pendingAction: "" });
    if (action === "addChild") {
      this.goAddChild();
    }
  },
});
