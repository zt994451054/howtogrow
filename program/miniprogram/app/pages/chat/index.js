const { getSystemMetrics } = require("../../utils/system");
const { createChatSession, listChatSessions, listChatMessages, sendChatMessage, streamChat } = require("../../services/chat");
const { getCachedMe, isProfileComplete } = require("../../services/auth");
const towxml = require("../../towxml/index");

const DEFAULT_AI_GREETING = "你好！我是你的育儿助手 Howtotalk。今天遇到了什么挑战吗？";

function toAiNodes(markdown) {
  try {
    return towxml(String(markdown ?? ""), "markdown", { theme: "light" });
  } catch {
    return { theme: "light", child: [], _e: { child: [] } };
  }
}

const DEFAULT_AI_GREETING_NODES = toAiNodes(DEFAULT_AI_GREETING);
const EMPTY_AI_NODES = toAiNodes("");

function toUiRole(role) {
  if (role === "user") return "user";
  return "ai";
}

function toUiMessages(descMessages) {
  const items = Array.isArray(descMessages) ? descMessages : [];
  return items
    .slice()
    .reverse()
    .map((m) => ({
      id: `m-${m.messageId}`,
      role: toUiRole(m.role),
      text: m.content || "",
      nodes: toUiRole(m.role) === "ai" ? toAiNodes(m.content || "") : undefined,
      messageId: m.messageId,
      createdAt: m.createdAt,
    }));
}

Page({
  data: {
    statusBarHeight: 20,
    drawerOpen: false,
    sessions: [],
    sessionId: 0,
    messages: [{ id: "m1", role: "ai", text: DEFAULT_AI_GREETING, nodes: DEFAULT_AI_GREETING_NODES }],
    input: "",
    inputTrim: "",
    typing: false,
    scrollIntoId: "bottom",
    showAuthModal: false,
    pendingSend: false,
    tabbarHeight: 0,
    bottomPadding: 0,
    historyLoading: false,
    historyLoadingMore: false,
    historyHasMore: false,
    historyBeforeMessageId: null,
  },
  onLoad() {
    const { statusBarHeight } = getSystemMetrics();
    this.setData({ statusBarHeight });
  },
  onShow() {
    const tab = this.getTabBar && this.getTabBar();
    tab && tab.setData && tab.setData({ selected: 2 });

    if (typeof wx.nextTick === "function") wx.nextTick(() => this.syncBottomLayout());
    else this.syncBottomLayout();

    this.refreshSessions().then(() => {
      if (this.data.sessionId) this.loadSessionMessages();
    });
  },
  syncBottomLayout() {
    const tab = this.getTabBar && this.getTabBar();

    let inputHeight = 0;
    let tabbarHeight = 0;

    const commit = () => {
      const bottomPadding = Math.max(0, Math.ceil(tabbarHeight + inputHeight + 12));
      this.setData({ tabbarHeight: Math.max(0, Math.ceil(tabbarHeight)), bottomPadding });
    };

    wx.createSelectorQuery()
      .select(".input")
      .boundingClientRect((rect) => {
        inputHeight = rect && rect.height ? rect.height : 0;
        commit();
      })
      .exec();

    if (!tab) return;
    wx.createSelectorQuery()
      .in(tab)
      .select(".tabbar")
      .boundingClientRect((rect) => {
        tabbarHeight = rect && rect.height ? rect.height : 0;
        commit();
      })
      .exec();
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
    const existing = Number(this.data.sessionId);
    if (existing) return Promise.resolve(existing);

    return createChatSession(null)
      .then((sessionId) => {
        this.setData({ sessionId });
        return sessionId;
      })
      .catch((e) => {
        this.setData({ sessionId: 0 });
        throw e;
      });
  },
  loadSessionMessages(loadMore = false) {
    const sessionId = Number(this.data.sessionId);
    if (!sessionId) return Promise.resolve();
    if (loadMore && (this.data.historyLoading || this.data.historyLoadingMore || !this.data.historyHasMore)) return Promise.resolve();
    if (!loadMore && this.data.historyLoading) return Promise.resolve();

    const limit = 20;
    const beforeMessageId = loadMore ? this.data.historyBeforeMessageId : null;
    const setLoading = loadMore ? { historyLoadingMore: true } : { historyLoading: true };
    this.setData(setLoading);

    return listChatMessages(sessionId, limit, beforeMessageId)
      .then((descMessages) => {
        const uiMessages = toUiMessages(descMessages);
        const oldest = uiMessages.length ? uiMessages[0].messageId : null;
        const hasMore = Array.isArray(descMessages) && descMessages.length === limit;

        if (!loadMore) {
          if (!uiMessages.length) {
            this.setData({
              messages: [{ id: "m1", role: "ai", text: DEFAULT_AI_GREETING, nodes: DEFAULT_AI_GREETING_NODES }],
              historyHasMore: false,
              historyBeforeMessageId: null,
            });
          } else {
            this.setData({ messages: uiMessages, historyHasMore: hasMore, historyBeforeMessageId: oldest });
            this.scrollToBottom();
          }
          return;
        }

        const merged = [...uiMessages, ...this.data.messages];
        this.setData({ messages: merged, historyHasMore: hasMore, historyBeforeMessageId: oldest || this.data.historyBeforeMessageId });
      })
      .catch(() => {
        // ignore
      })
      .finally(() => {
        this.setData({ historyLoading: false, historyLoadingMore: false });
      });
  },
  startNewChat() {
    this.setData({
      sessionId: 0,
      messages: [
        {
          id: `ai-${Date.now()}`,
          role: "ai",
          text: DEFAULT_AI_GREETING,
          nodes: DEFAULT_AI_GREETING_NODES,
        },
      ],
      input: "",
      inputTrim: "",
      typing: false,
      drawerOpen: false,
      historyLoading: false,
      historyLoadingMore: false,
      historyHasMore: false,
      historyBeforeMessageId: null,
    });
    return this.refreshSessions().then(() => this.scrollToBottom());
  },
  selectSession(e) {
    const id = Number(e.currentTarget.dataset.id);
    this.setData({
      sessionId: id,
      drawerOpen: false,
      historyHasMore: false,
      historyBeforeMessageId: null,
    });
    this.loadSessionMessages();
  },
  onMessagesScrollToUpper() {
    this.loadSessionMessages(true);
  },
  onInput(e) {
    const v = e.detail.value || "";
    this.setData({ input: v, inputTrim: String(v).trim() });
  },
  scrollToBottom() {
    this.setData({ scrollIntoId: `bottom-${Date.now()}` });
    this.setData({ scrollIntoId: "bottom" });
  },
  scheduleAiRender(aiMsgId, text) {
    this._aiRenderTarget = { aiMsgId, text: String(text ?? "") };
    if (this._aiRenderTimer) return;
    this._aiRenderTimer = setTimeout(() => {
      this._aiRenderTimer = null;
      const target = this._aiRenderTarget;
      this._aiRenderTarget = null;
      if (!target) return;

      const nodes = toAiNodes(target.text);
      const next = this.data.messages.map((m) => (m.id === target.aiMsgId ? { ...m, text: target.text, nodes } : m));
      this.setData({ messages: next });
      this.scrollToBottom();
    }, 50);
  },
  flushAiRender() {
    if (this._aiRenderTimer) {
      clearTimeout(this._aiRenderTimer);
      this._aiRenderTimer = null;
    }
    const target = this._aiRenderTarget;
    this._aiRenderTarget = null;
    if (!target) return;
    const nodes = toAiNodes(target.text);
    const next = this.data.messages.map((m) => (m.id === target.aiMsgId ? { ...m, text: target.text, nodes } : m));
    this.setData({ messages: next });
    this.scrollToBottom();
  },
  onSend() {
    const text = String(this.data.input).trim();
    if (!text || this.data.typing) return;

    const me = getCachedMe();
    if (!isProfileComplete(me)) {
      this.setData({ showAuthModal: true, pendingSend: true });
      return;
    }

    const ensure = this.data.sessionId ? Promise.resolve(this.data.sessionId) : this.ensureSession();
    ensure
      .then(() => {
        if (!this.data.sessionId) {
          wx.showToast({ title: "无法创建会话", icon: "none", duration: 3000, mask: true });
          return;
        }

        const userMsg = { id: `u-${Date.now()}`, role: "user", text };
        const aiMsgId = `a-${Date.now() + 1}`;
        const aiMsg = { id: aiMsgId, role: "ai", text: "", nodes: EMPTY_AI_NODES };
        this.setData({ messages: [...this.data.messages, userMsg, aiMsg], input: "", inputTrim: "", typing: true });
        this.scrollToBottom();

        sendChatMessage(this.data.sessionId, text)
          .then(() => {
            let finalText = "";
            const task = streamChat(this.data.sessionId, {
              onDelta: (delta) => {
                finalText += delta;
                this.scheduleAiRender(aiMsgId, finalText);
              },
              onDone: () => {
                this.flushAiRender();
                this.setData({ typing: false });
                this.refreshSessions();
              },
              onError: () => {
                if (!finalText) {
                  finalText = "（当前环境不支持流式，或网络异常。稍后再试）";
                  this.scheduleAiRender(aiMsgId, finalText);
                }
                this.flushAiRender();
                this.setData({ typing: false });
              },
            });
            if (!task.supportsChunk) wx.showToast({ title: "当前为非流式模式", icon: "none", duration: 3000 });
          })
          .catch(() => {
            this.setData({ typing: false });
            wx.showToast({ title: "发送失败", icon: "none", duration: 3000, mask: true });
          });
      })
      .catch(() => {
        wx.showToast({ title: "无法创建会话", icon: "none", duration: 3000, mask: true });
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
