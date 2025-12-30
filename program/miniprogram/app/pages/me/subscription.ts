import { getCachedMe, fetchMe } from "../../services/auth";
import { fetchPlans, createOrder } from "../../services/subscriptions";
import { getSystemMetrics } from "../../utils/system";

function requestPayment(payParams: any): Promise<void> {
  return new Promise((resolve, reject) => {
    wx.requestPayment({
      ...(payParams || {}),
      success: () => resolve(),
      fail: () => reject(new Error("payment failed")),
    });
  });
}

Page({
  data: {
    statusBarHeight: 20,
    plans: [] as any[],
    selectedPlanId: 0,
    paying: false,
    subscriptionEndAt: "",
    isSubscribed: false,
    isExpired: false,
  },
  onLoad() {
    const { statusBarHeight } = getSystemMetrics();
    this.setData({ statusBarHeight });
  },
  async onShow() {
    const cached = getCachedMe();
    if (cached) this.applyMe(cached);
    try {
      const me = await fetchMe();
      this.applyMe(me);
    } catch {
      // ignore
    }
    await this.loadPlans();
  },
  applyMe(me: any) {
    const endAt = me.subscriptionEndAt ? String(me.subscriptionEndAt).slice(0, 10) : "";
    const today = new Date().toISOString().slice(0, 10);
    const isSubscribed = Boolean(endAt);
    const isExpired = endAt ? endAt < today : false;
    this.setData({ subscriptionEndAt: endAt, isSubscribed, isExpired });
  },
  async loadPlans() {
    try {
      const plans = await fetchPlans();
      this.setData({
        plans: plans.map((p: any) => ({
          ...p,
          priceYuan: (Number(p.priceCent || 0) / 100).toFixed(2),
        })),
      });
    } catch {
      // fallback demo
      this.setData({
        plans: [
          { planId: 1, name: "月度会员", days: 30, priceCent: 2990, priceYuan: "29.90" },
          { planId: 2, name: "季度会员", days: 90, priceCent: 7990, priceYuan: "79.90" },
          { planId: 3, name: "年度会员", days: 365, priceCent: 19990, priceYuan: "199.90" },
        ] as any[],
      });
    }
  },
  onBack() {
    wx.navigateBack();
  },
  onSelect(e: WechatMiniprogram.BaseEvent) {
    const id = Number((e.currentTarget as any).dataset.id);
    this.setData({ selectedPlanId: id });
  },
  async onBuy() {
    if (!this.data.selectedPlanId || this.data.paying) return;
    this.setData({ paying: true });
    try {
      const res = await createOrder({ planId: this.data.selectedPlanId });
      await requestPayment(res.payParams);
      wx.showToast({ title: "支付成功", icon: "success" });
      try {
        await fetchMe();
      } catch {
        // ignore
      }
      wx.navigateBack();
    } catch {
      wx.showToast({ title: "支付取消或失败", icon: "none" });
    } finally {
      this.setData({ paying: false });
    }
  },
});
