const { getSystemMetrics } = require("../../utils/system");
const { createChatSession, listChatSessions, sendChatMessage, streamChat } = require("../../services/chat");
const { getCachedMe, isProfileComplete } = require("../../services/auth");

Page({
  data: {
    statusBarHeight: 20,
    drawerOpen: false,
    sessions: [],
    sessionId: 0,
    messages: [{ id: "m1", role: "ai", text: "你好！我是你的育儿助手 Howtotalk。今天遇到了什么挑战吗？" }],
    input: "",
    inputTrim: "",
    typing: false,
    scrollIntoId: "bottom",
    showAuthModal: false,
    pendingSend: false,
  },
  onLoad() {
    const { statusBarHeight } = getSystemMetrics();
    this.setData({ statusBarHeight });
  },
  onShow() {
    const tab = this.getTabBar && this.getTabBar();
    tab && tab.setData && tab.setData({ selected: 2 });

    this.refreshSessions().then(() => {
      if (!this.data.sessionId) this.ensureSession();
    });
  },
  refreshSessions() {
    return listChatSessions(20)
      .then((sessions) => this.setData({ sessions }))
      .catch(() => this.setData({ sessions: [] }));
  },
  openDrawer() {
    this.setData({ drawerOpen: true });
  },
  closeDrawer() {
    this.setData({ drawerOpen: false });
  },
  ensureSession() {
    return createChatSession(null).then((sessionId) => this.setData({ sessionId })).catch(() => this.setData({ sessionId: 0 }));
  },
  startNewChat() {
    this.setData({ messages: [{ id: `ai-${Date.now()}`, role: "ai", text: "开启了新会话！请问有什么可以帮您？" }], input: "", inputTrim: "", typing: false, drawerOpen: false });
    return this.ensureSession().then(() => this.refreshSessions()).then(() => this.scrollToBottom());
  },
  selectSession(e) {
    const id = Number(e.currentTarget.dataset.id);
    this.setData({ sessionId: id, drawerOpen: false, messages: [{ id: `ai-${Date.now()}`, role: "ai", text: "已切换会话（占位）。请继续输入你的问题。" }] });
    this.scrollToBottom();
  },
  onInput(e) {
    const v = e.detail.value || "";
    this.setData({ input: v, inputTrim: String(v).trim() });
  },
  scrollToBottom() {
    this.setData({ scrollIntoId: `bottom-${Date.now()}` });
    this.setData({ scrollIntoId: "bottom" });
  },
  onSend() {
    const text = String(this.data.input).trim();
    if (!text || this.data.typing) return;

    const me = getCachedMe();
    if (!isProfileComplete(me)) {
      this.setData({ showAuthModal: true, pendingSend: true });
      return;
    }

    const ensure = this.data.sessionId ? Promise.resolve() : this.ensureSession();
    ensure.then(() => {
      if (!this.data.sessionId) {
        wx.showToast({ title: "无法创建会话", icon: "none" });
        return;
      }

      const userMsg = { id: `u-${Date.now()}`, role: "user", text };
      const aiMsgId = `a-${Date.now() + 1}`;
      const aiMsg = { id: aiMsgId, role: "ai", text: "" };
      this.setData({ messages: [...this.data.messages, userMsg, aiMsg], input: "", inputTrim: "", typing: true });
      this.scrollToBottom();

      sendChatMessage(this.data.sessionId, text)
        .then(() => {
          let finalText = "";
          const task = streamChat(this.data.sessionId, {
            onDelta: (delta) => {
              finalText += delta;
              const next = this.data.messages.map((m) => (m.id === aiMsgId ? { ...m, text: finalText } : m));
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
                const next = this.data.messages.map((m) => (m.id === aiMsgId ? { ...m, text: finalText } : m));
                this.setData({ messages: next });
              }
              this.setData({ typing: false });
            },
          });
          if (!task.supportsChunk) wx.showToast({ title: "当前为非流式模式", icon: "none" });
        })
        .catch(() => {
          this.setData({ typing: false });
          wx.showToast({ title: "发送失败", icon: "none" });
        });
    });
  },
  onAuthSuccess() {
    const shouldSend = Boolean(this.data.pendingSend);
    this.setData({ showAuthModal: false, pendingSend: false });
    if (shouldSend) {
      this.onSend();
    }
  },
});
