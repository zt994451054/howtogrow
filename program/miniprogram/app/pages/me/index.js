const { STORAGE_KEYS } = require("../../services/config");
const { fetchMe, getCachedMe, ensureLoggedIn, isProfileComplete } = require("../../services/auth");
const { removeStorage } = require("../../services/storage");
const { getSystemMetrics } = require("../../utils/system");

function defaultAvatar() {
  return "https://i.pravatar.cc/150?img=5";
}

Page({
  data: {
    statusBarHeight: 20,
    nickname: "育儿新手",
    avatarUrl: defaultAvatar(),
    subscriptionEndAt: "",
    isSubscribed: false,
    isExpired: false,
    freeTrialUsed: false,
    showAuthModal: false,
    pendingNav: "",
  },
  onLoad() {
    const { statusBarHeight } = getSystemMetrics();
    this.setData({ statusBarHeight });
  },
  onShow() {
    const tab = this.getTabBar && this.getTabBar();
    tab && tab.setData && tab.setData({ selected: 3 });

    const cached = getCachedMe();
    if (cached) this.applyMe(cached);
    ensureLoggedIn()
      .then(() => fetchMe())
      .then((me) => this.applyMe(me))
      .catch(() => {});

    const nav = wx.getStorageSync(STORAGE_KEYS.navMe);
    if (nav && nav.view) {
      removeStorage(STORAGE_KEYS.navMe);
      if (nav.view === "children") {
        const action = nav.action ? `?action=${encodeURIComponent(nav.action)}` : "";
        wx.navigateTo({ url: `/pages/me/children${action}` });
      }
    }
  },
  applyMe(me) {
    const endAt = me.subscriptionEndAt ? String(me.subscriptionEndAt).slice(0, 10) : "";
    const today = new Date().toISOString().slice(0, 10);
    this.setData({
      nickname: me.nickname || "育儿新手",
      avatarUrl: me.avatarUrl || defaultAvatar(),
      subscriptionEndAt: endAt,
      isSubscribed: !!endAt,
      isExpired: endAt ? endAt < today : false,
      freeTrialUsed: !!me.freeTrialUsed,
    });
  },
  goChildren() {
    const me = getCachedMe();
    if (!isProfileComplete(me)) {
      this.setData({ showAuthModal: true, pendingNav: "children" });
      return;
    }
    wx.navigateTo({ url: "/pages/me/children" });
  },
  goHistory() {
    wx.navigateTo({ url: "/pages/me/history" });
  },
  goSubscription() {
    wx.navigateTo({ url: "/pages/me/subscription" });
  },
  goAgreement() {
    wx.navigateTo({ url: "/pages/me/agreement" });
  },
  goProfile() {
    wx.navigateTo({ url: "/pages/me/profile" });
  },
  onAuthSuccess() {
    const nav = this.data.pendingNav;
    this.setData({ showAuthModal: false, pendingNav: "" });
    if (nav === "children") {
      wx.navigateTo({ url: "/pages/me/children" });
    }
  },
});
