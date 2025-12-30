const { replaceDailyQuestion, submitDailyAssessment } = require("../../services/assessments");
const { clearDailySession, getDailySession, setDailySession } = require("../../services/daily-session");
const { getSystemMetrics } = require("../../utils/system");

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
    this.setData({
      currentItem: item,
      currentOptions,
      progressPercent: Math.round(((currentIndex + 1) / session.items.length) * 100),
      canNext: selected.length > 0,
      isLast: currentIndex === session.items.length - 1,
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
    clearDailySession();
    wx.switchTab({ url: "/pages/test/index" });
  },
  onSwap() {
    const session = this.data.session;
    const item = this.data.currentItem;
    wx.showLoading({ title: "换题中…" });
    replaceDailyQuestion(session.sessionId, { childId: session.childId, displayOrder: item.displayOrder })
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
      .catch(() => wx.showToast({ title: "换题失败", icon: "none" }))
      .finally(() => wx.hideLoading());
  },
  onNext() {
    const session = this.data.session;
    const item = this.data.currentItem;
    const selected = session.answers[item.questionId] || [];
    if (selected.length === 0) return;
    if (!this.data.isLast) {
      this.setData({ currentIndex: this.data.currentIndex + 1 });
      this.syncDerived();
      return;
    }
    wx.showLoading({ title: "提交中…" });
    const answers = session.items.map((it) => ({ questionId: it.questionId, optionIds: session.answers[it.questionId] || [] }));
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

