const { fetchAiSummary } = require("../../services/assessments");
const { clearDailySession, getDailySession, setDailySession } = require("../../services/daily-session");
const { getSystemMetrics } = require("../../utils/system");

function buildAdvice(optionIds) {
  const seed = (optionIds || []).reduce((a, b) => a + b, 0);
  const positive = seed % 2 === 0;
  return positive
    ? { sentiment: "positive", title: "共情并提供支架", advice: "你没有直接代劳，而是先接住情绪，再给出可执行的小步骤，让孩子保持掌控感。保持这种“连接在前、策略在后”的节奏，孩子会更愿意配合。" }
    : { sentiment: "negative", title: "避免权力之争", advice: "当我们急于纠正或命令时，很容易把问题变成对抗。你可以先描述事实与感受，再给出有限选择，把“对立”变成“合作”。" };
}

Page({
  data: { statusBarHeight: 20, mode: "complete", doneText: "完成测试", reviewItems: [], canGenerateAiSummary: false, aiLoading: false, aiSummary: "" },
  onLoad(query) {
    const { statusBarHeight } = getSystemMetrics();
    const mode = query.mode || "complete";
    this.setData({ statusBarHeight, mode, doneText: mode === "history" ? "返回记录" : "完成测试" });
  },
  onShow() {
    const session = getDailySession();
    if (!session) {
      wx.showToast({ title: "无结果数据", icon: "none" });
      wx.switchTab({ url: "/pages/test/index" });
      return;
    }
    const reviewItems = (session.items || [])
      .map((it) => {
        const optionIds = session.answers[it.questionId] || [];
        if (!optionIds.length) return null;
        const selectedOpt = (it.options || []).find((o) => o.optionId === optionIds[0]);
        const adv = buildAdvice(optionIds);
        return { questionId: it.questionId, question: String(it.content || "").replace(/\\n/g, ""), selected: selectedOpt ? selectedOpt.content : "（未选择）", sentiment: adv.sentiment, adviceTitle: adv.title, advice: adv.advice };
      })
      .filter(Boolean);
    this.setData({ reviewItems, canGenerateAiSummary: typeof session.assessmentId === "number" && session.assessmentId > 0, aiSummary: "" });
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
        this.setData({ aiSummary: content });
        setDailySession({ ...session });
      })
      .catch(() => wx.showToast({ title: "生成失败", icon: "none" }))
      .finally(() => this.setData({ aiLoading: false }));
  },
});

