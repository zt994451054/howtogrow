const { beginDailyAssessment } = require("../../services/assessments");
const { setDailySession } = require("../../services/daily-session");

Page({
  data: { childId: 0, childName: "", starting: false },
  onLoad(query) {
    this.setData({
      childId: Number(query.childId || 0),
      childName: query.childName ? decodeURIComponent(query.childName) : "孩子",
    });
  },
  onBack() {
    wx.navigateBack();
  },
  onStart() {
    if (!this.data.childId) {
      wx.showToast({ title: "请选择孩子", icon: "none" });
      return;
    }
    if (this.data.starting) return;
    this.setData({ starting: true });
    beginDailyAssessment(this.data.childId)
      .then((res) => {
        setDailySession({
          sessionId: res.sessionId,
          childId: this.data.childId,
          childName: this.data.childName,
          items: res.items,
          answers: {},
          submitResult: null,
          assessmentId: null,
        });
        wx.navigateTo({ url: "/pages/test/question" });
      })
      .catch(() => {})
      .finally(() => this.setData({ starting: false }));
  },
});
