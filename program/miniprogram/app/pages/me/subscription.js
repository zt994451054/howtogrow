const { getCachedMe, fetchMe } = require("../../services/auth");
const { fetchPlans, createOrder } = require("../../services/subscriptions");
const { getSystemMetrics } = require("../../utils/system");

function requestPayment(payParams) {
  return new Promise((resolve, reject) => {
    wx.requestPayment({
      ...(payParams || {}),
      success: () => resolve(),
      fail: () => reject(new Error("payment failed")),
    });
  });
}

Page({
  data: { statusBarHeight: 20, plans: [], selectedPlanId: 0, paying: false, subscriptionEndAt: "", isSubscribed: false, isExpired: false },
  onLoad() {
    const { statusBarHeight } = getSystemMetrics();
    this.setData({ statusBarHeight });
  },
  onShow() {
    const cached = getCachedMe();
    if (cached) this.applyMe(cached);
    fetchMe().then((me) => this.applyMe(me)).catch(() => {});
    this.loadPlans();
  },
  applyMe(me) {
    const endAt = me.subscriptionEndAt ? String(me.subscriptionEndAt).slice(0, 10) : "";
    const today = new Date().toISOString().slice(0, 10);
    this.setData({ subscriptionEndAt: endAt, isSubscribed: !!endAt, isExpired: endAt ? endAt < today : false });
  },
  loadPlans() {
    fetchPlans()
      .then((plans) => this.setData({ plans: plans.map((p) => ({ ...p, priceYuan: (Number(p.priceCent || 0) / 100).toFixed(2) })) }))
      .catch(() => {
        this.setData({
          plans: [
            { planId: 1, name: "月度会员", days: 30, priceCent: 2990, priceYuan: "29.90" },
            { planId: 2, name: "季度会员", days: 90, priceCent: 7990, priceYuan: "79.90" },
            { planId: 3, name: "年度会员", days: 365, priceCent: 19990, priceYuan: "199.90" },
          ],
        });
      });
  },
  onBack() {
    wx.navigateBack();
  },
  onSelect(e) {
    this.setData({ selectedPlanId: Number(e.currentTarget.dataset.id) });
  },
  onBuy() {
    if (!this.data.selectedPlanId || this.data.paying) return;
    this.setData({ paying: true });
    createOrder({ planId: this.data.selectedPlanId })
      .then((res) => requestPayment(res.payParams))
      .then(() => {
        wx.showToast({ title: "支付成功", icon: "success" });
        fetchMe().catch(() => {});
        wx.navigateBack();
      })
      .catch(() => wx.showToast({ title: "支付取消或失败", icon: "none" }))
      .finally(() => this.setData({ paying: false }));
  },
});

