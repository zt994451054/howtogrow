const { fetchAiSummary } = require("../../services/assessments");
const { clearDailySession, getDailySession, setDailySession } = require("../../services/daily-session");
const { getSystemMetrics } = require("../../utils/system");

function normalizeSuggestFlag(value) {
  const n = Number(value);
  return n === 0 ? 0 : 1;
}

Page({
  data: { statusBarHeight: 20, mode: "complete", doneText: "完成测试", reviewItems: [], canGenerateAiSummary: false, aiLoading: false, aiSummary: "" },
  onLoad(query) {
    const { statusBarHeight } = getSystemMetrics();
    const mode = query.mode || "complete";
    this.setData({ statusBarHeight, mode, doneText: mode === "history" ? "返回记录" : "完成测试" });
  },
  onBack() {
    this.onDone();
  },
  onShow() {
    const session = getDailySession();
    if (!session) {
      wx.showToast({ title: "无结果数据", icon: "none" });
      wx.switchTab({ url: "/pages/test/index" });
      return;
    }
    const existingAiSummary = typeof session.aiSummary === "string" ? session.aiSummary : "";
    const reviewItems = (session.items || [])
      .map((it) => {
        const optionIds = session.answers[it.questionId] || [];
        if (!optionIds.length) return null;
        const options = (it.options || [])
          .map((o) => ({
            optionId: o.optionId,
            content: o.content,
            sortNo: o.sortNo,
            suggestFlag: normalizeSuggestFlag(o.suggestFlag),
            improvementTip: o.improvementTip || "",
            selected: optionIds.includes(o.optionId),
          }))
          .sort((a, b) => (a.sortNo || 0) - (b.sortNo || 0) || (a.optionId || 0) - (b.optionId || 0));
        const selectedOptions = options.filter((o) => o.selected);
        const selectedText = selectedOptions
          .filter((o) => o.selected)
          .map((o) => o.content)
          .filter(Boolean)
          .join("、") || "（未选择）";
        return {
          questionId: it.questionId,
          question: String(it.content || "").replace(/\\n/g, ""),
          selectedText,
          selectedOptions,
        };
      })
      .filter(Boolean);
    this.setData({
      reviewItems,
      aiSummary: existingAiSummary,
      canGenerateAiSummary:
        typeof session.assessmentId === "number" && session.assessmentId > 0 && !existingAiSummary,
    });
  },
  onDone() {
    if (this.data.mode === "history") {
      wx.navigateBack();
      return;
    }
    clearDailySession();
    wx.switchTab({ url: "/pages/test/index" });
  },
  onGenerateAiSummary() {
    const session = getDailySession();
    if (!session || !session.assessmentId) return;
    this.setData({ aiLoading: true });
    fetchAiSummary(session.assessmentId)
      .then((content) => {
        session.aiSummary = content;
        setDailySession(session);
        this.setData({ aiSummary: content, canGenerateAiSummary: false });
      })
      .catch((err) => {
        const message = err && typeof err.message === "string" && err.message.trim() ? err.message.trim() : "生成失败";
        wx.showToast({ title: message, icon: "none" });
      })
      .finally(() => this.setData({ aiLoading: false }));
  },
});
