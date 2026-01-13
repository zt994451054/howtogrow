const { ensureLoggedIn } = require("../../services/auth");
const { listDailyRecords, getDailyRecordDetail } = require("../../services/assessments");
const { setDailySession } = require("../../services/daily-session");

function toDateLabel(iso) {
  if (!iso) return "";
  return String(iso).slice(0, 10);
}

function toAnswerMap(answerViews) {
  const map = {};
  (answerViews || []).forEach((a) => {
    const qid = a && a.questionId ? String(a.questionId) : "";
    if (!qid) return;
    const optionIds = a.optionIds || [];
    map[qid] = optionIds;
  });
  return map;
}

Page({
  data: {
    loading: false,
    records: [],
  },
  onShow() {
    this.loadRecords();
  },
  onBack() {
    wx.navigateBack();
  },
  loadRecords() {
    this.setData({ loading: true });
    ensureLoggedIn()
      .then(() => listDailyRecords(50, 0))
      .then((rows) => {
        const records = (rows || []).map((r) => ({
          id: String(r.assessmentId),
          assessmentId: r.assessmentId,
          date: toDateLabel(r.submittedAt),
          childName: r.childName || "（未知）",
          aiSummary: r.aiSummary || "",
        }));
        this.setData({ records });
      })
      .catch((err) => {
        console.error(err);
        wx.showToast({ title: "加载失败", icon: "none" });
        this.setData({ records: [] });
      })
      .finally(() => this.setData({ loading: false }));
  },
  onView(e) {
    const id = String(e.currentTarget.dataset.id || "");
    if (!id) return;
    const assessmentId = Number(id);
    if (!assessmentId) return;

    wx.showLoading({ title: "加载中..." });
    ensureLoggedIn()
      .then(() => getDailyRecordDetail(assessmentId))
      .then((detail) => {
        const answers = toAnswerMap(detail.answers);
        setDailySession({
          sessionId: `history-${assessmentId}`,
          childId: detail.childId || 0,
          childName: detail.childName || "（未知）",
          items: detail.items || [],
          answers,
          submitResult: null,
          assessmentId: detail.assessmentId || assessmentId,
          aiSummary: detail.aiSummary || "",
        });
        wx.navigateTo({ url: "/pages/test/result?mode=history" });
      })
      .catch((err) => {
        console.error(err);
        wx.showToast({ title: "加载失败", icon: "none" });
      })
      .finally(() => wx.hideLoading());
  },
});
