import { beginDailyAssessment } from "../../services/assessments";
import { setDailySession, type DailySession } from "../../services/daily-session";
import type { DailyAssessmentItemView } from "../../services/types";

function mockItems(): DailyAssessmentItemView[] {
  const option = (optionId: number, content: string, sortNo: number) => ({ optionId, content, sortNo });
  return [
    {
      displayOrder: 1,
      questionId: 101,
      content: "你的孩子知道不应该这样做，但是他们依然这样做…",
      questionType: "SINGLE",
      options: [
        option(1001, "\"Why are you telling me this, and not your boss?\"", 1),
        option(1002, "\"Forget about them! Just focus on the fun stuff at work instead\"", 2),
        option(1003, "\"It's not that bad — every job has those days, right?\"", 3),
        option(1004, "\"Oh well, that's bosses for you. That's just the way they are\"", 4),
      ],
    },
    {
      displayOrder: 2,
      questionId: 102,
      content: "当孩子因为动作慢、穿不好鞋子而大哭时…",
      questionType: "SINGLE",
      options: [
        option(1101, "“别哭了，我来帮你穿好就行了。”", 1),
        option(1102, "“哭也没用，自己想办法穿上。”", 2),
        option(1103, "“这双鞋确实有点难穿，我们要不要试试松开鞋带？”", 3),
      ],
    },
    {
      displayOrder: 3,
      questionId: 103,
      content: "当孩子拒绝分享玩具时，你通常会怎么说？（单选）",
      questionType: "SINGLE",
      options: [
        option(1201, "“要做个大方的孩子，快给弟弟玩一会儿。”", 1),
        option(1202, "“这是你的玩具，你有权决定什么时候分享。”", 2),
      ],
    },
    {
      displayOrder: 4,
      questionId: 104,
      content: "面对孩子的情绪崩溃，你的第一反应是？",
      questionType: "SINGLE",
      options: [option(1301, "“别哭了，给你买糖吃。”", 1), option(1302, "拥抱并说：“我知道你现在很难过。”", 2)],
    },
    {
      displayOrder: 5,
      questionId: 105,
      content: "到了睡觉时间孩子还在玩，你会怎么做？",
      questionType: "SINGLE",
      options: [option(1401, "直接关灯，强行抱上床。", 1), option(1402, "“你是想先刷牙还是先换睡衣？选好了我们就去睡觉。”", 2)],
    },
  ];
}

Page({
  data: {
    childId: 0,
    childName: "",
    starting: false,
  },
  onLoad(query: Record<string, string>) {
    const childId = Number(query.childId || 0);
    const childName = query.childName ? decodeURIComponent(query.childName) : "孩子";
    this.setData({ childId, childName });
  },
  onBack() {
    wx.navigateBack();
  },
  async onStart() {
    if (!this.data.childId) {
      wx.showToast({ title: "请选择孩子", icon: "none" });
      return;
    }

    this.setData({ starting: true });
    try {
      const res = await beginDailyAssessment(this.data.childId);
      const session: DailySession = {
        sessionId: res.sessionId,
        childId: this.data.childId,
        childName: this.data.childName,
        items: res.items,
        answers: {},
        submitResult: null,
        assessmentId: null,
      };
      setDailySession(session);
    } catch {
      const session: DailySession = {
        sessionId: `mock-${Date.now()}`,
        childId: this.data.childId,
        childName: this.data.childName,
        items: mockItems(),
        answers: {},
        submitResult: null,
        assessmentId: null,
      };
      setDailySession(session);
      wx.showToast({ title: "后端未连接，使用演示题目", icon: "none" });
    } finally {
      this.setData({ starting: false });
    }

    wx.navigateTo({ url: "/pages/test/question" });
  },
});

