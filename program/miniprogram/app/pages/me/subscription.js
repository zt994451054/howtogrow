const { getCachedMe, fetchMe } = require("../../services/auth");
const { fetchPlans, createOrder } = require("../../services/subscriptions");
const { getSystemMetrics } = require("../../utils/system");

function formatYuanFromCent(amountCent) {
  const cent = Number(amountCent || 0);
  if (!Number.isFinite(cent)) return "0.00";
  return (cent / 100).toFixed(2);
}

function normalizePlanForDisplay(plan) {
  const rawPriceCent = Number(plan.priceCent || 0);
  const priceCent = Number.isFinite(rawPriceCent) ? Math.max(rawPriceCent, 0) : 0;
  const rawOriginalCent = plan.originalPriceCent;
  const originalCandidate = rawOriginalCent == null ? priceCent : Number(rawOriginalCent);
  const originalPriceCent =
    Number.isFinite(originalCandidate) ? Math.max(originalCandidate, priceCent) : priceCent;
  const discountCent = Math.max(originalPriceCent - priceCent, 0);

  return {
    ...plan,
    priceCent,
    originalPriceCent,
    priceYuan: formatYuanFromCent(priceCent),
    originalPriceYuan: formatYuanFromCent(originalPriceCent),
    discountBadge: discountCent > 0 ? `立省¥${formatYuanFromCent(discountCent)}` : "",
    showOriginalPrice: originalPriceCent > priceCent,
  };
}

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
      .then((plans) => this.setData({ plans: (plans || []).map((p) => normalizePlanForDisplay(p)) }))
      .catch(() => {
        this.setData({
          plans: [
            normalizePlanForDisplay({ planId: 1, name: "月度会员", days: 30, originalPriceCent: 3990, priceCent: 2990 }),
            normalizePlanForDisplay({ planId: 2, name: "季度会员", days: 90, originalPriceCent: 9990, priceCent: 7990 }),
            normalizePlanForDisplay({ planId: 3, name: "年度会员", days: 365, originalPriceCent: 29990, priceCent: 19990 }),
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
