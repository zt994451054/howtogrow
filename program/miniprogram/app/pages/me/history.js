const { setDailySession } = require("../../services/daily-session");

function mockItems() {
  const option = (optionId, content, sortNo) => ({ optionId, content, sortNo });
  return [
    { displayOrder: 1, questionId: 101, content: "当孩子因为动作慢、穿不好鞋子而大哭时...", questionType: "SINGLE", options: [option(1101, "“别哭了，我来帮你穿好就行了。”", 1), option(1102, "“哭也没用，自己想办法穿上。”", 2), option(1103, "“这双鞋确实有点难穿，我们要不要试试松开鞋带？”", 3)] },
    { displayOrder: 2, questionId: 102, content: "当孩子拒绝分享玩具时，你通常会怎么说？", questionType: "SINGLE", options: [option(1201, "“要做个大方的孩子，快给弟弟玩一会儿。”", 1), option(1202, "“这是你的玩具，你有权决定什么时候分享。”", 2)] },
    { displayOrder: 3, questionId: 103, content: "面对孩子的情绪崩溃，第一反应是？", questionType: "SINGLE", options: [option(1301, "“别哭了，给你买糖吃。”", 1), option(1302, "拥抱并说：“我知道你现在很难过。”", 2)] },
    { displayOrder: 4, questionId: 104, content: "孩子这也不吃那也不吃，吃饭时你通常会？", questionType: "SINGLE", options: [option(1401, "“必须把青菜吃掉，不然不许看电视。”", 1), option(1402, "“那尝尝胡萝卜怎么样？”", 2)] },
    { displayOrder: 5, questionId: 105, content: "到了睡觉时间孩子还在玩，你会怎么做？", questionType: "SINGLE", options: [option(1501, "直接关灯，强行抱上床。", 1), option(1502, "“你是想先刷牙还是先换睡衣？”", 2)] },
  ];
}

Page({
  data: {
    records: [
      { id: "r1", date: "2025-12-26", childName: "Lucas", answers: { 101: [1103], 102: [1202], 103: [1302] } },
      { id: "r2", date: "2025-12-25", childName: "Lucas", answers: { 101: [1101], 102: [1201] } },
    ],
  },
  onBack() {
    wx.navigateBack();
  },
  onView(e) {
    const id = String(e.currentTarget.dataset.id);
    const record = this.data.records.find((r) => r.id === id);
    if (!record) return;
    setDailySession({ sessionId: `history-${id}`, childId: 0, childName: record.childName, items: mockItems(), answers: record.answers, submitResult: null, assessmentId: null });
    wx.navigateTo({ url: "/pages/test/result?mode=history" });
  },
});

