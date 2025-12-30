import { getSystemMetrics } from "../../utils/system";
import { createChatSession, listChatSessions, sendChatMessage, streamChat } from "../../services/chat";

type Msg = { id: string; role: "user" | "ai"; text: string };

Page({
  data: {
    statusBarHeight: 20,
    drawerOpen: false,
    sessions: [] as any[],
    sessionId: 0,
    messages: [{ id: "m1", role: "ai", text: "你好！我是你的育儿助手 Howtotalk。今天遇到了什么挑战吗？" }] as Msg[],
    input: "",
    inputTrim: "",
    typing: false,
    scrollIntoId: "bottom",
  },
  onLoad() {
    const { statusBarHeight } = getSystemMetrics();
    this.setData({ statusBarHeight });
  },
  async onShow() {
    const tab = (this as any).getTabBar?.();
    tab?.setData?.({ selected: 2 });

    await this.refreshSessions();
    if (!this.data.sessionId) {
      await this.ensureSession();
    }
  },
  async refreshSessions() {
    try {
      const sessions = await listChatSessions(20);
      this.setData({ sessions });
    } catch {
      this.setData({ sessions: [] });
    }
  },
  openDrawer() {
    this.setData({ drawerOpen: true });
  },
  closeDrawer() {
    this.setData({ drawerOpen: false });
  },
  async ensureSession() {
    try {
      const sessionId = await createChatSession(null);
      this.setData({ sessionId });
    } catch {
      this.setData({ sessionId: 0 });
    }
  },
  async startNewChat() {
    this.setData({
      messages: [{ id: `ai-${Date.now()}`, role: "ai", text: "开启了新会话！请问有什么可以帮您？" }],
      input: "",
      inputTrim: "",
      typing: false,
      drawerOpen: false,
    });
    await this.ensureSession();
    await this.refreshSessions();
    this.scrollToBottom();
  },
  async selectSession(e: WechatMiniprogram.BaseEvent) {
    const id = Number((e.currentTarget as any).dataset.id);
    this.setData({ sessionId: id, drawerOpen: false });
    this.setData({
      messages: [{ id: `ai-${Date.now()}`, role: "ai", text: "已切换会话（占位）。请继续输入你的问题。" }],
    });
    this.scrollToBottom();
  },
  onInput(e: WechatMiniprogram.Input) {
    const v = e.detail.value || "";
    this.setData({ input: v, inputTrim: String(v).trim() });
  },
  scrollToBottom() {
    this.setData({ scrollIntoId: `bottom-${Date.now()}` });
    this.setData({ scrollIntoId: "bottom" });
  },
  async onSend() {
    const text = String(this.data.input).trim();
    if (!text || this.data.typing) return;

    if (!this.data.sessionId) {
      await this.ensureSession();
      if (!this.data.sessionId) {
        wx.showToast({ title: "无法创建会话", icon: "none" });
        return;
      }
    }

    const userMsg: Msg = { id: `u-${Date.now()}`, role: "user", text };
    const aiMsgId = `a-${Date.now() + 1}`;
    const aiMsg: Msg = { id: aiMsgId, role: "ai", text: "" };

    this.setData({
      messages: [...this.data.messages, userMsg, aiMsg],
      input: "",
      inputTrim: "",
      typing: true,
    });
    this.scrollToBottom();

    try {
      await sendChatMessage(this.data.sessionId, text);
    } catch {
      this.setData({ typing: false });
      wx.showToast({ title: "发送失败", icon: "none" });
      return;
    }

    let finalText = "";
    const task = streamChat(this.data.sessionId, {
      onDelta: (delta) => {
        finalText += delta;
        const next = this.data.messages.map((m: Msg) => (m.id === aiMsgId ? { ...m, text: finalText } : m));
        this.setData({ messages: next });
        this.scrollToBottom();
      },
      onDone: () => {
        this.setData({ typing: false });
        this.refreshSessions();
      },
      onError: () => {
        if (!finalText) {
          finalText = "（当前环境不支持流式，或网络异常。稍后再试）";
          const next = this.data.messages.map((m: Msg) => (m.id === aiMsgId ? { ...m, text: finalText } : m));
          this.setData({ messages: next });
        }
        this.setData({ typing: false });
      },
    });

    if (!task.supportsChunk) {
      // 即使不是分块回调，也让用户知道仍可能是“非流式一次性返回”
      wx.showToast({ title: "当前为非流式模式", icon: "none" });
    }
  },
});
