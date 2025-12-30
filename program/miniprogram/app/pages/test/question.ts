import { replaceDailyQuestion, submitDailyAssessment } from "../../services/assessments";
import { clearDailySession, getDailySession, setDailySession, type DailySession } from "../../services/daily-session";
import { getSystemMetrics } from "../../utils/system";

Page({
  data: {
    statusBarHeight: 20,
    session: null as DailySession | null,
    currentIndex: 0,
    progressPercent: 0,
    currentItem: null as any,
    currentOptions: [] as Array<{ optionId: number; content: string; sortNo: number; _selected: boolean }>,
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
    const session = this.data.session!;
    const currentIndex = this.data.currentIndex;
    const item = session.items[currentIndex];
    const progressPercent = Math.round(((currentIndex + 1) / session.items.length) * 100);
    const selected = session.answers[item.questionId] || [];
    const isLast = currentIndex === session.items.length - 1;

    const currentOptions = (item.options || []).map((o: any) => ({
      ...o,
      _selected: selected.includes(o.optionId),
    }));

    this.setData({
      currentItem: item,
      progressPercent,
      currentOptions,
      canNext: selected.length > 0,
      isLast,
    });
  },
  onToggleOption(e: WechatMiniprogram.BaseEvent) {
    const optionId = Number((e.currentTarget as any).dataset.optionId);
    const session = this.data.session!;
    const item = this.data.currentItem;
    const current = session.answers[item.questionId] || [];

    let next: number[] = [];
    if (item.questionType === "SINGLE") {
      next = [optionId];
    } else {
      next = current.includes(optionId) ? current.filter((id: number) => id !== optionId) : [...current, optionId];
    }

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
  async onSwap() {
    const session = this.data.session!;
    const item = this.data.currentItem;

    wx.showLoading({ title: "换题中…" });
    try {
      const res = await replaceDailyQuestion(session.sessionId, { childId: session.childId, displayOrder: item.displayOrder });
      const idx = session.items.findIndex((i) => i.displayOrder === res.displayOrder);
      if (idx >= 0) {
        const oldQuestionId = session.items[idx].questionId;
        delete session.answers[oldQuestionId];
        session.items[idx] = res.newItem;
        setDailySession(session);
        this.setData({ session });
        this.syncDerived();
      }
    } catch {
      wx.showToast({ title: "换题失败", icon: "none" });
    } finally {
      wx.hideLoading();
    }
  },
  async onNext() {
    const session = this.data.session!;
    const item = this.data.currentItem;
    const selected = session.answers[item.questionId] || [];
    if (selected.length === 0) return;

    if (!this.data.isLast) {
      this.setData({ currentIndex: this.data.currentIndex + 1 });
      this.syncDerived();
      return;
    }

    wx.showLoading({ title: "提交中…" });
    try {
      const answers = session.items.map((it) => ({
        questionId: it.questionId,
        optionIds: session.answers[it.questionId] || [],
      }));
      const res = await submitDailyAssessment(session.sessionId, { childId: session.childId, answers });
      session.submitResult = res;
      session.assessmentId = res.assessmentId;
      setDailySession(session);
      wx.navigateTo({ url: "/pages/test/result" });
    } catch (error) {
      wx.showToast({ title: "提交失败", icon: "none" });
    } finally {
      wx.hideLoading();
    }
  },
});
