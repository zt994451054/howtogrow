const { STORAGE_KEYS } = require("../../services/config");
const { fetchMe, getCachedMe, ensureLoggedIn, isProfileComplete } = require("../../services/auth");
const { removeStorage } = require("../../services/storage");
const { getSystemMetrics } = require("../../utils/system");

function defaultAvatar() {
  return "https://i.pravatar.cc/150?img=5";
}

Page({
  data: {
    navBarHeight: 0,
    nickname: "育儿新手",
    avatarUrl: defaultAvatar(),
    subscriptionEndAt: "",
    subscriptionTip: "未订阅会员",
    subscriptionTipTone: "warn",
    isSubscribed: false,
    isExpired: false,
    freeTrialUsed: false,
    showAuthModal: false,
    pendingNav: "",
  },
  onLoad() {
    const { navBarHeight } = getSystemMetrics();
    const menuRect = wx.getMenuButtonBoundingClientRect ? wx.getMenuButtonBoundingClientRect() : null;
    const navHeight = menuRect && Number(menuRect.bottom || 0) > 0 ? Number(menuRect.bottom) : Number(navBarHeight || 0);
    this.setData({ navBarHeight: navHeight });
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
    const isSubscribed = !!endAt;
    const isExpired = endAt ? endAt < today : false;
    const subscriptionTip = !endAt ? "未订阅会员" : isExpired ? `已过期（${endAt}）` : `${endAt} 到期`;
    const subscriptionTipTone = !endAt || isExpired ? "warn" : "";
    this.setData({
      nickname: me.nickname || "育儿新手",
      avatarUrl: me.avatarUrl || defaultAvatar(),
      subscriptionEndAt: endAt,
      subscriptionTip,
      subscriptionTipTone,
      isSubscribed,
      isExpired,
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
