const { replaceDailyQuestion, submitDailyAssessment } = require("../../services/assessments");
const { clearDailySession, getDailySession, setDailySession } = require("../../services/daily-session");
const { getSystemMetrics } = require("../../utils/system");

function toIntroUrlFromSession(session) {
  const childId = Number(session?.childId || 0);
  const childName = encodeURIComponent(String(session?.childName || "孩子"));
  if (!childId) return "";
  return `/pages/test/intro?childId=${childId}&childName=${childName}`;
}

Page({
  data: {
    statusBarHeight: 20,
    session: null,
    currentIndex: 0,
    progressPercent: 0,
    currentItem: null,
    currentOptions: [],
    canNext: false,
    isLast: false,
    answeredCount: 0,
    canSubmit: false,
    canContinue: false,
  },
  onLoad() {
    const { statusBarHeight } = getSystemMetrics();
    this.setData({ statusBarHeight });
  },
  onShow() {
    const session = getDailySession();
    if (!session || !session.items || session.items.length === 0) {
      wx.showToast({ title: "请先开始测试", icon: "none" });
      wx.navigateBack();
      return;
    }
    this.setData({ session });
    this.syncDerived();
  },
  syncDerived() {
    const session = this.data.session;
    const currentIndex = this.data.currentIndex;
    const item = session.items[currentIndex];
    const selected = session.answers[item.questionId] || [];
    const currentOptions = (item.options || []).map((o) => ({ ...o, _selected: selected.includes(o.optionId) }));
    const answeredCount = Object.values(session.answers || {}).filter((v) => Array.isArray(v) && v.length > 0).length;
    const canSubmit = answeredCount >= 5 && selected.length > 0;
    this.setData({
      currentItem: item,
      currentOptions,
      progressPercent: Math.round(((currentIndex + 1) / session.items.length) * 100),
      canNext: selected.length > 0,
      isLast: currentIndex === session.items.length - 1,
      answeredCount,
      canSubmit,
      canContinue: selected.length > 0 && currentIndex < session.items.length - 1,
    });
  },
  onToggleOption(e) {
    const optionId = Number(e.currentTarget.dataset.optionId);
    const session = this.data.session;
    const item = this.data.currentItem;
    const current = session.answers[item.questionId] || [];
    let next = [];
    if (item.questionType === "SINGLE") next = [optionId];
    else next = current.includes(optionId) ? current.filter((id) => id !== optionId) : [...current, optionId];
    session.answers[item.questionId] = next;
    setDailySession(session);
    this.setData({ session });
    this.syncDerived();
  },
  onPrev() {
    if (this.data.currentIndex > 0) {
      this.setData({ currentIndex: this.data.currentIndex - 1 });
      this.syncDerived();
      return;
    }
    wx.navigateBack();
  },
  onClose() {
    const session = getDailySession();
    const url = toIntroUrlFromSession(session);
    clearDailySession();
    if (url) {
      wx.redirectTo({ url });
      return;
    }
    wx.switchTab({ url: "/pages/home/index" });
  },
  onSwap() {
    const session = this.data.session;
    const item = this.data.currentItem;
    wx.showLoading({ title: "换题中…" });
    replaceDailyQuestion(session.sessionId, { childId: session.childId, displayOrder: item.displayOrder }, { toast: false })
      .then((res) => {
        const idx = session.items.findIndex((i) => i.displayOrder === res.displayOrder);
        if (idx >= 0) {
          const oldQuestionId = session.items[idx].questionId;
          delete session.answers[oldQuestionId];
          session.items[idx] = res.newItem;
          setDailySession(session);
          this.setData({ session });
          this.syncDerived();
        }
      })
      .catch((err) => {
        const message = err && typeof err.message === "string" && err.message.trim() ? err.message.trim() : "换题失败";
        wx.showToast({ title: message, icon: "none" });
      })
      .finally(() => wx.hideLoading());
  },
  onNext() {
    const session = this.data.session;
    const item = this.data.currentItem;
    const selected = session.answers[item.questionId] || [];
    if (selected.length === 0) return;
    if (this.data.answeredCount >= 5) {
      return;
    }
    if (!this.data.isLast) {
      this.setData({ currentIndex: this.data.currentIndex + 1 });
      this.syncDerived();
      return;
    }
    this.onSubmit();
  },
  onContinue() {
    const session = this.data.session;
    const item = this.data.currentItem;
    const selected = session.answers[item.questionId] || [];
    if (selected.length === 0) return;
    if (this.data.isLast) return;
    this.setData({ currentIndex: this.data.currentIndex + 1 });
    this.syncDerived();
  },
  onSubmit() {
    const session = this.data.session;
    const item = this.data.currentItem;
    const selected = session.answers[item.questionId] || [];
    if (selected.length === 0) return;
    const answers = session.items
      .map((it) => ({ questionId: it.questionId, optionIds: session.answers[it.questionId] || [] }))
      .filter((a) => Array.isArray(a.optionIds) && a.optionIds.length > 0);
    if (answers.length < 5) {
      wx.showToast({ title: "请至少完成 5 道题目", icon: "none" });
      return;
    }
    wx.showLoading({ title: "提交中…" });
    submitDailyAssessment(session.sessionId, { childId: session.childId, answers })
      .then((res) => {
        session.submitResult = res;
        session.assessmentId = res.assessmentId;
        setDailySession(session);
        wx.navigateTo({ url: "/pages/test/result" });
      })
      .catch(() => wx.showToast({ title: "提交失败", icon: "none" }))
      .finally(() => wx.hideLoading());
  },
});
