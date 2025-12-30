import { STORAGE_KEYS } from "../../services/config";
import { fetchMe, getCachedMe, ensureLoggedIn } from "../../services/auth";
import { removeStorage } from "../../services/storage";
import { getSystemMetrics } from "../../utils/system";

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
  },
  onLoad() {
    const { statusBarHeight } = getSystemMetrics();
    this.setData({ statusBarHeight });
  },
  async onShow() {
    const tab = (this as any).getTabBar?.();
    tab?.setData?.({ selected: 3 });

    const cached = getCachedMe();
    if (cached) {
      this.applyMe(cached);
    }

    try {
      await ensureLoggedIn();
      const me = await fetchMe();
      this.applyMe(me);
    } catch {
      // ignore
    }

    const nav = wx.getStorageSync(STORAGE_KEYS.navMe) as { view?: string; action?: string } | undefined;
    if (nav && nav.view) {
      removeStorage(STORAGE_KEYS.navMe);
      if (nav.view === "children") {
        const action = nav.action ? `?action=${encodeURIComponent(nav.action)}` : "";
        wx.navigateTo({ url: `/pages/me/children${action}` });
      }
    }
  },
  applyMe(me: any) {
    const endAt = me.subscriptionEndAt ? String(me.subscriptionEndAt).slice(0, 10) : "";
    const today = new Date().toISOString().slice(0, 10);
    const isSubscribed = Boolean(endAt);
    const isExpired = endAt ? endAt < today : false;

    this.setData({
      nickname: me.nickname || "育儿新手",
      avatarUrl: me.avatarUrl || defaultAvatar(),
      subscriptionEndAt: endAt,
      isSubscribed,
      isExpired,
      freeTrialUsed: Boolean(me.freeTrialUsed),
    });
  },
  goChildren() {
    wx.navigateTo({ url: "/pages/me/children" });
  },
  goHistory() {
    wx.navigateTo({ url: "/pages/me/history" });
  },
  goGrowth() {
    wx.navigateTo({ url: "/pages/me/growth-report" });
  },
  goSubscription() {
    wx.navigateTo({ url: "/pages/me/subscription" });
  },
  goAgreement() {
    wx.navigateTo({ url: "/pages/me/agreement" });
  },
});
